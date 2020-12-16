package com.joyhong.test.video

import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.ToastUtils
import com.joyhong.test.BaseTestActivity
import com.joyhong.test.R
import com.joyhong.test.TestMainActivity
import com.joyhong.test.TestResultEnum
import com.joyhong.test.util.SysUtils
import com.joyhong.test.util.TestConstant
import kotlinx.android.synthetic.main.activity_video_test_view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class VideoViewTestActivity : BaseTestActivity() {

    private var mPaused = false
    private var mTabPosition: Int = 0
    private var mVideo = Video()
    private var mVideos = mutableListOf<Video>()
    private var mMediaPlayer: MediaPlayer? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mState = STATE_IDLE
    private var mCounter = 0
    private var mUserId = 0

    private val mSurfaceHolderCallback = object : SurfaceHolder.Callback {

        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {

        }

        override fun surfaceCreated(holder: SurfaceHolder?) {
            LogUtils.dTag(TAG, "surfaceCreated: $holder")
            mMediaPlayer?.setDisplay(holder)
            if (mState == STATE_IDLE) {
                try {
                    LogUtils.dTag(TAG, mVideo.data)
                    mMediaPlayer?.setDataSource(mVideo.data)
                    mMediaPlayer?.prepareAsync()
                } catch (e: Exception) {
                    handleException()
                }
            }
        }
    }

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
            tv_current_time_video_view.text =
                TimeTestUtil.formatTime(mMediaPlayer?.currentPosition?.toLong() ?: 0)
            sb_progress_video_view.progress = mMediaPlayer?.currentPosition ?: 0
            mHandler.removeCallbacks(this)
            mHandler.postDelayed(this, 200)
        }
    }

    override fun initLayout(): Int {
        if(TestConstant.isConfigTestMode){
            //避免进入页面EdiText自动弹出软键盘
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
        return R.layout.activity_video_test_view
    }
    private fun initTestData() {
        val files = getExternalFilesDir(null)!!.listFiles()
        for (f in files) {
            if (f.absolutePath.contains(".mp4") || f.absolutePath.contains(".avi")) {
                mVideo.data = f.absolutePath
                mVideo.displayName = f.name
            }
        }
    }
    override fun initData() {
        tv_title_video_view.text = mVideo.displayName
        if(TestConstant.isConfigTestMode){
            initTestData()
        }else{
            mTabPosition = intent.getIntExtra("tabPosition", 0)
            mUserId = intent.getIntExtra("userId", 0)
            mVideo = intent.getParcelableExtra("video")
        }
        LogUtils.dTag(TAG, "$mTabPosition, ${mVideo.data}, $mUserId")

        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mAudioManager.requestAudioFocus(
            mOnAudioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )

        GlobalScope.launch {
            val videos = queryVideo()
            launch(Dispatchers.Main) {
                mVideos.clear()
                mVideos.addAll(videos)
            }
        }

        mMediaPlayer = MediaPlayer()
        mSurfaceHolder = vsv_video_view.holder
        mSurfaceHolder?.addCallback(mSurfaceHolderCallback)

        if (TestConstant.isConfigTestMode) {
            conut_down_time.visibility = View.VISIBLE
            val label = "还剩下 "
            countdown_tv.setNormalText("")
                .setBeforeIndex(label.length)
                .setCountDownClickable(false)
                .setIsShowComplete(true)
                .setShowFormatTime(true)
                .setOnCountDownTickListener(object : CountDownTextView.OnCountDownTickListener {
                    override fun onTick(
                        untilFinished: Long,
                        showTime: String,
                        tv: CountDownTextView
                    ) {
                        tv.setText(
                            SysUtils.getMixedText(
                                label + showTime,
                                tv.timeIndexes,
                                true
                            )
                        )
                    }
                })
                .setOnCountDownFinishListener(object : CountDownTextView.OnCountDownFinishListener {
                    override fun onFinish() {
                        countdown_tv.setText("倒计时结束")
                        val testEntity =
                            TestMainActivity.testResult["${TestConstant.PACKAGE_NAME}$localClassName"]
                        testEntity!!.testResultEnum = TestResultEnum.PASS
                        SPUtils.getInstance().put("${TestConstant.PACKAGE_NAME}$localClassName",1)
                        if(TestConstant.isConfigTestMode){
                            finish()
                            return
                        }

                    }
                })
        }else{
            conut_down_time.visibility = View.GONE
        }

    }

    override fun initListener() {
        iv_back_video_view.setOnClickListener(this)
        iv_prev_video_view.setOnClickListener(this)
        iv_play_video_view.setOnClickListener(this)
        iv_next_video_view.setOnClickListener(this)
        countdown_go.setOnClickListener(this)
        countdown_ms.setOnClickListener(this)

        mMediaPlayer?.setOnPreparedListener {
            LogUtils.dTag(TAG, "${it.videoWidth}, ${it.videoHeight}")
            mCounter = 0
            vsv_video_view.adjustSize(it.videoWidth, it.videoHeight)
            it.start()
            mState = STATE_PLAYING
            iv_play_video_view.setImageResource(R.drawable.jz_click_pause_selector)
            val duration = it.duration
            tv_current_time_video_view.text = "00:00"
            tv_total_time_video_view.text = TimeTestUtil.formatTime(duration.toLong())
            sb_progress_video_view.max = duration
            sb_progress_video_view.progress = 0
            mHandler.removeCallbacks(mProgressRunnable)
            mHandler.post(mProgressRunnable)
        }

        mMediaPlayer?.setOnCompletionListener {
            LogUtils.dTag(TAG, "OnCompletionListener")
            skipToNext()
        }

        mMediaPlayer?.setOnErrorListener { mp, what, extra ->
            LogUtils.dTag(TAG, "$what, $extra")
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
        countdown_go.performClick()
    }

    override fun onClick(v: View?) {
        when (v) {
            iv_back_video_view -> {
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
                    mMediaPlayer?.pause()
                    iv_play_video_view.setImageResource(R.drawable.jz_click_play_selector)
                    mState = STATE_PAUSED
                    mHandler.removeCallbacks(mProgressRunnable)
                } else if (mState == STATE_PAUSED) {
                    mMediaPlayer?.start()
                    iv_play_video_view.setImageResource(R.drawable.jz_click_pause_selector)
                    mState = STATE_PLAYING
                    mHandler.removeCallbacks(mProgressRunnable)
                    mHandler.post(mProgressRunnable)
                }
            }
            iv_next_video_view -> {
                mHandler.removeCallbacks(mRunnable)
                mHandler.postDelayed(mRunnable, 3000)
                mHandler.removeCallbacks(mProgressRunnable)
                skipToNext()
            }
            countdown_go -> {
                val countDowmNumber: Long
                if(isFirstIn){
                    countDowmNumber = 2
                    isFirstIn = false
                }else{
                    countDowmNumber = SysUtils.parseLong(SysUtils.getSafeString(countdown_ms!!.text.toString()))
                }
                if (countDowmNumber >= 2) {
                    countdown_tv!!.startCountDown(countDowmNumber*3600)
                }else{
                    ToastUtils.showLong("测试时间至少2小时")
                }
            }
            countdown_ms ->{
//                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                countdown_ms.setFocusable(true)
                countdown_ms.setFocusableInTouchMode(true)
                countdown_ms.requestFocus()
                val inputManager =
                    countdown_ms.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.showSoftInput(countdown_ms, 0)

            }
        }
    }
    var isFirstIn:Boolean = true

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if(TestConstant.isConfigTestMode){

                    val imm =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(countdown_ms.getWindowToken(), 0)
                }
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
        if(TestConstant.isConfigTestMode){
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
           return
        }
        var position = mVideos.indexOf(mVideo)
        LogUtils.dTag(TAG, "position = $position, mVideos.size = ${mVideos.size}")
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
        if(TestConstant.isConfigTestMode){
            return
        }
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
        GlobalScope.launch {
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
                        if (TestConstant.isConfigTestMode) {
                            mMediaPlayer?.isLooping = true
                        }
                    } catch (e: Exception) {
                        handleException()
                    }
                }
            }
        }
    }

    private fun handleException() {
        ToastUtils.showShort(R.string.video_loading_faild)
        val testEntity =
            TestMainActivity.testResult["${TestConstant.PACKAGE_NAME}$localClassName"]
        testEntity!!.testResultEnum = TestResultEnum.FAIL
        SPUtils.getInstance().put(testEntity.getTag(),2)
        if(TestConstant.isConfigTestMode){
            finish()
            return
        }
        if (++mCounter > mVideos.size - 1) {
            finish()
        } else {
            LogUtils.dTag(TAG, "handleException")
            skipToNext()
        }
    }

    private fun refreshVideo(position: Int) {
        GlobalScope.launch {
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
        val selection = when (mTabPosition) {
            //屏蔽.vob格式
            0 -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
            1 -> "${MediaStore.Video.VideoColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
            else -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DESCRIPTION} like '%\"sender_id\":$mUserId%'"
        }
        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Video.VideoColumns._ID,
                MediaStore.Video.VideoColumns.DATA,
                MediaStore.Video.VideoColumns.SIZE,
                MediaStore.Video.VideoColumns.DISPLAY_NAME,
                MediaStore.Video.VideoColumns.TITLE,
                MediaStore.Video.VideoColumns.DURATION,
                MediaStore.Video.VideoColumns.RESOLUTION,
                MediaStore.Video.VideoColumns.DESCRIPTION,
                MediaStore.Video.VideoColumns.IS_PRIVATE
            ),
            selection,
            null,
            MediaStore.Video.Media.DEFAULT_SORT_ORDER
        )
        while (cursor?.moveToNext() == true) {
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns._ID))
            val data = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA))
            val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns.SIZE))
            val displayName =
                cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME))
            val title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.TITLE))
            val duration =
                cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION))
            val resolution =
                cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.RESOLUTION))
                    ?: ""
            val description =
                cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DESCRIPTION))
                    ?: ""
            val isprivate =
                cursor.getInt(cursor.getColumnIndex(MediaStore.Video.VideoColumns.IS_PRIVATE))
            val video = Video(
                id,
                data,
                size,
                displayName,
                title,
                duration,
                resolution,
                description,
                isprivate
            )
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
            else -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns._ID} = $mId and ${MediaStore.Video.VideoColumns.DESCRIPTION} like '%\"sender_id\":$mUserId%'"
        }
        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Video.VideoColumns._ID,
                MediaStore.Video.VideoColumns.DATA,
                MediaStore.Video.VideoColumns.SIZE,
                MediaStore.Video.VideoColumns.DISPLAY_NAME,
                MediaStore.Video.VideoColumns.TITLE,
                MediaStore.Video.VideoColumns.DURATION,
                MediaStore.Video.VideoColumns.RESOLUTION,
                MediaStore.Video.VideoColumns.DESCRIPTION,
                MediaStore.Video.VideoColumns.IS_PRIVATE
            ),
            selection,
            null,
            MediaStore.Video.Media.DEFAULT_SORT_ORDER
        )
        while (cursor?.moveToNext() == true) {
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns._ID))
            val data = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA))
            val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns.SIZE))
            val displayName =
                cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME))
                    ?: ""
            val title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.TITLE))
            val duration =
                cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION))
            val resolution =
                cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.RESOLUTION))
                    ?: ""
            val description =
                cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DESCRIPTION))
                    ?: ""
            val isprivate =
                cursor.getInt(cursor.getColumnIndex(MediaStore.Video.VideoColumns.IS_PRIVATE))
            val video = Video(
                id,
                data,
                size,
                displayName,
                title,
                duration,
                resolution,
                description,
                isprivate
            )
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
            else -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DESCRIPTION} like '%\"sender_id\":$mUserId%'"
        }
        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf("count(*)"),
            selection,
            null,
            null
        )
        cursor?.moveToFirst()
        val count = cursor?.getInt(0) ?: 0
        cursor?.close()
        return count
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LogUtils.dTag(TAG, "onConfigurationChanged")
        vsv_video_view.adjustSize(mMediaPlayer?.videoWidth ?: 0, mMediaPlayer?.videoHeight ?: 0)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        vsv_video_view.visibility = View.VISIBLE
        if (mPaused) {
            if (mState == STATE_PAUSED) {
                iv_play_video_view.performClick()
            }
            /*if (mVideos.size != countVideo()) {
                mHandler.removeCallbacks(mContentRunnable)
                mHandler.post(mContentRunnable)
            }*/
        }
        mPaused = false
    }

    override fun onPause() {
        super.onPause()
        mPaused = true
        vsv_video_view.visibility = View.GONE
        if (mState == STATE_PLAYING) {
            iv_play_video_view.performClick()
        }
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
        private val TAG = VideoViewTestActivity::class.java.simpleName
        private const val STATE_ERROR = -1
        private const val STATE_IDLE = 0
        private const val STATE_PREPARING = 1
        private const val STATE_PREPARED = 2
        private const val STATE_PLAYING = 3
        private const val STATE_PAUSED = 4
        private const val STATE_PLAYBACK_COMPLETED = 5
    }
}