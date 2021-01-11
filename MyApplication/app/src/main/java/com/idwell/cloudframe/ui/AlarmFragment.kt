package com.idwell.cloudframe.ui

import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseFragment
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.db.entity.Alarm
import com.idwell.cloudframe.util.AlarmUtil
import kotlinx.android.synthetic.main.fragment_alarm.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AlarmFragment : BaseFragment(), BaseQuickAdapter.OnItemChildClickListener {

    private var mAlarms = mutableListOf<Alarm>()

    private lateinit var mAlarmAdapter: BaseQuickAdapter<Alarm, BaseViewHolder>

    private var mAlarmActivity: AlarmActivity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_alarm, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        mAlarmActivity = context as AlarmActivity
        mAlarmAdapter = object : BaseQuickAdapter<Alarm, BaseViewHolder>(R.layout.item_fragment_alarm, mAlarms) {
            override fun convert(helper: BaseViewHolder, item: Alarm?) {
                if (item != null) {
                    helper.setText(R.id.tv_time_item_fragment_alarm, item.time)
                            .setText(R.id.tv_tag_item_fragment_alarm, item.label)
                            .setText(R.id.tv_repeat_item_fragment_alarm, convertRepeat(item))
                    if (isMore) {
                        helper.getView<Switch>(R.id.switch_item_fragment_alarm)
                                .visibility = View.GONE
                        helper.getView<View>(R.id.cl_more_item_fragment_alarm)
                                .visibility = View.VISIBLE
                    } else {
                        helper.getView<View>(R.id.cl_more_item_fragment_alarm)
                                .visibility = View.GONE
                        helper.getView<Switch>(R.id.switch_item_fragment_alarm)
                                .visibility = View.VISIBLE
                    }
                    helper.getView<Switch>(R.id.switch_item_fragment_alarm)
                            .isChecked = item.isChecked
                    helper.addOnClickListener(R.id.switch_item_fragment_alarm)
                    helper.addOnClickListener(R.id.iv_settings_more_item_fragment_alarm)
                    helper.addOnClickListener(R.id.iv_delete_more_item_fragment_alarm)
                }
            }
        }
        rv_alarm.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        context?.let {
            rv_alarm.addItemDecoration(HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider)))
        }
        rv_alarm.adapter = mAlarmAdapter
        MyDatabase.instance.alarmDao.queryOrderAsc().observe({ this.lifecycle }, { alarms ->
            mAlarms.clear()
            alarms?.let { mAlarms.addAll(it) }
            mAlarmAdapter.notifyDataSetChanged()
        })
    }

    override fun initListener() {
        tv_edit_alarm.setOnClickListener(this)
        tv_add_alarm.setOnClickListener(this)
        mAlarmAdapter.onItemChildClickListener = this
    }

    override fun onClick(v: View?) {
        when (v) {
            tv_edit_alarm -> {
                isMore = !isMore
                if (isMore) {
                    tv_edit_alarm.text = getString(R.string.cancel)
                } else {
                    tv_edit_alarm.text = getString(R.string.edit)
                }
                mAlarmAdapter.notifyDataSetChanged()
            }
            tv_add_alarm -> {
                if (isMore) {
                    isMore = false
                    tv_edit_alarm.setText(R.string.edit)
                    mAlarmAdapter.notifyDataSetChanged()
                }
                mAlarmActivity?.showFragment(1)
            }
        }
    }

    override fun onItemChildClick(baseQuickAdapter: BaseQuickAdapter<*, *>, view: View, i: Int) {
        when (view.id) {
            R.id.switch_item_fragment_alarm -> {
                GlobalScope.launch {
                    val alarm = mAlarms[i]
                    alarm.isChecked = !alarm.isChecked
                    if (alarm.isChecked) {
                        AlarmUtil.startAlarm(alarm)
                    } else {
                        AlarmUtil.cancelAlarm(alarm.id)
                    }
                    MyDatabase.instance.alarmDao.update(alarm)
                }
            }
            R.id.iv_settings_more_item_fragment_alarm -> mAlarmActivity?.showFragment(mAlarms[i])
            R.id.iv_delete_more_item_fragment_alarm -> {
                AlarmUtil.cancelAlarm(mAlarms[i].id)
                GlobalScope.launch {
                    MyDatabase.instance.alarmDao.delete(mAlarms[i])
                }
            }
        }
    }

    private fun convertRepeat(alarm: Alarm): String {
        return when (alarm.repeat.size) {
            0 -> getString(R.string.off)
            7 -> getString(R.string.everyday)
            else -> {
                val weeks = resources.getStringArray(R.array.weeks_abbr)
                val repeat = mutableListOf<String>()
                for (i in alarm.repeat) {
                    repeat.add(weeks[i])
                }
                Gson().toJson(repeat).replace("[\"", "").replace("\",\"", ", ").replace("\"]", "")
            }
        }
    }
}