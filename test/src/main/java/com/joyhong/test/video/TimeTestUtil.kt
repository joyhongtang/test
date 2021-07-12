package com.joyhong.test.video

import android.content.res.XmlResourceParser
import android.provider.Settings
import android.text.format.DateUtils

import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.Utils

import org.xmlpull.v1.XmlPullParserException

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object TimeTestUtil {

    private val TAG = TimeTestUtil::class.java.simpleName
    private const val TIMEZONE = "timezone"
    const val KEY_ID = "id"  // value: String
    const val KEY_NAME = "name"  // value: String
    const val KEY_GMT = "gmt"  // value: String
    private const val KEY_OFFSET = "offset"  // value: int (Integer)

    val TIME_FORMAT_MILLIS = TimeUtils.string2Millis("13:00", SimpleDateFormat("HH:mm", Locale.getDefault()))

    //是否自动从网络获取时间打开
    //是否自动从网络获取时间
    var autoTime: Int
        get() = Settings.Global.getInt(Utils.getApp().contentResolver, Settings.Global.AUTO_TIME)
        set(value) {
            Settings.Global.putInt(Utils.getApp().contentResolver, Settings.Global.AUTO_TIME, value)
        }

    //获取系统时区
    val timeZone: TimeZone
        get() = Calendar.getInstance(Locale.getDefault()).timeZone

    //判断系统是否是24小时制
    //设置系统24小时制
    var isTime24: Boolean
        get() {
            val time_12_24 = android.provider.Settings.System.getString(Utils.getApp().contentResolver, android.provider.Settings.System.TIME_12_24)
            return time_12_24 == "24"
        }
        set(time24) {
            Settings.System.putString(Utils.getApp().contentResolver, Settings.System.TIME_12_24, if (time24) "24" else "12")
        }


    //获取当前时间
    val curTime: String
        get() {
            val pattern = if (isTime24) "HH:mm" else "hh:mm a"
            return TimeUtils.millis2String(System.currentTimeMillis(), SimpleDateFormat(pattern, Locale.getDefault()))
        }

    private fun addTimeZoneItem(timeZoneList: MutableList<HashMap<String, Any>>, id: String, displayName: String, date: Long) {
        val hashMap = HashMap<String, Any>()
        hashMap[KEY_ID] = id
        hashMap[KEY_NAME] = displayName
        val timeZone = TimeZone.getTimeZone(id)
        val offset = timeZone.getOffset(date)
        val abs = Math.abs(offset)
        val name = StringBuilder()
        name.append("GMT")

        if (offset < 0) {
            name.append('-')
        } else {
            name.append('+')
        }

        name.append(abs / (60 * 60000))
        name.append(':')

        var min = abs / 60000
        min %= 60

        if (min < 10) {
            name.append('0')
        }
        name.append(min)

        hashMap[KEY_GMT] = name.toString()
        hashMap[KEY_OFFSET] = offset

        timeZoneList.add(hashMap)
    }

    fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000

        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours   = totalSeconds / 3600

        val formatter = Formatter(StringBuilder(), Locale.getDefault())

        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    //获取时间
    fun getTime(millis: Long): String {
        val pattern = if (isTime24) "HH:mm" else "hh:mm a"
        return TimeUtils.millis2String(millis, SimpleDateFormat(pattern, Locale.getDefault()))
    }

    //获取一年中的最后一天
    fun getLastDayOfYear(pattern: String): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR))
        return SimpleDateFormat(pattern, Locale.getDefault()).format(calendar.time)
    }
}