package com.idwell.cloudframe.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Handler
import android.provider.MediaStore
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.LogUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.MediaStoreSignature

import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.MyConstants
import com.idwell.cloudframe.widget.calendarview.Calendar
import com.idwell.cloudframe.widget.calendarview.CalendarView
import com.idwell.cloudframe.common.RotateTransformation
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.entity.Photo
import com.idwell.cloudframe.util.MyUtils
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.activity_calendar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : BaseActivity() {

    private var mPaused = false
    private var mMediaScannerRunnable: MediaScannerRunnable? = null
    private var mMediaScannerReceiver: MediaScannerReceiver? = null

    private var mPhotos = mutableListOf<Photo>()
    private var mPosition = 0

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null) {
                when (intent.action) {
                    Intent.ACTION_DATE_CHANGED -> {
                        tv_date_calendar.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(System.currentTimeMillis())
                        tv_week_calendar.text = SimpleDateFormat("EEEE", Locale.getDefault()).format(System.currentTimeMillis())

                        val ci = java.util.Calendar.getInstance()
                        val calendar = Calendar()
                        calendar.year = ci.get(java.util.Calendar.YEAR)
                        calendar.month = ci.get(java.util.Calendar.MONTH) + 1
                        calendar.day = ci.get(java.util.Calendar.DAY_OF_MONTH)
                        LogUtils.dTag(TAG, "${calendar.year}, ${calendar.month}, ${calendar.day}")

                        if (calendarView.mMonthPager != null && calendarView.mDelegate.mCurrentDate.month != calendar.month) {
                            val cur = calendarView.mMonthPager.currentItem
                            val position = cur + 1
                            calendarView.mMonthPager.currentItem = position
                        }

                        if (calendarView.mDelegate.mInnerListener != null) {
                            calendarView.updateCurrentDate()
                            calendarView.mDelegate.mInnerListener.onMonthDateSelected(calendar, true)
                        }
                    }
                }
            }
        }
    }

    private val mHandler = Handler()
    private val mRunnable = object : Runnable {
        override fun run() {
            slideshow()
        }
    }

    override fun initLayout(): Int {
        return R.layout.activity_calendar
    }

    override fun initData() {
        //tv_title_base.setText(R.string.calendar)
        iv_title_base.setImageResource(R.drawable.ic_calendar)
        onOrientationChanged(resources.configuration.orientation)

        tv_date_calendar.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(System.currentTimeMillis())
        tv_week_calendar.text = SimpleDateFormat("EEEE", Locale.getDefault()).format(System.currentTimeMillis())

        launch(Dispatchers.IO) {
            val images = queryImages()
            launch(Dispatchers.Main) {
                mPhotos.clear()
                mPhotos.addAll(images)
                if (mPhotos.isEmpty()) {
                    iv_slide_calendar.setImageResource(R.drawable.photo3_main)
                } else {
                    val photo = mPhotos[0]
                    //val description = Gson().fromJson(photo.description, Description::class.java) ?: Description()

                    val widthHeight = MyUtils.convertWidthHeight(photo, iv_slide_calendar.width, iv_slide_calendar.height)
                    val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .override(widthHeight[0], widthHeight[1])
                            .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                            .format(DecodeFormat.PREFER_ARGB_8888)
                    Glide.with(this@CalendarActivity).load(photo.data).apply(requestOptions)
                            .transform(RotateTransformation(photo.orientation))
                            .transition(DrawableTransitionOptions.withCrossFade(300))
                            .into(iv_slide_calendar)
                    if (mPhotos.size > 1) {
                        mHandler.removeCallbacks(mRunnable)
                        mHandler.postDelayed(mRunnable, 5000)
                    }
                }
            }
        }

        mMediaScannerRunnable = MediaScannerRunnable()

        //注册广播接收器
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_DATE_CHANGED)
        registerReceiver(mBroadcastReceiver, intentFilter)

        // 注册广播接收器
        val msIntentFilter = IntentFilter()
        msIntentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED)
        msIntentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED)
        msIntentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        msIntentFilter.addAction(MyConstants.ACTION_MEDIA_SCANNER_SCAN_FILE)
        msIntentFilter.addDataScheme("file")
        mMediaScannerReceiver = MediaScannerReceiver()
        registerReceiver(mMediaScannerReceiver, msIntentFilter)
    }

    override fun initListener() {
        calendarView.setOnCalendarSelectListener(object : CalendarView.OnCalendarSelectListener {
            override fun onCalendarOutOfRange(calendar: Calendar?) {

            }

            override fun onCalendarSelect(calendar: Calendar?, isClick: Boolean) {
                tv_date_calendar.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar?.timeInMillis)
                tv_week_calendar.text = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar?.timeInMillis)
            }
        })
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v) {
            iv_back_base -> finish()
        }
    }

    override fun initMessageEvent(event: MessageEvent) {

    }

    private fun slideshow() {
        if (++mPosition > mPhotos.size - 1) {
            mPosition = 0
        }
        launch(Dispatchers.IO) {
            var photo = mPhotos[mPosition]
            if (File(photo.data).exists()) {
                launch(Dispatchers.Main) {
                    //val description = Gson().fromJson(photo.description, Description::class.java) ?: Description()
                    val widthHeight = MyUtils.convertWidthHeight(photo, iv_slide_calendar.width, iv_slide_calendar.height)
                    val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .override(widthHeight[0], widthHeight[1])
                            .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                            .format(DecodeFormat.PREFER_ARGB_8888)
                    Glide.with(this@CalendarActivity).load(photo.data).apply(requestOptions)
                            .transform(RotateTransformation(photo.orientation))
                            .transition(DrawableTransitionOptions.withCrossFade(300))
                            .into(iv_slide_calendar)
                    mHandler.removeCallbacks(mRunnable)
                    mHandler.postDelayed(mRunnable, 5000)
                }
            } else {
                val images = queryImages()
                launch(Dispatchers.Main) {
                    mPhotos.clear()
                    mPhotos.addAll(images)
                    if (mPosition > mPhotos.size - 1) {
                        mPosition = 0
                    }
                    if (mPhotos.isEmpty()) {
                        iv_slide_calendar.setImageResource(R.drawable.photo3_main)
                    } else {
                        photo = mPhotos[mPosition]
                        //val description = Gson().fromJson(photo.description, Description::class.java) ?: Description()
                        val widthHeight = MyUtils.convertWidthHeight(photo, iv_slide_calendar.width, iv_slide_calendar.height)
                        val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .override(widthHeight[0], widthHeight[1])
                                .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                                .format(DecodeFormat.PREFER_ARGB_8888)
                        Glide.with(this@CalendarActivity).load(photo.data).apply(requestOptions)
                                .transform(RotateTransformation(photo.orientation))
                                .transition(DrawableTransitionOptions.withCrossFade(300))
                                .into(iv_slide_calendar)
                        if (mPhotos.size > 1) {
                            mHandler.removeCallbacks(mRunnable)
                            mHandler.postDelayed(mRunnable, 5000)
                        }
                    }
                }
            }
        }
    }

    private fun refreshImages() {
        launch(Dispatchers.IO) {
            val images = queryImages()
            launch(Dispatchers.Main) {
                mPhotos.clear()
                mPhotos.addAll(images)
                refreshSlide()
            }
        }
    }

    private fun refreshSlide() {
        if (mPhotos.isEmpty()) {
            iv_slide_calendar.setImageResource(R.drawable.photo3_main)
        } else {
            if (mPhotos.size == 1) {
                mPosition = 0
                val photo = mPhotos[0]
                //val description = Gson().fromJson(photo.description, Description::class.java) ?: Description()
                val widthHeight = MyUtils.convertWidthHeight(photo, iv_slide_calendar.width, iv_slide_calendar.height)
                val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .override(widthHeight[0], widthHeight[1])
                        .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                        .format(DecodeFormat.PREFER_ARGB_8888)
                Glide.with(this@CalendarActivity).load(photo.data).apply(requestOptions)
                        .transform(RotateTransformation(photo.orientation))
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(iv_slide_calendar)
            } else if (mPhotos.size > 1) {
                mHandler.removeCallbacks(mRunnable)
                mHandler.postDelayed(mRunnable, 5000)
            }
        }
    }

    private fun queryImages(): MutableList<Photo> {
        val images = mutableListOf<Photo>()
        val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.SIZE, MediaStore.Images.ImageColumns.DISPLAY_NAME, MediaStore.Images.ImageColumns.TITLE, MediaStore.Images.ImageColumns.DATE_MODIFIED, MediaStore.Images.ImageColumns.MIME_TYPE, MediaStore.Images.ImageColumns.WIDTH, MediaStore.Images.ImageColumns.HEIGHT, MediaStore.Images.ImageColumns.DESCRIPTION, MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.Images.ImageColumns.ORIENTATION), null, null, "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC")
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
            images.add(photo)
        }
        cursor?.close()
        return images
    }

    private fun queryImage(_data: String) {
        launch(Dispatchers.IO) {
            delay(200)
            var photo: Photo? = null
            val selection = "${MediaStore.Images.ImageColumns.DATA} = '$_data'"
            val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.SIZE, MediaStore.Images.ImageColumns.DISPLAY_NAME, MediaStore.Images.ImageColumns.TITLE, MediaStore.Images.ImageColumns.DATE_MODIFIED, MediaStore.Images.ImageColumns.MIME_TYPE, MediaStore.Images.ImageColumns.WIDTH, MediaStore.Images.ImageColumns.HEIGHT, MediaStore.Images.ImageColumns.DESCRIPTION, MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.Images.ImageColumns.ORIENTATION), selection, null, null)
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
                photo = Photo(id, data, size, displayName, title, date_modified, mime_type, width, height, description, datetaken, orientation)
            }
            cursor?.close()
            //Log.d("ooo", "photo: ${photo?.data}")
            if (photo != null) {
                launch(Dispatchers.Main) {
                    if (mPhotos.contains(photo)) {
                        val index = mPhotos.indexOf(photo)
                        mPhotos[index] = photo
                    } else {
                        mPhotos.add(photo)
                        mPhotos.sort()
                    }
                    refreshSlide()
                }
            }
        }
    }

    private fun countImages(): Int {
        val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf("count(*)"), null, null, null)
        cursor?.moveToFirst()
        val count = cursor?.getInt(0) ?: 0
        cursor?.close()
        return count
    }

    private fun onOrientationChanged(orientation: Int) {
        val constraintSet = ConstraintSet()
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            constraintSet.connect(R.id.cl_calendarview_calendar, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.cl_calendarview_calendar, ConstraintSet.END, R.id.iv_separate_calendar, ConstraintSet.START)
            constraintSet.connect(R.id.cl_calendarview_calendar, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_calendarview_calendar, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.constrainHeight(R.id.cl_calendarview_calendar, ConstraintSet.WRAP_CONTENT)
            constraintSet.setMargin(R.id.cl_calendarview_calendar, 3, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_calendarview_calendar, 4, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_calendarview_calendar, 6, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_calendarview_calendar, 7, ConvertUtils.dp2px(50f))

            iv_separate_calendar.setImageResource(R.drawable.ic_divider_calendar_land)
            constraintSet.connect(R.id.iv_separate_calendar, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.iv_separate_calendar, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.iv_separate_calendar, ConstraintSet.TOP, R.id.cl_calendarview_calendar, ConstraintSet.TOP)
            constraintSet.connect(R.id.iv_separate_calendar, ConstraintSet.BOTTOM, R.id.cl_calendarview_calendar, ConstraintSet.BOTTOM)
            constraintSet.constrainWidth(R.id.iv_separate_calendar, ConstraintSet.WRAP_CONTENT)
            constraintSet.constrainHeight(R.id.iv_separate_calendar, ConstraintSet.MATCH_CONSTRAINT)

            constraintSet.connect(R.id.cl_slide_calendar, ConstraintSet.START, R.id.iv_separate_calendar, ConstraintSet.END)
            constraintSet.connect(R.id.cl_slide_calendar, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.cl_slide_calendar, ConstraintSet.TOP, R.id.cl_calendarview_calendar, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_slide_calendar, ConstraintSet.BOTTOM, R.id.cl_calendarview_calendar, ConstraintSet.BOTTOM)
            constraintSet.constrainPercentHeight(R.id.cl_slide_calendar, 1.0f)
            //constraintSet.setMargin(R.id.cl_slide_calendar, 3, ConvertUtils.dp2px(50f))
            //constraintSet.setMargin(R.id.cl_slide_calendar, 4, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_slide_calendar, 6, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_slide_calendar, 7, ConvertUtils.dp2px(50f))
        } else {
            constraintSet.connect(R.id.cl_calendarview_calendar, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.cl_calendarview_calendar, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.cl_calendarview_calendar, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_calendarview_calendar, ConstraintSet.BOTTOM, R.id.iv_separate_calendar, ConstraintSet.TOP)
            constraintSet.constrainWidth(R.id.cl_calendarview_calendar, ConstraintSet.MATCH_CONSTRAINT)
            constraintSet.constrainHeight(R.id.cl_calendarview_calendar, ConstraintSet.WRAP_CONTENT)
            constraintSet.setMargin(R.id.cl_calendarview_calendar, 6, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_calendarview_calendar, 7, ConvertUtils.dp2px(50f))

            iv_separate_calendar.setImageResource(R.drawable.ic_divider_calendar_port)
            constraintSet.connect(R.id.iv_separate_calendar, ConstraintSet.START, R.id.cl_calendarview_calendar, ConstraintSet.START)
            constraintSet.connect(R.id.iv_separate_calendar, ConstraintSet.END, R.id.cl_calendarview_calendar, ConstraintSet.END)
            constraintSet.connect(R.id.iv_separate_calendar, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(R.id.iv_separate_calendar, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.constrainWidth(R.id.iv_separate_calendar, ConstraintSet.MATCH_CONSTRAINT)
            constraintSet.constrainHeight(R.id.iv_separate_calendar, ConstraintSet.WRAP_CONTENT)

            constraintSet.connect(R.id.cl_slide_calendar, ConstraintSet.START, R.id.cl_calendarview_calendar, ConstraintSet.START)
            constraintSet.connect(R.id.cl_slide_calendar, ConstraintSet.END, R.id.cl_calendarview_calendar, ConstraintSet.END)
            constraintSet.connect(R.id.cl_slide_calendar, ConstraintSet.TOP, R.id.iv_separate_calendar, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.cl_slide_calendar, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.constrainWidth(R.id.cl_slide_calendar, ConstraintSet.MATCH_CONSTRAINT)
            /*constraintSet.constrainPercentHeight(R.id.cl_slide_calendar, 0.46f)*/
            constraintSet.setMargin(R.id.cl_slide_calendar, 3, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_slide_calendar, 4, ConvertUtils.dp2px(50f))
        }
        constraintSet.applyTo(cl_calendar)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onOrientationChanged(newConfig.orientation)
    }

    override fun onResume() {
        super.onResume()
        if (mPaused) {
            if (mPhotos.size == countImages()) {
                if (mPhotos.size > 1) {
                    mHandler.removeCallbacks(mRunnable)
                    mHandler.postDelayed(mRunnable, 5000)
                }
            } else {
                mHandler.removeCallbacks(mMediaScannerRunnable)
                mHandler.post(mMediaScannerRunnable)
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
        unregisterReceiver(mBroadcastReceiver)
        unregisterReceiver(mMediaScannerReceiver)
    }

    companion object {
        private val TAG = CalendarActivity::class.java.simpleName
    }

    inner class MediaScannerRunnable : Runnable {
        override fun run() {
            //Log.d("lcs", "MediaScannerRunnable: ${mPhotos.size}, ${countImages()}")
            if (mPhotos.size != countImages()) {
                refreshImages()
                mHandler.postDelayed(mMediaScannerRunnable, 3000)
            }
        }
    }

    inner class MediaScannerReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //Log.d("ooo", intent?.action + ", " + intent?.data + ", " + intent?.data?.path + ", " + intent?.dataString + ", " + intent?.extras)
            if (intent != null) {
                if (intent.action == Intent.ACTION_MEDIA_SCANNER_STARTED) {
                    mHandler.removeCallbacks(mMediaScannerRunnable)
                    mHandler.postDelayed(mMediaScannerRunnable, 3000)
                } else if (intent.action == Intent.ACTION_MEDIA_SCANNER_FINISHED) {
                    mHandler.removeCallbacks(mMediaScannerRunnable)
                    if (mPhotos.size != countImages()) {
                        refreshImages()
                    }
                } else if (intent.action == Intent.ACTION_MEDIA_SCANNER_SCAN_FILE || intent.action == MyConstants.ACTION_MEDIA_SCANNER_SCAN_FILE) {
                    //Log.d("ooo", "${File(intent.data?.path).exists()}, ${mPhotos.size}, ${countImages()}")
                    val path = intent.data?.path
                    if (path != null) {
                        if (File(path).exists()) {
                            queryImage(path)
                        } else {
                            val photo = Photo(path)
                            if (mPhotos.contains(photo)) {
                                mPhotos.remove(photo)
                            }
                            refreshSlide()
                        }
                    }
                }
            }
        }
    }

}