package com.idwell.cloudframe.service

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.view.KeyEvent
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.MyConstants
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.widget.floatball.FloatBallManager
import com.idwell.cloudframe.widget.floatball.floatball.FloatBallCfg
import com.idwell.cloudframe.widget.floatball.menu.FloatMenuCfg
import com.idwell.cloudframe.widget.floatball.menu.MenuItem
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import android.hardware.input.InputManager
import android.os.SystemClock
import android.util.Log
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.idwell.cloudframe.util.MyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GlobalService : Service() {

    private lateinit var mFloatBallManager: FloatBallManager

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null) {
                when (intent.action) {
                    Intent.ACTION_LOCALE_CHANGED -> {
                        LogUtils.dTag(TAG, intent.action)
                        mFloatBallManager.buildMenu()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        EventBus.getDefault().register(this)
        initFloatBall()
        //注册广播接收器
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_LOCALE_CHANGED)
        registerReceiver(mReceiver, filter)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra(MyConstants.ACTION)) {
            Device.DISPLAY_SUSPENDED_BALL -> {
                if (Device.displaySuspendedBall) {
                    mFloatBallManager.show()
                } else {
                    mFloatBallManager.hide()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent) {

    }

    private fun initFloatBall() {
        val ballSize = resources.getDimension(R.dimen.dp_68).toInt()
        LogUtils.dTag(TAG, "$ballSize")
        val ballCfg = FloatBallCfg(ballSize, R.drawable.ic_float_ball)

        val menuSize = resources.getDimension(R.dimen.dp_340).toInt()
        val menuItemSize = resources.getDimension(R.dimen.dp_68).toInt()
        val menuCfg = FloatMenuCfg(menuSize, menuItemSize)

        mFloatBallManager = FloatBallManager(this, ballCfg, menuCfg)
        val backItem = object : MenuItem(R.drawable.ic_back_floatball_selector, R.string.back) {
            override fun action() {
                MyUtils.injectInputEvent(KeyEvent.KEYCODE_BACK)
            }
        }
        val homeItem = object : MenuItem(R.drawable.ic_home_selector, R.string.home) {
            override fun action() {
                //MyUtils.injectInputEvent(KeyEvent.KEYCODE_HOME)
                //ActivityUtils.finishToActivity(MainActivity::class.java, false)
                val intent= Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                ActivityUtils.startActivity(intent)
            }
        }
        val volMinusItem = object : MenuItem(R.drawable.ic_vol_minus_selector, R.string.vol_minus) {
            override fun action() {
                MyUtils.injectInputEvent(KeyEvent.KEYCODE_VOLUME_DOWN)
            }
        }
        val volPlusItem = object : MenuItem(R.drawable.ic_vol_plus_selector, R.string.vol_plus) {
            override fun action() {
                MyUtils.injectInputEvent(KeyEvent.KEYCODE_VOLUME_UP)
            }
        }
        val powerItem = object : MenuItem(R.drawable.ic_power_selector, R.string.power) {
            override fun action() {
                val uptimeMillis = SystemClock.uptimeMillis()
                val down = KeyEvent(uptimeMillis, uptimeMillis, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_POWER, 0)
                InputManager.getInstance()
                        .injectInputEvent(down, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
                GlobalScope.launch {
                    delay(500)
                    launch(Dispatchers.Main) {
                        val up = KeyEvent(uptimeMillis, uptimeMillis, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_POWER, 0)
                        InputManager.getInstance()
                                .injectInputEvent(up, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
                    }
                }
            }
        }
        mFloatBallManager.addMenuItem(backItem).addMenuItem(homeItem).addMenuItem(volMinusItem)
                .addMenuItem(volPlusItem).addMenuItem(powerItem).buildMenu()
        mFloatBallManager.setPermission(object : FloatBallManager.IFloatBallPermission {
            override fun onRequestFloatBallPermission(): Boolean {
                return true
            }

            override fun hasFloatBallPermission(context: Context?): Boolean {
                return true
            }

            override fun requestFloatBallPermission(activity: Activity?) {

            }
        })
        if (Device.displaySuspendedBall) {
            mFloatBallManager.show()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(mReceiver)
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    companion object {
        private val TAG = GlobalService::class.java.simpleName
    }
}