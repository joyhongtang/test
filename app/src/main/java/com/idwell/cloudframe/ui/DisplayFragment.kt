package com.idwell.cloudframe.ui

import android.content.Intent
import android.os.Bundle
import android.os.HumanSensor
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.MyApplication
import com.idwell.cloudframe.R
import com.idwell.cloudframe.adapter.DisplayAdapter
import com.idwell.cloudframe.base.BaseFragment
import com.idwell.cloudframe.common.MyConstants
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.common.Device.autoBrightness
import com.idwell.cloudframe.entity.MultipleItem
import com.idwell.cloudframe.service.GlobalService
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.widget.MaterialDialog
import com.idwell.cloudframe.widget.photoview.Util
import kotlinx.android.synthetic.main.fragment_recyclerview.*
import java.io.File
import java.io.FileWriter

class DisplayFragment : BaseFragment(), BaseQuickAdapter.OnItemClickListener {

    private lateinit var mDisplayAdapter: DisplayAdapter
    private var mData = mutableListOf<MultipleItem>()
    private var mSleep = mutableListOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recyclerview, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        mSleep.addAll(resources.getStringArray(R.array.sleep))
        mData.add(MultipleItem(DisplayAdapter.TEXTST_SEEKBARSB, getString(R.string.brightness), Device.getScreenBrightness(), 255))

        if(autoBrightness){
            var autoBrightness = Util.isAutoBrightness(
                MyApplication.instance().contentResolver
            )
            mData.add(MultipleItem(DisplayAdapter.TEXTST_TEXTSB_SWITCHEC_HEIGHT120, getString(R.string.auto_brightness_title), getString(R.string.auto_brightness_desp),  autoBrightness))
        }
        mData.add(MultipleItem(DisplayAdapter.TEXTST_TEXTSB_SWITCHEC, getString(R.string.suspended_ball_title), getString(R.string.suspended_ball_desc), Device.displaySuspendedBall))
        if (Device.hasHumanSensor) {
            mData.add(MultipleItem(DisplayAdapter.TEXTSC_TEXTEC_IMAGEEC, getString(R.string.motion_sensor), mSleep[Device.sleepArray.indexOf(Settings.System.getInt(context?.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, Int.MAX_VALUE))]))
        }
        if (Device.hasGravitySensor) {
            mData.add(MultipleItem(DisplayAdapter.TEXTSC_SWITCHEC, getString(R.string.accelerometer_title), Device.isAutoRotateScreen()))
        }
        mDisplayAdapter = DisplayAdapter(mData)
        rv_fragment_recyclerview.layoutManager = LinearLayoutManager(context)
        context?.let {
            rv_fragment_recyclerview.addItemDecoration(HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider)))
        }
        rv_fragment_recyclerview.adapter = mDisplayAdapter
    }

    override fun initListener() {
        mDisplayAdapter.onItemClickListener = this
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        var tag = mData.get(position).title
        when (tag) {
            getString(R.string.auto_brightness_title) ->{
                var autoBrightness = Util.isAutoBrightness(
                    MyApplication.instance().contentResolver
                )
                if(autoBrightness){
                    Util.stopAutoBrightness(MyApplication.instance())
                }else{
                    Util.startAutoBrightness(MyApplication.instance())
                }
                mData[position].isChecked = !autoBrightness
                mDisplayAdapter.notifyDataSetChanged()
            }
            getString(R.string.suspended_ball_title) -> {
                Device.displaySuspendedBall = !Device.displaySuspendedBall
                mData[position].isChecked = Device.displaySuspendedBall
                mDisplayAdapter.notifyDataSetChanged()
                val intent = Intent(context, GlobalService::class.java)
                intent.putExtra(MyConstants.ACTION, Device.DISPLAY_SUSPENDED_BALL)
                context?.startService(intent)
            }
            getString(R.string.motion_sensor) -> {
                context?.let {
                    var index = Device.sleepArray.indexOf(Device.sleep)
                    MaterialDialog.Builder(it).setTitle(R.string.motion_sensor)
                            .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textsc_imageec_dialog, mSleep) {
                                override fun convert(helper: BaseViewHolder, item: String) {
                                    helper.setText(R.id.tv_title_item_textsc_imageec_dialog, item)
                                    if (index == helper.adapterPosition) {
                                        helper.setImageResource(R.id.iv_item_textsc_imageec_dialog, R.drawable.ic_check_circle_blue)
                                    } else {
                                        helper.setImageResource(R.id.iv_item_textsc_imageec_dialog, R.drawable.gray_ring_shape)
                                    }
                                }
                            }, BaseQuickAdapter.OnItemClickListener { adapter1, _, position1 ->
                                index = position1
                                adapter1.notifyDataSetChanged()
                            }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content)))
                            .setLayoutParamsHeight().setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                                override fun onClick(dialog: MaterialDialog) {
                                    if (Device.sleepArray.indexOf(Device.sleep) != index) {
                                        mData[position].content = mSleep[index]
                                        mDisplayAdapter.notifyDataSetChanged()
                                        Device.sleep = Device.sleepArray[index]
                                        setMotionSensor()
                                    }
                                }
                            }).show().findViewById<RecyclerView>(R.id.rv_content_dialog_material)
                            .scrollToPosition(index)
                }
            }
            getString(R.string.accelerometer_title) -> {
                val isAutoRotateScreen = Device.isAutoRotateScreen()
                if (isAutoRotateScreen) {
                    val rotation = activity?.windowManager?.defaultDisplay?.rotation ?: 0
                    Settings.System.putInt(context?.contentResolver, Settings.System.USER_ROTATION, rotation)
                }
                Device.setAutoRotateScreen(if (isAutoRotateScreen) 0 else 1)
                mData[position].isChecked = !isAutoRotateScreen
                mDisplayAdapter.notifyItemChanged(position)
            }
        }
    }

    private fun setMotionSensor() {
        try {
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
        Settings.System.putInt(context?.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, Device.sleep)
    }
}
