package com.idwell.cloudframe.ui

import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.view.*
import android.widget.SeekBar
import android.widget.VideoView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.entity.Video
import com.idwell.cloudframe.util.MyUtils
import com.idwell.cloudframe.util.TimeUtil
import kotlinx.android.synthetic.main.activity_video_view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File

class VideoViewActivity : BaseActivity() {

    private var mPaused = false
    private var mTabPosition: Int = 0
    private var mVideo = Video()
    private var mVideos = mutableListOf<Video>()
    private var mMediaPlayer: MediaPlayer? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mState = STATE_IDLE
    private var mCounter = 0
    private var mUserId = 0

    private var mCurrentPosition = 0

    private val mSurfaceHolderCallback = SurfaceHolderCallback()
    private lateinit var mAudioManager: AudioManager
    private val mOnAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {}

    private val mHandler = Handler()
    private val mRunnable = object : Runnable {
        override fun run() {
            cl_bar_top_video_view.visibility = View.GONE
            cl_play_video_view.visibility = View.GONE
            cl_progress_video_view.visibility = View.GONE
        }
    }

    private val mProgressRunnable = object : Runnable {
        override fun run() {
            tv_current_time_video_view.text = TimeUtil.formatTime(mMediaPlayer?.currentPosition?.toLong() ?: 0)
            sb_progress_video_view.progress = mMediaPlayer?.currentPosition ?: 0
            mHandler.removeCallbacks(this)
            mHandler.postDelayed(this, 200)
        }
    }

    override fun initLayout(): Int {
        showTopBar = false
        return R.layout.activity_video_view
    }

    override fun initData() {
        tv_title_video_view.text = mVideo.displayName

        mTabPosition = intent.getIntExtra("tabPosition", 0)
        mVideo = intent.getParcelableExtra("video") ?: Video()
        mUserId = intent.getIntExtra("userId", 0)
        LogUtils.dTag(TAG, "$mTabPosition, ${mVideo.data}, $mUserId")

        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        launch(Dispatchers.IO) {
            val videos = queryVideo()
            launch(Dispatchers.Main) {
                mVideos.clear()
                mVideos.addAll(videos)
            }
        }

        mMediaPlayer = MediaPlayer()
        mSurfaceHolder = vsv_video_view.holder
        mSurfaceHolder?.addCallback(mSurfaceHolderCallback)
    }

    override fun initListener() {
        iv_back_video_view.setOnClickListener(this)
        iv_prev_video_view.setOnClickListener(this)
        iv_play_video_view.setOnClickListener(this)
        iv_next_video_view.setOnClickListener(this)

        mMediaPlayer?.setOnPreparedListener {
            //LogUtils.dTag("lcs", "${it.videoWidth}, ${it.videoHeight}")
            mState = STATE_PLAYING
            mCounter = 0
            vsv_video_view.adjustSize(it.videoWidth, it.videoHeight)
            it.start()
            if (mCurrentPosition > 0) {
                it.seekTo(mCurrentPosition)
                mCurrentPosition = 0
            }
            iv_play_video_view.setImageResource(R.drawable.jz_click_pause_selector)
            val duration = it.duration
            tv_current_time_video_view.text = "00:00"
            tv_total_time_video_view.text = TimeUtil.formatTime(duration.toLong())
            sb_progress_video_view.max = duration
            sb_progress_video_view.progress = 0
            mHandler.removeCallbacks(mProgressRunnable)
            mHandler.post(mProgressRunnable)
        }

        mMediaPlayer?.setOnCompletionListener {
            //LogUtils.dTag("lcs", "OnCompletionListener")
            mState = STATE_PLAYBACK_COMPLETED
            skipToNext()
        }

        mMediaPlayer?.setOnErrorListener { mp, what, extra ->
            //LogUtils.dTag("lcs", "$what, $extra")
            handleException()
            return@setOnErrorListener true
        }

        sb_progress_video_view.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mHandler.removeCallbacks(mRunnable)
                    mHandler.postDelayed(mRunnable, 3000)
                    mMediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }

    override fun onClick(v: View?) {
        when (v) {
            iv_back_video_view -> {
                EventBus.getDefault()
                        .post(MessageEvent(MessageEvent.VIDEOVIEW_CUR_VIDEO_DATA, mVideo.data))
                finish()
            }
            iv_prev_video_view -> {
                mHandler.removeCallbacks(mRunnable)
                mHandler.postDelayed(mRunnable, 3000)
                mHandler.removeCallbacks(mProgressRunnable)
                skipToPrevious()
            }
            iv_play_video_view -> {
                mHandler.removeCallbacks(mRunnable)
                mHandler.postDelayed(mRunnable, 3000)
                if (mState == STATE_PLAYING) {
                    mState = STATE_PAUSED
                    mHandler.removeCallbacks(mProgressRunnable)
                    mMediaPlayer?.pause()
                    iv_play_video_view.setImageResource(R.drawable.jz_click_play_selector)
                } else if (mState == STATE_PAUSED) {
                    mState = STATE_PLAYING
                    mHandler.removeCallbacks(mProgressRunnable)
                    mHandler.post(mProgressRunnable)
                    mMediaPlayer?.start()
                    iv_play_video_view.setImageResource(R.drawable.jz_click_pause_selector)
                }
            }
            iv_next_video_view -> {
                mHandler.removeCallbacks(mRunnable)
                mHandler.postDelayed(mRunnable, 3000)
                mHandler.removeCallbacks(mProgressRunnable)
                skipToNext()
            }
        }
    }

    override fun initMessageEvent(event: MessageEvent) {
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (cl_bar_top_video_view.visibility == View.GONE) {
                    cl_bar_top_video_view.visibility = View.VISIBLE
                    cl_play_video_view.visibility = View.VISIBLE
                    cl_progress_video_view.visibility = View.VISIBLE
                    mHandler.removeCallbacks(mRunnable)
                    mHandler.postDelayed(mRunnable, 3000)
                    if (mState == STATE_PLAYING) {
                        mHandler.removeCallbacks(mProgressRunnable)
                        mHandler.post(mProgressRunnable)
                    }
                } else {
                    cl_bar_top_video_view.visibility = View.GONE
                    cl_play_video_view.visibility = View.GONE
                    cl_progress_video_view.visibility = View.GONE
                    mHandler.removeCallbacks(mRunnable)
                    mHandler.removeCallbacks(mProgressRunnable)
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun skipToNext() {
        var position = mVideos.indexOf(mVideo)
        //LogUtils.dTag("lcs", "position = $position, mVideos.size = ${mVideos.size}")
        if (++position > mVideos.size - 1) {
            position = 0
            if (mVideos.size != countVideo()) {
                refreshVideo(position)
                return
            }
        }
        mVideo = mVideos[position]
        tv_title_video_view.text = mVideo.displayName
        try {
            if (File(mVideo.data).exists()) {
                mMediaPlayer?.reset()
                mMediaPlayer?.setDataSource(mVideo.data)
                mMediaPlayer?.prepareAsync()
            } else {
                fileNotExists()
            }
        } catch (e: Exception) {
            handleException()
        }
    }

    private fun skipToPrevious() {
        var position = mVideos.indexOf(mVideo)
        if (--position < 0) {
            position = mVideos.size - 1
        }
        mVideo = mVideos[position]
        tv_title_video_view.text = mVideo.displayName
        try {
            if (File(mVideo.data).exists()) {
                mMediaPlayer?.reset()
                mMediaPlayer?.setDataSource(mVideo.data)
                mMediaPlayer?.prepareAsync()
            } else {
                fileNotExists()
            }
        } catch (e: Exception) {
            handleException()
        }
    }

    private fun fileNotExists() {
        var position = mVideos.indexOf(mVideo)
        if (position == -1) {
            position = 0
        }
        launch(Dispatchers.IO) {
            val videos = queryVideo()
            launch(Dispatchers.Main) {
                mVideos.clear()
                mVideos.addAll(videos)
                if (mVideos.isEmpty()) {
                    finish()
                } else {
                    if (position > mVideos.size - 1) {
                        position = 0
                    }
                    mVideo = mVideos[position]
                    tv_title_video_view.text = mVideo.displayName
                    try {
                        mMediaPlayer?.reset()
                        mMediaPlayer?.setDataSource(mVideo.data)
                        mMediaPlayer?.prepareAsync()
                    } catch (e: Exception) {
                        handleException()
                    }
                }
            }
        }
    }

    private fun handleException() {
        ToastUtils.showShort(R.string.video_loading_faild)
        if (++mCounter > mVideos.size - 1) {
            finish()
        } else {
            //LogUtils.dTag("lcs", "handleException")
            skipToNext()
        }
    }

    private fun refreshVideo(position: Int) {
        launch(Dispatchers.IO) {
            val videos = queryVideo()
            launch(Dispatchers.Main) {
                mVideos.clear()
                mVideos.addAll(videos)
                if (mVideos.isEmpty()) {
                    finish()
                }
                mVideo = mVideos[position]
                tv_title_video_view.text = mVideo.displayName
                try {
                    if (File(mVideo.data).exists()) {
                        mMediaPlayer?.reset()
                        mMediaPlayer?.setDataSource(mVideo.data)
                        mMediaPlayer?.prepareAsync()
                    } else {
                        fileNotExists()
                    }
                } catch (e: Exception) {
                    handleException()
                }
            }
        }
    }

    private fun queryVideo(): MutableList<Video> {
        val videos = mutableListOf<Video>()

        var selection =""
        if(Device.isConfigUSBStorage && !Device.isConfigSdcard){
            selection = when (mTabPosition) {
                //屏蔽.vob格式
                0 -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                1 -> "${MediaStore.Video.VideoColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                2 -> "${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getUsbDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                else -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DESCRIPTION} like '%\"sender_id\":$mUserId%'"
            }
        }else if(!Device.isConfigUSBStorage && Device.isConfigSdcard){
            selection = when (mTabPosition) {
                //屏蔽.vob格式
                0 -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                1 -> "${MediaStore.Video.VideoColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                2 -> "${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getSdDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                else -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DESCRIPTION} like '%\"sender_id\":$mUserId%'"
            }
        }else if(!Device.isConfigUSBStorage && !Device.isConfigSdcard){
            selection = when (mTabPosition) {
                //屏蔽.vob格式
                0 -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                1 -> "${MediaStore.Video.VideoColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                else -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DESCRIPTION} like '%\"sender_id\":$mUserId%'"
            }
        }  else {
            selection = when (mTabPosition){
                //屏蔽.vob格式
                0 -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                1 -> "${MediaStore.Video.VideoColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                2 -> "${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getSdDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                3 -> "${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getUsbDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                else -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DESCRIPTION} like '%\"sender_id\":$mUserId%'"
            }
        }

        val cursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.VideoColumns._ID, MediaStore.Video.VideoColumns.DATA, MediaStore.Video.VideoColumns.SIZE, MediaStore.Video.VideoColumns.DISPLAY_NAME, MediaStore.Video.VideoColumns.TITLE, MediaStore.Video.VideoColumns.DURATION, MediaStore.Video.VideoColumns.RESOLUTION, MediaStore.Video.VideoColumns.DESCRIPTION, MediaStore.Video.VideoColumns.IS_PRIVATE), selection, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER)
        while (cursor?.moveToNext() == true) {
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns._ID))
            val data = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA))
            val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns.SIZE))
            val displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME))
            val title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.TITLE))
            val duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION))
            val resolution = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.RESOLUTION)) ?: ""
            val description = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DESCRIPTION)) ?: ""
            val isprivate = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.VideoColumns.IS_PRIVATE))
            val video = Video(id, data, size, displayName, title, duration, resolution, description, isprivate)
            LogUtils.dTag(TAG, video.data)
            videos.add(video)
        }
        cursor?.close()
        return videos
    }

    private fun queryVideo(mId: Long) {
        val selection = when (mTabPosition) {
            //屏蔽.vob格式
            0 -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns._ID} = $mId and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
            1 -> "${MediaStore.Video.VideoColumns._ID} = $mId and ${MediaStore.Video.VideoColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
            2 -> "${MediaStore.Video.VideoColumns._ID} = $mId and ${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getSdDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
            3 -> "${MediaStore.Video.VideoColumns._ID} = $mId and ${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getUsbDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
            else -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns._ID} = $mId and ${MediaStore.Video.VideoColumns.DESCRIPTION} like '%\"sender_id\":$mUserId%'"
        }
        val cursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.VideoColumns._ID, MediaStore.Video.VideoColumns.DATA, MediaStore.Video.VideoColumns.SIZE, MediaStore.Video.VideoColumns.DISPLAY_NAME, MediaStore.Video.VideoColumns.TITLE, MediaStore.Video.VideoColumns.DURATION, MediaStore.Video.VideoColumns.RESOLUTION, MediaStore.Video.VideoColumns.DESCRIPTION, MediaStore.Video.VideoColumns.IS_PRIVATE), selection, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER)
        while (cursor?.moveToNext() == true) {
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns._ID))
            val data = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA))
            val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns.SIZE))
            val displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME)) ?: ""
            val title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.TITLE))
            val duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION))
            val resolution = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.RESOLUTION)) ?: ""
            val description = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DESCRIPTION)) ?: ""
            val isprivate = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.VideoColumns.IS_PRIVATE))
            val video = Video(id, data, size, displayName, title, duration, resolution, description, isprivate)
            if (mVideos.contains(video)) {
                val index = mVideos.indexOf(video)
                mVideos[index] = video
            } else {
                mVideos.add(video)
                LogUtils.dTag(TAG, mVideos.size)
            }
        }
        cursor?.close()
    }

    private fun countVideo(): Int {
        val selection = when (mTabPosition) {
            //屏蔽.vob格式
            0 -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
            1 -> "${MediaStore.Video.VideoColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
            2 -> "${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getSdDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
            3 -> "${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getUsbDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
            else -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DESCRIPTION} like '%\"sender_id\":$mUserId%'"
        }
        val cursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf("count(*)"), selection, null, null)
        cursor?.moveToFirst()
        val count = cursor?.getInt(0) ?: 0
        cursor?.close()
        return count
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        //LogUtils.dTag("lcs", "onConfigurationChanged")
        vsv_video_view.adjustSize(mMediaPlayer?.videoWidth ?: 0, mMediaPlayer?.videoHeight ?: 0)
    }

    override fun onBackPressed() {
        EventBus.getDefault().post(MessageEvent(MessageEvent.VIDEOVIEW_CUR_VIDEO_DATA, mVideo.data))
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        //LogUtils.dTag("lcs", "onResume")
        vsv_video_view.visibility = View.VISIBLE
        mPaused = false
    }

    override fun onPause() {
        super.onPause()
        //LogUtils.dTag("lcs", "onPause")
        mPaused = true
        vsv_video_view.visibility = View.GONE
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaPlayer?.reset()
        mMediaPlayer?.release()
        mMediaPlayer = null
        mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener)
    }

    companion object {
        private val TAG = VideoViewActivity::class.java.simpleName
        private const val STATE_ERROR = -1
        private const val STATE_IDLE = 0
        private const val STATE_PREPARING = 1
        private const val STATE_PREPARED = 2
        private const val STATE_PLAYING = 3
        private const val STATE_PAUSED = 4
        private const val STATE_PLAYBACK_COMPLETED = 5
    }

    inner class SurfaceHolderCallback: SurfaceHolder.Callback {

        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            //LogUtils.dTag("lcs", "surfaceDestroyed: $mState")
            mCurrentPosition = mMediaPlayer?.currentPosition ?: 0
            mMediaPlayer?.reset()
            mState = STATE_IDLE
        }

        override fun surfaceCreated(holder: SurfaceHolder?) {
            //LogUtils.dTag("lcs", "surfaceCreated: $mState")
            try {
                //LogUtils.dTag("lcs", mVideo.data)
                mMediaPlayer?.reset()
                mMediaPlayer?.setDataSource(mVideo.data)
                mMediaPlayer?.setDisplay(holder)
                mMediaPlayer?.prepareAsync()
            } catch (e: Exception) {
                handleException()
            }
        }
    }
}