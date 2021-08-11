package com.idwell.cloudframe.adapter

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.R
import com.idwell.cloudframe.entity.MultipleItem

class StorageAdapter(data: MutableList<MultipleItem>) : BaseMultiItemQuickAdapter<MultipleItem, BaseViewHolder>(data) {

    var mPosition = 0

    init {
        addItemType(PHOTO_STORAGE, R.layout.item_photo_storage)
        addItemType(PHOTO_USER, R.layout.item_photo_storage)
        addItemType(MUSIC_STORAGE, R.layout.item_music_storage)
        addItemType(VIDEO_STORAGE, R.layout.item_video_storage)
        addItemType(VIDEO_USER, R.layout.item_video_storage)
    }

    override fun convert(helper: BaseViewHolder, item: MultipleItem?) {
        if (item != null){
            when(helper.itemViewType){
                PHOTO_STORAGE -> {
                    helper.setText(R.id.tv_title_item_photo_storage, item.title)
                    helper.setImageResource(R.id.iv_item_photo_storage, item.iconResId)
                    helper.getView<ImageView>(R.id.iv_item_photo_storage).isSelected = helper.layoutPosition == mPosition
                    helper.itemView.isSelected = helper.layoutPosition == mPosition
                }
                PHOTO_USER -> {
                    val user = item.user
                    val username = if (user.remarkname.isNotEmpty()) user.remarkname else if (user.name.isNotEmpty()) user.name else user.account.toString()
                    helper.setText(R.id.tv_title_item_photo_storage, username)
                    Glide.with(mContext).load(item.title)
                            .placeholder(R.drawable.ic_avatar)
                            .error(R.drawable.ic_avatar).circleCrop().into(helper.getView(R.id.iv_item_photo_storage))
                    helper.itemView.isSelected = helper.layoutPosition == mPosition
                }
                MUSIC_STORAGE -> {
                    helper.setText(R.id.tv_title_item_music_storage, item.title)
                    helper.setImageResource(R.id.iv_item_music_storage, item.iconResId)
                    helper.getView<ImageView>(R.id.iv_item_music_storage).isSelected = helper.layoutPosition == mPosition
                    helper.itemView.isSelected = helper.layoutPosition == mPosition
                }
                VIDEO_STORAGE -> {
                    helper.setText(R.id.tv_title_item_video_storage, item.title)
                    helper.setImageResource(R.id.iv_item_video_storage, item.iconResId)
                    helper.getView<ImageView>(R.id.iv_item_video_storage).isSelected = helper.layoutPosition == mPosition
                    helper.itemView.isSelected = helper.layoutPosition == mPosition
                }
                VIDEO_USER -> {
                    val user = item.user
                    val username = if (user.remarkname.isNotEmpty()) user.remarkname else if (user.name.isNotEmpty()) user.name else user.account.toString()
                    helper.setText(R.id.tv_title_item_video_storage, username)
                    Glide.with(mContext).load(item.title)
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar).circleCrop().into(helper.getView(R.id.iv_item_video_storage))
                    helper.itemView.isSelected = helper.layoutPosition == mPosition
                }
            }
        }
    }

    companion object {
        const val PHOTO_STORAGE = 0
        const val PHOTO_USER = 1
        const val MUSIC_STORAGE = 2
        const val VIDEO_STORAGE = 3
        const val VIDEO_USER = 4
    }
}