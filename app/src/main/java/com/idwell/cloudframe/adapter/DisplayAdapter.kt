package com.idwell.cloudframe.adapter

import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.widget.SeekBar
import android.widget.Switch
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.MyApplication
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.entity.MultipleItem
import com.idwell.cloudframe.widget.photoview.Util


class DisplayAdapter(data: List<MultipleItem>) : BaseMultiItemQuickAdapter<MultipleItem, BaseViewHolder>(data) {

    init {
        addItemType(TEXTSC_SWITCHEC, R.layout.item_textsc_switchec)
        addItemType(TEXTST_SEEKBARSB, R.layout.item_textst_seekbarsb)
        addItemType(TEXTST_SEEKBARSB_CHECKBOX, R.layout.item_textst_seekbarsb_checkbox)
        addItemType(TEXTST_TEXTSB_SWITCHEC, R.layout.item_textst_textsb_switchec)
        addItemType(TEXTST_TEXTSB_SWITCHEC_HEIGHT120, R.layout.item_textst_textsb_switchec_height120)
        addItemType(TEXTSC_TEXTEC_IMAGEEC, R.layout.item_textsc_textec_imageec)
    }

    override fun convert(helper: BaseViewHolder, item: MultipleItem?) {
        if (item != null) {
            when (helper.itemViewType) {
                TEXTSC_SWITCHEC -> {
                    helper.setText(R.id.tv_title_item_textsc_switchec, item.title)
                    helper.getView<Switch>(R.id.switch_item_textsc_switchec)
                            .isChecked = item.isChecked
                }
                TEXTST_SEEKBARSB,TEXTST_SEEKBARSB_CHECKBOX -> {
                    helper.setText(R.id.tv_title_item_textst_seekbarsb, item.title)
                    val mSeekBar = helper.getView<SeekBar>(R.id.seekbar_item_textst_seekbarsb)
                    mSeekBar.max = when (Build.VERSION.SDK_INT) {
                        23 -> item.max - 50
                        else -> item.max
                        //else -> item.max - 50
                    }
                    mSeekBar.progress = when (Build.VERSION.SDK_INT) {
                        23 -> item.progress - 50
                        else -> item.progress
                        //else -> 255 - item.progress - 50
                    }
                    mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            Device.setScreenBrightness(when (Build.VERSION.SDK_INT) {
                                23 -> progress + 50
                                else -> progress
                                //else -> 255 - progress - 50
                            })
                            data[0].progress = when (Build.VERSION.SDK_INT) {
                                23 -> progress + 50
                                else -> progress
                                //else -> progress + 50
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {

                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {

                        }
                    })
                    var autoBrightness = Util.isAutoBrightness(
                        MyApplication.instance().contentResolver
                    )
                    if(autoBrightness){
                        mSeekBar.isEnabled = false
                        Util.setSeekBarColor(mSeekBar,Color.LTGRAY)
                    }else{
                        Util.setSeekBarColor(mSeekBar, Color.WHITE)
                        mSeekBar.isEnabled = true
                    }
//                    if(null != mCheckBox) {
//                        mCheckBox.isChecked = autoBrightness
//                        if(autoBrightness){
//                            mSeekBar.isEnabled = false
//                            Util.setSeekBarColor(mSeekBar,Color.LTGRAY)
//                        }
//                        mCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
//                            if (isChecked) {
//                                Util.startAutoBrightness(MyApplication.instance())
//                                mSeekBar.isEnabled = false
//                                Util.setSeekBarColor(mSeekBar,Color.LTGRAY)
//                                notifyDataSetChanged()
//                            } else {
//                                Util.stopAutoBrightness(MyApplication.instance())
//                                Util.setSeekBarColor(mSeekBar,Color.WHITE)
//                                mSeekBar.isEnabled = true
//                                notifyDataSetChanged()
//                            }
//
//                        }
//                    }
                }
                TEXTST_TEXTSB_SWITCHEC,TEXTST_TEXTSB_SWITCHEC_HEIGHT120 -> {
                    helper.setText(R.id.tv_title_item_textst_textsb_switchec, item.title)
                    helper.setText(R.id.tv_content_item_textst_textsb_switchec, item.content)
                    val mSwitch = helper.getView<Switch>(R.id.switch_item_textst_textsb_switchec)
                    mSwitch.isChecked = item.isChecked
                }
                TEXTSC_TEXTEC_IMAGEEC -> {
                    helper.setText(R.id.tv_title_item_textsc_textec_imageec, item.title)
                            .setText(R.id.tv_content_item_textsc_textec_imageec, item.content)
                }
            }
        }
    }

    companion object {
        const val TEXTSC_SWITCHEC = 0
        const val TEXTST_SEEKBARSB = 1
        const val TEXTST_TEXTSB_SWITCHEC = 2
        const val TEXTSC_TEXTEC_IMAGEEC = 3
        const val TEXTST_SEEKBARSB_CHECKBOX = 4
        const val TEXTST_TEXTSB_SWITCHEC_HEIGHT120 = 5
    }
}