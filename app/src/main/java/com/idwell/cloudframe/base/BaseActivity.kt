package com.idwell.cloudframe.base

import android.os.Bundle
import android.os.HumanSensor
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.blankj.utilcode.util.SPUtils
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.common.MessageEvent
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileWriter
import java.math.BigDecimal

abstract class BaseActivity : AppCompatActivity(), View.OnClickListener, CoroutineScope by MainScope() {

    var showTopBar = true
    var isMore = false
    var mIndexes = mutableListOf<Int>()

    open fun initConfig() {}

    abstract fun initLayout(): Int

    abstract fun initData()

    abstract fun initListener()

    abstract fun initMessageEvent(event: MessageEvent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initConfig()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        setContentView(initLayout())
        EventBus.getDefault().register(this)
        initData()
        initListener()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(R.layout.activity_base)
        if (Device.isDualScreen) {
            val layoutParams = cl_base.layoutParams as FrameLayout.LayoutParams
            layoutParams.marginEnd = 195
            cl_base.setPadding(0, 0, 18, 0)
        }
        LayoutInflater.from(this).inflate(layoutResID, cl_content_base, true)
        if (showTopBar) {
            cl_navigation_base.visibility = View.VISIBLE
            iv_back_base.setOnClickListener(this)
            iv_more_base.setOnClickListener(this)
            iv_check_base.setOnClickListener(this)
            iv_delete_base.setOnClickListener(this)
            iv_copy_base.setOnClickListener(this)
        } else {
            cl_navigation_base.visibility = View.GONE
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            iv_back_base -> finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if(SPUtils.getInstance().getBoolean("stepinto_test")){
            if (Device.hasHumanSensor) {
                setMotionSensor()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onMessageEvent(event: MessageEvent) {
        initMessageEvent(event)
    }

    protected fun k2c(kelvin: Double): Double {
        return BigDecimal(kelvin - 273.15).setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
    }

    protected fun k2f(kelvin: Double): Double {
        return BigDecimal((kelvin - 273.15) * 9 / 5 + 32).setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
        EventBus.getDefault().unregister(this)
    }

    companion object {
        private val TAG = BaseActivity::class.java.simpleName
    }
    private fun setMotionSensor() {
        try {
            SPUtils.getInstance().put("stepinto_test", false)
            Class.forName("android.os.HumanSensor")
            if (Device.sleep == Int.MAX_VALUE) {
                HumanSensor.setMode(false)
            } else {
                HumanSensor.setMode(true)
            }
        } catch (e: Exception) {
            var fileWriter: FileWriter? = null
            try {
                val file = File("/data/data/com.idwell.cloudframe/sleepmode.txt")
                if (!file.exists()) {
                    file.createNewFile()
                }
                fileWriter = FileWriter(file)
                if (Device.sleep == Int.MAX_VALUE) {
                    fileWriter.write("200")
                } else {
                    fileWriter.write("300")
                }
                fileWriter.close()
            } catch (e: Exception) {
                fileWriter?.close()
            }
        }
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, Device.sleep)
    }
}