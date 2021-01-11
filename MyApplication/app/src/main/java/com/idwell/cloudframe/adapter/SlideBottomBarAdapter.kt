package com.idwell.cloudframe.adapter

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.R
import com.idwell.cloudframe.entity.MultipleItem

class SlideBottomBarAdapter(data:MutableList<MultipleItem>): BaseMultiItemQuickAdapter<MultipleItem, BaseViewHolder>(data) {

    init {
        addItemType(ICON, R.layout.item_bottom_bar_slide)
        addItemType(TEXT, R.layout.item_text_slide)
    }

    override fun convert(helper: BaseViewHolder, item: MultipleItem?) {
        if (item != null){
            when(helper.itemViewType){
                ICON -> {
                    helper.setImageResource(R.id.iv_icon_item_bottom_bar_slide, item.iconResId)
                }
                TEXT -> {
                    helper.setText(R.id.tv_title_item_text_slide, item.title)
                }
            }
        }
    }

    companion object {
        const val ICON = 0
        const val TEXT = 1
    }
}