package com.idwell.cloudframe.ui

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.*
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.blankj.utilcode.util.LogUtils

import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.adapter.DateTimeAdapter
import com.idwell.cloudframe.base.BaseFragment
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.entity.MultipleItem
import com.idwell.cloudframe.util.TimeUtil
import com.idwell.cloudframe.widget.MaterialDialog
import com.idwell.cloudframe.widget.MyTimePickerDialog
import kotlinx.android.synthetic.main.fragment_recyclerview.*

import org.greenrobot.eventbus.EventBus
import java.util.*

class DateTimeFragment : BaseFragment(), BaseQuickAdapter.OnItemClickListener {

    private lateinit var dateTimeAdapter: DateTimeAdapter
    private lateinit var data: MutableList<MultipleItem>
    private lateinit var cities: MutableList<String>
    private lateinit var gmts: MutableList<String>
    private lateinit var ids: MutableList<String>
    private lateinit var dateFormats: MutableList<String>

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_TIME_TICK, Intent.ACTION_TIME_CHANGED -> {
                    LogUtils.dTag(TAG, intent.action)
                    data[1].content = TimeUtil.curDate
                    data[2].content = TimeUtil.curTime
                    dateTimeAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recyclerview, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        LogUtils.dTag("DateTimeFragment", "initData")
        cities = mutableListOf()
        gmts = mutableListOf()
        ids = mutableListOf()
        dateFormats = ArrayList()
        val formats = Arrays.asList(*resources.getStringArray(R.array.date_format_values))
        for (format in formats) {
            dateFormats.add(TimeUtil.getLastDayOfYear(format))
        }
        val timeZoneList = TimeUtil.timeZoneList
        LogUtils.dTag("DateTimeFragment", "$timeZoneList")
        for (hashMap in timeZoneList) {
            cities.add(hashMap[TimeUtil.KEY_NAME] as String)
            gmts.add(hashMap[TimeUtil.KEY_GMT] as String)
            ids.add(hashMap[TimeUtil.KEY_ID] as String)
        }
        LogUtils.dTag("DateTimeFragment", "$ids")
        val position = ids.indexOf(TimeUtil.timeZone.id)
        LogUtils.dTag("DateTimeFragment", "${TimeUtil.timeZone}, ${TimeUtil.timeZone.id}")
        LogUtils.dTag("DateTimeFragment", TimeZone.getDefault())
        LogUtils.dTag("DateTimeFragment", Calendar.getInstance().getTimeZone())
        LogUtils.dTag("DateTimeFragment", "${TimeZone.getDefault().getDisplayName(true, TimeZone.SHORT)}")
        data = mutableListOf(MultipleItem(DateTimeAdapter.TEXTST_TEXTSB_SWITCHEC, getString(R.string.automatic_date_time), getString(R.string.use_network_provided_time), TimeUtil.autoTime == 1), MultipleItem(DateTimeAdapter.TEXTST_TEXTSB, getString(R.string.set_date), TimeUtil.curDate, TimeUtil.autoTime == 1), MultipleItem(DateTimeAdapter.TEXTST_TEXTSB, getString(R.string.set_time), TimeUtil.curTime, TimeUtil.autoTime == 1), MultipleItem(DateTimeAdapter.TEXTST_TEXTSB, getString(R.string.select_time_zone), gmts[position] + ", " + cities[position]), MultipleItem(DateTimeAdapter.TEXTST_TEXTSB_SWITCHEC, getString(R.string.use_24_hour_format), TimeUtil.getTime(TimeUtil.TIME_FORMAT_MILLIS), TimeUtil.isTime24), MultipleItem(DateTimeAdapter.TEXTST_TEXTSB, getString(R.string.choose_date_format), TimeUtil.lastDayOfYear))
        dateTimeAdapter = DateTimeAdapter(data)
        rv_fragment_recyclerview.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        context?.let {
            rv_fragment_recyclerview.addItemDecoration(HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider)))
        }
        rv_fragment_recyclerview.adapter = dateTimeAdapter

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_TICK)
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        context?.registerReceiver(mReceiver, filter)
    }

    override fun initListener() {
        dateTimeAdapter.onItemClickListener = this
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        val mCalendar: Calendar
        when (position) {
            0 -> {
                val isAutoTime = TimeUtil.autoTime == 1
                TimeUtil.autoTime = if (isAutoTime) 0 else 1
                data[0].isChecked = !isAutoTime
                data[1].isChecked = !isAutoTime
                data[2].isChecked = !isAutoTime
                dateTimeAdapter.notifyDataSetChanged()
            }
            1 -> {
                if (TimeUtil.autoTime == 0) {
                    mCalendar = Calendar.getInstance(Locale.getDefault())
                    context?.let {
                        val datePickerDialog = DatePickerDialog(it, R.style.DateTimePicker, DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                            mCalendar.set(Calendar.YEAR, year)
                            mCalendar.set(Calendar.MONTH, month)
                            mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            val alarmManager = it.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            alarmManager.setTime(mCalendar.timeInMillis)
                        }, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH))
                        datePickerDialog.show()
                        datePickerDialog.datePicker.calendarViewShown = false
                        datePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE).textSize = 24f
                        datePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE).textSize = 24f
                    }
                }
            }
            2 -> {
                if (TimeUtil.autoTime == 0) {
                    mCalendar = Calendar.getInstance(Locale.getDefault())
                    context?.let {
                        val timePickerDialog = MyTimePickerDialog(it, R.style.DateTimePicker, TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                            mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            mCalendar.set(Calendar.MINUTE, minute)
                            val alarmManager = it.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            alarmManager.setTime(mCalendar.timeInMillis)
                        }, mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE), TimeUtil.isTime24)
                        timePickerDialog.show()
                        timePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE).textSize = 24f
                        timePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE).textSize = 24f
                    }
                }
            }
            3 -> context?.let {
                var index = ids.indexOf(TimeUtil.timeZone.id)
                MaterialDialog.Builder(it).setTitle(R.string.select_time_zone)
                        .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textst_textsb_imageec_dialog, cities) {
                            override fun convert(helper: BaseViewHolder, item: String) {
                                val adapterPosition = helper.adapterPosition
                                helper.setText(R.id.tv_title_item_textst_textsb_imageec_dialog, cities[adapterPosition])
                                helper.setText(R.id.tv_content_item_textst_textsb_imageec_dialog, gmts[adapterPosition])
                                Glide.with(mContext)
                                        .load(if (index == adapterPosition) R.drawable.ic_check_circle_blue else R.drawable.gray_ring_shape)
                                        .into(helper.getView<View>(R.id.iv_item_textst_textsb_imageec_dialog) as ImageView)
                            }
                        }, BaseQuickAdapter.OnItemClickListener { adapter, view, position1 ->
                            index = position1
                            adapter.notifyDataSetChanged()
                        }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content)))
                        .setLayoutParamsHeight().setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                            override fun onClick(dialog: MaterialDialog) {
                                data[3].content = gmts[index] + "," + cities[index]
                                dateTimeAdapter.notifyDataSetChanged()
                                val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                alarmManager.setTimeZone(ids[index])
                                data[1].content = TimeUtil.curDate
                                data[2].content = TimeUtil.curTime
                                dateTimeAdapter.notifyDataSetChanged()
                            }
                        }).show()
                        .findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_content_dialog_material)
                        .scrollToPosition(index)
            }
            4 -> {
                val time24 = TimeUtil.isTime24
                TimeUtil.isTime24 = !time24
                data[2].content = TimeUtil.curTime
                data[4].content = TimeUtil.getTime(TimeUtil.TIME_FORMAT_MILLIS)
                data[4].isChecked = !time24
                dateTimeAdapter.notifyDataSetChanged()
                EventBus.getDefault().post(MessageEvent(MessageEvent.TIME_12_24))
            }
            5 -> context?.let {
                var index = Device.dateFormatIndex
                MaterialDialog.Builder(it).setTitle(R.string.choose_date_format)
                        .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textsc_imageec_dialog, dateFormats) {
                            override fun convert(helper: BaseViewHolder, item: String) {
                                val adapterPosition = helper.adapterPosition
                                helper.setText(R.id.tv_title_item_textsc_imageec_dialog, item)
                                Glide.with(mContext)
                                        .load(if (index == adapterPosition) R.drawable.ic_check_circle_blue else R.drawable.gray_ring_shape)
                                        .into(helper.getView<View>(R.id.iv_item_textsc_imageec_dialog) as ImageView)
                            }
                        }, BaseQuickAdapter.OnItemClickListener { adapter, view, position1 ->
                            index = position1
                            adapter.notifyDataSetChanged()
                        }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content)))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                            override fun onClick(dialog: MaterialDialog) {
                                if (Device.dateFormatIndex != index) {
                                    Device.dateFormatIndex = index
                                    data[1].content = TimeUtil.curDate
                                    data[5].content = dateFormats[index]
                                    dateTimeAdapter.notifyDataSetChanged()
                                }
                            }
                        }).show()
            }
        }
    }

    override fun onDestroy() {
        context?.unregisterReceiver(mReceiver)
        super.onDestroy()
    }

    companion object {
        private val TAG = DateTimeFragment::class.java.simpleName
    }
}