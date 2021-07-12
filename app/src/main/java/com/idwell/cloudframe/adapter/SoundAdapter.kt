package com.idwell.cloudframe.adapter

import android.widget.SeekBar
import android.widget.Switch

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.entity.MultipleItem

class SoundAdapter(data: List<MultipleItem>) : BaseMultiItemQuickAdapter<MultipleItem, BaseViewHolder>(data) {

    init {
        addItemType(TEXTST_SEEKBARSB, R.layout.item_textst_seekbarsb)
        addItemType(TEXTSC_SWITCHEC, R.layout.item_textsc_switchec)
    }

    override fun convert(holder: BaseViewHolder, item: MultipleItem) {
        when (holder.itemViewType) {
            TEXTST_SEEKBARSB -> {
                holder.setText(R.id.tv_title_item_textst_seekbarsb, item.title)
                val mSeekBar = holder.getView<SeekBar>(R.id.seekbar_item_textst_seekbarsb)
                mSeekBar.max = item.max
                mSeekBar.progress = item.progress
                if (item.title == mContext.getString(R.string.media_volume)) {
                    mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            Device.setStreamMusic(progress)
                            data[0].progress = progress
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {

                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {

                        }
                    })
                } else {
                    mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            Device.setStreamNotification(progress)
                            data[1].progress = progress
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {

                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {

                        }
                    })
                }
            }
            TEXTSC_SWITCHEC -> {
                holder.setText(R.id.tv_title_item_textsc_switchec, item.title)
                holder.addOnClickListener(R.id.switch_item_textsc_switchec)
                val mSwitch = holder.getView<Switch>(R.id.switch_item_textsc_switchec)
                mSwitch.isChecked = item.isChecked
                mSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                    Device.setSoundEffectsEnabled(if (isChecked) 1 else 0)
                }
            }
        }
    }

    companion object {
        const val TEXTST_SEEKBARSB = 0
        const val TEXTSC_SWITCHEC = 1
    }
}