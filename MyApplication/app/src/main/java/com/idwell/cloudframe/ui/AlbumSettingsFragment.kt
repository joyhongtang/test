package com.idwell.cloudframe.ui

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.adapter.AlbumSettingsAdapter
import com.idwell.cloudframe.base.BaseFragment
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.entity.Description
import com.idwell.cloudframe.entity.MultipleItem
import com.idwell.cloudframe.entity.Photo
import com.idwell.cloudframe.widget.MaterialDialog

import java.util.ArrayList

import kotlinx.android.synthetic.main.fragment_recyclerview.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class AlbumSettingsFragment : BaseFragment(), BaseQuickAdapter.OnItemClickListener {

    private lateinit var mAlbumSettingsAdapter: AlbumSettingsAdapter
    private val mData = ArrayList<MultipleItem>()
    private var mSlideshow = mutableListOf<String>()
    private var mSlideshowMode = mutableListOf<String>()
    private var mSlideshowAnimation = mutableListOf<String>()
    private var mSlideshowInterval = mutableListOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recyclerview, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        mSlideshow.addAll(resources.getStringArray(R.array.slideshow))
        mSlideshowMode.addAll(resources.getStringArray(R.array.slideshow_mode))
        mSlideshowAnimation.addAll(resources.getStringArray(R.array.slideshow_animation))
        mSlideshowInterval.addAll(resources.getStringArray(R.array.slideshow_interval))

        mData.add(MultipleItem(AlbumSettingsAdapter.TEXTST_TEXTSB_SWITCHEC, getString(R.string.full_screen), getString(R.string.the_photo_will_be_displayed_in_full_screen), Device.isPhotoFullScreen))
        mData.add(MultipleItem(AlbumSettingsAdapter.TEXTSC_TEXTEC_IMAGEEC, getString(R.string.slideshow), mSlideshow[Device.slideshowArray.indexOf(Device.slideshow)]))
        //mData.add(MultipleItem(AlbumSettingsAdapter.TEXTSC_SWITCHEC, getString(R.string.background_music), Device.isBackgroundMusicOn))
        mData.add(MultipleItem(AlbumSettingsAdapter.TEXTST_TEXTSB, getString(R.string.slideshow_mode), mSlideshowMode[Device.slideshowMode]))
        mData.add(MultipleItem(AlbumSettingsAdapter.TEXTSC_TEXTEC_IMAGEEC, getString(R.string.slideshow_interval), mSlideshowInterval[Device.slideIntervalArray.indexOf(Device.slideshowInterval)]))
        mData.add(MultipleItem(AlbumSettingsAdapter.TEXTSC_TEXTEC_IMAGEEC, getString(R.string.slideshow_transition_effect), mSlideshowAnimation[Device.slideshowTransitionEffect]))
        mData.add(MultipleItem(AlbumSettingsAdapter.TEXTST_TEXTSB, getString(R.string.delete_photos), getString(R.string.delete_photos_desc)))
        mData.add(MultipleItem(AlbumSettingsAdapter.TEXTSC, getString(R.string.restore_default_settings)))
        mAlbumSettingsAdapter = AlbumSettingsAdapter(mData)
        rv_fragment_recyclerview.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        context?.let {
            rv_fragment_recyclerview.addItemDecoration(HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider)))
        }
        rv_fragment_recyclerview.adapter = mAlbumSettingsAdapter
    }

    override fun initListener() {
        mAlbumSettingsAdapter.onItemClickListener = this
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        when (position) {
            0 -> {
                Device.isPhotoFullScreen = !Device.isPhotoFullScreen
                mData[position].isChecked = Device.isPhotoFullScreen
                mAlbumSettingsAdapter.notifyItemChanged(position)
            }
            1 -> {
                context?.let {
                    var index = Device.slideshowArray.indexOf(Device.slideshow)
                    MaterialDialog.Builder(it).setTitle(R.string.slideshow)
                            .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textsc_imageec_dialog, mSlideshow) {
                                override fun convert(helper: BaseViewHolder, item: String) {
                                    helper.setText(R.id.tv_title_item_textsc_imageec_dialog, item)
                                    Glide.with(mContext)
                                            .load(if (index == helper.layoutPosition) R.drawable.ic_check_circle_blue else R.drawable.gray_ring_shape)
                                            .into(helper.getView<View>(R.id.iv_item_textsc_imageec_dialog) as ImageView)
                                }
                            }, BaseQuickAdapter.OnItemClickListener { adapter1, _, position1 ->
                                index = position1
                                adapter1.notifyDataSetChanged()
                            }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content)))
                            .setLayoutParamsHeight().setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                                override fun onClick(dialog: MaterialDialog) {
                                    if (Device.slideshowArray.indexOf(Device.slideshow) != index) {
                                        mData[position].content = mSlideshow[index]
                                        mAlbumSettingsAdapter.notifyDataSetChanged()
                                        Device.slideshow = Device.slideshowArray[index]
                                        EventBus.getDefault()
                                                .post(MessageEvent(MessageEvent.SLIDE_AUTOPLAY_TIME_CHANGED))
                                    }
                                }
                            }).show().findViewById<RecyclerView>(R.id.rv_content_dialog_material)
                            .scrollToPosition(index)
                }
            }
            /*2 -> {
                Device.isBackgroundMusicOn = !Device.isBackgroundMusicOn
                mData[2].isChecked = Device.isBackgroundMusicOn
                mAlbumSettingsAdapter.notifyItemChanged(2)
            }*/
            2 -> {
                context?.let {
                    MaterialDialog.Builder(it).setTitle(R.string.slideshow_mode)
                            .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textsc_imageec_dialog, mSlideshowMode) {
                                override fun convert(helper: BaseViewHolder, item: String) {
                                    helper.setText(R.id.tv_title_item_textsc_imageec_dialog, mSlideshowMode[helper.layoutPosition])
                                    Glide.with(mContext)
                                            .load(if (Device.slideshowMode == helper.layoutPosition) R.drawable.ic_check_circle_blue else R.drawable.gray_ring_shape)
                                            .into(helper.getView<View>(R.id.iv_item_textsc_imageec_dialog) as ImageView)
                                }
                            }, BaseQuickAdapter.OnItemClickListener { adapter1, view, position1 ->
                                Device.slideshowMode = position1
                                adapter1.notifyDataSetChanged()
                            }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content)))
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                                override fun onClick(dialog: MaterialDialog) {
                                    mData[position].content = mSlideshowMode[Device.slideshowMode]
                                    mAlbumSettingsAdapter.notifyDataSetChanged()
                                }
                            }).show()
                }
            }
            3 -> {
                context?.let {
                    var index = Device.slideIntervalArray.indexOf(Device.slideshowInterval)
                    MaterialDialog.Builder(it).setTitle(R.string.slideshow_interval)
                            .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textsc_imageec_dialog, mSlideshowInterval) {
                                override fun convert(helper: BaseViewHolder, item: String) {
                                    helper.setText(R.id.tv_title_item_textsc_imageec_dialog, item)
                                    Glide.with(mContext)
                                            .load(if (index == helper.layoutPosition) R.drawable.ic_check_circle_blue else R.drawable.gray_ring_shape)
                                            .into(helper.getView<View>(R.id.iv_item_textsc_imageec_dialog) as ImageView)
                                }
                            }, BaseQuickAdapter.OnItemClickListener { adapter1, view, position1 ->
                                index = position1
                                adapter1.notifyDataSetChanged()
                            }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content)))
                            .setLayoutParamsHeight().setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                                override fun onClick(dialog: MaterialDialog) {
                                    if (Device.slideIntervalArray.indexOf(Device.slideshowInterval) != index) {
                                        mData[position].content = mSlideshowInterval[index]
                                        mAlbumSettingsAdapter.notifyDataSetChanged()
                                        Device.slideshowInterval = Device.slideIntervalArray[index]
                                    }
                                }
                            }).show().findViewById<RecyclerView>(R.id.rv_content_dialog_material)
                            .scrollToPosition(index)
                }
            }
            4 -> {
                context?.let {
                    var index = Device.slideshowTransitionEffect
                    MaterialDialog.Builder(it).setTitle(R.string.slideshow_transition_effect)
                            .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textsc_imageec_dialog, mSlideshowAnimation) {
                                override fun convert(helper: BaseViewHolder, item: String) {
                                    helper.setText(R.id.tv_title_item_textsc_imageec_dialog, item)
                                    Glide.with(mContext)
                                            .load(if (index == helper.layoutPosition) R.drawable.ic_check_circle_blue else R.drawable.gray_ring_shape)
                                            .into(helper.getView<View>(R.id.iv_item_textsc_imageec_dialog) as ImageView)
                                }
                            }, BaseQuickAdapter.OnItemClickListener { adapter1, view, position1 ->
                                index = position1
                                adapter1.notifyDataSetChanged()
                            }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content)))
                            .setLayoutParamsHeight().setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                                override fun onClick(dialog: MaterialDialog) {
                                    if (Device.slideshowTransitionEffect != index) {
                                        Device.slideshowTransitionEffect = index
                                        mData[position].content = mSlideshowAnimation[index]
                                        mAlbumSettingsAdapter.notifyDataSetChanged()
                                    }
                                }
                            }).show().findViewById<RecyclerView>(R.id.rv_content_dialog_material)
                            .scrollToPosition(index)
                }
            }
            5 -> context?.let {
                MaterialDialog.Builder(it).setTitle(R.string.delete_photos)
                        .setContent(R.string.delete_photos_desc)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                            override fun onClick(dialog: MaterialDialog) {
                                deleteAllPhotos()
                            }
                        }).show()
            }
            6 -> context?.let {
                MaterialDialog.Builder(it).setTitle(R.string.restore_default_settings)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                            override fun onClick(dialog: MaterialDialog) {
                                Device.resetAlbumSettings()
                                mData[0].isChecked = Device.isPhotoFullScreen
                                mData[1].content = mSlideshow[Device.slideshowArray.indexOf(Device.slideshow)]
                                mData[2].content = mSlideshowMode[Device.slideshowMode]
                                mData[3].content = mSlideshowInterval[Device.slideIntervalArray.indexOf(Device.slideshowInterval)]
                                mData[4].content = mSlideshowAnimation[Device.slideshowTransitionEffect]
                                mAlbumSettingsAdapter.notifyDataSetChanged()
                            }
                        }).show()
            }
        }
    }

    private fun deleteAllPhotos() {
        mDialog?.show()
        GlobalScope.launch {
            val photos = query()
            for (photo in photos) {
                FileUtils.delete(photo.data)
                delete(photo.id)
            }
            mDialog?.dismiss()
        }
    }

    private fun query(): MutableList<Photo> {
        val photos = mutableListOf<Photo>()
        val selection = "${MediaStore.Images.ImageColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'"
        val cursor = context?.contentResolver?.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.ImageColumns._ID), selection, null, null)
        while (cursor?.moveToNext() == true) {
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID))
            val photo = Photo(id)
            photos.add(photo)
        }
        cursor?.close()
        return photos
    }

    /**
     * 删除Images表中指定ID数据
     */
    private fun delete(id: Long) {
        context?.contentResolver?.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "${MediaStore.Images.ImageColumns._ID}=$id", null)
    }
}