package com.idwell.cloudframe.ui

import android.animation.ObjectAnimator
import android.content.*
import android.content.res.Configuration
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.RemoteException
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.FileUtils

import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.R
import com.idwell.cloudframe.adapter.StorageAdapter
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.*
import com.idwell.cloudframe.entity.MultipleItem
import com.idwell.cloudframe.entity.Music
import com.idwell.cloudframe.service.MusicService
import com.idwell.cloudframe.util.MyUtils
import com.idwell.cloudframe.util.TimeUtil
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.widget.MaterialDialog
import com.idwell.cloudframe.widget.RingView
import com.idwell.cloudframe.widget.StorageItemDecoration
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.activity_music.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*

class MusicActivity : BaseActivity(), BaseQuickAdapter.OnItemClickListener, BaseQuickAdapter.OnItemLongClickListener, SeekBar.OnSeekBarChangeListener {

    private var mPaused = false

    private var mTempDialog: MaterialDialog? = null

    private lateinit var mMediaBrowser: MediaBrowserCompat
    private var mMediaController: MediaControllerCompat? = null
    private var mState = 0
    private var mTabPosition = 0
    private var mMusic = Music()
    //是否正在处理文件
    private var isProcessFile = false
    private val mProcessFileObserver = MutableLiveData<Boolean>()
    //是否正在刷新列表
    private var isRefreshing = false

    private lateinit var mStorageAdapter: StorageAdapter

    private var mMusics = mutableListOf<Music>()
    private lateinit var mMusicAdapter: BaseQuickAdapter<Music, BaseViewHolder>
    private lateinit var mLinearLayoutManager: androidx.recyclerview.widget.LinearLayoutManager
    private lateinit var mAudioManager: AudioManager
    private var currentVolume: Int = 0
    private lateinit var diskAnimator: ObjectAnimator
    private val mStorages = mutableListOf<MultipleItem>()

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null) {
                when (intent.action) {
                    "android.media.VOLUME_CHANGED_ACTION" -> {
                        currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        sb_volume.progress = currentVolume
                    }
                }
            }
        }
    }

    private val mHandler = Handler()
    private val mContentRunnable = object : Runnable {
        override fun run() {
            launch(Dispatchers.IO) {
                val musics = queryAudio()
                launch(Dispatchers.Main) {
                    mMusics.clear()
                    mMusics.addAll(musics)
                    if (isMore) {
                        isMore = false
                        mIndexes.clear()
                        refreshMore()
                    }
                    if (mMusic.data.isEmpty() && mMusics.isNotEmpty()) {
                        mMusic = mMusics[0]
                        refreshContent()
                    }
                    refreshMusicList()
                }
            }
        }
    }

    private val mContentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (uri != null && !isProcessFile) {
                LogUtils.dTag(LOG_TAG, "$selfChange, $uri")
                val uriPath = uri.toString()
                if (!mPaused && uriPath == MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString() && mMusics.size != countAudio()) {
                    mHandler.removeCallbacks(mContentRunnable)
                    mHandler.postDelayed(mContentRunnable, 200)
                } else if (uriPath.contains(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString() + "/")) {
                    val mId = uriPath.substring(uriPath.lastIndexOf("/") + 1).toLong()
                    queryAudio(mId)
                }
            }
        }
    }

    override fun initLayout(): Int {
        return R.layout.activity_music
    }

    override fun initData() {
        //tv_title_base.setText(R.string.music)
        iv_title_base.setImageResource(R.drawable.ic_music)
        onOrientationChanged(resources.configuration.orientation)

        iv_more_base.visibility = View.VISIBLE
        mTabPosition = Device.musicTabPosition

        mMediaBrowser = MediaBrowserCompat(this, ComponentName(this, MusicService::class.java), //绑定浏览器服务
                mMediaBrowserConnectionCallback, null)
        mMediaBrowser.connect()

        mStorages.add(MultipleItem(StorageAdapter.MUSIC_STORAGE, R.drawable.ic_all, getString(R.string.all)))
        mStorages.add(MultipleItem(StorageAdapter.MUSIC_STORAGE, R.drawable.ic_internal_storage, getString(R.string.internal_storage)))
        if(Device.isConfigSdcard)
        mStorages.add(MultipleItem(StorageAdapter.MUSIC_STORAGE, R.drawable.ic_sd, getString(R.string.sd_card)))
        if(Device.isConfigUSBStorage)
        mStorages.add(MultipleItem(StorageAdapter.MUSIC_STORAGE, R.drawable.ic_usb, getString(R.string.usb)))
        mStorageAdapter = StorageAdapter(mStorages)
        mStorageAdapter.mPosition = mTabPosition
        rv_storage_music.layoutManager = GridLayoutManager(this, 4, RecyclerView.VERTICAL, false)
        rv_storage_music.addItemDecoration(StorageItemDecoration(2))
        rv_storage_music.adapter = mStorageAdapter

        iv_play_mode.setImageLevel(Device.musicPlayMode)
        mMusicAdapter = object : BaseQuickAdapter<Music, BaseViewHolder>(R.layout.item_sidebar_music, mMusics) {
            override fun convert(helper: BaseViewHolder, item: Music?) {
                if (item != null) {
                    helper.setText(R.id.tv_item_sidebar_music, item.displayName)
                    if (isMore) {
                        helper.setBackgroundRes(R.id.cl_item_sidebar_music, R.drawable.transparent)
                        if (item.isSelected) {
                            helper.setImageResource(R.id.iv_item_sidebar_music, R.drawable.ic_checked)
                        } else {
                            helper.setImageResource(R.id.iv_item_sidebar_music, R.drawable.ic_unchecked)
                        }
                        helper.getView<ImageView>(R.id.iv_item_sidebar_music)
                                .visibility = View.VISIBLE
                    } else {
                        if (item == mMusic) {
                            helper.setBackgroundColor(R.id.cl_item_sidebar_music, ContextCompat.getColor(mContext, R.color.main))
                        } else {
                            helper.setBackgroundColor(R.id.cl_item_sidebar_music, ContextCompat.getColor(mContext, R.color.transparent))
                        }
                        helper.getView<ImageView>(R.id.iv_item_sidebar_music).visibility = View.GONE
                    }
                }
            }
        }
        mLinearLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rv_music.layoutManager = mLinearLayoutManager
        rv_music.addItemDecoration(HorizontalItemDecoration(ContextCompat.getColor(this, R.color.divider)))
        rv_music.adapter = mMusicAdapter

        rv_music.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (vsb_music.isTouched) return
                //滑动到顶部
                else if (!rv_music.canScrollVertically(-1)) vsb_music.progress = 0
                //滑动到底部
                else if (!rv_music.canScrollVertically(1)) vsb_music.progress = mMusics.size - 1
                else {
                    //获取第一个可见view的位置
                    val firstItemPosition = mLinearLayoutManager.findFirstVisibleItemPosition()
                    //获取最后一个可见view的位置
                    val lastItemPosition = mLinearLayoutManager.findLastVisibleItemPosition()
                    vsb_music.progress = (firstItemPosition + lastItemPosition) / 2
                }
            }
        })
        vsb_music.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (vsb_music.isTouched) rv_music.scrollToPosition(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        //获取系统最大音量和当前音量
        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        sb_volume.max = maxVolume
        sb_volume.progress = currentVolume

        diskAnimator = ObjectAnimator.ofFloat(iv_disk_music, "rotation", 0f, -360f)
        diskAnimator.duration = 6000
        diskAnimator.interpolator = LinearInterpolator()
        diskAnimator.repeatCount = -1
        diskAnimator.repeatMode = ObjectAnimator.RESTART

        //注册音量变化广播
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.media.VOLUME_CHANGED_ACTION")
        registerReceiver(mBroadcastReceiver, intentFilter)

        launch(Dispatchers.IO) {
            val musics = queryAudio()
            launch(Dispatchers.Main) {
                mMusics.clear()
                mMusics.addAll(musics)
                mMusicAdapter.setNewData(mMusics)
            }
        }

        mProcessFileObserver.observe({ this.lifecycle }, { processFile ->
            isProcessFile = processFile
        })

        //注册媒体库监听
        contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mContentObserver)
    }

    override fun initListener() {
        iv_play_mode.setOnClickListener(this)
        mStorageAdapter.onItemClickListener = this
        mMusicAdapter.onItemClickListener = this
        mMusicAdapter.onItemLongClickListener = this
        iv_play.setOnClickListener(this)
        iv_next.setOnClickListener(this)
        iv_prev.setOnClickListener(this)
        sb_volume.setOnSeekBarChangeListener(this)
        sb_play.setOnSeekBarChangeListener(this)
    }

    override fun initMessageEvent(event: MessageEvent) {

    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v) {
            iv_play_mode -> switchPlayMode()
            iv_play -> when (mState) {
                PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.STATE_PAUSED -> mMediaController?.transportControls?.play()
                PlaybackStateCompat.STATE_PLAYING -> mMediaController?.transportControls?.pause()
            }
            iv_next -> mMediaController?.transportControls?.skipToNext()
            iv_prev -> mMediaController?.transportControls?.skipToPrevious()
            iv_more_base -> {
                isMore = !isMore
                for (index in mIndexes) {
                    mMusics[index].isSelected = false
                }
                mIndexes.clear()
                refreshMore()
                mMusicAdapter.notifyDataSetChanged()
            }
            iv_check_base -> {
                val isChecked = mIndexes.size == mMusics.size
                mIndexes.clear()
                for (music in mMusics) {
                    music.isSelected = !isChecked
                    if (music.isSelected) {
                        mIndexes.add(mMusics.indexOf(music))
                    }
                }
                refreshMore()
                mMusicAdapter.notifyDataSetChanged()
            }
            iv_copy_base -> {
                when (mTabPosition) {
                    1 -> {
                        copyFileToExternalStorage()
                    }
                    2, 3 -> {
                        copyFileToInternalStorage()
                    }
                }
            }
            iv_delete_base -> {
                MaterialDialog.Builder(this).setTitle(R.string.delete_the_selected_files)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                            override fun onClick(dialog: MaterialDialog) {
                                val view = View.inflate(this@MusicActivity, R.layout.view_process, null)
                                dialog.setContentView(view)
                                dialog.setCanceledOnTouchOutside(false)
                                dialog.setCancelable(false)
                                dialog.show()
                                mTempDialog = dialog
                                mProcessFileObserver.postValue(true)
                                val indexes = ArrayList<Int>()
                                indexes.addAll(mIndexes)
                                launch(Dispatchers.IO) {
                                    val musics = mutableListOf<Music>()
                                    for (index in indexes) {
                                        if (mPaused) {
                                            return@launch
                                        }
                                        val music = mMusics[index]
                                        FileUtils.delete(music.data)
                                        delete(music.id)
                                        musics.add(music)
                                    }
                                    launch(Dispatchers.Main) {
                                        isMore = false
                                        for (music in musics) {
                                            val index = mMusics.indexOf(music)
                                            if (index != -1) {
                                                mMusicAdapter.remove(index)
                                                if (index > 0) {
                                                    mMusicAdapter.notifyItemRangeChanged(0, index)
                                                }
                                            }
                                        }
                                        mIndexes.clear()
                                        refreshMore()
                                        refreshVerticalSeekBar()
                                        mProcessFileObserver.postValue(false)
                                        dialog.dismiss()
                                    }
                                }
                            }
                        }).show()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        LogUtils.dTag(LOG_TAG, keyCode)
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                mMediaController?.transportControls?.skipToPrevious()
                return true
            }
            KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_DPAD_CENTER  -> {
                when (mState) {
                    PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.STATE_PAUSED -> {
                        mMediaController?.transportControls?.play()
                    }
                    PlaybackStateCompat.STATE_PLAYING -> {
                        mMediaController?.transportControls?.pause()
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                mMediaController?.transportControls?.skipToNext()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {
        if (seekBar === sb_play) {
            tv_current_time.text = TimeUtil.formatTime(progress.toLong())
        } else if (seekBar === sb_volume) {
            currentVolume = progress
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_PLAY_SOUND)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (seekBar === sb_play) {
            mMediaController?.transportControls?.seekTo(seekBar.progress.toLong())
        }
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        when (adapter) {
            mStorageAdapter -> {
                if (!isRefreshing) {
                    isRefreshing = true
                    mTabPosition = position
                    mStorageAdapter.mPosition = position
                    mStorageAdapter.notifyDataSetChanged()
                    mMusicAdapter.setNewData(null)
                    launch(Dispatchers.IO) {
                        val musics = queryAudio()
                        launch(Dispatchers.Main) {
                            mMusics.clear()
                            mMusics.addAll(musics)
                            isMore = false
                            mIndexes.clear()
                            refreshMore()
                            setNewMusicList()
                            isRefreshing = false
                        }
                    }
                }
            }
            mMusicAdapter -> {
                if (isMore) {
                    if (mMusics[position].isSelected) {
                        mMusics[position].isSelected = false
                        mIndexes.remove(position)
                    } else {
                        mMusics[position].isSelected = true
                        mIndexes.add(position)
                    }
                    mMusicAdapter.notifyDataSetChanged()
                    refreshMore()
                } else {
                    if (mMusics.size == 0) return
                    if (mMusics[position] == mMusic) {
                        when (mState) {
                            PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.STATE_PAUSED -> mMediaController?.transportControls?.play()
                            PlaybackStateCompat.STATE_PLAYING -> mMediaController?.transportControls?.pause()
                        }
                    } else {
                        Device.musicTabPosition = mTabPosition
                        mMusic = mMusics[position]
                        if (File(mMusic.data).exists()) {
                            refreshMusicList()
                            refreshContent()
                            val bundle = Bundle()
                            bundle.putParcelable("music", mMusic)
                            mMediaController?.transportControls?.sendCustomAction(MyConstants.MEDIA_SESSION_ITEM_CLICK, bundle)
                        } else {
                            launch(Dispatchers.IO) {
                                val musics = queryAudio()
                                launch(Dispatchers.Main) {
                                    mMusics.clear()
                                    mMusics.addAll(musics)
                                    var index = mMusics.indexOf(mMusic)
                                    if (index == -1) {
                                        index = 0
                                    }
                                    if (mMusics.isEmpty()) {
                                        mMusic = Music()
                                    } else {
                                        mMusic = mMusics[index]
                                    }
                                    mMusicAdapter.setNewData(mMusics)
                                    rv_music.scrollToPosition(index)
                                    vsb_music.max = if (mMusics.size > 0) mMusics.size - 1 else 0
                                    vsb_music.progress = index
                                    refreshContent()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onItemLongClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int): Boolean {
        if (isMore) {
            if (mMusics[position].isSelected) {
                mMusics[position].isSelected = false
                mIndexes.remove(position)
            } else {
                mMusics[position].isSelected = true
                mIndexes.add(position)
            }
        } else {
            isMore = true
            mIndexes.add(position)
            mMusics[position].isSelected = true
        }
        refreshMore()
        mMusicAdapter.notifyDataSetChanged()
        return true
    }

    private fun refreshMore() {
        if (mMusics.isEmpty()) {
            cl_more_base.visibility = View.GONE
        } else {
            if (isMore) {
                if (mIndexes.isEmpty()) {
                    iv_check_base.setImageResource(R.drawable.ic_unchecked)
                    iv_delete_base.setImageResource(R.drawable.ic_undelete)
                    iv_delete_base.isClickable = false
                    tv_total_base.visibility = View.GONE
                } else {
                    if (mIndexes.size == mMusics.size) {
                        iv_check_base.setImageResource(R.drawable.ic_checked)
                    } else {
                        iv_check_base.setImageResource(R.drawable.ic_unchecked)
                    }
                    iv_delete_base.setImageResource(R.drawable.ic_delete)
                    iv_delete_base.isClickable = true
                    tv_total_base.text = "${mIndexes.size}"
                    tv_total_base.visibility = View.VISIBLE
                }
                iv_check_base.visibility = View.VISIBLE
                iv_delete_base.visibility = View.VISIBLE
                if (mTabPosition == 0 || !MyUtils.isMountSdOrUsb()) {
                    iv_copy_base.visibility = View.GONE
                } else {
                    if (mIndexes.isEmpty()) {
                        iv_copy_base.setImageResource(R.drawable.ic_uncopy)
                        iv_copy_base.isClickable = false
                    } else {
                        iv_copy_base.setImageResource(R.drawable.ic_copy)
                        iv_copy_base.isClickable = true
                    }
                    iv_copy_base.visibility = View.VISIBLE
                }
                iv_more_base.setImageResource(R.drawable.ic_cancel)
            } else {
                tv_total_base.visibility = View.GONE
                iv_check_base.visibility = View.GONE
                iv_delete_base.visibility = View.GONE
                iv_copy_base.visibility = View.GONE
                iv_more_base.setImageResource(R.drawable.ic_more)
            }
            cl_more_base.visibility = View.VISIBLE
        }
    }

    private fun switchPlayMode() {
        when (Device.musicPlayMode) {
            PlayMode.LOOP.ordinal -> {
                Device.musicPlayMode = PlayMode.SHUFFLE.ordinal
                ToastUtils.showShort(R.string.mode_shuffle)
            }
            PlayMode.SHUFFLE.ordinal -> {
                Device.musicPlayMode = PlayMode.SINGLE.ordinal
                ToastUtils.showShort(R.string.mode_one)
            }
            PlayMode.SINGLE.ordinal -> {
                Device.musicPlayMode = PlayMode.LOOP.ordinal
                ToastUtils.showShort(R.string.mode_loop)
            }
        }
        iv_play_mode.setImageLevel(Device.musicPlayMode)
    }

    private val mMediaBrowserConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            LogUtils.dTag(LOG_TAG, "onConnected")
            val mediaId = mMediaBrowser.root
            mMediaBrowser.unsubscribe(mediaId)
            mMediaBrowser.subscribe(mediaId, object : MediaBrowserCompat.SubscriptionCallback() {})
            try {
                mMediaController = MediaControllerCompat(this@MusicActivity, mMediaBrowser.sessionToken)
                mMediaController?.registerCallback(mMediaControllerCallback)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    private val mMediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            mState = state.state
            when (state.state) {
                PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.STATE_PAUSED -> {
                    diskAnimator.pause()
                    iv_play.isSelected = false
                }
                PlaybackStateCompat.STATE_PLAYING -> {
                    diskAnimator.start()
                    iv_play.isSelected = true
                }
            }
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            if (extras != null) {
                when (event) {
                    MyConstants.MEDIA_SESSION_CHANGE_MUSIC -> {
                        val music = extras.getParcelable<Music>(MyConstants.MUSIC)
                        if (music != null) {
                            mMusic = music
                            if (mTabPosition == Device.musicTabPosition) {
                                refreshMusicList()
                            }
                            refreshContent()
                        }
                    }
                    MyConstants.MEDIA_SESSION_CHANGE_PROGRESS -> {
                        val progress = extras.getInt(MyConstants.PROGRESS)
                        sb_play.progress = progress
                        tv_current_time.text = TimeUtil.formatTime(progress.toLong())
                    }
                    MyConstants.MEDIA_SESSION_FILE_NOT_EXISTS -> {
                        val music = extras.getParcelable<Music>(MyConstants.MUSIC)
                        if (music != null) {
                            mMusic = music
                            fileNotExists()
                        }
                    }
                    MyConstants.MEDIA_SESSION_FILES_IS_EMPTY -> {
                        filesIsEmpty()
                    }
                }
            }
        }
    }

    private fun refreshMusicList() {
        var position = mMusics.indexOf(mMusic)
        if (position == -1) {
            position = 0
        }
        mMusicAdapter.notifyDataSetChanged()
        rv_music.scrollToPosition(position)
        vsb_music.max = if (mMusics.size > 0) mMusics.size - 1 else 0
        vsb_music.progress = position
    }

    private fun refreshVerticalSeekBar() {
        var position = mMusics.indexOf(mMusic)
        if (position == -1) {
            position = 0
        }
        vsb_music.max = if (mMusics.size > 0) mMusics.size - 1 else 0
        vsb_music.progress = position
    }

    private fun refreshContent() {
        if (mMusic.data.isEmpty()) {
            diskAnimator.pause()
            iv_play.isSelected = false
            tv_album.text = ""
            tv_artist.text = ""
            sb_play.max = 0
            sb_play.progress = 0
            tv_current_time.setText(R.string.play_time_start)
            tv_total_time.setText(R.string.play_time_start)
        } else {
            tv_album.text = mMusic.album
            tv_artist.text = mMusic.artist
            sb_play.max = mMusic.duration.toInt()
            sb_play.progress = 0
            tv_current_time.setText(R.string.play_time_start)
            tv_total_time.text = TimeUtil.formatTime(mMusic.duration)
        }
    }

    private fun setNewMusicList() {
        mMusicAdapter.setNewData(mMusics)
        vsb_music.max = if (mMusics.size > 0) mMusics.size - 1 else 0
        vsb_music.progress = 0
    }

    private fun copyFileToInternalStorage() {
        MaterialDialog.Builder(this).setContent(R.string.import_files_to_memory)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                    override fun onClick(dialog: MaterialDialog) {
                        val view = View.inflate(this@MusicActivity, R.layout.view_ring, null)
                        val ringView = view.findViewById<RingView>(R.id.rv_view_ring)
                        dialog.setContentView(view)
                        dialog.setCanceledOnTouchOutside(false)
                        dialog.setCancelable(false)
                        dialog.show()
                        mTempDialog = dialog
                        mProcessFileObserver.postValue(true)
                        launch(Dispatchers.IO) {
                            var totalSize = 0L
                            var writtenSize = 0L
                            for (music in mMusics) {
                                if (music.isSelected) {
                                    totalSize += music.size
                                }
                            }
                            for (music in mMusics) {
                                if (mPaused) {
                                    return@launch
                                }
                                if (music.isSelected) {
                                    val srcFile = File(music.data)
                                    val destFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), srcFile.name)
                                    val inputStream: FileInputStream
                                    try {
                                        inputStream = FileInputStream(srcFile)
                                    } catch (e: Exception) {
                                        break
                                    }
                                    var outputStream: OutputStream? = null
                                    try {
                                        outputStream = BufferedOutputStream(FileOutputStream(destFile, false))
                                        val byteSize = 8192
                                        val data = ByteArray(byteSize)
                                        var len: Int
                                        while (inputStream.read(data, 0, byteSize).also {
                                                    len = it
                                                } != -1) {
                                            outputStream.write(data, 0, len)
                                            writtenSize += len.toLong()
                                            ringView.setProgress((writtenSize * 100 / totalSize).toInt())
                                        }
                                        MyUtils.scanFile(destFile.absolutePath)
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    } finally {
                                        try {
                                            inputStream.close()
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                        try {
                                            outputStream?.close()
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                            launch(Dispatchers.Main) {
                                isMore = false
                                for (index in mIndexes) {
                                    mMusics[index].isSelected = false
                                }
                                mIndexes.clear()
                                refreshMore()
                                mMusicAdapter.notifyDataSetChanged()
                                mProcessFileObserver.postValue(false)
                                ToastUtils.showShort(R.string.import_success)
                                dialog.dismiss()
                            }
                        }
                    }
                }).show()
    }

    private fun copyFileToExternalStorage() {
        val devNames = mutableListOf<Int>()
        var pos = 0
        if (MyUtils.isMountSd()) {
            devNames.add(R.string.sd_card)
        }
        if (MyUtils.isMountUsb()) {
            devNames.add(R.string.usb)
        }
        MaterialDialog.Builder(this).setTitle(R.string.export_files_to_external_storage)
                .setAdapter(object : BaseQuickAdapter<Int, BaseViewHolder>(R.layout.item_textsc_imageec_dialog, devNames) {
                    override fun convert(helper: BaseViewHolder, item: Int?) {
                        if (item != null) {
                            val position = helper.layoutPosition
                            helper.setText(R.id.tv_title_item_textsc_imageec_dialog, item)
                            if (pos == position) Glide.with(mContext).load(R.drawable.ic_check_circle_blue).into(helper.getView(R.id.iv_item_textsc_imageec_dialog))
                            else Glide.with(mContext).load(R.drawable.gray_ring_shape).into(helper.getView(R.id.iv_item_textsc_imageec_dialog))
                        }
                    }
                }, BaseQuickAdapter.OnItemClickListener { adapter, _, position ->
                    pos = position
                    adapter.notifyDataSetChanged()
                }).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                    override fun onClick(dialog: MaterialDialog) {
                        val view = View.inflate(this@MusicActivity, R.layout.view_ring, null)
                        val ringView = view.findViewById<RingView>(R.id.rv_view_ring)
                        dialog.setContentView(view)
                        dialog.setCanceledOnTouchOutside(false)
                        dialog.setCancelable(false)
                        dialog.show()
                        mTempDialog = dialog
                        mProcessFileObserver.postValue(true)
                        launch(Dispatchers.IO) {
                            var totalSize = 0L
                            var writtenSize = 0L
                            for (music in mMusics) {
                                if (music.isSelected) {
                                    totalSize += music.size
                                }
                            }
                            for (music in mMusics) {
                                if (mPaused) {
                                    return@launch
                                }
                                if (music.isSelected) {
                                    val srcFile = File(music.data)
                                    val destFile = if (devNames[pos] == R.string.sd_card) File(MyUtils.getSdDir(), srcFile.name) else File(MyUtils.getUsbDir(), srcFile.name)
                                    val inputStream = FileInputStream(srcFile)
                                    var outputStream: OutputStream? = null
                                    try {
                                        outputStream = BufferedOutputStream(FileOutputStream(destFile, false))
                                        val byteSize = 8192
                                        val data = ByteArray(byteSize)
                                        var len: Int
                                        while (inputStream.read(data, 0, byteSize).also {
                                                    len = it
                                                } != -1) {
                                            outputStream.write(data, 0, len)
                                            writtenSize += len
                                            ringView.setProgress((writtenSize * 100 / totalSize).toInt())
                                        }
                                        MyUtils.scanFile(destFile.absolutePath)
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    } finally {
                                        try {
                                            inputStream.close()
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                        try {
                                            outputStream?.close()
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                            launch(Dispatchers.Main) {
                                isMore = false
                                for (index in mIndexes) {
                                    mMusics[index].isSelected = false
                                }
                                mIndexes.clear()
                                refreshMore()
                                mMusicAdapter.notifyDataSetChanged()
                                mProcessFileObserver.postValue(false)
                                ToastUtils.showShort(R.string.export_success)
                                dialog.dismiss()
                            }
                        }
                    }
                }).show()
    }

    private fun fileNotExists() {
        if (mTabPosition == Device.musicTabPosition) {
            launch(Dispatchers.IO) {
                val musics = queryAudio()
                launch(Dispatchers.Main) {
                    mMusics.clear()
                    mMusics.addAll(musics)
                    var index = mMusics.indexOf(mMusic)
                    if (index == -1) {
                        index = 0
                    }
                    mMusicAdapter.setNewData(mMusics)
                    rv_music.scrollToPosition(index)
                    vsb_music.max = if (mMusics.size > 0) mMusics.size - 1 else 0
                    vsb_music.progress = index
                }
            }
        }
        refreshContent()
    }

    private fun filesIsEmpty() {
        mMusic = Music()
        if (mTabPosition == Device.musicTabPosition) {
            mMusics.clear()
            mMusicAdapter.setNewData(mMusics)
            vsb_music.max = 0
            vsb_music.progress = 0
        }
        refreshContent()
    }

    private fun queryAudio(): MutableList<Music> {
        val musics = mutableListOf<Music>()
        val selection = when (mTabPosition) {
            0 -> "${MediaStore.Audio.AudioColumns.DATA} is not null"
            1 -> "${MediaStore.Audio.AudioColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'"
            2 -> "${MediaStore.Audio.AudioColumns.DATA} like '${MyUtils.getSdDir()}/%'"
            3 -> "${MediaStore.Audio.AudioColumns.DATA} like '${MyUtils.getUsbDir()}/%'"
            else -> "${MediaStore.Audio.AudioColumns.DATA} is not null"
        }
        val cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Audio.AudioColumns.IS_MUSIC, MediaStore.Audio.AudioColumns._ID, MediaStore.Audio.AudioColumns.TITLE, MediaStore.Audio.AudioColumns.DISPLAY_NAME, MediaStore.Audio.AudioColumns.DATA, MediaStore.Audio.AudioColumns.SIZE, MediaStore.Audio.AudioColumns.ARTIST, MediaStore.Audio.AudioColumns.ALBUM, MediaStore.Audio.AudioColumns.ALBUM_ID, MediaStore.Audio.AudioColumns.DURATION), selection, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER)
        while (cursor?.moveToNext() == true) {
            val isMusic = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.IS_MUSIC))
            if (isMusic == 0) continue
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns._ID))
            val title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE))
            val displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DISPLAY_NAME)) ?: ""
            val data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA))
            val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.SIZE))
            val artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)) ?: ""
            val album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)) ?: ""
            val albumID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID))
            val duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION))
            val music = Music(id, title, displayName, data, size, artist, album, albumID, duration, false)
            musics.add(music)
        }
        cursor?.close()
        return musics
    }

    private fun queryAudio(mId: Long) {
        val selection = when (mTabPosition) {
            0 -> "${MediaStore.Audio.AudioColumns.DATA} is not null and ${MediaStore.Audio.AudioColumns._ID} = $mId"
            1 -> "${MediaStore.Audio.AudioColumns._ID} = $mId and ${MediaStore.Audio.AudioColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'"
            2 -> "${MediaStore.Audio.AudioColumns._ID} = $mId and ${MediaStore.Audio.AudioColumns.DATA} like '${MyUtils.getSdDir()}/%'"
            3 -> "${MediaStore.Audio.AudioColumns._ID} = $mId and ${MediaStore.Audio.AudioColumns.DATA} like '${MyUtils.getUsbDir()}/%'"
            else -> "${MediaStore.Audio.AudioColumns.DATA} is not null and ${MediaStore.Audio.AudioColumns._ID} = $mId"
        }
        val cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Audio.AudioColumns.IS_MUSIC, MediaStore.Audio.AudioColumns._ID, MediaStore.Audio.AudioColumns.TITLE, MediaStore.Audio.AudioColumns.DISPLAY_NAME, MediaStore.Audio.AudioColumns.DATA, MediaStore.Audio.AudioColumns.SIZE, MediaStore.Audio.AudioColumns.ARTIST, MediaStore.Audio.AudioColumns.ALBUM, MediaStore.Audio.AudioColumns.ALBUM_ID, MediaStore.Audio.AudioColumns.DURATION), selection, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER)
        while (cursor?.moveToNext() == true) {
            val isMusic = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.IS_MUSIC))
            if (isMusic == 0) continue
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns._ID))
            val title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE))
            val displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DISPLAY_NAME)) ?: ""
            val data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA))
            val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.SIZE))
            val artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)) ?: ""
            val album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)) ?: ""
            val albumID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID))
            val duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION))
            val music = Music(id, title, displayName, data, size, artist, album, albumID, duration, false)
            if (mMusics.contains(music)) {
                val index = mMusics.indexOf(music)
                mMusicAdapter.setData(index, music)
            } else {
                mMusicAdapter.addData(music)
                LogUtils.dTag(LOG_TAG, mMusics.size)
            }
        }
        cursor?.close()
    }

    private fun countAudio(): Int {
        val selection = when (mTabPosition) {
            0 -> "${MediaStore.Audio.AudioColumns.DATA} is not null"
            1 -> "${MediaStore.Audio.AudioColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'"
            2 -> "${MediaStore.Audio.AudioColumns.DATA} like '${MyUtils.getSdDir()}/%'"
            3 -> "${MediaStore.Audio.AudioColumns.DATA} like '${MyUtils.getUsbDir()}/%'"
            else -> "${MediaStore.Audio.AudioColumns.DATA} is not null"
        }
        val cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf("count(*)"), selection, null, null)
        cursor?.moveToFirst()
        val count = cursor?.getInt(0) ?: 0
        cursor?.close()
        return count
    }

    /**
     * 删除Audio表中指定ID数据
     */
    private fun delete(id: Long) {
        contentResolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "${MediaStore.Audio.Media._ID}=$id", null)
    }

    private fun onOrientationChanged(orientation: Int) {
        val constraintSet = ConstraintSet()
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            constraintSet.connect(R.id.rv_music, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.rv_music, ConstraintSet.END, R.id.vsb_music, ConstraintSet.START)
            constraintSet.connect(R.id.rv_music, ConstraintSet.TOP, R.id.rv_storage_music, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.rv_music, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.setHorizontalWeight(R.id.rv_music, 0.5f)
            constraintSet.constrainHeight(R.id.rv_music, ConstraintSet.MATCH_CONSTRAINT)
            constraintSet.setMargin(R.id.rv_music, 3, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.rv_music, 4, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.rv_music, 6, ConvertUtils.dp2px(20f))
            constraintSet.setMargin(R.id.rv_music, 7, ConvertUtils.dp2px(50f))

            constraintSet.connect(R.id.vsb_music, ConstraintSet.START, R.id.rv_music, ConstraintSet.END)
            constraintSet.connect(R.id.vsb_music, ConstraintSet.END, R.id.cl_preview_music, ConstraintSet.START)
            constraintSet.connect(R.id.vsb_music, ConstraintSet.TOP, R.id.rv_music, ConstraintSet.TOP)
            constraintSet.connect(R.id.vsb_music, ConstraintSet.BOTTOM, R.id.rv_music, ConstraintSet.BOTTOM)
            constraintSet.constrainWidth(R.id.vsb_music, ConstraintSet.WRAP_CONTENT)
            constraintSet.constrainHeight(R.id.vsb_music, ConstraintSet.MATCH_CONSTRAINT)

            constraintSet.connect(R.id.cl_preview_music, ConstraintSet.START, R.id.vsb_music, ConstraintSet.END)
            constraintSet.connect(R.id.cl_preview_music, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.cl_preview_music, ConstraintSet.TOP, R.id.rv_music, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_preview_music, ConstraintSet.BOTTOM, R.id.rv_music, ConstraintSet.BOTTOM)
            constraintSet.setHorizontalWeight(R.id.cl_preview_music, 0.5f)
            constraintSet.constrainPercentHeight(R.id.cl_preview_music, 1.0f)
            constraintSet.setMargin(R.id.cl_preview_music, 6, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_preview_music, 7, ConvertUtils.dp2px(50f))
        } else {
            constraintSet.connect(R.id.rv_music, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.rv_music, ConstraintSet.END, R.id.vsb_music, ConstraintSet.START)
            constraintSet.connect(R.id.rv_music, ConstraintSet.TOP, R.id.rv_storage_music, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.rv_music, ConstraintSet.BOTTOM, R.id.cl_preview_music, ConstraintSet.TOP)
            constraintSet.setHorizontalWeight(R.id.rv_music, 1.0f)
            constraintSet.setVerticalWeight(R.id.rv_music, 0.5f)
            constraintSet.setMargin(R.id.rv_music, 3, ConvertUtils.dp2px(20f))
            constraintSet.setMargin(R.id.rv_music, 6, ConvertUtils.dp2px(20f))
            constraintSet.setMargin(R.id.rv_music, 7, ConvertUtils.dp2px(50f))

            constraintSet.connect(R.id.vsb_music, ConstraintSet.START, R.id.rv_music, ConstraintSet.END)
            constraintSet.connect(R.id.vsb_music, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.vsb_music, ConstraintSet.TOP, R.id.rv_music, ConstraintSet.TOP)
            constraintSet.connect(R.id.vsb_music, ConstraintSet.BOTTOM, R.id.rv_music, ConstraintSet.BOTTOM)
            constraintSet.constrainWidth(R.id.vsb_music, ConstraintSet.WRAP_CONTENT)
            constraintSet.constrainHeight(R.id.vsb_music, ConstraintSet.MATCH_CONSTRAINT)
            constraintSet.setMargin(R.id.vsb_music, 7, ConvertUtils.dp2px(20f))

            constraintSet.connect(R.id.cl_preview_music, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.cl_preview_music, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.cl_preview_music, ConstraintSet.TOP, R.id.rv_music, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.cl_preview_music, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.constrainWidth(R.id.cl_preview_music, ConstraintSet.MATCH_CONSTRAINT)
            constraintSet.setVerticalWeight(R.id.cl_preview_music, 0.5f)
            constraintSet.setMargin(R.id.cl_preview_music, 3, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_preview_music, 4, ConvertUtils.dp2px(20f))
            constraintSet.setMargin(R.id.cl_preview_music, 6, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_preview_music, 7, ConvertUtils.dp2px(50f))
        }
        constraintSet.applyTo(cl_music)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onOrientationChanged(newConfig.orientation)
    }

    override fun onResume() {
        super.onResume()
        if (mPaused && mMusics.size != countAudio()) {
            mHandler.removeCallbacks(mContentRunnable)
            mHandler.post(mContentRunnable)
        }
        mPaused = false
    }

    override fun onPause() {
        super.onPause()
        mPaused = true
        if (isMore && mTempDialog != null) {
            isMore = false
            for (index in mIndexes) {
                mMusics[index].isSelected = false
            }
            mIndexes.clear()
            refreshMore()
            mMusicAdapter.notifyDataSetChanged()
            mProcessFileObserver.postValue(false)

            mTempDialog?.dismiss()
            mTempDialog = null
        }
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaBrowser.disconnect()
        mMediaController = null
        diskAnimator.end()
        unregisterReceiver(mBroadcastReceiver)
        contentResolver.unregisterContentObserver(mContentObserver)
    }

    companion object {
        private val LOG_TAG = MusicActivity::class.java.simpleName
    }
}