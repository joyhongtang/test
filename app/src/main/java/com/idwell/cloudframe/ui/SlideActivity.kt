package com.idwell.cloudframe.ui

import android.content.*
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.view.ViewCompat
import android.transition.*
import android.view.*
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.FileUtils

import com.blankj.utilcode.util.LogUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.MediaStoreSignature
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.idwell.cloudframe.R
import com.idwell.cloudframe.adapter.SlideBottomBarAdapter
import com.idwell.cloudframe.widget.transformer.Transformer
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.*
import com.idwell.cloudframe.common.Device.configBmpConvertJpg
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.entity.BaseItem
import com.idwell.cloudframe.entity.Photo
import com.idwell.cloudframe.entity.Description
import com.idwell.cloudframe.entity.MultipleItem
import com.idwell.cloudframe.widget.photoview.PhotoView
import com.idwell.cloudframe.util.MyUtils
import com.idwell.cloudframe.util.PicUtils.ImgToJPG
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.widget.MaterialDialog
import com.idwell.cloudframe.widget.photoview.PhotoBackgroundView
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_slide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import org.greenrobot.eventbus.EventBus
import java.io.File
import java.text.DateFormat
import java.util.*

class SlideActivity : BaseActivity(), ViewPager.OnPageChangeListener, BaseQuickAdapter.OnItemClickListener {

    private var mPaused = false
    private var isPlaying = true
    private var mMaterialDialog1: MaterialDialog? = null
    private var mMaterialDialog2: MaterialDialog? = null
    private var mSlideshowAnimation = mutableListOf<String>()
    private var mSlideshowInterval = mutableListOf<String>()

    private val mSlideBottomBarData = mutableListOf<MultipleItem>()
    private lateinit var mSlideBottomBarAdapter: SlideBottomBarAdapter

    private var mState = 0

    var mPosition: Int = 0
    private var mUserId = 0

    private var mPhotos = mutableListOf<Photo>()

    private var mTabPosition: Int = 0

    private lateinit var mMediaBrowser: MediaBrowserCompat
    private var mMediaController: MediaControllerCompat? = null
    private var isPauseBackgroundMusic = false

    private var mSlidePagerAdapter = SlidePagerAdapter()
    private lateinit var mImageView: PhotoBackgroundView
    private lateinit var mPhotoView: PhotoView
    private var isInit = true

    private val onClickListener = View.OnClickListener {
        if (cl_top_bar_slide.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mBarRunnable)
            cl_top_bar_slide.visibility = View.INVISIBLE
            cl_bottom_bar_slide.visibility = View.INVISIBLE
        } else {
            cl_top_bar_slide.visibility = View.VISIBLE
            cl_bottom_bar_slide.visibility = View.VISIBLE
            mHandler.removeCallbacks(mBarRunnable)
            mHandler.postDelayed(mBarRunnable, 8000)
        }
    }

    private val mHandler = Handler()
    private val mRunnable = object : Runnable {
        override fun run() {
            if (isPlaying && mPhotos.size > 1 && Device.slideshowInterval > 0) {
                if (Device.slideshowTransitionEffect == 12) {
                    vp_slide.clearAnimation()
                    vp_slide.setPageTransformer(true, Transformer.getPageTransformer(Random().nextInt(12)).newInstance())
                }
                if (++mPosition > mPhotos.size - 1) {
                    mPosition = 0
                    if (mPhotos.size != countImages()) {
                        refreshImages(0)
                        return
                    }
                }
                if (File(mPhotos[mPosition].data).exists()) {
                    LogUtils.dTag(LOG_TAG, mPhotos[mPosition].data)
                    if (mPosition == 0) {
                        if (Device.slideshowMode == 1) {
                            mPhotos.shuffle()
                        }
                        vp_slide.currentItem = mPosition
                    } else {
                        vp_slide.setCurrentItem(mPosition, true)
                    }
                    mHandler.removeCallbacks(this)
                    mHandler.postDelayed(this, Device.slideshowInterval)
                } else {
                    LogUtils.dTag(LOG_TAG, mPhotos[mPosition].data)
                    fileNotExists()
                }
            }
        }
    }

    private val mBarRunnable = object : Runnable {
        override fun run() {
            mMaterialDialog1?.dismiss()
            mMaterialDialog2?.dismiss()
            cl_top_bar_slide.visibility = View.INVISIBLE
            cl_bottom_bar_slide.visibility = View.INVISIBLE
        }
    }

    override fun initLayout(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
        }
        showTopBar = false
        return R.layout.activity_slide
    }

    override fun initData() {
        //mMediaBrowser = MediaBrowserCompat(this, ComponentName(this, MusicService::class.java), mMediaBrowserConnectionCallback, null)
        mTabPosition = intent.getIntExtra("tab_position", 0)
        //mPosition = intent.getIntExtra("position", 0)
        val photoPath = intent.getStringExtra("photo_path")
        mUserId = intent.getIntExtra("user_id", 0)

        mSlideshowAnimation.addAll(resources.getStringArray(R.array.slideshow_animation))
        mSlideshowInterval.addAll(resources.getStringArray(R.array.slideshow_interval))

        launch(Dispatchers.IO) {
            val images = queryImages()
            if (photoPath != null) {
                val index = images.indexOf(Photo(photoPath))
                if (index == -1) {
                    index == 0
                }
                mPosition = index
            }
            launch(Dispatchers.Main) {
                try {
                    mPhotos.clear()
                    mPhotos.addAll(images)
                    LogUtils.dTag(LOG_TAG, "$mTabPosition, ${mPhotos.size}, $mPosition")
                    if (Device.slideshowMode == 1) {
                        val photo = mPhotos[mPosition]
                        mPhotos.shuffle()
                        mPosition = mPhotos.indexOf(photo)
                    }
                    val photo = mPhotos[mPosition]
                    val description = Gson().fromJson(photo.description, Description::class.java) ?: Description()
                    if (mTabPosition > 4 && description.isNew) {
                        description.isNew = false
                        photo.description = Gson().toJson(description)
                        val contentValues = ContentValues()
                        contentValues.put(MediaStore.Images.ImageColumns.DESCRIPTION, photo.description)
                        contentResolver.update(Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString() + "/${photo.id}"), contentValues, null, null)
                    }
                    mSlideBottomBarData.add(MultipleItem(SlideBottomBarAdapter.ICON, R.drawable.ic_rotate))
                    mSlideBottomBarData.add(MultipleItem(SlideBottomBarAdapter.ICON, if (Device.isPhotoFullScreen) R.drawable.ic_scale_out else R.drawable.ic_scale_in))
                    mSlideBottomBarData.add(MultipleItem(SlideBottomBarAdapter.ICON, R.drawable.ic_pause_slide))
                    mSlideBottomBarData.add(MultipleItem(SlideBottomBarAdapter.ICON, R.drawable.ic_delete_slide))
                    mSlideBottomBarData.add(MultipleItem(SlideBottomBarAdapter.ICON, R.drawable.ic_zoom_in))
                    mSlideBottomBarData.add(MultipleItem(SlideBottomBarAdapter.ICON, R.drawable.ic_settings_slide))
                    mSlideBottomBarData.add(MultipleItem(SlideBottomBarAdapter.ICON, R.drawable.ic_info))
                    if (!arrayOf(0, 2, 3).contains(mTabPosition)) {
                        if (description.favorite) {
                            mSlideBottomBarData.add(MultipleItem(SlideBottomBarAdapter.ICON, R.drawable.ic_favorite_red))
                        } else {
                            mSlideBottomBarData.add(MultipleItem(SlideBottomBarAdapter.ICON, R.drawable.ic_favorite_white))
                        }
                    }else{
                        var title =  getString(R.string.all)
                        try {
                            var mStorages: ArrayList<MultipleItem> =
                                intent.getParcelableArrayListExtra("tab_adapter")
                            title = mStorages.get(mTabPosition).title
                        }catch (e:java.lang.Exception){
                            e.printStackTrace()
                        }
                        when (title) {
                            getString(R.string.favorites) ->  if (description.favorite) {
                                mSlideBottomBarData.add(MultipleItem(SlideBottomBarAdapter.ICON, R.drawable.ic_favorite_red))
                            } else {
                                mSlideBottomBarData.add(MultipleItem(SlideBottomBarAdapter.ICON, R.drawable.ic_favorite_white))
                            }
                            else -> ""
                        }
                    }
                    mSlideBottomBarData.add(MultipleItem(SlideBottomBarAdapter.TEXT, getString(R.string.digital_indicator, mPosition + 1, mPhotos.size)))
                    mSlideBottomBarAdapter = SlideBottomBarAdapter(mSlideBottomBarData)
                    rv_bottom_bar_slide.layoutManager = LinearLayoutManager(this@SlideActivity, LinearLayoutManager.HORIZONTAL, false)
                    rv_bottom_bar_slide.adapter = mSlideBottomBarAdapter
                    mSlideBottomBarAdapter.onItemClickListener = this@SlideActivity

                    if (Device.slideshowTransitionEffect != 12) {
                        vp_slide.setPageTransformer(true, Transformer.getPageTransformer(Device.slideshowTransitionEffect).newInstance())
                    }
                    vp_slide.adapter = mSlidePagerAdapter
                    vp_slide.currentItem = mPosition
                }catch (e:Exception){
                    e.printStackTrace()
                }

            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewCompat.setTransitionName(vp_slide, "image")
            val changeBounds = ChangeBounds()
            changeBounds.duration = 800
            window.sharedElementEnterTransition = changeBounds
            val fade = Fade()
            fade.duration = 800
            window.enterTransition = fade
        }

    }

    override fun initListener() {
        iv_back_slide.setOnClickListener(this)
        vp_slide.addOnPageChangeListener(this)
    }

    override fun initMessageEvent(event: MessageEvent) {

    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> mHandler.removeCallbacks(mRunnable)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                mHandler.removeCallbacks(mBarRunnable)
                mHandler.postDelayed(mBarRunnable, 8000)
                mHandler.removeCallbacks(mRunnable)
                mHandler.postDelayed(mRunnable, Device.slideshowInterval)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v) {
            iv_back_slide -> {
                EventBus.getDefault()
                        .post(MessageEvent(MessageEvent.SLIDE_CUR_IMAGE_DATA, mPhotos[mPosition].data))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAfterTransition()
                } else {
                    finish()
                }
            }
        }
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        when (position) {
            0 -> {
                val photo = mPhotos[mPosition]
                photo.orientation += 90
                if (photo.orientation == 360) {
                    photo.orientation = 0
                }
                mImageView.setRotationTo(photo.orientation.toFloat())
                mPhotoView.setRotationTo(photo.orientation.toFloat())
                val contentValues = ContentValues()
                contentValues.put(MediaStore.Images.ImageColumns.ORIENTATION, photo.orientation)
                contentResolver.update(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues, "${MediaStore.Images.ImageColumns._ID}=${photo.id}", null)
                //Log.d("lcs", "onItemClick: contentResolver.update $photo")
                // 发送广播
                val intent = Intent(MyConstants.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = Uri.fromFile(File(photo.data))
                sendBroadcast(intent)
            }
            1 -> {
                Device.isPhotoFullScreen = !Device.isPhotoFullScreen
                if (Device.isPhotoFullScreen) {
                    mPhotoView.scaleType = ImageView.ScaleType.CENTER_CROP
                    mSlideBottomBarData[position].iconResId = R.drawable.ic_scale_out
                } else {
                    mPhotoView.scaleType = ImageView.ScaleType.FIT_CENTER
                    mSlideBottomBarData[position].iconResId = R.drawable.ic_scale_in
                }
                mSlideBottomBarAdapter.notifyItemChanged(position)
                mSlidePagerAdapter.notifyDataSetChanged()
            }
            2 -> {
                if (isPlaying) {
                    isPlaying = false
                    mHandler.removeCallbacks(mRunnable)
                    mSlideBottomBarData[position].iconResId = R.drawable.ic_play_slide
                    mSlideBottomBarAdapter.notifyItemChanged(position)
                } else {
                    isPlaying = true
                    mHandler.removeCallbacks(mRunnable)
                    mHandler.postDelayed(mRunnable, Device.slideshowInterval)
                    mSlideBottomBarData[position].iconResId = R.drawable.ic_pause_slide
                    mSlideBottomBarAdapter.notifyItemChanged(position)
                }
            }
            3 -> {
                MaterialDialog.Builder(this).setDarkBg().setTitle(R.string.delete_file)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                            override fun onClick(dialog: MaterialDialog) {
                                delete(mPhotos[mPosition])
                            }
                        }).show()
            }
            4 -> {
                val scale = mPhotoView.scale
                val ratio = mPhotoView.ratio
                LogUtils.dTag(LOG_TAG, "$scale, $ratio")
                if (scale < mPhotoView.mediumScale * ratio) {
                    mPhotoView.setScale(mPhotoView.mediumScale * ratio, true)
                } else if (scale >= mPhotoView.mediumScale * ratio && scale < mPhotoView.maximumScale * ratio) {
                    mPhotoView.setScale(mPhotoView.maximumScale * ratio, true)
                } else {
                    mPhotoView.setScale(mPhotoView.minimumScale * ratio, true)
                }
            }
            5 -> {
                val data = mutableListOf<BaseItem>()
                //data.add(BaseItem(R.string.background_music, if (Device.isBackgroundMusicOn) getString(R.string.on) else getString(R.string.off)))
                data.add(BaseItem(R.string.slideshow_interval, mSlideshowInterval[Device.slideIntervalArray.indexOf(Device.slideshowInterval)]))
                data.add(BaseItem(R.string.slideshow_transition_effect, mSlideshowAnimation[Device.slideshowTransitionEffect]))
                mMaterialDialog1 = MaterialDialog.Builder(this).setDarkBg()
                        .setAdapter(object : BaseQuickAdapter<BaseItem, BaseViewHolder>(R.layout.item_text_start_text_end, data) {
                            override fun convert(helper: BaseViewHolder, item: BaseItem?) {
                                if (item != null) {
                                    helper.setText(R.id.tv_title_item_text_start_text_end, item.titleResId)
                                            .setText(R.id.tv_content_item_text_start_text_end, item.content)
                                }
                            }
                        }, BaseQuickAdapter.OnItemClickListener { adapter1, _, position1 ->
                            when (position1) {
                                /*0 -> {
                                    if (data[position1].content == getString(R.string.on)) {
                                        Device.isBackgroundMusicOn = false
                                        data[position1].content = getString(R.string.off)
                                        mMediaController?.transportControls?.pause()
                                    } else {
                                        Device.isBackgroundMusicOn = true
                                        data[position1].content = getString(R.string.on)
                                        isPauseBackgroundMusic = true
                                        mMediaController?.transportControls?.play()
                                    }
                                    adapter1.notifyDataSetChanged()
                                }*/
                                0 -> {
                                    mMaterialDialog1?.hide()
                                    mMaterialDialog2 = MaterialDialog.Builder(this).setDarkBg()
                                            .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textsc_imageec_dialog, mSlideshowInterval) {
                                                override fun convert(helper: BaseViewHolder, item: String) {
                                                    helper.setText(R.id.tv_title_item_textsc_imageec_dialog, item)
                                                            .setTextColor(R.id.tv_title_item_textsc_imageec_dialog, ContextCompat.getColor(mContext, R.color.white))
                                                            .setImageResource(R.id.iv_item_textsc_imageec_dialog, if (Device.slideIntervalArray.indexOf(Device.slideshowInterval) == helper.layoutPosition) R.drawable.ic_check_circle_blue else R.drawable.gray_ring_shape)
                                                }
                                            }, BaseQuickAdapter.OnItemClickListener { adapter2, _, position2 ->
                                                adapter2.notifyDataSetChanged()
                                                mMaterialDialog2?.dismiss()
                                                data[position1].content = mSlideshowInterval[position2]
                                                adapter1.notifyDataSetChanged()
                                                mMaterialDialog1?.show()
                                                Device.slideshowInterval = Device.slideIntervalArray[position2]
                                            }, HorizontalItemDecoration(this, false)).show()
                                    mMaterialDialog2?.findViewById<RecyclerView>(R.id.rv_content_dialog_material)
                                            ?.scrollToPosition(Device.slideIntervalArray.indexOf(Device.slideshowInterval))
                                }
                                1 -> {
                                    mMaterialDialog1?.hide()
                                    mMaterialDialog2 = MaterialDialog.Builder(this).setDarkBg()
                                            .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textsc_imageec_dialog, mSlideshowAnimation) {
                                                override fun convert(helper: BaseViewHolder, item: String) {
                                                    helper.setText(R.id.tv_title_item_textsc_imageec_dialog, item)
                                                            .setTextColor(R.id.tv_title_item_textsc_imageec_dialog, ContextCompat.getColor(mContext, R.color.white))
                                                            .setImageResource(R.id.iv_item_textsc_imageec_dialog, if (Device.slideshowTransitionEffect == helper.layoutPosition) R.drawable.ic_check_circle_blue else R.drawable.gray_ring_shape)
                                                }
                                            }, BaseQuickAdapter.OnItemClickListener { adapter2, _, position2 ->
                                                Device.slideshowTransitionEffect = position2
                                                adapter2.notifyDataSetChanged()
                                                mMaterialDialog2?.dismiss()
                                                data[position1].content = mSlideshowAnimation[position2]
                                                adapter1.notifyDataSetChanged()
                                                mMaterialDialog1?.show()
                                                vp_slide.clearAnimation()
                                                if (Device.slideshowTransitionEffect == 12) {
                                                    vp_slide.setPageTransformer(true, Transformer.getPageTransformer(Random().nextInt(12)).newInstance())
                                                } else {
                                                    vp_slide.setPageTransformer(true, Transformer.getPageTransformer(position2).newInstance())
                                                }
                                            }, HorizontalItemDecoration(this, false))
                                            .setLayoutParamsHeight().show()
                                    mMaterialDialog2?.findViewById<RecyclerView>(R.id.rv_content_dialog_material)
                                            ?.scrollToPosition(Device.slideshowTransitionEffect)
                                }
                            }
                        }, HorizontalItemDecoration(this, false)).show()
            }
            6 -> {
                val photo = mPhotos[mPosition]
                val data = mutableListOf<String>()
                data.add(getString(R.string.file_path, photo.data))
                data.add(getString(R.string.file_size, ConvertUtils.byte2FitMemorySize(photo.size)))
                data.add(getString(R.string.file_date, DateFormat.getDateTimeInstance().format(photo.datetaken)))
                if (mTabPosition > 4) {
                    val description = Gson().fromJson(photo.description, Description::class.java) ?: Description()
                    data.add(getString(R.string.file_desc, description.text))
                }
                mMaterialDialog1 = MaterialDialog.Builder(this).setDarkBg()
                        .setTitle(photo.display_name)
                        .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textsc_dialog, data) {
                            override fun convert(helper: BaseViewHolder, item: String?) {
                                if (item != null) {
                                    helper.setText(R.id.tv_item_textsc_dialog, item)
                                }
                            }
                        }, null).show()
            }
            7 -> {
                var isFavorites:Boolean = false
                var title =  getString(R.string.all)
                try {
                    var mStorages: ArrayList<MultipleItem> =
                        intent.getParcelableArrayListExtra("tab_adapter")
                    title = mStorages.get(mTabPosition).title
                }catch (e:java.lang.Exception){
                    e.printStackTrace()
                }
                when (title) {
                    getString(R.string.favorites) ->  isFavorites = true
                    else -> ""
                }
                if (arrayOf(0, 2, 3).contains(mTabPosition) && !isFavorites) return
                val photo = mPhotos[mPosition]
                val description = Gson().fromJson(photo.description, Description::class.java) ?: Description()
                description.favorite = !description.favorite
                photo.description = Gson().toJson(description)
                val contentValues = ContentValues()
                contentValues.put(MediaStore.Images.ImageColumns.DESCRIPTION, photo.description)
                contentResolver.update(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues, "${MediaStore.Images.ImageColumns._ID}=${photo.id}", null)
                if (description.favorite) {
                    mSlideBottomBarData[position].iconResId = R.drawable.ic_favorite_red
                } else {
                    mSlideBottomBarData[position].iconResId = R.drawable.ic_favorite_white
                }
                mSlideBottomBarAdapter.notifyItemChanged(position)
            }
        }
    }

    override fun onPageScrollStateChanged(p0: Int) {
    }

    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
    }

    override fun onPageSelected(position: Int) {
        try {
            LogUtils.dTag(LOG_TAG, "onPageSelected: $position")
            mPosition = position
            val photo = mPhotos[position]
            val description = Gson().fromJson(photo.description, Description::class.java) ?: Description()
            if (!arrayOf(0, 2, 3).contains(mTabPosition)) {
                if (description.favorite) {
                    mSlideBottomBarData[7].iconResId = R.drawable.ic_favorite_red
                } else {
                    mSlideBottomBarData[7].iconResId = R.drawable.ic_favorite_white
                }
                mSlideBottomBarAdapter.notifyItemChanged(7)
            }else{

                var title =  getString(R.string.all)
                try {
                    var mStorages: ArrayList<MultipleItem> =
                        intent.getParcelableArrayListExtra("tab_adapter")
                    title = mStorages.get(mTabPosition).title
                }catch (e:java.lang.Exception){
                    e.printStackTrace()
                }
                when (title) {
                    getString(R.string.favorites) ->  if (description.favorite) {
                        mSlideBottomBarData[7].iconResId = R.drawable.ic_favorite_red
                        mSlideBottomBarAdapter.notifyItemChanged(7)
                    } else {
                        mSlideBottomBarData[7].iconResId = R.drawable.ic_favorite_white
                        mSlideBottomBarAdapter.notifyItemChanged(7)
                    }
                    else -> ""
                }

            }
            if (mTabPosition > 4 && description.isNew) {
                description.isNew = false
                photo.description = Gson().toJson(description)
                val contentValues = ContentValues()
                contentValues.put(MediaStore.Images.ImageColumns.DESCRIPTION, photo.description)
                contentResolver.update(Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString() + "/${photo.id}"), contentValues, null, null)
            }
            mSlideBottomBarData.last()
                    .title = getString(R.string.digital_indicator, position + 1, mPhotos.size)
            mSlideBottomBarAdapter.notifyItemChanged(mSlideBottomBarData.lastIndex)
        } catch (e: Exception) {
        }
    }

    private fun fileNotExists() {
        launch(Dispatchers.IO) {
            val images = queryImages()
            launch(Dispatchers.Main) {
                if (images.isEmpty()) {
                    finish()
                } else {
                    if (mPosition <= 0) {
                        mPosition = 0
                    }
                    if (mPosition > images.size - 1) {
                        mPosition = 0
                        if (Device.slideshowMode == 1) {
                            images.shuffle()
                        }
                        mPhotos.clear()
                        mPhotos.addAll(images)
                        mSlidePagerAdapter.notifyDataSetChanged()
                        vp_slide.currentItem = mPosition
                    } else {
                        mPhotos.clear()
                        mPhotos.addAll(images)
                        mSlidePagerAdapter.notifyDataSetChanged()
                        vp_slide.setCurrentItem(mPosition, true)
                    }
                    mHandler.removeCallbacks(mRunnable)
                    mHandler.postDelayed(mRunnable, Device.slideshowInterval)
                }
            }
        }
    }

    private fun refreshImages(mode: Int) {
        launch(Dispatchers.IO) {
            val images = queryImages()
            launch(Dispatchers.Main) {
                if (images.isEmpty()) {
                    finish()
                } else {
                    if (mode == 0) {
                        if (Device.slideshowMode == 1) {
                            images.shuffle()
                        }
                        mPhotos.clear()
                        mPhotos.addAll(images)
                        mSlidePagerAdapter.notifyDataSetChanged()
                        vp_slide.currentItem = mPosition
                    } else if (mode == 1) {
                        if (mPosition > images.size - 1) {
                            mPosition = 0
                            if (Device.slideshowMode == 1) {
                                images.shuffle()
                            }
                            mPhotos.clear()
                            mPhotos.addAll(images)
                            mSlidePagerAdapter.notifyDataSetChanged()
                            vp_slide.currentItem = mPosition
                        } else {
                            mPhotos.clear()
                            mPhotos.addAll(images)
                            mSlidePagerAdapter.notifyDataSetChanged()
                            vp_slide.setCurrentItem(mPosition, true)
                        }
                    }
                    mHandler.removeCallbacks(mRunnable)
                    mHandler.postDelayed(mRunnable, Device.slideshowInterval)
                }
            }
        }
    }

    private fun queryImages(): MutableList<Photo> {
        val images = mutableListOf<Photo>()
        var title =  getString(R.string.all)
        try {
            var mStorages: ArrayList<MultipleItem> =
                intent.getParcelableArrayListExtra("tab_adapter")
            title = mStorages.get(mTabPosition).title
        }catch (e:java.lang.Exception){
            e.printStackTrace()
        }
        val selection = when (title) {
            getString(R.string.all) -> "${MediaStore.Images.ImageColumns.DATA} is not null"
            getString(R.string.internal_storage) -> "${MediaStore.Images.ImageColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'"
            getString(R.string.sd_card) -> "${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getSdDir()}/%'"
            getString(R.string.usb) -> "${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getUsbDir()}/%'"
            getString(R.string.favorites) -> "${MediaStore.Images.ImageColumns.DATA} is not null and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"favorite\":true%'"
            else -> "${MediaStore.Images.ImageColumns.DATA} is not null and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"sender_id\":$mUserId%'"
        }
        val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.SIZE, MediaStore.Images.ImageColumns.DISPLAY_NAME, MediaStore.Images.ImageColumns.TITLE, MediaStore.Images.ImageColumns.DATE_MODIFIED, MediaStore.Images.ImageColumns.MIME_TYPE, MediaStore.Images.ImageColumns.WIDTH, MediaStore.Images.ImageColumns.HEIGHT, MediaStore.Images.ImageColumns.DESCRIPTION, MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.Images.ImageColumns.ORIENTATION), selection, null, "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC")
        while (cursor?.moveToNext() == true) {
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID))
            val data = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)) ?: ""
            val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns.SIZE))
            val displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME)) ?: ""
            val title = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.TITLE)) ?: ""
            val date_modified = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_MODIFIED))
            val mime_type = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.MIME_TYPE)) ?: ""
            val width = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.WIDTH))
            val height = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT))
            val description = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DESCRIPTION)) ?: ""
            val datetaken = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN))
            val orientation = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION))
            val photo = Photo(id, data, size, displayName, title, date_modified, mime_type, width, height, description, datetaken, orientation)
           //增加bmp转jpg进行显示
           if(configBmpConvertJpg) {
               try {
                   if (photo.data.toLowerCase().contains(".bmp")) {
                       ImgToJPG(File(photo.data))
                   }
               } catch (e: java.lang.Exception) {
                   e.printStackTrace()
               }
               try {
                   if (!photo.data.toLowerCase().contains(".bmp")) {
                       images.add(photo)
                   }
               } catch (e: java.lang.Exception) {
                   e.printStackTrace()
               }
           }else{
               images.add(photo)
           }
        }
        cursor?.close()
        return images
    }

    private fun countImages(): Int {
        val selection = when (mTabPosition) {
            0 -> "${MediaStore.Images.ImageColumns.DATA} is not null"
            1 -> "${MediaStore.Images.ImageColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'"
            2 -> "${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getSdDir()}/%'"
            3 -> "${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getUsbDir()}/%'"
            4 -> "${MediaStore.Images.ImageColumns.DATA} is not null and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"favorite\":true%'"
            else -> "${MediaStore.Images.ImageColumns.DATA} is not null and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"sender_id\":$mUserId%'"
        }
        val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf("count(*)"), selection, null, null)
        cursor?.moveToFirst()
        val count = cursor?.getInt(0) ?: 0
        cursor?.close()
        return count
    }

    /**
     * 连接状态的回调接口，连接成功时会调用onConnected()方法
     */
    private val mMediaBrowserConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            LogUtils.dTag(LOG_TAG, "onConnected")
            val mediaId = mMediaBrowser.root
            mMediaBrowser.unsubscribe(mediaId)
            mMediaBrowser.subscribe(mediaId, mMediaBrowserSubscriptionCallback)
            try {
                mMediaController = MediaControllerCompat(this@SlideActivity, mMediaBrowser.sessionToken)
                mMediaController?.registerCallback(mMediaControllerCallback)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 向媒体浏览器服务(MediaBrowserService)发起数据订阅请求的回调接口
     */
    private val mMediaBrowserSubscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>) {
            LogUtils.dTag(LOG_TAG, "onChildrenLoaded")
        }
    }

    /**
     * 媒体控制器控制播放过程中的回调接口，可以用来根据播放状态更新UI
     */
    private val mMediaControllerCallback = object : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            if (state != null) {
                mState = state.state
            }
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            if (extras != null) {
                when (event) {
                    MyConstants.MEDIA_SESSION_CHANGE_MUSIC -> {
                        if (Device.isBackgroundMusicOn && mState != PlaybackStateCompat.STATE_PLAYING) {
                            isPauseBackgroundMusic = true
                            mMediaController?.transportControls?.play()
                        } else if (!Device.isBackgroundMusicOn && mState == PlaybackStateCompat.STATE_PLAYING) {
                            mMediaController?.transportControls?.pause()
                        }
                    }
                }
            }
        }
    }

    inner class SlidePagerAdapter : PagerAdapter() {

        override fun getCount(): Int {
            return mPhotos.size
        }

        override fun isViewFromObject(view: View, any: Any): Boolean {
            return view === any
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            LogUtils.dTag(LOG_TAG, "instantiateItem: $position")
            val photo = mPhotos[position]
            //val description = Gson().fromJson(photo.description, Description::class.java) ?: Description()
            val view = View.inflate(this@SlideActivity, R.layout.item_slide, null)
            val imageView = view.findViewById<PhotoBackgroundView>(R.id.iv_background_slide)
            val photoView = view.findViewById<PhotoView>(R.id.pv_slide)
            imageView.setDegrees(photo.orientation.toFloat())
            photoView.setDegrees(photo.orientation.toFloat())
            if (Device.isPhotoFullScreen) {
                photoView.scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                photoView.scaleType = ImageView.ScaleType.FIT_CENTER
            }
            photoView.setOnClickListener(onClickListener)

            val bgRequestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(100, 100)
                    .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                    .format(DecodeFormat.PREFER_RGB_565)
            Glide.with(this@SlideActivity).load(photo.data).apply(bgRequestOptions)
                    .transform(MultiTransformation<Bitmap>(RotateTransformation(photo.orientation), BlurTransformation(25)))
                    .dontAnimate().into(imageView)

            val circularProgressDrawable = CircularProgressDrawable(this@SlideActivity)
            circularProgressDrawable.centerRadius = 30f
            circularProgressDrawable.setColorSchemeColors(ContextCompat.getColor(this@SlideActivity, R.color.main))
            circularProgressDrawable.start()

            val widthHeight = MyUtils.convertWidthHeight(photo)
            val pvRequestOptions = if (Device.isPhotoFullScreen) {
                RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .override(widthHeight[0], widthHeight[1])
                        .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                        .format(DecodeFormat.PREFER_ARGB_8888).centerCrop()
            } else {
                RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .override(widthHeight[0], widthHeight[1])
                        .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                        .format(DecodeFormat.PREFER_ARGB_8888).fitCenter()
            }
            Glide.with(this@SlideActivity).load(photo.data).apply(pvRequestOptions)
                    .placeholder(circularProgressDrawable)
                    .transform(RotateTransformation(photo.orientation))
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            circularProgressDrawable.stop()
                            if (isInit && position == mPosition) {
                                isInit = false
                                mHandler.removeCallbacks(mRunnable)
                                mHandler.postDelayed(mRunnable, Device.slideshowInterval)
                            }
                            return false
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            circularProgressDrawable.stop()
                            if (isInit && position == mPosition) {
                                LogUtils.dTag(LOG_TAG, "position = $position")
                                isInit = false
                                mHandler.removeCallbacks(mRunnable)
                                mHandler.postDelayed(mRunnable, Device.slideshowInterval)
                            }
                            return false
                        }
                    }).into(photoView)
            container.addView(view)
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, any: Any) {
            container.removeView(any as View)
        }

        override fun setPrimaryItem(container: ViewGroup, position: Int, any: Any) {
            super.setPrimaryItem(container, position, any)
            val view = any as View
            mImageView = view.findViewById(R.id.iv_background_slide)
            mPhotoView = view.findViewById(R.id.pv_slide)
        }

        override fun getItemPosition(any: Any): Int {
            return POSITION_NONE
        }
    }

    /**
     * 删除Images表中指定ID数据
     */
    private fun delete(photo: Photo) {
        FileUtils.delete(photo.data)
        EventBus.getDefault().post(MessageEvent(MessageEvent.SLIDE_IMAGE_DELETED, photo.data))
        contentResolver.delete(Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString() + "/${photo.id}"), null, null)

        mPhotos.remove(photo)
        if (mPhotos.isEmpty()) {
            finish()
        } else {
            LogUtils.dTag(LOG_TAG, "mPosition: $mPosition")
            onPageSelected(mPosition)
            mSlidePagerAdapter.notifyDataSetChanged()
            mHandler.removeCallbacks(mRunnable)
            mHandler.postDelayed(mRunnable, Device.slideshowInterval)
        }
    }

    override fun onBackPressed() {
        if(mPhotos.size > 0)
        EventBus.getDefault()
                .post(MessageEvent(MessageEvent.SLIDE_CUR_IMAGE_DATA, mPhotos[mPosition].data))
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        //mMediaBrowser.connect()
        if (mPaused) {
            mHandler.removeCallbacks(mBarRunnable)
            mHandler.postDelayed(mBarRunnable, 8000)
            if (mPhotos.size == countImages()) {
                mHandler.removeCallbacks(mRunnable)
                mHandler.postDelayed(mRunnable, Device.slideshowInterval)
            } else {
                refreshImages(1)
            }
        }
        mPaused = false
    }

    override fun onPause() {
        super.onPause()
        mPaused = true
        if (isPauseBackgroundMusic) {
            mMediaController?.transportControls?.pause()
        }
        //mMediaBrowser.disconnect()
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        //mMediaController = null
    }

    companion object {
        private val LOG_TAG = SlideActivity::class.java.simpleName
    }
}