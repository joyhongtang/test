package com.idwell.cloudframe.ui

import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.idwell.cloudframe.R
import com.idwell.cloudframe.adapter.AutoPowerAdapter
import com.idwell.cloudframe.base.BaseFragment
import com.idwell.cloudframe.common.MyConstants
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.db.entity.AutoOnOff
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.entity.MultipleItem
import com.idwell.cloudframe.util.AlarmUtil
import com.idwell.cloudframe.util.TimeUtil
import com.idwell.cloudframe.widget.MaterialDialog
import com.idwell.cloudframe.widget.MyTimePickerDialog
import kotlinx.android.synthetic.main.fragment_recyclerview.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class AutoOnOffFragment : BaseFragment(), BaseQuickAdapter.OnItemClickListener {

    lateinit var mAutoOnOff: AutoOnOff

    private var autoPowerAdapter: AutoPowerAdapter? = null
    private lateinit var data: MutableList<MultipleItem>
    private var weeks: MutableList<String>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recyclerview, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        MyDatabase.instance.powerDao.queryLiveData().observe({ this.lifecycle }, { autoOnOff ->
            if (autoOnOff == null) {
                mAutoOnOff = AutoOnOff(-1, 8, 0, 22, 0, mutableListOf(0, 1, 2, 3, 4, 5, 6), true)
                GlobalScope.launch { MyDatabase.instance.powerDao.insertPower(mAutoOnOff) }
            } else {
                mAutoOnOff = autoOnOff
                if (autoPowerAdapter == null) {
                    initRecyclerView()
                } else {
                    data.clear()
                    data.addAll(mutableListOf(MultipleItem(AutoPowerAdapter.TEXTSC_SWITCHEC, getString(R.string.auto_power_on_off), mAutoOnOff.isChecked), MultipleItem(AutoPowerAdapter.TEXTST_TEXTSB, getString(R.string.auto_on_time), formatTime(mAutoOnOff.onHour, mAutoOnOff.onMinute), mAutoOnOff.isChecked), MultipleItem(AutoPowerAdapter.TEXTST_TEXTSB, getString(R.string.auto_off_time), formatTime(mAutoOnOff.offHour, mAutoOnOff.offMinute), mAutoOnOff.isChecked), MultipleItem(AutoPowerAdapter.TEXTST_TEXTSB, getString(R.string.repeat), convertRepeat(), mAutoOnOff.isChecked)))
                    autoPowerAdapter?.notifyDataSetChanged()
                }
            }
        })
    }

    private fun initRecyclerView() {
        weeks = context?.resources?.getStringArray(R.array.weeks_abbr)?.toMutableList()
        data = mutableListOf(MultipleItem(AutoPowerAdapter.TEXTSC_SWITCHEC, getString(R.string.auto_power_on_off), mAutoOnOff.isChecked), MultipleItem(AutoPowerAdapter.TEXTST_TEXTSB, getString(R.string.auto_on_time), formatTime(mAutoOnOff.onHour, mAutoOnOff.onMinute), mAutoOnOff.isChecked), MultipleItem(AutoPowerAdapter.TEXTST_TEXTSB, getString(R.string.auto_off_time), formatTime(mAutoOnOff.offHour, mAutoOnOff.offMinute), mAutoOnOff.isChecked), MultipleItem(AutoPowerAdapter.TEXTST_TEXTSB, getString(R.string.repeat), convertRepeat(), mAutoOnOff.isChecked))
        autoPowerAdapter = AutoPowerAdapter(data)
        rv_fragment_recyclerview.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        context?.let {
            rv_fragment_recyclerview.addItemDecoration(HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider)))
        }
        rv_fragment_recyclerview.adapter = autoPowerAdapter
        autoPowerAdapter?.onItemClickListener = this
    }

    override fun initListener() {

    }

    override fun onMessageEvent(event: MessageEvent) {
        when (event.message) {
            MessageEvent.TIME_12_24 -> {
                data[1].content = formatTime(mAutoOnOff.onHour, mAutoOnOff.onMinute)
                data[2].content = formatTime(mAutoOnOff.offHour, mAutoOnOff.offMinute)
                autoPowerAdapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        when (position) {
            0 -> {
                data[0].isChecked = !mAutoOnOff.isChecked
                data[1].isChecked = !mAutoOnOff.isChecked
                data[2].isChecked = !mAutoOnOff.isChecked
                data[3].isChecked = !mAutoOnOff.isChecked
                autoPowerAdapter?.notifyDataSetChanged()
                mAutoOnOff.isChecked = !mAutoOnOff.isChecked
                GlobalScope.launch { MyDatabase.instance.powerDao.updatePower(mAutoOnOff) }
                powerOnOff()
            }
            1 -> {
                if (mAutoOnOff.isChecked) {
                    val timePickerDialog = MyTimePickerDialog(context, R.style.DateTimePicker, TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                        data[position].content = formatTime(hour, minute)
                        autoPowerAdapter?.notifyDataSetChanged()
                        mAutoOnOff.onHour = hour
                        mAutoOnOff.onMinute = minute
                        GlobalScope.launch { MyDatabase.instance.powerDao.updatePower(mAutoOnOff) }
                        powerOnOff()
                    }, mAutoOnOff.onHour, mAutoOnOff.onMinute, TimeUtil.isTime24)
                    timePickerDialog.show()
                    timePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE).textSize = 24f
                    timePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE).textSize = 24f
                }
            }
            2 -> {
                if (mAutoOnOff.isChecked) {
                    val timePickerDialog = MyTimePickerDialog(context, R.style.DateTimePicker, TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                        data[position].content = formatTime(hour, minute)
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
            3 -> {
                if (mAutoOnOff.isChecked) {
                    context?.let {
                        val repeat = mutableListOf<Int>()
                        repeat.addAll(mAutoOnOff.repeat)
                        MaterialDialog.Builder(it)
                                .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textsc_imageec_dialog, weeks) {
                                    override fun convert(helper: BaseViewHolder, item: String) {
                                        helper.setText(R.id.tv_title_item_textsc_imageec_dialog, item)
                                                .setImageResource(R.id.iv_item_textsc_imageec_dialog, if (repeat.contains(helper.layoutPosition)) R.drawable.ic_check_circle_blue else R.drawable.gray_ring_shape)
                                    }
                                }, BaseQuickAdapter.OnItemClickListener { adapter, _, position1 ->
                                    if (repeat.contains(position1)) {
                                        repeat.remove(position1)
                                    } else {
                                        repeat.add(position1)
                                        repeat.sort()
                                    }
                                    adapter.notifyDataSetChanged()
                                }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content)))
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                                    override fun onClick(dialog: MaterialDialog) {
                                        mAutoOnOff.repeat.clear()
                                        mAutoOnOff.repeat.addAll(repeat)
                                        GlobalScope.launch { MyDatabase.instance.powerDao.updatePower(mAutoOnOff) }
                                        powerOnOff()
                                    }
                                }).show()
                    }
                }
            }
        }
    }

    private fun powerOnOff() {
        AlarmUtil.cancelPower(MyConstants.POWER_OFF_ID)
        AlarmUtil.cancelPower(MyConstants.POWER_ON_ID)
        if (mAutoOnOff.isChecked) {
            AlarmUtil.startPower(mAutoOnOff)
        }
    }

    private fun convertRepeat(): String {
        return when (mAutoOnOff.repeat.size) {
            0 -> getString(R.string.off)
            7 -> getString(R.string.everyday)
            else -> {
                val weeks = resources.getStringArray(R.array.weeks_abbr)
                val repeat = mutableListOf<String>()
                for (i in mAutoOnOff.repeat) {
                    repeat.add(weeks[i])
                }
                Gson().toJson(repeat).replace("[\"", "").replace("\",\"", ", ").replace("\"]", "")
            }
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        return DateFormat.getTimeFormat(context).format(calendar.timeInMillis)
    }

    companion object {
        private val TAG = AutoOnOffFragment::class.java.simpleName
    }
}
