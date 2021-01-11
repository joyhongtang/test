package com.idwell.cloudframe.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.os.PowerManager
import android.os.SystemClock

import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.MyConstants
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.db.entity.Alarm
import com.idwell.cloudframe.util.AlarmUtil
import com.idwell.cloudframe.widget.SlideView

import java.io.IOException

import kotlinx.android.synthetic.main.activity_ring.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RingActivity : BaseActivity(), MediaPlayer.OnPreparedListener, SlideView.SlidingTipListener {

    private lateinit var mPowerManager: PowerManager
    private var isWakeUp = false

    private var mMediaPlayer: MediaPlayer? = null

    private lateinit var mAlarm: Alarm

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null) {
                when(intent.action){
                    Intent.ACTION_SCREEN_OFF -> {
                        finish()
                    }
                }
            }
        }
    }

    override fun initLayout(): Int {
        showTopBar = false
        return R.layout.activity_ring
    }

    override fun initData() {
        mPowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!mPowerManager.isScreenOn) {
            isWakeUp = true
            mPowerManager.wakeUp(SystemClock.uptimeMillis())
        }
        val animationDrawable = iv_sliding_tip_ring.background as AnimationDrawable
        //启动动画
        animationDrawable.start()

        val type = intent.getIntExtra(MyConstants.ALARM_TYPE, 0)
        LogUtils.dTag(TAG, "type = $type")
        mAlarm = Gson().fromJson(intent.getStringExtra(MyConstants.ALARM_CLOCK), Alarm::class.java)
        if (type == 0) {
            if (mAlarm.repeat.isEmpty()) {
                GlobalScope.launch {
                    // 单次响铃
                    mAlarm.isChecked = false
                    MyDatabase.instance.alarmDao.insert(mAlarm)
                    LogUtils.dTag(TAG, "单次响铃")
                }
            } else {
                // 重复周期响铃
                AlarmUtil.startAlarm(mAlarm)
                LogUtils.dTag(TAG, "重复周期响铃")
            }
        }

        mMediaPlayer = MediaPlayer()
        mMediaPlayer?.isLooping = true
        try {
            mMediaPlayer?.setDataSource(mAlarm.ringtone)
            mMediaPlayer?.prepareAsync()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        registerReceiver(mBroadcastReceiver, intentFilter)
    }

    override fun initListener() {
        sv_ring.setSlidingTipListener(this)
        mMediaPlayer?.setOnPreparedListener(this)
    }

    override fun initMessageEvent(event: MessageEvent) {

    }

    override fun onPrepared(mediaPlayer: MediaPlayer) {
        mMediaPlayer?.start()
        GlobalScope.launch {
            delay(60000)
            finish()
        }
    }

    override fun onSlidFinish() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaPlayer?.reset()
        mMediaPlayer?.release()
        mMediaPlayer = null
        unregisterReceiver(mBroadcastReceiver)
        if (isWakeUp && Device.powerState == "off" && mPowerManager.isScreenOn){
            mPowerManager.goToSleep(SystemClock.uptimeMillis())
        }
    }

    companion object {
        private val TAG = RingActivity::class.java.simpleName
    }
}