package com.idwell.cloudframe.util

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.blankj.utilcode.util.LogUtils

import com.blankj.utilcode.util.Utils
import com.google.gson.Gson
import com.idwell.cloudframe.common.MyConstants
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.db.entity.Alarm
import com.idwell.cloudframe.db.entity.AutoOnOff
import com.idwell.cloudframe.receiver.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.*

object AlarmUtil {

    private val TAG = AlarmUtil::class.java.simpleName

    fun startPower(autoOnOff: AutoOnOff) {
        val powerOffIntent = Intent(Utils.getApp(), AlarmReceiver::class.java)
        powerOffIntent.action = Device.ACTION_AUTO_OFF
        val powerOffPendingIntent = PendingIntent.getBroadcast(Utils.getApp(),
                MyConstants.POWER_OFF_ID, powerOffIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        val powerOnIntent = Intent(Utils.getApp(), AlarmReceiver::class.java)
        powerOnIntent.action = Device.ACTION_AUTO_ON
        val powerOnPendingIntent = PendingIntent.getBroadcast(Utils.getApp(),
                MyConstants.POWER_ON_ID, powerOnIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmManager = Utils.getApp().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // 取得下次响铃时间
        val powerOffTime = calcNextTime(autoOnOff.offHour, autoOnOff.offMinute, autoOnOff.repeat)
        // 当前版本为19（4.4）或以上使用精准闹钟
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, powerOffTime, powerOffPendingIntent)
        // 取得下次响铃时间
        val powerOnTime = calcNextTime(autoOnOff.onHour, autoOnOff.onMinute, autoOnOff.repeat)
        LogUtils.dTag(TAG, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(powerOnTime))
        // 当前版本为19（4.4）或以上使用精准闹钟
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, powerOnTime, powerOnPendingIntent)
    }

    fun cancelPower(powerId: Int) {
        val intent = Intent(Utils.getApp(), AlarmReceiver::class.java)
        when(powerId){
            MyConstants.POWER_OFF_ID -> {
                intent.action = Device.ACTION_AUTO_OFF
            }
            MyConstants.POWER_ON_ID -> {
                intent.action = Device.ACTION_AUTO_ON
            }
        }
        val pendingIntent = PendingIntent.getBroadcast(Utils.getApp(), powerId,
                intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val manager = Utils.getApp().getSystemService(Activity.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingIntent)
    }

    /**
     * 开启闹钟
     * @param alarm 闹钟实例
     */
    @TargetApi(19)
    fun startAlarm(alarm: Alarm) {
        val intent = Intent(Utils.getApp(), AlarmReceiver::class.java)
        intent.action = Device.ACTION_ALARM
        intent.putExtra(MyConstants.ALARM_CLOCK, Gson().toJson(alarm))
        val pendingIntent = PendingIntent.getBroadcast(Utils.getApp(),
                alarm.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmManager = Utils.getApp().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // 取得下次响铃时间
        val nextTime = calcNextTime(alarm.hour, alarm.minute, alarm.repeat)
        LogUtils.dTag(TAG, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(nextTime))
        // 当前版本为19（4.4）或以上使用精准闹钟
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
    }

    /**
     * 取消闹钟
     * @param alarmId 闹钟启动code
     */
    fun cancelAlarm(alarmId: Int) {
        val intent = Intent(Utils.getApp(), AlarmReceiver::class.java)
        intent.action = Device.ACTION_ALARM
        val pendingIntent = PendingIntent.getBroadcast(Utils.getApp(), alarmId,
                intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmManager = Utils.getApp().getSystemService(Activity.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    /**
     * 取得下次响铃时间
     *
     * @param hour        小时
     * @param minute      分钟
     * @param repeat 周
     * @return 下次响铃时间
     */
     fun calcNextTime(hour: Int, minute: Int, repeat: MutableList<Int>): Long {
        // 当前系统时间
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        // 下次响铃时间
        var nextTime: Long = 0
        // 当单次响铃时
        if (repeat.isEmpty()) {
            nextTime = calendar.timeInMillis
            // 当设置时间大于系统时间时
            if (nextTime < System.currentTimeMillis()) {
                // 设置的时间加一天
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                nextTime = calendar.timeInMillis
            }
            return nextTime
        } else {
            // 取得响铃重复周期
            for (index in repeat) {
                // 设置重复的周
                calendar.set(Calendar.DAY_OF_WEEK, index + 1)
                var tempTime = calendar.timeInMillis
                // 当设置时间小于等于当前系统时间时
                if (tempTime <= System.currentTimeMillis()) {
                    // 设置时间加7天
                    tempTime += AlarmManager.INTERVAL_DAY * 7
                }
                // 比较取得最小时间为下次响铃时间
                nextTime = if (nextTime == 0L) tempTime else Math.min(tempTime, nextTime)
            }
            return nextTime
        }
    }
}