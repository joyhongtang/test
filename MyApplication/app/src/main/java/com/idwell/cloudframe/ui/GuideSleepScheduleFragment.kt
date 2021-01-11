package com.idwell.cloudframe.ui

import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder

import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseFragment
import com.idwell.cloudframe.common.MyConstants
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.db.entity.AutoOnOff
import com.idwell.cloudframe.entity.BaseItem
import com.idwell.cloudframe.util.AlarmUtil
import com.idwell.cloudframe.util.TimeUtil
import kotlinx.android.synthetic.main.fragment_guide_sleep_schedule.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class GuideSleepScheduleFragment : BaseFragment(), BaseQuickAdapter.OnItemClickListener {

    private lateinit var mAutoOnOff: AutoOnOff
    private val mSleepScheduleData = mutableListOf<BaseItem>()
    private var mSleepScheduleAdapter: BaseQuickAdapter<BaseItem, BaseViewHolder>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_guide_sleep_schedule, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        MyDatabase.instance.powerDao.queryLiveData().observe({ this.lifecycle }, { autoOnOff ->
            if (autoOnOff == null) {
                mAutoOnOff = AutoOnOff(-1, 8, 0, 22, 0, mutableListOf(0, 1, 2, 3, 4, 5, 6), true)
                GlobalScope.launch { MyDatabase.instance.powerDao.insertPower(mAutoOnOff) }
            } else {
                mAutoOnOff = autoOnOff
                if (mSleepScheduleAdapter == null) {
                    initRecyclerView()
                } else {
                    mSleepScheduleData[0].content = formatTime(mAutoOnOff.onHour, mAutoOnOff.onMinute)
                    mSleepScheduleData[1].content = formatTime(mAutoOnOff.offHour, mAutoOnOff.offMinute)
                    mSleepScheduleAdapter?.notifyDataSetChanged()
                }
            }
        })
    }

    override fun initListener() {
        iv_prev_guide_sleep_schedule.setOnClickListener(this)
        iv_next_guide_sleep_schedule.setOnClickListener(this)
        tv_skip_guide_sleep_schedule.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            iv_prev_guide_sleep_schedule -> {
                activity?.let {
                    (it as GuideActivity).showFragment(1)
                }
            }
            iv_next_guide_sleep_schedule -> {
                activity?.let {
                    (it as GuideActivity).showFragment(3)
                }
            }
            tv_skip_guide_sleep_schedule -> {
                activity?.let {
                    (it as GuideActivity).showFragment(3)
                }
            }
        }
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        when (position) {
            0 -> {
                val timePickerDialog = TimePickerDialog(context, R.style.DateTimePicker, TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                    mSleepScheduleData[0].content = formatTime(hour, minute)
                    mSleepScheduleAdapter?.notifyDataSetChanged()
                    mAutoOnOff.onHour = hour
                    mAutoOnOff.onMinute = minute
                    GlobalScope.launch { MyDatabase.instance.powerDao.updatePower(mAutoOnOff) }
                    powerOnOff()
                }, mAutoOnOff.onHour, mAutoOnOff.onMinute, TimeUtil.isTime24)
                timePickerDialog.show()
                timePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE).textSize = 24f
                timePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE).textSize = 24f
            }
            1 -> {
                val timePickerDialog = TimePickerDialog(context, R.style.DateTimePicker, TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                    mSleepScheduleData[1].content = formatTime(hour, minute)
                    mSleepScheduleAdapter?.notifyDataSetChanged()
                    mAutoOnOff.offHour = hour
                    mAutoOnOff.offMinute = minute
                    GlobalScope.launch { MyDatabase.instance.powerDao.updatePower(mAutoOnOff) }
                    powerOnOff()
                }, mAutoOnOff.offHour, mAutoOnOff.offMinute, TimeUtil.isTime24)
                timePickerDialog.show()
                timePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE).textSize = 24f
                timePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE).textSize = 24f
            }
        }
    }

    private fun initRecyclerView() {
        mSleepScheduleData.add(BaseItem(R.string.auto_on_time, formatTime(mAutoOnOff.onHour, mAutoOnOff.onMinute)))
        mSleepScheduleData.add(BaseItem(R.string.auto_off_time, formatTime(mAutoOnOff.offHour, mAutoOnOff.offMinute)))
        mSleepScheduleAdapter = object : BaseQuickAdapter<BaseItem, BaseViewHolder>(R.layout.item_0_sleep_schedule, mSleepScheduleData) {
            override fun convert(helper: BaseViewHolder, item: BaseItem?) {
                if (item != null) {
                    helper.setText(R.id.tv_title_item_0_sleep_schedule, item.titleResId)
                            .setText(R.id.tv_content_item_0_sleep_schedule, item.content)
                }
            }
        }
        rv_content_guide_sleep_schedule.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        context?.let {
            rv_content_guide_sleep_schedule.addItemDecoration(HorizontalItemDecoration(it, resources.getDimension(R.dimen.dp_10).toInt(), false))
        }
        rv_content_guide_sleep_schedule.adapter = mSleepScheduleAdapter

        mSleepScheduleAdapter?.onItemClickListener = this
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        return DateFormat.getTimeFormat(context).format(calendar.timeInMillis)
    }

    private fun powerOnOff() {
        AlarmUtil.cancelPower(MyConstants.POWER_OFF_ID)
        AlarmUtil.cancelPower(MyConstants.POWER_ON_ID)
        if (mAutoOnOff.isChecked) {
            AlarmUtil.startPower(mAutoOnOff)
        }
    }

    companion object {
        private val TAG = GuideSleepScheduleFragment::class.java.simpleName
    }
}
