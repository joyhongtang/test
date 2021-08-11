package com.idwell.cloudframe.ui

import android.annotation.SuppressLint
import android.app.backup.BackupManager
import android.content.Intent
import android.content.res.Configuration
import androidx.fragment.app.FragmentTransaction
import com.blankj.utilcode.util.LogUtils
import com.idwell.cloudframe.MyApplication
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.Device

import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.service.GlobalService
import com.idwell.cloudframe.service.NavigationService
import com.joyhong.test.TestMainActivity
import com.joyhong.test.util.TestConstant
import java.util.*

class GuideActivity : BaseActivity() {

    private var mGuideWiFiFragment: GuideWiFiFragment? = null
    private var mGuideLanCityTimeFragment: GuideLanCityTimeFragment? = null
    private var mGuideSleepScheduleFragment: GuideSleepScheduleFragment? = null
    private var mGuideDeviceInfoFragment: GuideDeviceInfoFragment? = null

    override fun initLayout(): Int {
        showTopBar = false
        return R.layout.activity_guide
    }

    override fun initData() {
        TestConstant.initTest(MyApplication.instance(), "aaa", "aaa")
        startActivity(Intent(this, TestMainActivity::class.java))
        showFragment(0)
    }

    override fun initListener() {

    }

    override fun initMessageEvent(event: MessageEvent) {

    }

    fun showFragment(position: Int) {
        val transaction = supportFragmentManager.beginTransaction()
        hideFragment(transaction)
        when (position) {
            0 -> {
                if (mGuideWiFiFragment == null) {
                    mGuideWiFiFragment = GuideWiFiFragment()
                    mGuideWiFiFragment?.let { transaction.add(R.id.fl_guide, it) }
                } else {
                    mGuideWiFiFragment?.let { transaction.show(it) }
                }
            }
            1 -> {
                if (mGuideLanCityTimeFragment == null) {
                    mGuideLanCityTimeFragment = GuideLanCityTimeFragment()
                    mGuideLanCityTimeFragment?.let {
                        transaction.add(R.id.fl_guide, it)
                    }
                } else {
                    mGuideLanCityTimeFragment?.let {
                        transaction.show(it)
                    }
                }
            }
            2 -> {
                if (mGuideSleepScheduleFragment == null) {
                    mGuideSleepScheduleFragment = GuideSleepScheduleFragment()
                    mGuideSleepScheduleFragment?.let {
                        transaction.add(R.id.fl_guide, it)
                    }
                } else {
                    mGuideSleepScheduleFragment?.let {
                        transaction.show(it)
                    }
                }
            }
            3 -> {
                if (mGuideDeviceInfoFragment == null) {
                    mGuideDeviceInfoFragment = GuideDeviceInfoFragment()
                    mGuideDeviceInfoFragment?.let { transaction.add(R.id.fl_guide, it) }
                } else {
                    mGuideDeviceInfoFragment?.let { transaction.show(it) }
                }
            }
        }
        transaction.commit()
    }

    private fun hideFragment(transaction: FragmentTransaction) {
        mGuideWiFiFragment?.let { transaction.hide(it) }
        mGuideLanCityTimeFragment?.let { transaction.hide(it) }
        mGuideSleepScheduleFragment?.let { transaction.hide(it) }
        mGuideDeviceInfoFragment?.let { transaction.hide(it) }
    }

    fun removeFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        mGuideWiFiFragment?.let { transaction.remove(it) }
        mGuideLanCityTimeFragment?.let { transaction.remove(it) }
        mGuideSleepScheduleFragment?.let { transaction.remove(it) }
        mGuideDeviceInfoFragment?.let { transaction.remove(it) }
        transaction.commit()
    }

    @SuppressLint("PrivateApi")
    fun changeSystemLanguage(locale: Locale) {
        val classActivityManagerNative = Class.forName("android.app.ActivityManagerNative")
        val getDefault = classActivityManagerNative.getDeclaredMethod("getDefault")
        val objIActivityManager = getDefault.invoke(classActivityManagerNative)
        val classIActivityManager = Class.forName("android.app.IActivityManager")
        val getConfiguration = classIActivityManager.getDeclaredMethod("getConfiguration")
        val config = getConfiguration.invoke(objIActivityManager) as Configuration
        config.setLocale(locale)
        val clzConfig = Class.forName("android.content.res.Configuration")
        val userSetLocale = clzConfig.getField("userSetLocale")
        userSetLocale.set(config, true)
        val clzParams = arrayOf<Class<*>>(Configuration::class.java)
        val updateConfiguration = classIActivityManager.getDeclaredMethod("updateConfiguration", *clzParams)
        updateConfiguration.invoke(objIActivityManager, config)
        BackupManager.dataChanged("com.android.providers.settings")
    }

    override fun onBackPressed() {
        Device.isFirstIn = false
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!Device.isFirstIn) {
            //启动悬浮球
            startService(Intent(this, GlobalService::class.java))
            if (Device.isDualScreen) {
                startService(Intent(this, NavigationService::class.java))
            }
        }
    }

    companion object {
        private val TAG = GuideActivity::class.java.simpleName
    }
}