package com.idwell.cloudframe

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.Process
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.multidex.MultiDexApplication
import com.alibaba.sdk.android.push.CommonCallback
import com.alibaba.sdk.android.push.noonesdk.PushServiceFactory
import com.blankj.utilcode.util.CrashUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.Utils
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.service.MessageService
import com.idwell.cloudframe.service.UploadFileService
import com.idwell.cloudframe.ui.MainActivity
import com.idwell.cloudframe.util.MyLogUtils
import com.joyhong.test.TestMainActivity
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.util.ArrayList
import kotlin.properties.Delegates

class MyApplication : MultiDexApplication() {

    private val mProcessNames = ArrayList<String>()

    override fun onCreate() {
        super.onCreate()
        mProcessNames.add("com.idwell.cloudframe")
        mProcessNames.add("com.idwell.cloudframe:upload_log")
        //获取当前进程名
        val processName = getCurrentProcessName()
        var currentTime = System.currentTimeMillis()
        if (mProcessNames.contains(processName)) {
            instance = this
            Utils.init(this)
            LogUtils.getConfig().isLogSwitch = true
            CrashUtils.init()
            MyLogUtils.getConfig().setLogSwitch(true);
            if (mProcessNames[0] == processName) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val builder = StrictMode.VmPolicy.Builder()
                    StrictMode.setVmPolicy(builder.build())
                }
                //重置已登录标识
                Device.isSignined = false
                //物理分辨率
                val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val point = Point()
                windowManager.defaultDisplay.getRealSize(point);
                if (point.x >= point.y) {
                    physicalSize = "${point.x}x${point.y}"
                } else {
                    physicalSize = "${point.y}x${point.x}"
                }
                val args: String = when (Build.VERSION.SDK_INT) {
                    19 -> "ro.snnumber"
                    27 -> "sys.serialno"
                    else -> "ro.serialno"
                }
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Device.snnumber = Build.getSerial()
                }else {
                    Device.snnumber = Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, args).toString()
                }
                //Device.snnumber = getString(R.string.serialno)
                //Log.d("lcs", Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, args).toString())
                if (Device.isFirstIn) {
//                    Device.setSoundEffectsEnabled(0)
//                    Device.setScreenBrightness((255 * 0.8).toInt())
//                    Device.setAutoRotateScreen(if (Device.hasGravitySensor) 1 else 0)
//                    Settings.Secure.putInt(contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 1)
//                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
//                    alarmManager.setTimeZone("America/Los_Angeles")
                }
//                startService(Intent(this, MessageService::class.java))
//                //启动log上传服务
//                startService(Intent(this, UploadFileService::class.java))
            }
        }else{
        }
        initCloudChannel()

    }


    private fun getCurrentProcessName(): String {
        return try {
            val file = File("/proc/" + Process.myPid() + "/" + "cmdline")
            val mBufferedReader = BufferedReader(FileReader(file))
            val processName = mBufferedReader.readLine().trim { it <= ' ' }
            mBufferedReader.close()
            processName
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    companion object {
        private val TAG = MyApplication::class.java.simpleName

        private var instance: MyApplication by Delegates.notNull()

        @JvmStatic
        fun instance() = instance

        //物理分辨率
        private var physicalSize: String by Delegates.notNull()

        @JvmStatic
        fun physicalSize() = physicalSize
    }
    private fun initCloudChannel() {
        PushServiceFactory.init(this)
        val cloudPushService = PushServiceFactory.getCloudPushService()
        cloudPushService.register(this, object : CommonCallback {
            override fun onSuccess(application: String?) {
                LogUtils.eTag(TAG, "$application, ${cloudPushService.deviceId}")
                Device.pushToken = cloudPushService.deviceId
                sendBroadcast(Intent(Device.ACTION_SIGN_IN))
            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                LogUtils.eTag(TAG, "$errorCode, $errorMessage")
            }
        })
    }

}