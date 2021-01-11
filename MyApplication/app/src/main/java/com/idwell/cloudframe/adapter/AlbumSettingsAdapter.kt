package com.idwell.cloudframe.adapter

import android.widget.Switch

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.entity.MultipleItem

class AlbumSettingsAdapter(data: List<MultipleItem>) : BaseMultiItemQuickAdapter<MultipleItem, BaseViewHolder>(data) {

    init {
        addItemType(TEXTSC, R.layout.item_textsc)
        addItemType(TEXTST_TEXTSB, R.layout.item_textst_textsb)
        addItemType(TEXTSC_TEXTEC_IMAGEEC, R.layout.item_textsc_textec_imageec)
        addItemType(TEXTSC_SWITCHEC, R.layout.item_textsc_switchec)
        addItemType(TEXTST_TEXTSB_SWITCHEC, R.layout.item_textst_textsb_switchec)
    }

    override fun convert(holder: BaseViewHolder, item: MultipleItem) {
        val mSwitch: Switch
        when (holder.itemViewType) {
            TEXTSC -> holder.setText(R.id.tv_item_textsc, item.title)
            TEXTST_TEXTSB -> {
                holder.setText(R.id.tv_title_item_textst_textsb, item.title)
                holder.setText(R.id.tv_content_item_textst_textsb, item.content)
            }
            TEXTSC_TEXTEC_IMAGEEC -> {
                holder.setText(R.id.tv_title_item_textsc_textec_imageec, item.title)
                holder.setText(R.id.tv_content_item_textsc_textec_imageec, item.content)
            }
            TEXTSC_SWITCHEC -> {
                holder.setText(R.id.tv_title_item_textsc_switchec, item.title)
                mSwitch = holder.getView(R.id.switch_item_textsc_switchec)
                mSwitch.isChecked = item.isChecked
            }
            TEXTST_TEXTSB_SWITCHEC -> {
                holder.setText(R.id.tv_title_item_textst_textsb_switchec, item.title)
                holder.setText(R.id.tv_content_item_textst_textsb_switchec, item.content)
                mSwitch = holder.getView(R.id.switch_item_textst_textsb_switchec)
                mSwitch.isChecked = item.isChecked
            }
        }
    }

    companion object {
        const val TEXTSC = 0
        const val TEXTST_TEXTSB = 1
        const val TEXTSC_SWITCHEC = 2
        const val TEXTSC_TEXTEC_IMAGEEC = 3
        const val TEXTST_TEXTSB_SWITCHEC = 4
    }
}