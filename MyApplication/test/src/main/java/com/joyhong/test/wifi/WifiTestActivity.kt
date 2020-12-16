package com.joyhong.test.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.text.TextUtils
import android.view.View
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.ToastUtils
import com.idwell.cloudframe.ui.test.wifi.WifiUtils
import com.joyhong.test.BaseTestActivity
import com.joyhong.test.R
import com.joyhong.test.TestMainActivity
import com.joyhong.test.TestResultEnum
import com.joyhong.test.util.TestConstant
import com.joyhong.test.util.TestConstant.RSSI_NOT_EXIST
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_wifi_test.*
import java.util.concurrent.TimeUnit

class WifiTestActivity : BaseTestActivity() {

    override fun initLayout(): Int {
        //开启扫描
        WifiUtils.disableWifi()
        return R.layout.activity_wifi_test
    }

    override fun initListener() {
        countdown_go.setOnClickListener(this)
        registerReceiver(rssiReceiver, IntentFilter(WifiManager.RSSI_CHANGED_ACTION))
        registerReceiver(rssiReceiver, IntentFilter(RSSI_NOT_EXIST))
    }

    override fun initData() {
        connectWifi()
    }

    var checkWifiSig: Thread? = null
    var checkWifi : Boolean = true
    fun connectWifi() {
        var wifiName = wifi_name.text.toString()
        var wifiPwd = wifi_pwd.text.toString()
        //查询wifi信息需要申请权限，权限工具类就不要在意了，重点在下面
        PermissionUtils.permission(PermissionConstants.LOCATION)
            .callback(object : PermissionUtils.SimpleCallback {
                override fun onGranted() {
                    //    每2秒发送一次事件
                    var connectObs = Observable.interval(2, TimeUnit.SECONDS)
                        //    取30次，还没连上就结束，算这次超时
                        .take(Long.MAX_VALUE)
                        .subscribeOn(Schedulers.computation())
                        .subscribe({
                            //开启扫描
                            WifiUtils.startScan()
                            //连接
                            var resule = WifiUtils.connect(wifi_name.text.toString(), wifiPwd)
                            if (null != resule) {
                                updateViewBySign(resule.level)
//                                if (null == checkWifiSig) {
//                                    checkWifiSig = Thread(Runnable {
//                                        while (checkWifi){
//                                            updateViewBySign(obtainWifiInfo())
//                                            Thread.sleep(3 * 1000)
//                                        }
//
//                                    })
//                                    checkWifiSig!!.start()
//                                }
                            }
                        }, {}, {
                            ToastUtils.showLong("连接屏热点超时")
                        })
                }

                override fun onDenied() {
                    ToastUtils.showShort("您拒绝了请求位置权限，我们无法搜寻网络连接上屏，请在设置中打开")
                }
            }).request()
    }


    override fun onClick(v: View?) {
        super.onClick(v)
        if (v == countdown_go) {
            connectWifi()
        }
    }
    var wifiManager: WifiManager?=null
    private fun obtainWifiInfo(): Int {
        // Wifi的连接速度及信号强度：
        var strength = 0
        if(null == wifiManager) {
            wifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        }
        // WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        val info: WifiInfo = wifiManager!!.getConnectionInfo()
        var wifiName = info.ssid.replace("\"", "")
        if (TextUtils.equals(wifiName, wifi_name.text.toString())) {
            // 链接信号强度，5为获取的信号强度值在5以内
//            strength = WifiManager.calculateSignalLevel(info.getRssi(), 5)
            strength = info.getRssi()
            // 链接速度
            val speed: Int = info.getLinkSpeed()
            // 链接速度单位
            val units: String = WifiInfo.LINK_SPEED_UNITS
            // Wifi源名称
            val ssid: String = info.getSSID()
        }
        //        return info.toString();
        return strength
    }

    fun updateViewBySign(s:Int){
        runOnUiThread {
            if (s >= -56  && s!=0 ) {
                val testEntity =
                    TestMainActivity.testResult["${TestConstant.PACKAGE_NAME}$localClassName"]
                testEntity!!.testResultEnum = TestResultEnum.PASS
                SPUtils.getInstance().put(testEntity.getTag(),1)
                finish()
            }
            wifi_sign.setText(s.toString() + " dbm")
        }
    }
    var rssiReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
//            val s = obtainWifiInfo()
//            if (s >= -56 && s!=0) {
//                val testEntity =
//                    TestMainActivity.testResult["$packageName.$localClassName"]
//                testEntity!!.testResultEnum = TestResultEnum.PASS
//                SPUtils.getInstance().put(testEntity.getTag(),1)
//                finish()
//                return
//            }
//            wifi_sign.setText(s.toString() + " dbm")
        }
    }

    override fun onDestroy() {
        checkWifi = false
        unregisterReceiver(rssiReceiver)
        super.onDestroy()
    }

}