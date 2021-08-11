package com.idwell.cloudframe.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.MyConstants
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.common.PlayMode
import com.idwell.cloudframe.entity.Music
import com.idwell.cloudframe.util.MyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

import java.lang.Runnable
import java.util.*

class MusicService : MediaBrowserServiceCompat(), AudioManager.OnAudioFocusChangeListener {

    private lateinit var mSession: MediaSessionCompat
    private var mMediaPlayer: MediaPlayer? = null
    private lateinit var mPlaybackState: PlaybackStateCompat
    private var mMusic = Music()
    private var mMusics = mutableListOf<Music>()
    private var mTabPosition = 0
    private var mExceptionCount = 0

    private lateinit var mAudioManager: AudioManager

    //关屏开屏
    private var isScreenOffPause = false
    private val mBroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null){
                when(intent.action){
                    Intent.ACTION_SCREEN_OFF -> {
                        if (mMediaPlayer?.isPlaying == true) {
                            isScreenOffPause = true
                            mMediaSessionCallback.onPause()
                        }
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        if (isScreenOffPause){
                            isScreenOffPause = false
                            mMediaSessionCallback.onPlay()
                        }
                    }
                }
            }
        }
    }

    private var mHandler: Handler = Handler()
    private val mRunnable = object : Runnable {
        override fun run() {
            if (File(mMusic.data).exists()) {
                val bundle = Bundle()
                bundle.putInt(MyConstants.PROGRESS, mMediaPlayer?.currentPosition ?: 0)
                mSession.sendSessionEvent(MyConstants.MEDIA_SESSION_CHANGE_PROGRESS, bundle)
                mHandler.removeCallbacks(this)
                mHandler.postDelayed(this, 200L)
            }
        }
    }

    private val mContentRunnable = object : Runnable {
        override fun run() {
            GlobalScope.launch {
                val musics = queryAudio()
                launch(Dispatchers.Main) {
                    mMusics.clear()
                    mMusics.addAll(musics)
                }
            }
        }
    }

    private val mContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (uri != null){
                LogUtils.dTag(TAG, "$selfChange, $uri")
                val uriPath = uri.toString()
                if (uriPath == MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString() && mMusics.size != countAudio()) {
                    mHandler.removeCallbacks(mContentRunnable)
                    mHandler.postDelayed(mContentRunnable, 200)
                }else if (uriPath.contains(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString() + "/")) {
                    val mId = uriPath.substring(uriPath.lastIndexOf("/") + 1).toLong()
                    queryAudio(mId)
                }
            }
        }
    }

    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            if (mMusics.isEmpty()) {
                return
            }
            mHandler.removeCallbacks(mRunnable)
            var position = mMusics.indexOf(mMusic)
            if (position == -1) {
                position = 0
            }
            when (Device.musicPlayMode) {
                PlayMode.LOOP.ordinal -> {
                    if (--position == -1) {
                        position = mMusics.size - 1
                    }
                    mMusic = mMusics[position]
                }
                PlayMode.SHUFFLE.ordinal -> {
                    position = Random().nextInt(mMusics.size)
                    mMusic = mMusics[position]
                }
            }
            mMediaPlayer?.reset()
            try {
                if (File(mMusic.data).exists()) {
                    mMediaPlayer?.setDataSource(mMusic.data)
                    mMediaPlayer?.prepareAsync()
                    val bundle = Bundle()
                    bundle.putParcelable(MyConstants.MUSIC, mMusic)
                    mSession.sendSessionEvent(MyConstants.MEDIA_SESSION_CHANGE_MUSIC, bundle)
                }else {
                    fileNotExists(position)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onSkipToNext()
            }
        }

        override fun onPlay() {
            super.onPlay()
            if (mMusics.isEmpty() || mPlaybackState.state == PlaybackStateCompat.STATE_PLAYING) {
                return
            }
            mHandler.removeCallbacks(mRunnable)
            if (mPlaybackState.state == PlaybackStateCompat.STATE_NONE) {
                var position = mMusics.indexOf(mMusic)
                if (position == -1) {
                    position = 0
                    mMusic = mMusics[position]
                }
                try {
                    if (File(mMusic.data).exists()){
                        mMediaPlayer?.reset()
                        mMediaPlayer?.setDataSource(mMusic.data)
                        mMediaPlayer?.prepareAsync()
                    }else {
                        fileNotExists(position)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    onSkipToNext()
                }
            } else {
                mMediaPlayer?.start()
                mPlaybackState = PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f).build()
                mSession.setPlaybackState(mPlaybackState)
                mHandler.post(mRunnable)
            }
        }

        override fun onPause() {
            super.onPause()
            if (mMediaPlayer?.isPlaying == true) {
                mMediaPlayer?.pause()
                mPlaybackState = PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f).build()
                mSession.setPlaybackState(mPlaybackState)
                mHandler.removeCallbacks(mRunnable)
            }
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            if (mMusics.isEmpty()) {
                return
            }
            mHandler.removeCallbacks(mRunnable)
            var position = mMusics.indexOf(mMusic)
            if (position == -1) {
                position = 0
            }
            when (Device.musicPlayMode) {
                PlayMode.LOOP.ordinal -> {
                    if (++position == mMusics.size) {
                        position = 0
                    }
                    mMusic = mMusics[position]
                }
                PlayMode.SHUFFLE.ordinal -> {
                    position = Random().nextInt(mMusics.size)
                    mMusic = mMusics[position]
                }
            }
            mMediaPlayer?.reset()
            try {
                if (File(mMusic.data).exists()) {
                    mMediaPlayer?.setDataSource(mMusic.data)
                    mMediaPlayer?.prepareAsync()
                    val bundle = Bundle()
                    bundle.putParcelable(MyConstants.MUSIC, mMusic)
                    mSession.sendSessionEvent(MyConstants.MEDIA_SESSION_CHANGE_MUSIC, bundle)
                }else {
                    fileNotExists(position)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (++mExceptionCount < mMusics.size) {
                    onSkipToNext()
                } else {
                    mExceptionCount = 0
                    val bundle = Bundle()
                    bundle.putParcelable(MyConstants.MUSIC, mMusic)
                    mSession.sendSessionEvent(MyConstants.MEDIA_SESSION_CHANGE_MUSIC, bundle)
                    ToastUtils.showShort(R.string.playback_failed)
                }
            }
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            super.onCustomAction(action, extras)
            if (action != null && extras != null) {
                when (action) {
                    MyConstants.MEDIA_SESSION_ITEM_CLICK -> {
                        mHandler.removeCallbacks(mRunnable)
                        val musics = mutableListOf<Music>()
                        GlobalScope.launch {
                            if (mTabPosition != Device.musicTabPosition) {
                                mTabPosition = Device.musicTabPosition
                                musics.addAll(queryAudio())
                            }
                            launch(Dispatchers.Main) {
                                if (musics.isNotEmpty()) {
                                    mMusics.clear()
                                    mMusics.addAll(mMusics)
                                }
                                val music = extras.getParcelable<Music>("music")
                                if (music != null) {
                                    mMusic = music
                                    mMediaPlayer?.reset()
                                    try {
                                        LogUtils.dTag(TAG, mMusic.data)
                                        mMediaPlayer?.setDataSource(mMusic.data)
                                        mMediaPlayer?.prepareAsync()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        onSkipToNext()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            mHandler.removeCallbacks(mRunnable)
            mMediaPlayer?.seekTo(pos.toInt())
            mHandler.post(mRunnable)
        }
    }

    override fun onCreate() {
        super.onCreate()
        LogUtils.dTag(TAG, "onCreate")
        EventBus.getDefault().register(this)
        mTabPosition = Device.musicTabPosition
        GlobalScope.launch {
            val musics = queryAudio()
            launch(Dispatchers.Main) {
                mMusics.clear()
                mMusics.addAll(musics)
            }
        }
        mMediaPlayer = MediaPlayer()
        mMediaPlayer?.setOnPreparedListener {
            mExceptionCount = 0
            mMediaPlayer?.start()
            mPlaybackState = PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f).build()
            mSession.setPlaybackState(mPlaybackState)
            mHandler.removeCallbacks(mRunnable)
            mHandler.post(mRunnable)
        }
        mMediaPlayer?.setOnCompletionListener {
            mHandler.removeCallbacks(mRunnable)
            mMediaSessionCallback.onSkipToNext()
        }
        mMediaPlayer?.setOnErrorListener { mp, what, extra ->
            LogUtils.dTag(TAG, "$mp, $what, $extra")
            if (mPlaybackState.state != PlaybackStateCompat.STATE_NONE) {
                mPlaybackState = PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f).build()
                mSession.setPlaybackState(mPlaybackState)
            }
            true
        }

        mPlaybackState = PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, mMediaPlayer?.currentPosition?.toLong() ?: 0, 1.0f)
                .build()

        mSession = MediaSessionCompat(this, "MusicService")
        mSession.setCallback(mMediaSessionCallback)
        mSession.isActive = true
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mSession.setPlaybackState(mPlaybackState)
        //设置token后会触发MediaBrowserCompat.ConnectionCallback的回调方法
        //表示MediaBrowser与MediaBrowserService连接成功
        sessionToken = mSession.sessionToken

        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        //注册广播接收器
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        registerReceiver(mBroadcastReceiver, intentFilter)
        //注册媒体库监听
        contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mContentObserver)
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        LogUtils.dTag(TAG, "onGetRoot")
        return BrowserRoot("/", null)
    }

    override fun onLoadChildren(parentMediaId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        LogUtils.dTag(TAG, "onLoadChildren")
        result.detach()
        mSession.setPlaybackState(mPlaybackState)
        LogUtils.dTag(TAG, mPlaybackState)
        if (mMusics.size > 0) {
            var position = mMusics.indexOf(mMusic)
            if (position == -1) {
                position = 0
                mMusic = mMusics[position]
            }
            LogUtils.dTag(TAG, "$position, ${mMusic.data}")
            val bundle = Bundle()
            bundle.putParcelable(MyConstants.MUSIC, mMusic)
            mSession.sendSessionEvent(MyConstants.MEDIA_SESSION_CHANGE_MUSIC, bundle)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent) {
        when(event.message) {

        }
    }

    private fun fileNotExists(position: Int) {
        GlobalScope.launch {
            val musics = queryAudio()
            launch(Dispatchers.Main) {
                mMusics.clear()
                mMusics.addAll(musics)
                if (mMusics.isEmpty()){
                    mMediaPlayer?.reset()
                    mPlaybackState = PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f).build()
                    mSession.setPlaybackState(mPlaybackState)
                    mSession.sendSessionEvent(MyConstants.MEDIA_SESSION_FILES_IS_EMPTY, null)
                }else {
                    if (position > mMusics.size - 1){
                        mMusic = mMusics[0]
                    }else {
                        mMusic = mMusics[position]
                    }
                    mExceptionCount = 0
                    mPlaybackState = PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f).build()
                    mSession.setPlaybackState(mPlaybackState)
                    mMediaSessionCallback.onPlay()
                    val bundle = Bundle()
                    bundle.putParcelable(MyConstants.MUSIC, mMusic)
                    mSession.sendSessionEvent(MyConstants.MEDIA_SESSION_FILE_NOT_EXISTS, bundle)
                }
            }
        }
    }

    private fun queryAudio(): MutableList<Music> {
        val musics = mutableListOf<Music>()
        val selection = when (mTabPosition) {
            0 -> null
            1 -> "${MediaStore.Audio.AudioColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'"
            2 -> "${MediaStore.Audio.AudioColumns.DATA} like '${MyUtils.getSdDir()}/%'"
            3 -> "${MediaStore.Audio.AudioColumns.DATA} like '${MyUtils.getUsbDir()}/%'"
            else -> null
        }
        val cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Audio.AudioColumns.IS_MUSIC, MediaStore.Audio.AudioColumns._ID, MediaStore.Audio.AudioColumns.TITLE, MediaStore.Audio.AudioColumns.DISPLAY_NAME, MediaStore.Audio.AudioColumns.DATA, MediaStore.Audio.AudioColumns.SIZE, MediaStore.Audio.AudioColumns.ARTIST, MediaStore.Audio.AudioColumns.ALBUM, MediaStore.Audio.AudioColumns.ALBUM_ID, MediaStore.Audio.AudioColumns.DURATION), selection, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER)
        while (cursor?.moveToNext() == true) {
            val isMusic = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.IS_MUSIC))
            if (isMusic == 0) continue
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns._ID))
            val title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE)) ?: ""
            val displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DISPLAY_NAME)) ?: ""
            val data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA)) ?: ""
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
            0 -> "${MediaStore.Audio.AudioColumns._ID} = $mId"
            1 -> "${MediaStore.Audio.AudioColumns._ID} = $mId and ${MediaStore.Audio.AudioColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'"
            2 -> "${MediaStore.Audio.AudioColumns._ID} = $mId and ${MediaStore.Audio.AudioColumns.DATA} like '${MyUtils.getSdDir()}/%'"
            3 -> "${MediaStore.Audio.AudioColumns._ID} = $mId and ${MediaStore.Audio.AudioColumns.DATA} like '${MyUtils.getUsbDir()}/%'"
            else -> "${MediaStore.Audio.AudioColumns._ID} = $mId"
        }
        val cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Audio.AudioColumns.IS_MUSIC, MediaStore.Audio.AudioColumns._ID, MediaStore.Audio.AudioColumns.TITLE, MediaStore.Audio.AudioColumns.DISPLAY_NAME, MediaStore.Audio.AudioColumns.DATA, MediaStore.Audio.AudioColumns.SIZE, MediaStore.Audio.AudioColumns.ARTIST, MediaStore.Audio.AudioColumns.ALBUM, MediaStore.Audio.AudioColumns.ALBUM_ID, MediaStore.Audio.AudioColumns.DURATION), selection, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER)
        while (cursor?.moveToNext() == true) {
            val isMusic = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.IS_MUSIC))
            if (isMusic == 0) continue
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns._ID))
            val title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE)) ?: ""
            val displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DISPLAY_NAME)) ?: ""
            val data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA)) ?: ""
            val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.SIZE))
            val artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)) ?: ""
            val album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)) ?: ""
            val albumID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID))
            val duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION))
            val music = Music(id, title, displayName, data, size, artist, album, albumID, duration, false)
            if (mMusics.contains(music)) {
                val index = mMusics.indexOf(music)
                mMusics[index] = music
            } else {
                mMusics.add(music)
                LogUtils.dTag(TAG, mMusics.size)
            }
        }
        cursor?.close()
    }

    private fun countAudio(): Int {
        val selection = when (mTabPosition) {
            0 -> null
            1 -> "${MediaStore.Audio.AudioColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'"
            2 -> "${MediaStore.Audio.AudioColumns.DATA} like '${MyUtils.getSdDir()}/%'"
            3 -> "${MediaStore.Audio.AudioColumns.DATA} like '${MyUtils.getUsbDir()}/%'"
            else -> null
        }
        val cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf("count(*)"), selection, null, null)
        cursor?.moveToFirst()
        val count = cursor?.getInt(0) ?: 0
        cursor?.close()
        return count
    }

    override fun onAudioFocusChange(focusChange: Int) {
        LogUtils.dTag(TAG, focusChange)
        when (focusChange) {
            // 永久丢失焦点，如被其他播放器抢占
            // 短暂丢失焦点，如来电
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (mPlaybackState.state == PlaybackStateCompat.STATE_PLAYING) mMediaSessionCallback.onPause()
            // 瞬间丢失焦点，如通知
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (mPlaybackState.state == PlaybackStateCompat.STATE_PLAYING) mMediaPlayer?.setVolume(0.5f, 0.5f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.dTag(TAG, "onDestroy")
        EventBus.getDefault().unregister(this)
        mAudioManager.abandonAudioFocus(this)
        mMediaPlayer?.reset()
        mMediaPlayer?.release()
        mMediaPlayer = null
        mSession.release()
        unregisterReceiver(mBroadcastReceiver)
        contentResolver.unregisterContentObserver(mContentObserver)
        mHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        private val TAG = MusicService::class.java.simpleName
    }
}