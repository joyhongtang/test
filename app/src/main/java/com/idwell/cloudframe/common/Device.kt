package com.idwell.cloudframe.common

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.constant.MemoryConstants
import com.blankj.utilcode.util.*
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.db.entity.DeviceLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Device {

    const val ACTION_ALARM = "com.idwell.cloudframe.action.Alarm"
    const val ACTION_AUTO_ON = "com.idwell.cloudframe.action.AutoOn"
    const val ACTION_AUTO_OFF = "com.idwell.cloudframe.action.AutoOff"
    const val ACTION_SIGN_IN = "com.idwell.cloudframe.action.SIGN_IN"
    //共享应用日志
    const val ACTION_SHARE_APP_LOG = "com.idwell.cloudframe.action.SHARE_APP_LOG"
    const val ACTION_SHARE_CRASH_LOG = "com.idwell.cloudframe.action.SHARE_CRASH_LOG"

    //1.Joyhong 2.Aluratek 3.Ourphoto
    const val companyName = "Ourphoto"

    //双屏
    const val isDualScreen = false

    //人体感应
    var hasHumanSensor = true

    //自动调节亮度
    var autoBrightness = true

    //重力感应
    var hasGravitySensor = true

    //开放系统设置
    const val openSystemSettings = true

    //开放系统设置
    const val configBmpConvertJpg = true

    //开放系统设置
    var showedFloatingBall = false

    //无限流量
     var isUnlimitedData = true

    //是否有SDCARD
     var isConfigSdcard = true
    //是否有U盘存储
     var isConfigUSBStorage = true
    /**
     * 0: 默认
     * 1: PowerManagerService发送监听广播
     */
    const val userActivityListenerMode = 0

    var powerState = "on"
    //1.已获取 2.获取中 3.未获取
    var infoState = MutableLiveData<Int>()

    var musicTabPosition = 0

    //1.已获取
    var weatherState = MutableLiveData<Int>()

    val slideshowArray = longArrayOf(15_000, 30_000, 60_000, 120_000, 300_000, 600_000, 1800_000, 0)
    val slideIntervalArray = longArrayOf(5_000, 10_000, 15_000, 30_000, 60_000, 300_000)
    //人体感应
    val sleepArray = intArrayOf(Int.MAX_VALUE, 15_000, 30_000, 60_000, 300_000, 900_000, 1800_000)

    private const val VERSION_CODE = "version.CODE"
    private const val VERSION_DESC = "version.DESC"

    private const val IS_FIRST_IN = "isFirstIn"
    private const val SNNUMBER = "snnumber"
    private const val TOKEN = "token"
    private const val ID = "id"
    private const val EMAIL = "email"
    private const val DATA_FLOW = "dataFlow"
    private const val ACCEPT_NEW_USERS = "acceptNewUsers"
    private const val PUSH_TOKEN = "pushToken"
    //登录接口是否调用成功
    private const val IS_SIGNINED = "device.is_signined"
    private const val ACTIVATION_DESC = "activationDesc"
    private const val IOS_DESC = "iosDesc"
    private const val ANDROID_DESC = "androidDesc"
    private const val EMAIL_DESC = "emailDesc"
    private const val FACEBOOK_DESC = "facebookDesc"
    private const val TWITTER_DESC = "twitterDesc"
    const val DISPLAY_SUSPENDED_BALL = "displaySuspendedBall"
    private const val DISPLAY_FAHRENHEIT = "displayFahrenheit"
    private const val MUSIC_PLAY_MODE = "musicPlayMode"
    private const val CLOCK_MODE = "clock_mode"
    private const val CUR_CITY = "cur.city"
    private const val WEATHER = "weather"
    private const val IS_PHOTO_FULL_SCREEN = "isPhotoFullScreen"
    private const val SLIDESHOW = "slideshow"
    private const val BACKGROUND_MUSIC_ON = "background_music_on"
    private const val SLIDESHOW_MODE = "slideshow.MODE"
    private const val SLIDESHOW_TRANSITION_EFFECT = "slideshow.TRANSITION_EFFECT"
    private const val SLIDESHOW_INTERVAL = "slideshow.INTERVAL"
    private const val DATE_FORMAT_INDEX = "dateFormatIndex"
    private const val SLEEP = "sleep"

    var versionCode: Int
        get() = SPUtils.getInstance().getInt(VERSION_CODE)
        set(value) = SPUtils.getInstance().put(VERSION_CODE, value)

    var versionDesc: String
        get() = SPUtils.getInstance().getString(VERSION_DESC)
        set(versionDesc) = SPUtils.getInstance().put(VERSION_DESC, versionDesc)

    var isFirstIn: Boolean
        get() = SPUtils.getInstance().getBoolean(IS_FIRST_IN, true)
        set(value) = SPUtils.getInstance().put(IS_FIRST_IN, value)

    var snnumber: String
        get() = SPUtils.getInstance().getString(SNNUMBER)
        set(value) = SPUtils.getInstance().put(SNNUMBER, value)

    var token: String
        get() = SPUtils.getInstance().getString(TOKEN)
        set(value) = SPUtils.getInstance().put(TOKEN, value)

    var pushToken: String
        get() = SPUtils.getInstance().getString(PUSH_TOKEN)
        set(value) = SPUtils.getInstance().put(PUSH_TOKEN, value)

    var isSignined: Boolean
        get() = SPUtils.getInstance().getBoolean(IS_SIGNINED)
        set(value) = SPUtils.getInstance().put(IS_SIGNINED, value)

    var id: Int
        get() = SPUtils.getInstance().getInt(ID)
        set(value) = SPUtils.getInstance().put(ID, value)

    var email: String
        get() = SPUtils.getInstance().getString(EMAIL)
        set(value) = SPUtils.getInstance().put(EMAIL, value)

    var flow: Float
        get() = SPUtils.getInstance().getFloat(DATA_FLOW, 10_000f)
        set(value) = SPUtils.getInstance().put(DATA_FLOW, value)

    var acceptNewUsers: String
        get() = SPUtils.getInstance().getString(ACCEPT_NEW_USERS, "1")
        set(value) = SPUtils.getInstance().put(ACCEPT_NEW_USERS, value)

    var activationDesc: String
        get() = SPUtils.getInstance().getString(ACTIVATION_DESC)
        set(value) = SPUtils.getInstance().put(ACTIVATION_DESC, value)

    var iosDesc: String
        get() = SPUtils.getInstance().getString(IOS_DESC)
        set(value) = SPUtils.getInstance().put(IOS_DESC, value)

    var androidDesc: String
        get() = SPUtils.getInstance().getString(ANDROID_DESC)
        set(value) = SPUtils.getInstance().put(ANDROID_DESC, value)

    var emailDesc: String
        get() = SPUtils.getInstance().getString(EMAIL_DESC)
        set(value) = SPUtils.getInstance().put(EMAIL_DESC, value)

    var facebookDesc: String
        get() = SPUtils.getInstance().getString(FACEBOOK_DESC)
        set(value) = SPUtils.getInstance().put(FACEBOOK_DESC, value)

    var twitterDesc: String
        get() = SPUtils.getInstance().getString(TWITTER_DESC)
        set(value) = SPUtils.getInstance().put(TWITTER_DESC, value)

    var displaySuspendedBall: Boolean
        get() = SPUtils.getInstance().getBoolean(DISPLAY_SUSPENDED_BALL, true)
        set(value) = SPUtils.getInstance().put(DISPLAY_SUSPENDED_BALL, value)

    var displayFahrenheit: Boolean
        get() = SPUtils.getInstance().getBoolean(DISPLAY_FAHRENHEIT, true)
        set(value) = SPUtils.getInstance().put(DISPLAY_FAHRENHEIT, value)

    var musicPlayMode: Int
        get() = SPUtils.getInstance().getInt(MUSIC_PLAY_MODE, 0)
        set(value) = SPUtils.getInstance().put(MUSIC_PLAY_MODE, value)

    var clockMode: Int
        get() = SPUtils.getInstance().getInt(CLOCK_MODE)
        set(value) = SPUtils.getInstance().put(CLOCK_MODE, value)

    var curCity: String
        get() = SPUtils.getInstance().getString(CUR_CITY)
        set(value) = SPUtils.getInstance().put(CUR_CITY, value)

    var weather: String
        get() = SPUtils.getInstance().getString(WEATHER)
        set(value) = SPUtils.getInstance().put(WEATHER, value)

    var isPhotoFullScreen: Boolean
        get() = SPUtils.getInstance().getBoolean(IS_PHOTO_FULL_SCREEN, true)
        set(value) = SPUtils.getInstance().put(IS_PHOTO_FULL_SCREEN, value)

    var slideshow: Long
        get() = SPUtils.getInstance().getLong(SLIDESHOW, 60_000)
        set(value) = SPUtils.getInstance().put(SLIDESHOW, value)

    var isBackgroundMusicOn: Boolean
        get() = SPUtils.getInstance().getBoolean(BACKGROUND_MUSIC_ON, false)
        set(value) = SPUtils.getInstance().put(BACKGROUND_MUSIC_ON, value)

    var slideshowMode: Int
        get() = SPUtils.getInstance().getInt(SLIDESHOW_MODE, 0)
        set(value) = SPUtils.getInstance().put(SLIDESHOW_MODE, value)

    var slideshowTransitionEffect: Int
        get() = SPUtils.getInstance().getInt(SLIDESHOW_TRANSITION_EFFECT, 12)
        set(value) = SPUtils.getInstance().put(SLIDESHOW_TRANSITION_EFFECT, value)

    var slideshowInterval: Long
        get() = SPUtils.getInstance().getLong(SLIDESHOW_INTERVAL, 5000)
        set(value) = SPUtils.getInstance().put(SLIDESHOW_INTERVAL, value)

    var dateFormatIndex: Int
        get() = SPUtils.getInstance().getInt(DATE_FORMAT_INDEX, 0)
        set(value) = SPUtils.getInstance().put(DATE_FORMAT_INDEX, value)

    var sleep: Int
        get() = SPUtils.getInstance().getInt(SLEEP, 300_000)
        set(value) = SPUtils.getInstance().put(SLEEP, value)

    fun resetAlbumSettings() {
        SPUtils.getInstance().remove(IS_PHOTO_FULL_SCREEN)
        SPUtils.getInstance().remove(SLIDESHOW)
        SPUtils.getInstance().remove(BACKGROUND_MUSIC_ON)
        SPUtils.getInstance().remove(SLIDESHOW_MODE)
        SPUtils.getInstance().remove(SLIDESHOW_INTERVAL)
        SPUtils.getInstance().remove(SLIDESHOW_TRANSITION_EFFECT)
    }

    fun deleteMyDevice(){
        SPUtils.getInstance().remove(TOKEN)
        SPUtils.getInstance().remove(IS_SIGNINED)
        SPUtils.getInstance().remove(ID)
        SPUtils.getInstance().remove(EMAIL)
        SPUtils.getInstance().remove(ACTIVATION_DESC)
        SPUtils.getInstance().remove(IOS_DESC)
        SPUtils.getInstance().remove(ANDROID_DESC)
        SPUtils.getInstance().remove(EMAIL_DESC)
        SPUtils.getInstance().remove(FACEBOOK_DESC)
        SPUtils.getInstance().remove(TWITTER_DESC)
        SPUtils.getInstance().remove(ACCEPT_NEW_USERS)
    }

    fun isAutoRotateScreen(): Boolean{
        return Settings.System.getInt(Utils.getApp().contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
    }

    fun setAutoRotateScreen(value: Int){
        Settings.System.putInt(Utils.getApp().contentResolver, Settings.System.ACCELEROMETER_ROTATION, value)
    }

    fun isSoundEffectsEnabled(): Boolean{
        return Settings.System.getInt(Utils.getApp().contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 0) == 1
    }

    fun setSoundEffectsEnabled(value: Int){
        Settings.System.putInt(Utils.getApp().contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, value)
    }

    fun getScreenBrightness(): Int{
        return Settings.System.getInt(Utils.getApp().contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
    }

    fun setScreenBrightness(value: Int){
        Settings.System.putInt(Utils.getApp().contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
    }

    fun setStreamMusic(index: Int){
        (Utils.getApp().getSystemService(Context.AUDIO_SERVICE) as AudioManager).setStreamVolume(AudioManager.STREAM_MUSIC, index, AudioManager.FLAG_PLAY_SOUND)
    }

    fun setStreamNotification(index: Int){
        (Utils.getApp().getSystemService(Context.AUDIO_SERVICE) as AudioManager).setStreamVolume(AudioManager.STREAM_NOTIFICATION, index, AudioManager.FLAG_PLAY_SOUND)
    }

    fun getCountry(): String{
        return Utils.getApp().resources.configuration.locale.country
    }

    fun getAvailableBytes(): String {
        val byteSize = StatFs(Environment.getExternalStorageDirectory().absolutePath).availableBytes
        return ConvertUtils.byte2FitMemorySize(byteSize)
    }

    fun getTotalBytes(): String {
        val byteSize = StatFs(Environment.getExternalStorageDirectory().absolutePath).totalBytes * 1.0f / MemoryConstants.GB
        if (byteSize <= 2f) {
            return "2GB"
        }else if (byteSize <= 4f) {
            return "4GB"
        }else if (byteSize <= 8f) {
            return "8GB"
        }else if (byteSize <= 16f) {
            return "16GB"
        }else if (byteSize <= 32f) {
            return "32GB"
        }else if (byteSize <= 64f) {
            return "64GB"
        }else if (byteSize <= 128f) {
            return "128GB"
        }else {
            return "128GB"
        }
    }

    fun getLogFilePath(): String {
        val currentTimeMillis = System.currentTimeMillis()
        val logFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CloudFrame" + "/log" + "/log_" + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentTimeMillis)
        if (!File(logFilePath).exists()) {
            val head = "************* Log Head ****************" +
                    "\nTime Of Log        : " + SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(currentTimeMillis) +
                    "\nDevice Manufacturer: " + Build.MANUFACTURER +
                    "\nDevice Model       : " + Build.MODEL +
                    "\nAndroid Version    : " + Build.VERSION.RELEASE +
                    "\nAndroid SDK        : " + Build.VERSION.SDK_INT +
                    "\nApp VersionName    : " + AppUtils.getAppVersionName() +
                    "\nApp VersionCode    : " + AppUtils.getAppVersionCode() +
                    "\nTotal Storage      : " + getTotalBytes() +
                    "\nAvailable Storage  : " + getAvailableBytes() +
                    "\nDevice Serialno    : " + snnumber +
                    "\n************* Log Head ****************\n\n"
            val result = FileIOUtils.writeFileFromString(logFilePath, head, true)
            if (result) {
                MyDatabase.instance.deviceLogDao.add(DeviceLog(logFilePath, "log"))
            }
        }
        return logFilePath
    }
}