package com.idwell.cloudframe.adapter

import androidx.core.content.ContextCompat
import android.widget.Switch

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.R

import com.idwell.cloudframe.entity.MultipleItem

class DateTimeAdapter(data: List<MultipleItem>) : BaseMultiItemQuickAdapter<MultipleItem, BaseViewHolder>(data) {

    init {
        addItemType(TEXTST_TEXTSB, R.layout.item_textst_textsb)
        addItemType(TEXTST_TEXTSB_SWITCHEC, R.layout.item_textst_textsb_switchec)
    }

    override fun convert(helper: BaseViewHolder, item: MultipleItem?) {
        if (item != null){
            when (helper.itemViewType) {
                TEXTST_TEXTSB -> {
                    helper.setText(R.id.tv_title_item_textst_textsb, item.title)
                    helper.setText(R.id.tv_content_item_textst_textsb, item.content)
                    if ((helper.layoutPosition == 1 || helper.layoutPosition == 2) && item.isChecked) {
                        helper.setTextColor(R.id.tv_title_item_textst_textsb, ContextCompat.getColor(mContext, R.color.text_gray))
                        helper.setTextColor(R.id.tv_content_item_textst_textsb, ContextCompat.getColor(mContext, R.color.text_gray))
                    } else {
                        helper.setTextColor(R.id.tv_title_item_textst_textsb, ContextCompat.getColor(mContext, R.color.white))
                        helper.setTextColor(R.id.tv_content_item_textst_textsb, ContextCompat.getColor(mContext, R.color.white))
                    }
                }
                TEXTST_TEXTSB_SWITCHEC -> {
                    helper.setText(R.id.tv_title_item_textst_textsb_switchec, item.title)
                    helper.setText(R.id.tv_content_item_textst_textsb_switchec, item.content)
                    val mSwitch = helper.getView<Switch>(R.id.switch_item_textst_textsb_switchec)
                    mSwitch.isChecked = item.isChecked
                }
            }
        }
    }

    companion object {
        const val TEXTST_TEXTSB = 0
        const val TEXTST_TEXTSB_SWITCHEC = 1
    }
}
