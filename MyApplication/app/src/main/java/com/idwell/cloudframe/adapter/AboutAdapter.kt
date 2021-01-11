package com.idwell.cloudframe.adapter

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.R
import com.idwell.cloudframe.entity.MultipleItem

class AboutAdapter(data: List<MultipleItem>) : BaseMultiItemQuickAdapter<MultipleItem, BaseViewHolder>(data) {

    init {
        addItemType(TEXTST_TEXTSB, R.layout.item_textst_textsb)
        addItemType(TEXTST_TEXTSB_TEXTEB, R.layout.item_textst_textsb_texteb)
        addItemType(TEXTST_TEXTSB_TEXTET_TEXTEB, R.layout.item_textst_textsb_textet_texteb)
    }

    override fun convert(helper: BaseViewHolder, item: MultipleItem?) {
        if (item != null) {
            when (helper.itemViewType) {
                TEXTST_TEXTSB -> {
                    helper.setText(R.id.tv_title_item_textst_textsb, item.title)
                            .setText(R.id.tv_content_item_textst_textsb, item.content)
                }
                TEXTST_TEXTSB_TEXTEB -> {
                    helper.setText(R.id.tv_title_item_textst_textsb_texteb, item.title)
                            .setText(R.id.tv_content_item_textst_textsb_texteb, item.content)
                            .setText(R.id.tv_desc_item_textst_textsb_texteb, item.desc)
                }
                TEXTST_TEXTSB_TEXTET_TEXTEB -> {
                    helper.setText(R.id.tv_title_item_textst_textsb_textet_texteb, item.title)
                            .setText(R.id.tv_content_item_textst_textsb_textet_texteb, item.content)
                            .setText(R.id.tv_desc_item_textst_textsb_textet_texteb, item.desc)
                            .setText(R.id.tv_ext_item_textst_textsb_textet_texteb, item.ext)
                }
            }
        }
    }

    companion object {
        const val TEXTST_TEXTSB = 0
        const val TEXTST_TEXTSB_TEXTEB = 1
        const val TEXTST_TEXTSB_TEXTET_TEXTEB = 2
    }
}