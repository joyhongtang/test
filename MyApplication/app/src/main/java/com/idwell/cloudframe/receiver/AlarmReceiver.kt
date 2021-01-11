package com.idwell.cloudframe.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.SystemClock
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.idwell.cloudframe.common.MyConstants
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.db.entity.Alarm
import com.idwell.cloudframe.ui.MainActivity
import com.idwell.cloudframe.ui.RingActivity
import com.idwell.cloudframe.util.AlarmUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AlarmReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null){
            LogUtils.dTag(TAG, intent.action)
            when(intent.action){
                Device.ACTION_ALARM -> {
                    val alarm = Gson().fromJson(intent.getStringExtra(MyConstants.ALARM_CLOCK), Alarm::class.java)
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, alarm.hour)
                    calendar.set(Calendar.MINUTE, alarm.minute)
                    calendar.set(Calendar.SECOND, 0)
                    if (Math.abs(calendar.timeInMillis - System.currentTimeMillis()) < 15_000 && alarm.isChecked){
                        val mIntent = Intent(context, RingActivity::class.java)
                        mIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        mIntent.putExtra(MyConstants.ALARM_CLOCK, intent.getStringExtra(MyConstants.ALARM_CLOCK))
                        context.startActivity(mIntent)
                    }
                }
                Device.ACTION_AUTO_OFF -> {
                    GlobalScope.launch {
                        val autoOnOff = MyDatabase.instance.powerDao.query()
                        if (autoOnOff != null){
                            val calendar = Calendar.getInstance()
                            calendar.set(Calendar.HOUR_OF_DAY, autoOnOff.offHour)
                            calendar.set(Calendar.MINUTE, autoOnOff.offMinute)
                            calendar.set(Calendar.SECOND, 0)
                            if (Math.abs(calendar.timeInMillis - System.currentTimeMillis()) < 15_000 && autoOnOff.isChecked){
                                Device.powerState = "off"
                                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                                powerManager.goToSleep(SystemClock.uptimeMillis())
                                if (autoOnOff.repeat.isEmpty()){
                                    autoOnOff.isChecked =false
                                    MyDatabase.instance.powerDao.updatePower(autoOnOff)
                                }
                                launch(Dispatchers.Main) {
                                    ActivityUtils.finishToActivity(MainActivity::class.java, false)
                                }
                            }
                        }
                    }
                }
                Device.ACTION_AUTO_ON -> {
                    GlobalScope.launch {
                        val autoOnOff = MyDatabase.instance.powerDao.query()
                        if (autoOnOff != null) {
                            val calendar = Calendar.getInstance()
                            calendar.set(Calendar.HOUR_OF_DAY, autoOnOff.onHour)
                            calendar.set(Calendar.MINUTE, autoOnOff.onMinute)
                            calendar.set(Calendar.SECOND, 0)
                            LogUtils.dTag(TAG, "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.timeInMillis)}, ${Math.abs(calendar.timeInMillis - System.currentTimeMillis())}")
                            if (Math.abs(calendar.timeInMillis - System.currentTimeMillis()) < 15_000){
                                Device.powerState = "on"
                                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                                powerManager.wakeUp(SystemClock.uptimeMillis())
                                if (autoOnOff.repeat.isNotEmpty()){
                                    AlarmUtil.cancelPower(MyConstants.POWER_OFF_ID)
                                    AlarmUtil.cancelPower(MyConstants.POWER_ON_ID)
                                    AlarmUtil.startPower(autoOnOff)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = AlarmReceiver::class.java.simpleName
    }
}