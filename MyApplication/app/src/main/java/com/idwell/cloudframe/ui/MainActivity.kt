package com.idwell.cloudframe.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.*
import android.os.storage.VolumeInfo
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.text.format.DateFormat
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.sdk.android.push.CommonCallback
import com.alibaba.sdk.android.push.noonesdk.PushServiceFactory
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.FileUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.MediaStoreSignature
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.idwell.cloudframe.MyApplication
import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.common.Device.showedFloatingBall
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.common.MyConstants
import com.idwell.cloudframe.common.RotateTransformation
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.db.entity.Alarm
import com.idwell.cloudframe.entity.Photo
import com.idwell.cloudframe.http.RetrofitManager
import com.idwell.cloudframe.http.entity.Base
import com.idwell.cloudframe.http.entity.City
import com.idwell.cloudframe.http.entity.Forecast
import com.idwell.cloudframe.http.entity.Version
import com.idwell.cloudframe.http.service.VersionService
import com.idwell.cloudframe.http.service.WeatherService
import com.idwell.cloudframe.service.GlobalService
import com.idwell.cloudframe.service.MessageService
import com.idwell.cloudframe.service.NavigationService
import com.idwell.cloudframe.service.UploadFileService
import com.idwell.cloudframe.util.AlarmUtil
import com.idwell.cloudframe.util.MyLogUtils
import com.idwell.cloudframe.util.MyUtils
import com.idwell.cloudframe.util.PicUtils
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.widget.MaterialDialog
import com.idwell.cloudframe.widget.RingView
import com.joyhong.test.TestMainActivity
import com.joyhong.test.service.TestService
import com.joyhong.test.util.TestConstant
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener1
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : BaseActivity(), BaseQuickAdapter.OnItemClickListener {

    private var mPaused = false
    private var mMediaScannerRunnable: MediaScannerRunnable? = null
    private var mMediaScannerReceiver: MediaScannerReceiver? = null
    //外置存储
    private var mSdDir: String? = null
    private var mUsbDir: String? = null
    //检测新版本
    private var isCheckNewVersion = true
    private var mRingDialog: Dialog? = null
    private var mRingView: RingView? = null
    private var mDownloadTask: DownloadTask? = null
    //休眠唤醒管理
    private lateinit var mPowerManager: PowerManager
    private lateinit var mWakeLock: PowerManager.WakeLock
    //媒体浏览器
    private lateinit var mMediaBrowser: MediaBrowserCompat
    //媒体控制器
    private var mMediaController: MediaControllerCompat? = null

    private var mPhotos = mutableListOf<Photo>()
    private var mImagePosition = 2

    private val mWeatherDatas = mutableListOf<Forecast.DaysData>()
    private val mWeatherAdapter = object : BaseQuickAdapter<Forecast.DaysData, BaseViewHolder>(R.layout.item_weather_main, mWeatherDatas) {
        override fun convert(helper: BaseViewHolder, item: Forecast.DaysData) {
            //日期
            helper.setText(R.id.tv_date_item_weather_main, DateFormat.format("EEEE", item.max.dt * 1000L))
            //温度
            val tempMin = if (Device.displayFahrenheit) k2f(item.min.main.temp) else k2c(item.min.main.temp)
            val tempMax = if (Device.displayFahrenheit) k2f(item.max.main.temp) else k2c(item.max.main.temp)
            helper.setText(R.id.tv_temp_item_weather_main, mContext.getString(if (Device.displayFahrenheit) R.string.temp_f_range else R.string.temp_c_range, tempMin, tempMax))
            //天气图标
            val imageView = helper.getView<ImageView>(R.id.iv_weather_item_weather_main)
            val path = RetrofitManager.WEATHER_ICON_URL + item.max.weather[0].icon + ".png"
            Glide.with(mContext).load(path).into(imageView)
        }
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null) {
                LogUtils.dTag(LOG_TAG, intent.action)
                when (intent.action) {
                    Intent.ACTION_TIME_TICK -> {
                        refreshDateTime()
                        if (NetworkUtils.isConnected()) {
                            if (Device.weather.isNotEmpty()) {
                                LogUtils.dTag(LOG_TAG, Device.weather)
                                val weather = Gson().fromJson(Device.weather, Forecast::class.java)
                                if (System.currentTimeMillis() - weather.cur_data.dt * 1000L > 10_800_000) {
                                    getWeather(weather.name, weather.id)
                                }
                            } else if (Device.curCity.isNotEmpty()) {
                                val city = Gson().fromJson(Device.curCity, City::class.java)
                                getWeather(city)
                            }
                        }
                    }
                    Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                        refreshDateTime()
                        launch(Dispatchers.IO) {
                            val alarms = MyDatabase.instance.alarmDao.query()
                            for (alarm in alarms) {
                                if (alarm.isChecked) {
                                    LogUtils.dTag(LOG_TAG, alarm.time)
                                    AlarmUtil.cancelAlarm(alarm.id)
                                    AlarmUtil.startAlarm(alarm)
                                }
                            }
                            val autoOnOff = MyDatabase.instance.powerDao.query()
                            if (autoOnOff != null && autoOnOff.isChecked) {
                                AlarmUtil.cancelPower(MyConstants.POWER_OFF_ID)
                                AlarmUtil.cancelPower(MyConstants.POWER_ON_ID)
                                AlarmUtil.startPower(autoOnOff)
                            }
                        }
                    }
                    WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                        val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0)
                        when (wifiState) {
                            WifiManager.WIFI_STATE_ENABLED -> {
                                iv_wifi_main.setImageResource(R.drawable.ic_appwidget_settings_wifi_off_holo)
                            }
                            WifiManager.WIFI_STATE_DISABLED -> {
                                iv_wifi_main.setImageResource(R.drawable.ic_signal_wifi_off)
                            }
                        }
                    }
                    ConnectivityManager.CONNECTIVITY_ACTION -> {
                        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                        val activeNetworkInfo = connectivityManager.activeNetworkInfo
                        LogUtils.dTag(LOG_TAG, "$activeNetworkInfo")
                        if (activeNetworkInfo == null) {
                            iv_wifi_main.setImageResource(R.drawable.ic_appwidget_settings_wifi_off_holo)
                        } else if (activeNetworkInfo.isConnected) {
                            connected()
                            if (isCheckNewVersion) {
                                checkNewVersion()
                            }
                        }
                    }
                    WifiManager.RSSI_CHANGED_ACTION -> {
                        if (NetworkUtils.isConnected()) {
                            connected()
                        }
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        mWakeLock.acquire()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        mWakeLock.release()
                    }
                    "com.idwell.cloudframe.USER_ACTIVITY" -> {
                        val topActivity = ActivityUtils.getTopActivity()
                        if (!mPaused || topActivity is SettingsActivity || topActivity is SystemActivity) {
                            if (Device.slideshow > 0) {
                                mHandler.removeCallbacks(mSlideRunnable)
                                mHandler.postDelayed(mSlideRunnable, Device.slideshow)
                            }
                        }
                    }
                }
            }
        }
    }

    private val mVolumeStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null) {
                //Log.d("lcs", intent.action)
                when (intent.action) {
                    Intent.ACTION_MEDIA_MOUNTED -> {
                        LogUtils.dTag(LOG_TAG, intent.data?.path)
                        val path: String = intent.data?.path ?: ""
                        val toast = Toast(context)
                        val view = View.inflate(context, R.layout.toast_external_storage, null)
                        val imageView = view.findViewById<ImageView>(R.id.iv_toast_external_storage)
                        val textView = view.findViewById<TextView>(R.id.tv_toast_external_storage)
                        toast.view = view
                        toast.duration = Toast.LENGTH_SHORT
                        toast.setGravity(Gravity.CENTER, 0, 0)
                        if (path.contains("extsd")) {
                            imageView.setImageResource(R.drawable.ic_card_mounted)
                            textView.setText(R.string.storage_card_is_inserted)
                            toast.show()
                        } else if (path.contains("usb")) {
                            imageView.setImageResource(R.drawable.ic_usb_mounted)
                            textView.setText(R.string.usb_flash_disk_is_inserted)
                            toast.show()
                        }
                        //Log.d("lcs", "mVolumeStateReceiver: ${intent.action}, $path")
                    }
                    Intent.ACTION_MEDIA_REMOVED, Intent.ACTION_MEDIA_BAD_REMOVAL -> {
                        LogUtils.dTag(LOG_TAG, intent.data?.path)
                        val path: String = intent.data?.path ?: ""
                        val toast = Toast(context)
                        val view = View.inflate(context, R.layout.toast_external_storage, null)
                        val imageView = view.findViewById<ImageView>(R.id.iv_toast_external_storage)
                        val textView = view.findViewById<TextView>(R.id.tv_toast_external_storage)
                        toast.view = view
                        toast.duration = Toast.LENGTH_SHORT
                        toast.setGravity(Gravity.CENTER, 0, 0)
                        if (path.contains("extsd")) {
                            imageView.setImageResource(R.drawable.ic_card_unmounted)
                            textView.setText(R.string.storage_card_has_been_taken_out)
                            toast.show()
                        } else if (path.contains("usb")) {
                            imageView.setImageResource(R.drawable.ic_usb_unmounted)
                            textView.setText(R.string.usb_flash_disk_has_been_taken_out)
                            toast.show()
                        }
                        val mediaIntent = Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://${Environment.getExternalStorageDirectory()}"))
                        sendBroadcast(mediaIntent)
                        //Log.d("lcs", "mVolumeStateReceiver: ${intent.action}, $path")
                    }
                    VolumeInfo.ACTION_VOLUME_STATE_CHANGED -> {
                        val state = intent.getIntExtra(VolumeInfo.EXTRA_VOLUME_STATE, -1)
                        if (state == VolumeInfo.STATE_MOUNTED) {
                            LogUtils.dTag(LOG_TAG, state)
                            val sdDir = MyUtils.getSdDir()
                            val usbDir = MyUtils.getUsbDir()
                            if (mSdDir == null && sdDir != null) {
                                mSdDir = sdDir
                                //SD已插入
                                val toast = Toast(context)
                                val view = View.inflate(context, R.layout.toast_external_storage, null)
                                val imageView = view.findViewById<ImageView>(R.id.iv_toast_external_storage)
                                val textView = view.findViewById<TextView>(R.id.tv_toast_external_storage)
                                toast.view = view
                                toast.duration = Toast.LENGTH_SHORT
                                toast.setGravity(Gravity.CENTER, 0, 0)
                                imageView.setImageResource(R.drawable.ic_card_mounted)
                                textView.setText(R.string.storage_card_is_inserted)
                                toast.show()
                                //EventBus.getDefault().post(MessageEvent(MessageEvent.SD_IN))
                            } else if (mUsbDir == null && usbDir != null) {
                                mUsbDir = usbDir
                                //USB已插入
                                val toast = Toast(context)
                                val view = View.inflate(context, R.layout.toast_external_storage, null)
                                val imageView = view.findViewById<ImageView>(R.id.iv_toast_external_storage)
                                val textView = view.findViewById<TextView>(R.id.tv_toast_external_storage)
                                toast.view = view
                                toast.duration = Toast.LENGTH_SHORT
                                toast.setGravity(Gravity.CENTER, 0, 0)
                                imageView.setImageResource(R.drawable.ic_usb_mounted)
                                textView.setText(R.string.usb_flash_disk_is_inserted)
                                toast.show()
                                //EventBus.getDefault().post(MessageEvent(MessageEvent.USB_IN))
                            }
                        } else if (state == VolumeInfo.STATE_REMOVED || state == VolumeInfo.STATE_BAD_REMOVAL) {
                            LogUtils.dTag(LOG_TAG, state)
                            val sd = MyUtils.getSdDir()
                            val ud = MyUtils.getUsbDir()
                            if (mSdDir != null && sd == null) {
                                mSdDir = null
                                //SD已拔出
                                val toast = Toast(context)
                                val view = View.inflate(context, R.layout.toast_external_storage, null)
                                val imageView = view.findViewById<ImageView>(R.id.iv_toast_external_storage)
                                val textView = view.findViewById<TextView>(R.id.tv_toast_external_storage)
                                toast.view = view
                                toast.duration = Toast.LENGTH_SHORT
                                toast.setGravity(Gravity.CENTER, 0, 0)
                                imageView.setImageResource(R.drawable.ic_card_unmounted)
                                textView.setText(R.string.storage_card_has_been_taken_out)
                                toast.show()
                                //EventBus.getDefault().post(MessageEvent(MessageEvent.SD_OUT))
                            } else if (mUsbDir != null && ud == null) {
                                mUsbDir = null
                                //USB已拔出
                                val toast = Toast(context)
                                val view = View.inflate(context, R.layout.toast_external_storage, null)
                                val imageView = view.findViewById<ImageView>(R.id.iv_toast_external_storage)
                                val textView = view.findViewById<TextView>(R.id.tv_toast_external_storage)
                                toast.view = view
                                toast.duration = Toast.LENGTH_SHORT
                                toast.setGravity(Gravity.CENTER, 0, 0)
                                imageView.setImageResource(R.drawable.ic_usb_unmounted)
                                textView.setText(R.string.usb_flash_disk_has_been_taken_out)
                                toast.show()
                                //EventBus.getDefault().post(MessageEvent(MessageEvent.USB_OUT))
                            }
                        }
                    }
                }
            }
        }
    }

    private val mHandler = Handler()
    private var mPhotoRunnable: PhotoRunnable? = null
    private var mSlideRunnable: SlideRunnable? = null

    override fun initConfig() {
        if (Device.isFirstIn) {
            //启动引导页
            startActivity(Intent(this, GuideActivity::class.java))
        } else {
            //启动悬浮球
            if(!showedFloatingBall) {
                startService(Intent(this, GlobalService::class.java))
                showedFloatingBall = true
            }
            if (Device.isDualScreen) {
                startService(Intent(this, NavigationService::class.java))
            }
        }

        val intent = Intent(this, TestService::class.java)
        intent.setAction(TestService.ACTION_OPEN_WIFI)
        startService(intent)
    }

    override fun initLayout(): Int {
        showTopBar = false
        return R.layout.activity_main
    }

    override fun initData() {
        onOrientationChanged(resources?.configuration?.orientation ?: 2)

        mPhotoRunnable = PhotoRunnable()
        mSlideRunnable = SlideRunnable()
        mMediaScannerRunnable = MediaScannerRunnable()

        //初始化SD和USB路径
        mSdDir = MyUtils.getSdDir()
        mUsbDir = MyUtils.getUsbDir()
        //休眠唤醒管理
        mPowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG)
        mWakeLock.setReferenceCounted(false)
        //绑定音乐服务
        //mMediaBrowser = MediaBrowserCompat(this, ComponentName(this, MusicService::class.java), mMediaBrowserConnectionCallback, null)
        //mMediaBrowser.connect()
        //天气布局管理
        rv_content_weather_main.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rv_content_weather_main.addItemDecoration(HorizontalItemDecoration(ContextCompat.getColor(this, R.color.divider)))
        rv_content_weather_main.adapter = mWeatherAdapter
        //刷新日期时间
        refreshDateTime()
        //Log.e("GGGGG","CR3 "+Device.curCity)
        //刷新天气
        if (Device.weather.isNotEmpty()) {
            //Log.e("GGGGG","CR4 "+Device.curCity)
            refreshWeather()
        } else if (Device.curCity.isNotEmpty()) {
            //Log.e("GGGGG","CR5 "+Device.curCity)
            if (NetworkUtils.isConnected()) {
                //Log.e("GGGGG","CR2 "+Device.curCity)
                val city = Gson().fromJson(Device.curCity, City::class.java)
                getWeather(city)
            }
        }
        Device.weatherState.observe({ lifecycle }, { state ->
            when (state) {
                1 -> {
                    refreshWeather()
                }
            }
        })
        //初始化本地数据
        launch(Dispatchers.IO) {
            //初始化闹钟
            if (Device.isFirstIn) {
                val files = FileUtils.listFilesInDir("/system/media/audio/alarms")
                if (!files.isNullOrEmpty()) {
                    val alarmDao = MyDatabase.instance.alarmDao
                    alarmDao.insert(Alarm(0, mutableListOf(), files[0].absolutePath, getString(R.string.alarm), 8, 0, "08:00", false))
                }
            }
            val alarms = MyDatabase.instance.alarmDao.query()
            for (alarm in alarms) {
                if (alarm.isChecked) {
                    AlarmUtil.startAlarm(alarm)
                }
            }
            //初始化自动休眠唤醒
            val autoOnOff = MyDatabase.instance.powerDao.query()
            if (autoOnOff != null && autoOnOff.isChecked) {
                AlarmUtil.cancelPower(MyConstants.POWER_OFF_ID)
                AlarmUtil.cancelPower(MyConstants.POWER_ON_ID)
                AlarmUtil.startPower(autoOnOff)
            }
            //查询图片媒体库
            val images = queryImages()
            launch(Dispatchers.Main) {
                mPhotos.clear()
                mPhotos.addAll(images)
                LogUtils.dTag(LOG_TAG, mPhotos.size)
                showImages()
                if (mPhotos.size > 3) {
                    mHandler.removeCallbacks(mPhotoRunnable)
                    mHandler.postDelayed(mPhotoRunnable, 5000)
                }
            }
        }

        //注册广播接收器
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_TIME_TICK)
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED)
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction("com.idwell.cloudframe.USER_ACTIVITY")
        registerReceiver(mBroadcastReceiver, intentFilter)
        //注册外部存储广播接收器
        val volumeStateFilter = IntentFilter()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            volumeStateFilter.addAction(VolumeInfo.ACTION_VOLUME_STATE_CHANGED)
        } else {
            volumeStateFilter.addAction(Intent.ACTION_MEDIA_MOUNTED)
            volumeStateFilter.addAction(Intent.ACTION_MEDIA_REMOVED)
            volumeStateFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
            volumeStateFilter.addDataScheme("file")
        }
        registerReceiver(mVolumeStateReceiver, volumeStateFilter)

        // 注册广播接收器
        val msIntentFilter = IntentFilter()
        msIntentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED)
        msIntentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED)
        msIntentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        msIntentFilter.addAction(MyConstants.ACTION_MEDIA_SCANNER_SCAN_FILE)
        msIntentFilter.addDataScheme("file")
        mMediaScannerReceiver = MediaScannerReceiver()
        registerReceiver(mMediaScannerReceiver, msIntentFilter)

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(mBatteryReceiver, filter)
        cl_photo_main.postDelayed(Runnable {
            startService(Intent(this, MessageService::class.java))
            //启动log上传服务
            startService(Intent(this, UploadFileService::class.java))
            readConfig()
            //初始化人体感应
            if (Device.hasHumanSensor) {
                setMotionSensor()
            }

        },5*1000)
    }

    override fun initListener() {
        cl_photo_main.setOnClickListener(this)
        cl_music_main.setOnClickListener(this)
        cl_weather_main.setOnClickListener(this)
        cl_alarm_main.setOnClickListener(this)
        cl_clock_main.setOnClickListener(this)
        cl_settings_main.setOnClickListener(this)
        cl_video_main.setOnClickListener(this)
        cl_calendar_main.setOnClickListener(this)
        mWeatherAdapter.onItemClickListener = this
    }

    override fun initMessageEvent(event: MessageEvent) {
        when (event.message) {
            MessageEvent.TIME_12_24 -> {
                refreshDateTime()
            }
            MessageEvent.SLIDE_AUTOPLAY_TIME_CHANGED -> {
                mHandler.removeCallbacks(mSlideRunnable)
                if (Device.slideshow > 0) {
                    mHandler.postDelayed(mSlideRunnable, Device.slideshow)
                }
            }
            MessageEvent.START_ACTIVITY_PHOTO -> {
                val intent = Intent(this, PhotoActivity::class.java)
                intent.putExtra("userId", event.number)
                startActivity(intent)
            }
            MessageEvent.START_ACTIVITY_VIDEO -> {
                val intent = Intent(this, VideoActivity::class.java)
                intent.putExtra("userId", event.number)
                intent.putExtra("filePath", event.text)
                startActivity(intent)
            }
            MessageEvent.START_ACTIVITY_ALARM -> {
                startActivity(Intent(this, AlarmActivity::class.java))
                ActivityUtils.finishActivity(CalendarActivity::class.java)
                ActivityUtils.finishActivity(WeatherActivity::class.java)
                ActivityUtils.finishActivity(SearchCityActivity::class.java)
            }
            MessageEvent.START_ACTIVITY_CALENDAR -> {
                startActivity(Intent(this, CalendarActivity::class.java))
                ActivityUtils.finishActivity(AlarmActivity::class.java)
                ActivityUtils.finishActivity(WeatherActivity::class.java)
                ActivityUtils.finishActivity(SearchCityActivity::class.java)
            }
            MessageEvent.START_ACTIVITY_WEATHER -> {
                startActivity(Intent(this, WeatherActivity::class.java))
                ActivityUtils.finishActivity(AlarmActivity::class.java)
                ActivityUtils.finishActivity(CalendarActivity::class.java)
                ActivityUtils.finishActivity(SearchCityActivity::class.java)
            }
            MessageEvent.START_ACTIVITY_SEARCH_CITY -> {
                startActivity(Intent(this, SearchCityActivity::class.java))
                ActivityUtils.finishActivity(AlarmActivity::class.java)
                ActivityUtils.finishActivity(CalendarActivity::class.java)
                ActivityUtils.finishActivity(WeatherActivity::class.java)
            }
            MessageEvent.START_FRAGMENT_USER_MANAGEMENT -> {
                val intent = Intent(this, SettingsActivity::class.java)
                intent.putExtra("position", 1)
                startActivity(intent)
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (Device.userActivityListenerMode == 0) {
            when (ev?.action) {
                MotionEvent.ACTION_DOWN -> {
                    mHandler.removeCallbacks(mSlideRunnable)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                    if (Device.slideshow > 0) {
                        mHandler.removeCallbacks(mSlideRunnable)
                        mHandler.postDelayed(mSlideRunnable, Device.slideshow)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v) {
            cl_photo_main -> {
                startActivity(Intent(this, PhotoActivity::class.java))
            }
            cl_music_main -> {
                startActivity(Intent(this, MusicActivity::class.java))
            }
            cl_weather_main -> {
                if (Device.weather.isEmpty()) {
                    startActivity(Intent(this, SearchCityActivity::class.java))
                } else {
                    startActivity(Intent(this, WeatherActivity::class.java))
                }
            }
            cl_alarm_main -> {
                startActivity(Intent(this, AlarmActivity::class.java))
            }
            cl_clock_main -> {
                if (Device.clockMode == -1) {
                    Device.clockMode = 0
                    startActivity(Intent(this, ClockSkinActivity::class.java))
                } else {
                    startActivity(Intent(this, ClockActivity::class.java))
                }
            }
            cl_settings_main -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            cl_video_main -> {
                startActivity(Intent(this, VideoActivity::class.java))
            }
            cl_calendar_main -> {
                startActivity(Intent(this, CalendarActivity::class.java))
            }
        }
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        if (Device.weather.isEmpty()) {
            startActivity(Intent(this, SearchCityActivity::class.java))
        } else {
            startActivity(Intent(this, WeatherActivity::class.java))
        }
    }

    private fun playPhoto() {
        if (++mImagePosition > mPhotos.size - 1) {
            mImagePosition = 0
        }
        var photo = mPhotos[mImagePosition]
        if (File(photo.data).exists()) {
            //val description = Gson().fromJson(photo.description, Description::class.java) ?: Description()
            when {
                mImagePosition % 3 == 0 -> {
                    val widthHeight = MyUtils.convertWidthHeight(photo, iv_photo1_main.width, iv_photo1_main.height)
                    val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .override(widthHeight[0], widthHeight[1])
                        .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                        .format(DecodeFormat.PREFER_ARGB_8888)
                    Glide.with(this@MainActivity).load(photo.data).apply(requestOptions)
                        .transform(RotateTransformation(photo.orientation))
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(iv_photo1_main)
                }
                mImagePosition % 3 == 1 -> {
                    val widthHeight = MyUtils.convertWidthHeight(photo, iv_photo2_main.width, iv_photo2_main.height)
                    val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .override(widthHeight[0], widthHeight[1])
                        .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                        .format(DecodeFormat.PREFER_ARGB_8888)
                    Glide.with(this@MainActivity).load(photo.data).apply(requestOptions)
                        .transform(RotateTransformation(photo.orientation))
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(iv_photo2_main)
                }
                else -> {
                    val widthHeight = MyUtils.convertWidthHeight(photo, iv_photo3_main.width, iv_photo3_main.height)
                    val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .override(widthHeight[0], widthHeight[1])
                        .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                        .format(DecodeFormat.PREFER_ARGB_8888)
                    Glide.with(this@MainActivity).load(photo.data).apply(requestOptions)
                        .transform(RotateTransformation(photo.orientation))
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(iv_photo3_main)
                }
            }
            mHandler.removeCallbacks(mPhotoRunnable)
            mHandler.postDelayed(mPhotoRunnable, 5000)
        } else {
            launch(Dispatchers.IO) {
                val images = queryImages()
                launch(Dispatchers.Main) {
                    mPhotos.clear()
                    mPhotos.addAll(images)
                    if (mPhotos.size > 3) {
                        if (mImagePosition > mPhotos.size - 1) {
                            mImagePosition = 0
                        }
                        photo = mPhotos[mImagePosition]
                        //val description = Gson().fromJson(photo.description, Description::class.java) ?: Description()
                        when {
                            mImagePosition % 3 == 0 -> {
                                val widthHeight = MyUtils.convertWidthHeight(photo, iv_photo1_main.width, iv_photo1_main.height)
                                val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                    .override(widthHeight[0], widthHeight[1])
                                    .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                                    .format(DecodeFormat.PREFER_ARGB_8888)
                                Glide.with(this@MainActivity).load(photo.data).apply(requestOptions)
                                    .transform(RotateTransformation(photo.orientation))
                                    .transition(DrawableTransitionOptions.withCrossFade(300))
                                    .into(iv_photo1_main)
                            }
                            mImagePosition % 3 == 1 -> {
                                val widthHeight = MyUtils.convertWidthHeight(photo, iv_photo2_main.width, iv_photo2_main.height)
                                val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                    .override(widthHeight[0], widthHeight[1])
                                    .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                                    .format(DecodeFormat.PREFER_ARGB_8888)
                                Glide.with(this@MainActivity).load(photo.data).apply(requestOptions)
                                    .transform(RotateTransformation(photo.orientation))
                                    .transition(DrawableTransitionOptions.withCrossFade(300))
                                    .into(iv_photo2_main)
                            }
                            else -> {
                                val widthHeight = MyUtils.convertWidthHeight(photo, iv_photo3_main.width, iv_photo3_main.height)
                                val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                    .override(widthHeight[0], widthHeight[1])
                                    .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                                    .format(DecodeFormat.PREFER_ARGB_8888)
                                Glide.with(this@MainActivity).load(photo.data).apply(requestOptions)
                                    .transform(RotateTransformation(photo.orientation))
                                    .transition(DrawableTransitionOptions.withCrossFade(300))
                                    .into(iv_photo3_main)
                            }
                        }
                        mHandler.removeCallbacks(mPhotoRunnable)
                        mHandler.postDelayed(mPhotoRunnable, 5000)
                    } else {
                        showImages()
                    }
                }
            }
        }
    }

    private fun connected() {
        @SuppressLint("WifiManagerLeak") val manager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = manager.connectionInfo
        //根据获得的信号强度发送信息
        val level = WifiManager.calculateSignalLevel(info.rssi, 4)
        when (level) {
            0 -> iv_wifi_main.setImageResource(R.drawable.ic_wifi_signal_1_dark)
            1 -> iv_wifi_main.setImageResource(R.drawable.ic_wifi_signal_2_dark)
            2 -> iv_wifi_main.setImageResource(R.drawable.ic_wifi_signal_3_dark)
            3 -> iv_wifi_main.setImageResource(R.drawable.ic_wifi_signal_4_dark)
        }
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

    private fun refreshWeather() {
        cl_no_weather_main.visibility = View.INVISIBLE
        cl_content_weather_main.visibility = View.VISIBLE
        val weather = Gson().fromJson(Device.weather, Forecast::class.java)
        tv_city_current_content_weather_main.text = weather.name
        val icon = RetrofitManager.WEATHER_ICON_URL + weather.cur_data.weather[0].icon + ".png"
        Glide.with(this).load(icon).into(iv_weather_current_content_weather_main)
        tv_temp_current_content_weather_main.text = getString(if (Device.displayFahrenheit) R.string.temp_f else R.string.temp_c, if (Device.displayFahrenheit) k2f(weather.cur_data.main.temp) else k2c(weather.cur_data.main.temp))
        mWeatherDatas.clear()
        if (weather.days_data.size > 3) {
            for (i in 0..2) {
                mWeatherDatas.add(weather.days_data[i])
            }
        } else {
            mWeatherDatas.addAll(weather.days_data)
        }
        mWeatherAdapter.notifyDataSetChanged()
    }

    private fun refreshDateTime() {
        tv_time_main.text = DateFormat.getTimeFormat(this).format(System.currentTimeMillis())
        tv_month_main.text = DateFormat.format("MMMM", System.currentTimeMillis())
        tv_day_main.text = DateFormat.format("dd", System.currentTimeMillis())
    }

    private fun getWeather(city: City) {
        RetrofitManager.getService(WeatherService::class.java)
            .weather(city.lat, city.lon, SimpleDateFormat("ZZZZ", Locale.getDefault()).format(System.currentTimeMillis()))
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Base<Forecast>> {
                override fun onComplete() {

                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: Base<Forecast>) {
                    if (t != null) {
                        val status = t.status
                        val forecast = t.data
                        if (status == 200 && forecast != null) {
                            forecast.name = city.name
                            Device.weather = Gson().toJson(forecast)
                            Device.weatherState.postValue(1)
                        }
                    }
                }

                override fun onError(e: Throwable) {

                }
            })
    }

    private fun getWeather(name: String, city_id: String) {
        RetrofitManager.getService(WeatherService::class.java)
            .weather(city_id, SimpleDateFormat("ZZZZ", Locale.getDefault()).format(System.currentTimeMillis()))
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Base<Forecast>> {
                override fun onComplete() {

                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: Base<Forecast>) {
                    if (t != null) {
                        val status = t.status
                        val forecast = t.data
                        if (status == 200 && forecast != null) {
                            forecast.name = name
                            Device.weather = Gson().toJson(forecast)
                            Device.weatherState.postValue(1)
                        }
                    }
                }

                override fun onError(e: Throwable) {

                }
            })
    }

    private val mMediaBrowserConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            if (mMediaBrowser.isConnected) {
                //mediaId即为MediaBrowserService.onGetRoot的返回值
                //若Service允许客户端连接，则返回结果不为null，其值为数据内容层次结构的根ID
                //若拒绝连接，则返回null
                val mediaId = mMediaBrowser.root

                //Browser通过订阅的方式向Service请求数据，发起订阅请求需要两个参数，其一为mediaId
                //而如果该mediaId已经被其他Browser实例订阅，则需要在订阅之前取消mediaId的订阅者
                //虽然订阅一个 已被订阅的mediaId 时会取代原Browser的订阅回调，但却无法触发onChildrenLoaded回调

                //ps：虽然基本的概念是这样的，但是Google在官方demo中有这么一段注释...
                // This is temporary: A bug is being fixed that will make subscribe
                // consistently call onChildrenLoaded initially, no matter if it is replacing an existing
                // subscriber or not. Currently this only happens if the mediaID has no previous
                // subscriber or if the media content changes on the service side, so we need to
                // unsubscribe first.
                //大概的意思就是现在这里还有BUG，即只要发送订阅请求就会触发onChildrenLoaded回调
                //所以无论怎样我们发起订阅请求之前都需要先取消订阅
                mMediaBrowser.unsubscribe(mediaId)
                //之前说到订阅的方法还需要一个参数，即设置订阅回调SubscriptionCallback
                //当Service获取数据后会将数据发送回来，此时会触发SubscriptionCallback.onChildrenLoaded回调
                mMediaBrowser.subscribe(mediaId, object : MediaBrowserCompat.SubscriptionCallback() {})
                mMediaController = MediaControllerCompat(this@MainActivity, mMediaBrowser.sessionToken)
                mMediaController?.registerCallback(object : MediaControllerCompat.Callback() {})
            }
        }
    }

    private fun refreshImages() {
        launch(Dispatchers.IO) {
            val images = queryImages()
            launch(Dispatchers.Main) {
                mPhotos.clear()
                mPhotos.addAll(images)
                LogUtils.dTag(LOG_TAG, mPhotos.size)
                if (mPhotos.size > 3) {
                    mHandler.removeCallbacks(mPhotoRunnable)
                    mHandler.postDelayed(mPhotoRunnable, 5000)
                } else {
                    showImages()
                }
            }
        }
    }

    private fun queryImages(): MutableList<Photo> {
        val images = mutableListOf<Photo>()
        val selection = "${MediaStore.Images.ImageColumns.DATA} is not null"
        val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.SIZE, MediaStore.Images.ImageColumns.DISPLAY_NAME, MediaStore.Images.ImageColumns.TITLE, MediaStore.Images.ImageColumns.DATE_MODIFIED, MediaStore.Images.ImageColumns.MIME_TYPE, MediaStore.Images.ImageColumns.WIDTH, MediaStore.Images.ImageColumns.HEIGHT, MediaStore.Images.ImageColumns.DESCRIPTION, MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.Images.ImageColumns.ORIENTATION), selection, null, "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC")
        while (cursor?.moveToNext() == true) {
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID))
            val data = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)) ?: ""
            val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns.SIZE))
            val displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME)) ?: ""
            val title = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.TITLE)) ?: ""
            val date_modified = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_MODIFIED))
            val mime_type = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.MIME_TYPE)) ?: ""
            val width = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.WIDTH))
            val height = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT))
            val description = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DESCRIPTION)) ?: ""
            val datetaken = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN))
            val orientation = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION))
            val photo = Photo(id, data, size, displayName, title, date_modified, mime_type, width, height, description, datetaken, orientation)
//            images.add(photo)
            //增加bmp转jpg进行显示
            if(Device.configBmpConvertJpg) {
                try {
                    if (photo.data.toLowerCase().contains(".bmp")) {
                        var file = PicUtils.ImgToJPG(File(photo.data))
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
                try {
                    if (!photo.data.toLowerCase().contains(".bmp")) {
                        images.add(photo)
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }else{
                images.add(photo)
            }
        }
        cursor?.close()
        return images
    }

    private fun queryImage(_data: String) {
        launch(Dispatchers.IO) {
            delay(200)
            var photo: Photo? = null
            val selection = "${MediaStore.Images.ImageColumns.DATA} = '$_data'"
            val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.SIZE, MediaStore.Images.ImageColumns.DISPLAY_NAME, MediaStore.Images.ImageColumns.TITLE, MediaStore.Images.ImageColumns.DATE_MODIFIED, MediaStore.Images.ImageColumns.MIME_TYPE, MediaStore.Images.ImageColumns.WIDTH, MediaStore.Images.ImageColumns.HEIGHT, MediaStore.Images.ImageColumns.DESCRIPTION, MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.Images.ImageColumns.ORIENTATION), selection, null, null)
            while (cursor?.moveToNext() == true) {
                val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID))
                val data = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)) ?: ""
                val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns.SIZE))
                val displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME)) ?: ""
                val title = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.TITLE)) ?: ""
                val date_modified = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_MODIFIED))
                val mime_type = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.MIME_TYPE)) ?: ""
                val width = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.WIDTH))
                val height = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT))
                val description = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DESCRIPTION)) ?: ""
                val datetaken = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN))
                val orientation = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION))
                photo = Photo(id, data, size, displayName, title, date_modified, mime_type, width, height, description, datetaken, orientation)
            }
            cursor?.close()
            //Log.d("ooo", "photo: ${photo?.data}")
            if (photo != null) {
                launch(Dispatchers.Main) {
                    if (mPhotos.contains(photo)) {
                        val index = mPhotos.indexOf(photo)
                        mPhotos[index] = photo
                    } else {
                        mPhotos.add(photo)
                        mPhotos.sort()
                    }
                    if (mPhotos.size > 3) {
                        if (!mPaused) {
                            mHandler.removeCallbacks(mPhotoRunnable)
                            mHandler.postDelayed(mPhotoRunnable, 5000)
                        }
                    } else {
                        showImages()
                    }
                }
            }
        }
    }

    private fun countImages(): Int {
        val selection = "${MediaStore.Images.ImageColumns.DATA} is not null"
        val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf("count(*)"), selection, null, null)
        cursor?.moveToFirst()
        val count = cursor?.getInt(0) ?: 0
        cursor?.close()
        return count
    }

    private fun countImages(selection: String): Int {
        val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf("count(*)"), "${MediaStore.Images.ImageColumns.DATA} is not null and $selection", null, null)
        cursor?.moveToFirst()
        val count = cursor?.getInt(0) ?: 0
        cursor?.close()
        return count
    }

    private fun showImages() {
        mHandler.removeCallbacks(mPhotoRunnable)
        mImagePosition = 2
        when {
            mPhotos.size == 0 -> {
                iv_photo1_main.setImageResource(R.drawable.photo1_main)
                iv_photo2_main.setImageResource(R.drawable.photo2_main)
                iv_photo3_main.setImageResource(R.drawable.photo3_main)
            }
            mPhotos.size == 1 -> {
                val photo0 = mPhotos[0]
                //val description0 = Gson().fromJson(photo0.description, Description::class.java) ?: Description()
//                val widthHeight = MyUtils.convertWidthHeight(photo0, iv_photo1_main.width, iv_photo1_main.height)
                val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(300,300)
                    .signature(MediaStoreSignature(photo0.mime_type, photo0.date_modified, photo0.orientation))
                    .format(DecodeFormat.PREFER_ARGB_8888)
                Glide.with(this).load(photo0.data).apply(requestOptions)
                    .transform(RotateTransformation(photo0.orientation))
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(iv_photo1_main)
                iv_photo2_main.setImageResource(R.drawable.photo2_main)
                iv_photo3_main.setImageResource(R.drawable.photo3_main)
            }
            mPhotos.size == 2 -> {
                val photo0 = mPhotos[0]
                //val description0 = Gson().fromJson(photo0.description, Description::class.java) ?: Description()
                val photo1 = mPhotos[1]
                //val description1 = Gson().fromJson(photo1.description, Description::class.java) ?: Description()
//                val widthHeight0 = MyUtils.convertWidthHeight(photo0, iv_photo1_main.width, iv_photo1_main.height)
                val requestOptions0 = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(300,300)
                    .signature(MediaStoreSignature(photo0.mime_type, photo0.date_modified, photo0.orientation))
                    .format(DecodeFormat.PREFER_ARGB_8888)
                Glide.with(this).load(photo0.data).apply(requestOptions0)
                    .transform(RotateTransformation(photo0.orientation))
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(iv_photo1_main)

//                val widthHeight1 = MyUtils.convertWidthHeight(photo1, iv_photo2_main.width, iv_photo2_main.height)
                val requestOptions1 = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(300,300)
                    .signature(MediaStoreSignature(photo1.mime_type, photo1.date_modified, photo1.orientation))
                    .format(DecodeFormat.PREFER_ARGB_8888)
                Glide.with(this).load(photo1.data).apply(requestOptions1)
                    .transform(RotateTransformation(photo1.orientation))
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(iv_photo2_main)
                iv_photo3_main.setImageResource(R.drawable.photo3_main)
            }
            mPhotos.size > 2 -> {
                val photo0 = mPhotos[0]
                //val description0 = Gson().fromJson(photo0.description, Description::class.java) ?: Description()
                val photo1 = mPhotos[1]
                //val description1 = Gson().fromJson(photo1.description, Description::class.java) ?: Description()
                val photo2 = mPhotos[2]
                //val description2 = Gson().fromJson(photo2.description, Description::class.java) ?: Description()
//                val widthHeight0 = MyUtils.convertWidthHeight(photo0, iv_photo1_main.width, iv_photo1_main.height)
                val requestOptions0 = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(300,300)
                    .signature(MediaStoreSignature(photo0.mime_type, photo0.date_modified, photo0.orientation))
                    .format(DecodeFormat.PREFER_ARGB_8888)
                Glide.with(this).load(photo0.data).error(R.drawable.photo1_main)
                    .apply(requestOptions0).transform(RotateTransformation(photo0.orientation))
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(iv_photo1_main)

//                val widthHeight1 = MyUtils.convertWidthHeight(photo1, iv_photo2_main.width, iv_photo2_main.height)
                val requestOptions1 = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(300,300)
                    .signature(MediaStoreSignature(photo1.mime_type, photo1.date_modified, photo1.orientation))
                    .format(DecodeFormat.PREFER_ARGB_8888)
                Glide.with(this).load(photo1.data).error(R.drawable.photo2_main)
                    .apply(requestOptions1).transform(RotateTransformation(photo1.orientation))
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(iv_photo2_main)

//                val widthHeight2 = MyUtils.convertWidthHeight(photo1, iv_photo3_main.width, iv_photo3_main.height)
                val requestOptions2 = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(300,300)
                    .signature(MediaStoreSignature(photo1.mime_type, photo1.date_modified, photo1.orientation))
                    .format(DecodeFormat.PREFER_ARGB_8888)
                Glide.with(this).load(photo2.data).error(R.drawable.photo3_main)
                    .apply(requestOptions2).transform(RotateTransformation(photo2.orientation))
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(iv_photo3_main)
            }
        }
    }

    /**
     * 检测新版本
     */
    private fun checkNewVersion() {
        RetrofitManager.getService(VersionService::class.java)
            .version(Device.id, AppUtils.getAppVersionCode()).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Base<Version>> {
                override fun onComplete() {

                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(t: Base<Version>) {
                    if (t != null) {
                        val status = t.status
                        val data = t.data
                        if (status == 200 && data != null) {
                            isCheckNewVersion = false
                            if (Device.versionCode == data.last_version && Device.versionCode > AppUtils.getAppVersionCode() && File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CloudPhotoFrame.apk").exists()) {
                                MaterialDialog.Builder(this@MainActivity)
                                    .setTitle(R.string.install_new_version)
                                    .setContent(data.version_desc)
                                    .setNegativeButton(R.string.cancel, null)
                                    .setPositiveButton(R.string.install, object : MaterialDialog.OnClickListener {
                                        override fun onClick(dialog: MaterialDialog) {
                                            installApp()
                                        }
                                    }).show()
                            } else {
                                if (AppUtils.getAppVersionCode() < data.last_version) {
                                    MaterialDialog.Builder(this@MainActivity)
                                        .setTitle(R.string.find_new_version)
                                        .setContent(data.version_desc)
                                        .setNegativeButton(R.string.cancel, null)
                                        .setPositiveButton(R.string.download, object : MaterialDialog.OnClickListener {
                                            override fun onClick(dialog: MaterialDialog) {
                                                FileUtils.delete(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CloudPhotoFrame.apk"))
                                                downloadApk(data)
                                            }
                                        }).show()
                                }
                            }
                        }
                    }
                }

                override fun onError(e: Throwable) {

                }
            })
    }

    private fun showRingDialog() {
        if (mRingDialog == null) {
            val view = View.inflate(this, R.layout.view_ring, null)
            mRingView = view.findViewById(R.id.rv_view_ring)
            mRingDialog = Dialog(this, R.style.LoadingDialog)
            mRingDialog?.setContentView(view)
            mRingDialog?.setCanceledOnTouchOutside(false)
        } else {
            mRingView?.setProgress(0)
        }
        mRingDialog?.show()
    }

    /**
     * 下载APK
     */
    private fun downloadApk(data: Version) {
        /*RetrofitManager.getProgressService(object : ProgressListener {
            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                mRingView.setProgress((bytesRead * 100 / contentLength).toInt())
            }
        }, DownloadService::class.java).download(data.download_link).map { body ->
            //保存文件
            FileIOUtils.writeFileFromBytesByStream(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CloudPhotoFrame.apk"), body.bytes())
            ""
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : BaseObserver<Any>() {
                    override fun onSubscribe(d: Disposable) {
                        showRingDialog()
                    }

                    override fun onNext(t: Any) {
                        Device.versionCode = data.last_version
                        Device.versionDesc = data.version_desc
                        dismissRingDialog()
                        installApp()
                    }

                    override fun onError(e: Throwable) {
                        dismissRingDialog()
                    }
                })*/
        mDownloadTask = DownloadTask.Builder(data.download_link, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            .setFilename("CloudPhotoFrame.apk").setMinIntervalMillisCallbackProcess(200)
            .setPassIfAlreadyCompleted(false).build()
        mDownloadTask?.enqueue(object : DownloadListener1() {
            override fun taskStart(task: DownloadTask, model: Listener1Assist.Listener1Model) {
                MyLogUtils.file("$task")
                showRingDialog()
            }

            override fun taskEnd(task: DownloadTask, cause: EndCause, realCause: Exception?, model: Listener1Assist.Listener1Model) {
                MyLogUtils.file("$task, cause = $cause, realCause = $realCause")
                when (cause) {
                    EndCause.COMPLETED -> {
                        Device.versionCode = data.last_version
                        Device.versionDesc = data.version_desc
                        dismissRingDialog()
                        installApp()
                    }
                    else -> {
                        dismissRingDialog()
                    }
                }
            }

            override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
                mRingView?.setProgress((currentOffset * 100f / totalLength).toInt())
            }

            override fun connected(task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long) {
                MyLogUtils.file("$task, blockCount = $blockCount, currentOffset = $currentOffset, totalLength = $totalLength")
            }

            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
                MyLogUtils.file("$task, cause = $cause")
            }
        })
    }

    private fun installApp() {
        //普通安装
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.fromFile(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CloudPhotoFrame.apk")), "application/vnd.android.package-archive")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        //静默安装
        /*GlobalScope.launch {
            val command = "pm install -r -i com.idwell.cloudframe --user 0 ${context?.filesDir}/CloudPhotoFrame.apk"
            Runtime.getRuntime().exec(command)
        }*/
    }

    private fun dismissRingDialog() {
        if (mRingDialog == null) return
        mRingDialog?.dismiss()
    }

    private fun onOrientationChanged(orientation: Int) {
        val constraintSet = ConstraintSet()
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            constraintSet.connect(R.id.cl_photo_main, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.cl_photo_main, ConstraintSet.END, R.id.cl_music_video_main, ConstraintSet.START)
            constraintSet.connect(R.id.cl_photo_main, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_photo_main, ConstraintSet.BOTTOM, R.id.cl_alarm_clock_settings_main, ConstraintSet.TOP)
            constraintSet.setHorizontalWeight(R.id.cl_photo_main, 0.57f)
            constraintSet.setVerticalWeight(R.id.cl_photo_main, 0.6f)

            constraintSet.setMargin(R.id.cl_alarm_clock_settings_main, 3, ConvertUtils.dp2px(20f))
            constraintSet.connect(R.id.cl_alarm_clock_settings_main, ConstraintSet.START, R.id.cl_photo_main, ConstraintSet.START)
            constraintSet.connect(R.id.cl_alarm_clock_settings_main, ConstraintSet.END, R.id.cl_photo_main, ConstraintSet.END)
            constraintSet.connect(R.id.cl_alarm_clock_settings_main, ConstraintSet.TOP, R.id.cl_photo_main, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.cl_alarm_clock_settings_main, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.setVerticalWeight(R.id.cl_alarm_clock_settings_main, 0.4f)
            val acsConstraintSet = ConstraintSet()
            acsConstraintSet.connect(R.id.cl_alarm_main, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            acsConstraintSet.connect(R.id.cl_alarm_main, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            acsConstraintSet.connect(R.id.cl_alarm_main, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            acsConstraintSet.connect(R.id.cl_alarm_main, ConstraintSet.END, R.id.cl_clock_main, ConstraintSet.START)
            acsConstraintSet.setHorizontalWeight(R.id.cl_alarm_main, 1.0f)
            acsConstraintSet.setVisibility(R.id.cl_clock_main, View.GONE)
            //acsConstraintSet.setMargin(R.id.cl_clock_main, 6, ConvertUtils.dp2px(20f))
            acsConstraintSet.connect(R.id.cl_clock_main, ConstraintSet.TOP, R.id.cl_alarm_main, ConstraintSet.TOP)
            acsConstraintSet.connect(R.id.cl_clock_main, ConstraintSet.BOTTOM, R.id.cl_alarm_main, ConstraintSet.BOTTOM)
            acsConstraintSet.connect(R.id.cl_clock_main, ConstraintSet.START, R.id.cl_alarm_main, ConstraintSet.END)
            acsConstraintSet.connect(R.id.cl_clock_main, ConstraintSet.END, R.id.cl_settings_main, ConstraintSet.START)
            acsConstraintSet.setHorizontalWeight(R.id.cl_clock_main, 1.0f)
            acsConstraintSet.setMargin(R.id.cl_settings_main, 6, ConvertUtils.dp2px(20f))
            acsConstraintSet.connect(R.id.cl_settings_main, ConstraintSet.TOP, R.id.cl_alarm_main, ConstraintSet.TOP)
            acsConstraintSet.connect(R.id.cl_settings_main, ConstraintSet.BOTTOM, R.id.cl_alarm_main, ConstraintSet.BOTTOM)
            acsConstraintSet.connect(R.id.cl_settings_main, ConstraintSet.START, R.id.cl_clock_main, ConstraintSet.END)
            acsConstraintSet.connect(R.id.cl_settings_main, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            acsConstraintSet.setHorizontalWeight(R.id.cl_settings_main, 2.0f)
            acsConstraintSet.applyTo(cl_alarm_clock_settings_main)

            constraintSet.setMargin(R.id.cl_music_video_main, 6, ConvertUtils.dp2px(20f))
            constraintSet.connect(R.id.cl_music_video_main, ConstraintSet.TOP, R.id.cl_photo_main, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_music_video_main, ConstraintSet.BOTTOM, R.id.cl_alarm_clock_settings_main, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.cl_music_video_main, ConstraintSet.START, R.id.cl_photo_main, ConstraintSet.END)
            constraintSet.connect(R.id.cl_music_video_main, ConstraintSet.END, R.id.cl_weather_calendar_main, ConstraintSet.START)
            constraintSet.setHorizontalWeight(R.id.cl_music_video_main, 0.2f)
            val mvConstraintSet = ConstraintSet()
            mvConstraintSet.setVisibility(R.id.cl_music_main, View.GONE)
            mvConstraintSet.constrainWidth(R.id.cl_music_main, ConstraintSet.MATCH_CONSTRAINT)
            mvConstraintSet.constrainHeight(R.id.cl_music_main, 0)
            mvConstraintSet.connect(R.id.cl_music_main, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            mvConstraintSet.connect(R.id.cl_music_main, ConstraintSet.BOTTOM, R.id.cl_video_main, ConstraintSet.TOP)
            mvConstraintSet.setHorizontalWeight(R.id.cl_music_main, 1.0f)
            mvConstraintSet.constrainWidth(R.id.cl_video_main, ConstraintSet.MATCH_CONSTRAINT)
            mvConstraintSet.constrainHeight(R.id.cl_music_main, 0)
            mvConstraintSet.connect(R.id.cl_video_main, ConstraintSet.TOP, R.id.cl_music_main, ConstraintSet.BOTTOM)
            mvConstraintSet.connect(R.id.cl_video_main, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            mvConstraintSet.connect(R.id.cl_video_main, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            mvConstraintSet.connect(R.id.cl_video_main, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            mvConstraintSet.setHorizontalWeight(R.id.cl_video_main, 1.0f)
            mvConstraintSet.applyTo(cl_music_video_main)

            constraintSet.connect(R.id.cl_weather_calendar_main, ConstraintSet.TOP, R.id.cl_music_video_main, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_weather_calendar_main, ConstraintSet.BOTTOM, R.id.cl_music_video_main, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.cl_weather_calendar_main, ConstraintSet.START, R.id.cl_music_video_main, ConstraintSet.END)
            constraintSet.connect(R.id.cl_weather_calendar_main, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            if (Device.isDualScreen) {
                constraintSet.setHorizontalWeight(R.id.cl_weather_calendar_main, 0.0f)
            } else {
                constraintSet.setMargin(R.id.cl_weather_calendar_main, 6, ConvertUtils.dp2px(20f))
                constraintSet.setHorizontalWeight(R.id.cl_weather_calendar_main, 0.23f)
            }
        } else {
            constraintSet.connect(R.id.cl_photo_main, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.cl_photo_main, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.cl_photo_main, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_photo_main, ConstraintSet.BOTTOM, R.id.cl_alarm_clock_settings_main, ConstraintSet.TOP)
            constraintSet.setHorizontalWeight(R.id.cl_photo_main, 1.0f)
            constraintSet.setVerticalWeight(R.id.cl_photo_main, 0.34f)

            constraintSet.setMargin(R.id.cl_alarm_clock_settings_main, 3, ConvertUtils.dp2px(20f))
            constraintSet.connect(R.id.cl_alarm_clock_settings_main, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.cl_alarm_clock_settings_main, ConstraintSet.END, R.id.cl_music_video_main, ConstraintSet.START)
            constraintSet.connect(R.id.cl_alarm_clock_settings_main, ConstraintSet.TOP, R.id.cl_photo_main, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.cl_alarm_clock_settings_main, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.setHorizontalWeight(R.id.cl_alarm_clock_settings_main, 1.0f)
            constraintSet.setVerticalWeight(R.id.cl_alarm_clock_settings_main, 0.66f)
            val acsConstraintSet = ConstraintSet()
            acsConstraintSet.connect(R.id.cl_alarm_main, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            acsConstraintSet.connect(R.id.cl_alarm_main, ConstraintSet.BOTTOM, R.id.cl_clock_main, ConstraintSet.TOP)
            acsConstraintSet.connect(R.id.cl_alarm_main, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            acsConstraintSet.connect(R.id.cl_alarm_main, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            acsConstraintSet.setVerticalWeight(R.id.cl_alarm_main, 1.0f)
            acsConstraintSet.setVisibility(R.id.cl_clock_main, View.GONE)
            //acsConstraintSet.setMargin(R.id.cl_clock_main, 3, ConvertUtils.dp2px(20f))
            acsConstraintSet.connect(R.id.cl_clock_main, ConstraintSet.TOP, R.id.cl_alarm_main, ConstraintSet.BOTTOM)
            acsConstraintSet.connect(R.id.cl_clock_main, ConstraintSet.BOTTOM, R.id.cl_settings_main, ConstraintSet.TOP)
            acsConstraintSet.connect(R.id.cl_clock_main, ConstraintSet.START, R.id.cl_alarm_main, ConstraintSet.START)
            acsConstraintSet.connect(R.id.cl_clock_main, ConstraintSet.END, R.id.cl_alarm_main, ConstraintSet.END)
            acsConstraintSet.setVerticalWeight(R.id.cl_clock_main, 1.0f)
            acsConstraintSet.setMargin(R.id.cl_settings_main, 3, ConvertUtils.dp2px(20f))
            acsConstraintSet.connect(R.id.cl_settings_main, ConstraintSet.TOP, R.id.cl_clock_main, ConstraintSet.BOTTOM)
            acsConstraintSet.connect(R.id.cl_settings_main, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            acsConstraintSet.connect(R.id.cl_settings_main, ConstraintSet.START, R.id.cl_alarm_main, ConstraintSet.START)
            acsConstraintSet.connect(R.id.cl_settings_main, ConstraintSet.END, R.id.cl_alarm_main, ConstraintSet.END)
            acsConstraintSet.setVerticalWeight(R.id.cl_settings_main, 2.0f)
            acsConstraintSet.applyTo(cl_alarm_clock_settings_main)

            constraintSet.setMargin(R.id.cl_music_video_main, 6, ConvertUtils.dp2px(20f))
            constraintSet.connect(R.id.cl_music_video_main, ConstraintSet.TOP, R.id.cl_alarm_clock_settings_main, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_music_video_main, ConstraintSet.BOTTOM, R.id.cl_alarm_clock_settings_main, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.cl_music_video_main, ConstraintSet.START, R.id.cl_alarm_clock_settings_main, ConstraintSet.END)
            constraintSet.connect(R.id.cl_music_video_main, ConstraintSet.END, R.id.cl_weather_calendar_main, ConstraintSet.START)
            constraintSet.setHorizontalWeight(R.id.cl_music_video_main, 1.0f)
            val mvConstraintSet = ConstraintSet()
            mvConstraintSet.setVisibility(R.id.cl_music_main, View.GONE)
            mvConstraintSet.constrainWidth(R.id.cl_music_main, ConstraintSet.MATCH_CONSTRAINT)
            mvConstraintSet.constrainHeight(R.id.cl_music_main, 0)
            mvConstraintSet.connect(R.id.cl_music_main, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            mvConstraintSet.connect(R.id.cl_music_main, ConstraintSet.BOTTOM, R.id.cl_video_main, ConstraintSet.TOP)
            mvConstraintSet.setHorizontalWeight(R.id.cl_music_main, 1.0f)
            mvConstraintSet.constrainWidth(R.id.cl_video_main, ConstraintSet.MATCH_CONSTRAINT)
            mvConstraintSet.constrainHeight(R.id.cl_music_main, 0)
            mvConstraintSet.connect(R.id.cl_video_main, ConstraintSet.TOP, R.id.cl_music_main, ConstraintSet.BOTTOM)
            mvConstraintSet.connect(R.id.cl_video_main, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            mvConstraintSet.connect(R.id.cl_video_main, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            mvConstraintSet.connect(R.id.cl_video_main, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            mvConstraintSet.setHorizontalWeight(R.id.cl_video_main, 1.0f)
            mvConstraintSet.applyTo(cl_music_video_main)

            constraintSet.setMargin(R.id.cl_weather_calendar_main, 6, ConvertUtils.dp2px(20f))
            constraintSet.connect(R.id.cl_weather_calendar_main, ConstraintSet.TOP, R.id.cl_music_video_main, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_weather_calendar_main, ConstraintSet.BOTTOM, R.id.cl_music_video_main, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.cl_weather_calendar_main, ConstraintSet.START, R.id.cl_music_video_main, ConstraintSet.END)
            constraintSet.connect(R.id.cl_weather_calendar_main, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.setHorizontalWeight(R.id.cl_weather_calendar_main, 1.0f)
        }
        constraintSet.applyTo(cl_content_main)
    }

    override fun onBackPressed() {

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onOrientationChanged(newConfig.orientation)
    }

    override fun onResume() {
        super.onResume()
        if (Device.slideshow > 0) {
            mHandler.removeCallbacks(mSlideRunnable)
            mHandler.postDelayed(mSlideRunnable, Device.slideshow)
        }
        if (mPaused) {
            if (mPhotos.size == countImages()) {
                if (mPhotos.size > 3) {
                    mHandler.removeCallbacks(mPhotoRunnable)
                    mHandler.postDelayed(mPhotoRunnable, 5000)
                }
            } else {
                mHandler.removeCallbacks(mMediaScannerRunnable)
                mHandler.post(mMediaScannerRunnable)
            }
        }
        mPaused = false
    }

    override fun onPause() {
        super.onPause()
        mPaused = true
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        mDownloadTask?.cancel()
        //mMediaBrowser.disconnect()
        //mMediaController = null

        mPhotoRunnable = null
        mSlideRunnable = null
        mMediaScannerRunnable = null

        unregisterReceiver(mBroadcastReceiver)
        unregisterReceiver(mVolumeStateReceiver)
        unregisterReceiver(mMediaScannerReceiver)
    }

    companion object {
        private val LOG_TAG = MainActivity::class.java.simpleName
    }

    inner class PhotoRunnable : Runnable {
        override fun run() {
            if (mPaused) {
                return
            }
            playPhoto()
        }
    }

    inner class SlideRunnable : Runnable {
        override fun run() {
            if (mPaused) {
                return
            }
            when {
                countImages("${MediaStore.Images.ImageColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'") > 0 -> {
                    val intent = Intent(this@MainActivity, SlideActivity::class.java)
                    intent.putExtra("tab position", 0)
                    startActivity(intent)
                }
                countImages("${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getSdDir()}/%'") > 0 -> {
                    val intent = Intent(this@MainActivity, SlideActivity::class.java)
                    intent.putExtra("tab position", 1)
                    startActivity(intent)
                }
                countImages("${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getUsbDir()}/%'") > 0 -> {
                    val intent = Intent(this@MainActivity, SlideActivity::class.java)
                    intent.putExtra("tab position", 2)
                    startActivity(intent)
                }
            }
        }
    }

    inner class MediaScannerRunnable : Runnable {
        override fun run() {
            //Log.d("lcs", "MediaScannerRunnable: ${mPhotos.size}, ${countImages()}")
            if (mPhotos.size != countImages()) {
                refreshImages()
                mHandler.postDelayed(mMediaScannerRunnable, 3000)
            }
        }
    }

    inner class MediaScannerReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //Log.d("ooo", intent?.action + ", " + intent?.data + ", " + intent?.data?.path + ", " + intent?.dataString + ", " + intent?.extras)
            if (intent != null) {
                if (intent.action == Intent.ACTION_MEDIA_SCANNER_STARTED) {
                    mHandler.removeCallbacks(mMediaScannerRunnable)
                    mHandler.postDelayed(mMediaScannerRunnable, 3000)
                } else if (intent.action == Intent.ACTION_MEDIA_SCANNER_FINISHED) {
                    mHandler.removeCallbacks(mMediaScannerRunnable)
                    if (mPhotos.size != countImages()) {
                        refreshImages()
                    }
                } else if (intent.action == Intent.ACTION_MEDIA_SCANNER_SCAN_FILE || intent.action == MyConstants.ACTION_MEDIA_SCANNER_SCAN_FILE) {
                    //Log.d("ooo", "${File(intent.data?.path).exists()}, ${mPhotos.size}, ${countImages()}")
                    val path = intent.data?.path
                    if (path != null) {
                        if (File(path).exists()) {
                            queryImage(path)
                        } else {
                            val photo = Photo(path)
                            if (mPhotos.contains(photo)) {
                                mPhotos.remove(photo)
                            }
                            if (mPhotos.size <= 3) {
                                showImages()
                            }
                        }
                    }
                }
            }
        }
    }


    //获取电池状况
    // 声明广播接受者对象
    private var mBatteryReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // TODO Auto-generated method stub
            val action = intent.action
            if (action == Intent.ACTION_BATTERY_CHANGED) {
                // 得到电池状态：
                // BatteryManager.BATTERY_STATUS_CHARGING：充电状态。
                // BatteryManager.BATTERY_STATUS_DISCHARGING：放电状态。
                // BatteryManager.BATTERY_STATUS_NOT_CHARGING：未充满。
                // BatteryManager.BATTERY_STATUS_FULL：充满电。
                // BatteryManager.BATTERY_STATUS_UNKNOWN：未知状态。
                val status = intent.getIntExtra("status", 0)
                // 得到健康状态：
                // BatteryManager.BATTERY_HEALTH_GOOD：状态良好。
                // BatteryManager.BATTERY_HEALTH_DEAD：电池没有电。
                // BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE：电池电压过高。
                // BatteryManager.BATTERY_HEALTH_OVERHEAT：电池过热。
                // BatteryManager.BATTERY_HEALTH_UNKNOWN：未知状态。
                val health = intent.getIntExtra("health", 0)
                // boolean类型
                val present = intent.getBooleanExtra("present", false)
                // 得到电池剩余容量
                val level = intent.getIntExtra("level", 0)
                // 得到电池最大值。通常为100。
                val scale = intent.getIntExtra("scale", 0)
                // 得到图标ID
                val icon_small = intent.getIntExtra("icon-small", 0)
                // 充电方式：　BatteryManager.BATTERY_PLUGGED_AC：AC充电。　BatteryManager.BATTERY_PLUGGED_USB：USB充电。
                val plugged = intent.getIntExtra("plugged", 0)
                var pluggedInfo = "UNKNOW"
                if (plugged == BatteryManager.BATTERY_PLUGGED_AC) {
                    pluggedInfo = "AC"
                } else if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
                    pluggedInfo = "USB"
                }
                // 得到电池的电压
                val voltage = intent.getIntExtra("voltage", 0)
                // 得到电池的温度,0.1度单位。例如 表示197的时候，意思为19.7度
                val temperature = intent.getIntExtra("temperature", 0)
                // 得到电池的类型
                val technology = intent.getStringExtra("technology")
                // 得到电池状态
                var statusString = ""
                when (status) {
                    BatteryManager.BATTERY_STATUS_UNKNOWN -> statusString = "UNKNOWN"
                    BatteryManager.BATTERY_STATUS_CHARGING -> statusString = "CHARGING"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> statusString = "DISCHARGING"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> statusString = "NOT_CHARGING"
                    BatteryManager.BATTERY_STATUS_FULL -> statusString = "FULL"
                }
                //得到电池的寿命状态
                var healthString = ""
                when (health) {
                    BatteryManager.BATTERY_HEALTH_UNKNOWN -> healthString = "UNKNOWN"
                    BatteryManager.BATTERY_HEALTH_GOOD -> healthString = "GOOD"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> healthString = "OVERHEAT"
                    BatteryManager.BATTERY_HEALTH_DEAD -> healthString = "HEALTH_DEAD"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> healthString = "OVER_VOLTAGE"
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> healthString = "UNSPECIFIED_FAILURE"
                }
                //得到充电模式
                var acString = ""
                when (plugged) {
                    BatteryManager.BATTERY_PLUGGED_AC -> acString = "AC 充电"
                    BatteryManager.BATTERY_PLUGGED_USB -> acString = "USB 充电"
                }
                val batteryInfoDetail = """
                    Battery_status:$statusString
                    Battery_health:$healthString
                    Battery_level:$level
                    Battery_max:$scale
                    Battery_plugged:$pluggedInfo
                    Battery_voltage:$voltage
                    Battery_temperature:${temperature.toFloat() * 0.1}
                    Battery_technology:$technology
                    """.trimIndent()
                SPUtils.getInstance().put("com.joyhong.test.BatteryInfoActivity" + "_detail", batteryInfoDetail)
            }
        }
    }

    private fun readConfig(){
        var inputReader: InputStreamReader? = null
        var bufReader: BufferedReader? = null
        try {
            inputReader = InputStreamReader(resources.assets.open("config.txt"))
            bufReader = BufferedReader(inputReader)
            var line = ""
            while (bufReader.readLine().also { line = it } != null) {
                if (line.contains("UnlimitedData_0")) {
                    Device.isUnlimitedData = false
                } else if (line.contains("Sdcard_0")) {
                    Device.isConfigSdcard = false
                } else if (line.contains("USB_0")) {
                    Device.isConfigUSBStorage = false
                }else if (line.contains("G-sensor_0")) {
                    Device.hasGravitySensor = false
                }else if (line.contains("Human-sensor_0")) {
                    Device.hasHumanSensor = false
                }else if (line.contains("AutoBrightness_0")) {
                    Device.autoBrightness = false
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            try {
                if (null != bufReader) {
                    bufReader.close()
                    bufReader = null
                }
                if (null != inputReader) {
                    inputReader.close()
                    inputReader = null
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }
}