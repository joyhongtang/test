package com.joyhong.test.photo

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.transition.ChangeBounds
import android.transition.Fade
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.FileUtils
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
import com.joyhong.test.photo.common.RotateTransformation
import com.joyhong.test.BaseTestActivity
import com.joyhong.test.R
import com.joyhong.test.TestMainActivity
import com.joyhong.test.TestResultEnum
import com.joyhong.test.photo.photoview.PhotoBackgroundView
import com.joyhong.test.photo.photoview.PhotoView
import com.joyhong.test.util.MyTestUtils
import com.joyhong.test.util.TestConstant
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_test_slide.*
import kotlinx.coroutines.Dispatchers

class SlideTestActivity : BaseTestActivity(), ViewPager.OnPageChangeListener, BaseQuickAdapter.OnItemClickListener {

    private var mPaused = false
    private var isPlaying = true
    private var mSlideshowAnimation = mutableListOf<String>()
    private var mSlideshowInterval = mutableListOf<String>()

    private var mState = 0

    var mPosition: Int = 0
    private var mUserId = 0

    private var mPhotos = mutableListOf<Photo>()

    private var mTabPosition: Int = 0

    private var isPauseBackgroundMusic = false

    private var mSlidePagerAdapter = SlidePagerAdapter()
    private lateinit var mImageView: PhotoBackgroundView
    private lateinit var mPhotoView: PhotoView
    private var isInit = true

    private val onClickListener = View.OnClickListener {
        if(!TestConstant.isConfigTestMode){
            if (cl_top_bar_slide.visibility == View.VISIBLE) {
                mHandler.removeCallbacks(mBarRunnable)
                cl_top_bar_slide.visibility = View.INVISIBLE
                cl_left_slide.visibility = View.INVISIBLE
                cl_right_slide.visibility = View.INVISIBLE
                cl_bottom_bar_slide.visibility = View.INVISIBLE
            } else {
                cl_left_slide.visibility = View.VISIBLE
                cl_right_slide.visibility = View.VISIBLE
                cl_top_bar_slide.visibility = View.VISIBLE
                cl_bottom_bar_slide.visibility = View.VISIBLE
                mHandler.removeCallbacks(mBarRunnable)
                mHandler.postDelayed(mBarRunnable, 8000)
            }
        }

    }

    private val mHandler = Handler()
    private val mRunnable = object : Runnable {
        override fun run() {
            if(TestConstant.isConfigTestMode){
                return
            }
           
        }
    }

    private val mBarRunnable = object : Runnable {
        override fun run() {
            cl_top_bar_slide.visibility = View.INVISIBLE
            cl_bottom_bar_slide.visibility = View.INVISIBLE
            cl_left_slide.visibility = View.INVISIBLE
            cl_right_slide.visibility = View.INVISIBLE
        }
    }

    override fun initLayout(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
        }
        return R.layout.activity_test_slide
    }

    override fun initData() {
        mTabPosition = intent.getIntExtra("tab_position", 0)
        //mPosition = intent.getIntExtra("position", 0)
        val photoPath = intent.getStringExtra("photo_path")
        //origin code
//        if(TestConstant.isConfigTestMode && !TextUtils.isEmpty(photoPath)){
//            mPhotos.add(Photo(photoPath))
//        }
        if(TestConstant.isConfigTestMode){
            var files = getExternalFilesDir(null)!!.listFiles();
            for (f in files){
                if(f.absolutePath.contains("png"))
                    mPhotos.add(Photo(f.absolutePath))
            }
        }
        mUserId = intent.getIntExtra("user_id", 0)

        mSlideshowAnimation.addAll(resources.getStringArray(R.array.slideshow_animation))
        mSlideshowInterval.addAll(resources.getStringArray(R.array.slideshow_interval))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewCompat.setTransitionName(vp_slide, "image")
            val changeBounds = ChangeBounds()
            changeBounds.duration = 800
            window.sharedElementEnterTransition = changeBounds
            val fade = Fade()
            fade.duration = 800
            window.enterTransition = fade
        }
        if(TestConstant.isConfigTestMode){
            cl_left_slide.visibility = View.INVISIBLE
            cl_right_slide.visibility = View.INVISIBLE
        }
        vp_slide.adapter = mSlidePagerAdapter
        vp_slide.currentItem = mPosition

    }

    override fun initListener() {
        iv_back_slide.setOnClickListener(this)
        cl_left_slide.setOnClickListener(this)
        cl_right_slide.setOnClickListener(this)
        findViewById<View>(R.id.pass).setOnClickListener(this)
        findViewById<View>(R.id.fail).setOnClickListener(this)
        vp_slide.addOnPageChangeListener(this)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> mHandler.removeCallbacks(mRunnable)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                if(TestConstant.isConfigTestMode){
                    cl_right_slide.performClick()
                }else{
                    mHandler.removeCallbacks(mBarRunnable)
                    mHandler.postDelayed(mBarRunnable, 8000)
                    mHandler.removeCallbacks(mRunnable)
                }

            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v) {
            iv_back_slide -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAfterTransition()
                } else {
                    finish()
                }
            }
            cl_left_slide ->{
                mPosition --
                if(mPosition <=0){
                    mPosition = 0
                }
                vp_slide.setCurrentItem(mPosition, true)
            }
            cl_right_slide ->{
                mPosition ++
                if(mPosition >= mPhotos.size){
                    mPosition = 0
                    vp_slide.currentItem = mPosition
                }else
                vp_slide.setCurrentItem(mPosition, true)
            }
            pass ->{
                val testEntity =
                    TestMainActivity.testResult["${TestConstant.PACKAGE_NAME}$localClassName"]
                testEntity!!.testResultEnum = TestResultEnum.PASS
                SPUtils.getInstance().put("${TestConstant.PACKAGE_NAME}$localClassName",1)
                finish()
            }
            fail ->{
                val testEntity2 =
                    TestMainActivity.testResult["${TestConstant.PACKAGE_NAME}$localClassName"]
                testEntity2!!.testResultEnum = TestResultEnum.FAIL
                SPUtils.getInstance().put(testEntity2.getTag(),2)
                finish()
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
        }
    }

    override fun onPageScrollStateChanged(p0: Int) {
    }

    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
    }

    override fun onPageSelected(position: Int) {
        LogUtils.dTag(LOG_TAG, "onPageSelected: $position")
        mPosition = position
        val photo = mPhotos[position]
        if(TestConstant.isConfigTestMode && position == mPhotos.size-1){
            test_result.visibility = View.VISIBLE
            fail.requestFocus()
        }else{
            test_result.visibility = View.INVISIBLE
        }
    }


    private fun refreshImages(mode: Int) {

    }

    private fun queryImages(): MutableList<Photo> {
        val images = mutableListOf<Photo>()
        return images
    }

    private fun countImages(): Int {
        return 0
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
            val view = View.inflate(this@SlideTestActivity, R.layout.item_slide_test, null)
            val imageView = view.findViewById<PhotoBackgroundView>(R.id.iv_background_slide)
            val photoView = view.findViewById<PhotoView>(R.id.pv_slide)
            imageView.setDegrees(photo.orientation.toFloat())
            photoView.setDegrees(photo.orientation.toFloat())
//            if (Device.isPhotoFullScreen) {
//                photoView.scaleType = ImageView.ScaleType.CENTER_CROP
//            } else {
                photoView.scaleType = ImageView.ScaleType.FIT_CENTER
//            }
            photoView.setOnClickListener(onClickListener)

            val bgRequestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(100, 100)
                    .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                    .format(DecodeFormat.PREFER_RGB_565)
            Glide.with(this@SlideTestActivity).load(photo.data).apply(bgRequestOptions)
                    .transform(MultiTransformation<Bitmap>(RotateTransformation(photo.orientation), BlurTransformation(25)))
                    .dontAnimate().into(imageView)

            val circularProgressDrawable = CircularProgressDrawable(this@SlideTestActivity)
            circularProgressDrawable.centerRadius = 30f
            circularProgressDrawable.setColorSchemeColors(ContextCompat.getColor(this@SlideTestActivity, R.color.main))
            circularProgressDrawable.start()

            val widthHeight = MyTestUtils.convertWidthHeight(photo)
            val pvRequestOptions = if (false) {
                RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .override(widthHeight[0], widthHeight[1])
                        .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                        .format(DecodeFormat.PREFER_RGB_565).centerCrop()
            } else {
                RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .override(widthHeight[0], widthHeight[1])
                        .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                        .format(DecodeFormat.PREFER_RGB_565).fitCenter()
            }
            Glide.with(this@SlideTestActivity).load(photo.data).apply(pvRequestOptions)
                    .placeholder(circularProgressDrawable)
                    .transform(RotateTransformation(photo.orientation))
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            circularProgressDrawable.stop()
                            if (isInit && position == mPosition) {
                                isInit = false
                                mHandler.removeCallbacks(mRunnable)
                            }
                            return false
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            circularProgressDrawable.stop()
                            if (isInit && position == mPosition) {
                                LogUtils.dTag(LOG_TAG, "position = $position")
                                isInit = false
                                mHandler.removeCallbacks(mRunnable)
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

    override fun onBackPressed() {
        if(mPhotos.size > 0) {
        }
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        if (mPaused) {
            mHandler.removeCallbacks(mBarRunnable)
            mHandler.postDelayed(mBarRunnable, 8000)
            if (mPhotos.size == countImages()) {
                mHandler.removeCallbacks(mRunnable)
            } else {
                refreshImages(1)
            }
        }
        mPaused = false
    }

    override fun onPause() {
        super.onPause()
        mPaused = true
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        tc_time_slide.handler.removeCallbacksAndMessages(null)
        tc_date_slide.handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private val LOG_TAG = SlideTestActivity::class.java.simpleName
    }
}