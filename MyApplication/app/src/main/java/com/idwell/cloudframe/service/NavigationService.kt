package com.idwell.cloudframe.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.db.entity.Alarm
import com.idwell.cloudframe.http.RetrofitManager
import com.idwell.cloudframe.http.entity.Forecast
import com.idwell.cloudframe.util.AlarmUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.math.BigDecimal

class NavigationService : Service(), LifecycleOwner {

    private val mLifecycleRegistry = LifecycleRegistry(this)
    private lateinit var cl_no_weather: ConstraintLayout
    private lateinit var iv_weather: ImageView
    private lateinit var tv_temperature: TextView
    private lateinit var tv_city: TextView
    private lateinit var tv_time: TextView
    private lateinit var tv_date: TextView
    private lateinit var tv_week: TextView

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action){
                Intent.ACTION_TIME_TICK, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                    LogUtils.dTag(TAG, intent.action)
                    refreshDateTime()
                }
                Intent.ACTION_LOCALE_CHANGED -> {
                    LogUtils.dTag(TAG, intent.action)
                    refreshDateTime()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        EventBus.getDefault().register(this)
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        initNavigation()
        //注册广播接收器
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_TICK)
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        filter.addAction(Intent.ACTION_LOCALE_CHANGED)
        registerReceiver(mReceiver, filter)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent) {
        when(event.message){
            MessageEvent.TIME_12_24 -> refreshDateTime()
        }
    }

    private fun initNavigation(){
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        //初始化窗口
        val layoutParams = WindowManager.LayoutParams()
        //前面有SYSTEM才可以遮挡状态栏，不然的话只能在状态栏下显示通知栏
        val sdkInt = Build.VERSION.SDK_INT
        when {
            sdkInt < Build.VERSION_CODES.KITKAT -> layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
            sdkInt < Build.VERSION_CODES.N_MR1 -> layoutParams.type = WindowManager.LayoutParams.TYPE_TOAST
            sdkInt < Build.VERSION_CODES.O -> layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
            else -> layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        layoutParams.format = PixelFormat.TRANSLUCENT
        //设置必须触摸通知栏才可以关掉
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        // 设置通知栏的长和宽
        layoutParams.width = 195
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.gravity = Gravity.END

        val navigation = View.inflate(this, R.layout.navigation, null)
        val cl_weather = navigation.findViewById<ConstraintLayout>(R.id.cl_weather_navigation)
        val cl_calendar = navigation.findViewById<ConstraintLayout>(R.id.cl_calendar_navigation)
        val cl_alarm = navigation.findViewById<ConstraintLayout>(R.id.cl_alarm_navigation)
        val rv_alarm = navigation.findViewById<RecyclerView>(R.id.rv_alarm_navigation)
        cl_no_weather = navigation.findViewById(R.id.cl_no_weather_navigation)
        iv_weather = navigation.findViewById(R.id.iv_weather_navigation)
        tv_temperature = navigation.findViewById(R.id.tv_temperature_navigation)
        tv_city = navigation.findViewById(R.id.tv_city_navigation)
        tv_time = navigation.findViewById(R.id.tv_time_navigation)
        tv_date = navigation.findViewById(R.id.tv_date_navigation)
        tv_week = navigation.findViewById(R.id.tv_week_navigation)

        windowManager.addView(navigation, layoutParams)

        val mAlarms = mutableListOf<Alarm>()
        val alarmAdapter = object : BaseQuickAdapter<Alarm, BaseViewHolder>(R.layout.item_alarm_navigation, mAlarms) {
            override fun convert(helper: BaseViewHolder, item: Alarm?) {
                if (item != null) {
                    helper.setText(R.id.tv_time_item_alarm_navigation, item.time)
                    helper.getView<Switch>(R.id.switch_item_alarm_navigation).isChecked = item.isChecked
                    helper.addOnClickListener(R.id.switch_item_alarm_navigation)
                }
            }
        }
        rv_alarm.layoutManager = LinearLayoutManager(this)
        rv_alarm.addItemDecoration(
            HorizontalItemDecoration(
                ContextCompat.getColor(this, R.color.divider)
            )
        )
        rv_alarm.adapter = alarmAdapter
        MyDatabase.instance.alarmDao.queryOrderAsc().observe({ this.lifecycle }, { alarms ->
            mAlarms.clear()
            alarms?.let { mAlarms.addAll(it) }
            alarmAdapter.notifyDataSetChanged()
        })
        if (Device.weather.isNotEmpty()) {
            refreshWeather()
        }
        Device.weatherState.observe({ lifecycle }, { state ->
            when (state) {
                1 -> {
                    refreshWeather()
                }
            }
        })
        refreshDateTime()
        alarmAdapter.setOnItemClickListener { adapter, view, position ->
            EventBus.getDefault().post(MessageEvent(MessageEvent.START_ACTIVITY_ALARM))
        }
        alarmAdapter.setOnItemChildClickListener { adapter, view, position ->
            GlobalScope.launch {
                val alarm = mAlarms[position]
                alarm.isChecked = !alarm.isChecked
                if (alarm.isChecked) {
                    AlarmUtil.startAlarm(alarm)
                } else {
                    AlarmUtil.cancelAlarm(alarm.id)
                }
                MyDatabase.instance.alarmDao.update(alarm)
            }
        }

        cl_weather.setOnClickListener {
            if (Device.weather.isEmpty()) {
                EventBus.getDefault().post(MessageEvent(MessageEvent.START_ACTIVITY_SEARCH_CITY))
            } else {
                EventBus.getDefault().post(MessageEvent(MessageEvent.START_ACTIVITY_WEATHER))
            }
        }
        cl_calendar.setOnClickListener {
            EventBus.getDefault().post(MessageEvent(MessageEvent.START_ACTIVITY_CALENDAR))
        }
        cl_alarm.setOnClickListener {
            EventBus.getDefault().post(MessageEvent(MessageEvent.START_ACTIVITY_ALARM))
        }
    }

    private fun refreshWeather() {
        cl_no_weather.visibility = View.INVISIBLE
        val weather = Gson().fromJson(Device.weather, Forecast::class.java)
        tv_city.text = weather.name
        val icon = RetrofitManager.WEATHER_ICON_URL + weather.cur_data.weather[0].icon + ".png"
        Glide.with(this).load(icon).into(iv_weather)
        tv_temperature.text = getString(if (Device.displayFahrenheit) R.string.temp_f else R.string.temp_c, if (Device.displayFahrenheit) k2f(weather.cur_data.main.temp) else k2c(weather.cur_data.main.temp))
    }

    private fun k2c(kelvin: Double): Double {
        return BigDecimal(kelvin - 273.15).setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
    }

    private fun k2f(kelvin: Double): Double {
        return BigDecimal((kelvin - 273.15) * 9 / 5 + 32).setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
    }

    private fun refreshDateTime(){
        tv_date.text = DateFormat.getDateFormat(this).format(System.currentTimeMillis())
        tv_time.text = DateFormat.getTimeFormat(this).format(System.currentTimeMillis())
        tv_week.text = DateFormat.format("EEEE", System.currentTimeMillis())
    }

    override fun getLifecycle(): Lifecycle {
        return mLifecycleRegistry
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
        EventBus.getDefault().unregister(this)
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    companion object {
        private val TAG = NavigationService::class.java.simpleName
    }
}