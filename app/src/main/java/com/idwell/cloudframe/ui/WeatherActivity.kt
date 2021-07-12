package com.idwell.cloudframe.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Handler
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ConvertUtils

import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.MediaStoreSignature
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.*
import com.idwell.cloudframe.entity.Photo
import com.idwell.cloudframe.http.RetrofitManager
import com.idwell.cloudframe.http.entity.Forecast
import com.idwell.cloudframe.util.MyUtils
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.activity_weather.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

import java.text.DateFormat
import java.util.ArrayList

class WeatherActivity : BaseActivity() {

    private var mPaused = false
    private var mMediaScannerRunnable: MediaScannerRunnable? = null
    private var mMediaScannerReceiver: MediaScannerReceiver? = null

    private var mPhotos = mutableListOf<Photo>()
    private var mPosition = 0
    private lateinit var mWeatherAdapter: BaseQuickAdapter<Forecast.DaysData, BaseViewHolder>
    private lateinit var mWeatherDatas: ArrayList<Forecast.DaysData>
    private lateinit var curDataEntity: Forecast.CurData
    private val mHandler = Handler()
    private val mRunnable = object : Runnable {
        override fun run() {
            slideshow()
        }
    }

    override fun initLayout(): Int {
        return R.layout.activity_weather
    }

    override fun initData() {
        //tv_title_base.setText(R.string.weather)
        iv_title_base.setImageResource(R.drawable.ic_weather)
        onOrientationChanged(resources.configuration.orientation)

        stv_temp_weather.isChecked = Device.displayFahrenheit
        mWeatherDatas = ArrayList()
        //设置适配器
        mWeatherAdapter = object : BaseQuickAdapter<Forecast.DaysData, BaseViewHolder>(R.layout.item_weather, mWeatherDatas) {
            override fun convert(helper: BaseViewHolder, item: Forecast.DaysData?) {
                if (item != null) {
                    //时间
                    val time = item.max.dt * 1000L
                    helper.setText(R.id.tv_weather_item_weather, item.max.weather[0].main)
                    //日期
                    helper.setText(R.id.tv_date_item_weather, DateFormat.getDateInstance().format(time))
                    //温度
                    val tempMin = if (Device.displayFahrenheit) k2f(item.min.main.temp) else k2c(item.min.main.temp)
                    val tempMax = if (Device.displayFahrenheit) k2f(item.max.main.temp) else k2c(item.max.main.temp)
                    helper.setText(R.id.tv_temp_item_weather, mContext.getString(if (Device.displayFahrenheit) R.string.temp_f_range else R.string.temp_c_range, tempMin, tempMax))
                    //天气图标
                    val ivWeather = helper.getView<ImageView>(R.id.iv_weather_item_weather)
                    val path = RetrofitManager.WEATHER_ICON_URL + item.max.weather[0].icon + ".png"
                    Glide.with(mContext).load(path).into(ivWeather)
                }
            }
        }
        rv_weather.adapter = mWeatherAdapter
        //获取天气数据
        if (Device.weather.isNotEmpty()) {
            Device.weatherState.postValue(1)
        }
        Device.weatherState.observe({ lifecycle }, { state ->
            when (state) {
                1 -> {
                    refreshWeather()
                }
            }
        })

        launch(Dispatchers.IO) {
            val images = queryImages()
            launch(Dispatchers.Main) {
                mPhotos.clear()
                mPhotos.addAll(images)
                if (mPhotos.isEmpty()) {
                    iv_slide_weather.setImageResource(R.drawable.photo3_main)
                } else {
                    val photo = mPhotos[0]
                    //val description = Gson().fromJson(photo.description, Description::class.java) ?: Description()
                    val widthHeight = MyUtils.convertWidthHeight(photo, iv_slide_weather.width, iv_slide_weather.height)
                    val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .override(widthHeight[0], widthHeight[1])
                            .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                            .format(DecodeFormat.PREFER_ARGB_8888)
                    Glide.with(this@WeatherActivity).load(photo.data).apply(requestOptions)
                            .transform(RotateTransformation(photo.orientation))
                            .transition(DrawableTransitionOptions.withCrossFade(300))
                            .into(iv_slide_weather)
                    if (mPhotos.size > 1) {
                        mHandler.removeCallbacks(mRunnable)
                        mHandler.postDelayed(mRunnable, 5000)
                    }
                }
            }
        }

        mMediaScannerRunnable = MediaScannerRunnable()

        // 注册广播接收器
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED)
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED)
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        intentFilter.addAction(MyConstants.ACTION_MEDIA_SCANNER_SCAN_FILE)
        intentFilter.addDataScheme("file")
        mMediaScannerReceiver = MediaScannerReceiver()
        registerReceiver(mMediaScannerReceiver, intentFilter)
    }

    override fun initListener() {
        iv_back_base.setOnClickListener(this)
        iv_location_weather.setOnClickListener(this)
        tv_location_weather.setOnClickListener(this)
        stv_temp_weather.setOnClickCheckedListener {
            Device.displayFahrenheit = stv_temp_weather.isChecked
            Device.weatherState.postValue(1)
        }
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v) {
            iv_back_base -> finish()
            iv_location_weather, tv_location_weather -> {
                startActivity(Intent(this, SearchCityActivity::class.java))
            }
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
                    val widthHeight = MyUtils.convertWidthHeight(photo, iv_slide_weather.width, iv_slide_weather.height)
                    val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .override(widthHeight[0], widthHeight[1])
                            .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                            .format(DecodeFormat.PREFER_ARGB_8888)
                    Glide.with(this@WeatherActivity).load(photo.data).apply(requestOptions)
                            .transform(RotateTransformation(photo.orientation))
                            .transition(DrawableTransitionOptions.withCrossFade(300))
                            .into(iv_slide_weather)
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
                        iv_slide_weather.setImageResource(R.drawable.photo3_main)
                    } else {
                        photo = mPhotos[mPosition]
                        //val description = Gson().fromJson(photo.description, Description::class.java) ?: Description()
                        val widthHeight = MyUtils.convertWidthHeight(photo, iv_slide_weather.width, iv_slide_weather.height)
                        val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .override(widthHeight[0], widthHeight[1])
                                .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                                .format(DecodeFormat.PREFER_ARGB_8888)
                        Glide.with(this@WeatherActivity).load(photo.data).apply(requestOptions)
                                .transform(RotateTransformation(photo.orientation))
                                .transition(DrawableTransitionOptions.withCrossFade(300))
                                .into(iv_slide_weather)
                        if (mPhotos.size > 1) {
                            mHandler.removeCallbacks(mRunnable)
                            mHandler.postDelayed(mRunnable, 5000)
                        }
                    }
                }
            }
        }
    }

    private fun refreshWeather() {
        val weather = Gson().fromJson(Device.weather, Forecast::class.java)
        tv_location_weather.text = weather.name
        curDataEntity = weather.cur_data
        tv_date_weather.text = getString(R.string.date_weather, DateFormat.getDateInstance().format(curDataEntity.dt * 1000L), DateFormat.getTimeInstance(DateFormat.SHORT).format(curDataEntity.dt * 1000L))
        tv_temp_weather.text = getString(if (Device.displayFahrenheit) R.string.temp_f else R.string.temp_c, if (Device.displayFahrenheit) k2f(curDataEntity.main.temp) else k2c(curDataEntity.main.temp))
        val iconCurPath = RetrofitManager.WEATHER_ICON_URL + curDataEntity.weather[0].icon + ".png"
        Glide.with(this).load(iconCurPath).into(iv_weather_weather)
        tv_weather_weather.text = curDataEntity.weather[0].main
        mWeatherDatas.clear()
        if (weather.days_data.size > 3) {
            for (i in 0..2) {
                mWeatherDatas.add(weather.days_data[i])
            }
        } else {
            mWeatherDatas.addAll(weather.days_data)
        }
        mWeatherAdapter.notifyDataSetChanged()
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
            iv_slide_weather.setImageResource(R.drawable.photo3_main)
        } else {
            if (mPhotos.size == 1) {
                mPosition = 0
                val photo = mPhotos[0]
                //val description = Gson().fromJson(photo.description, Description::class.java) ?: Description()
                val widthHeight = MyUtils.convertWidthHeight(photo, iv_slide_weather.width, iv_slide_weather.height)
                val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .override(widthHeight[0], widthHeight[1])
                        .signature(MediaStoreSignature(photo.mime_type, photo.date_modified, photo.orientation))
                        .format(DecodeFormat.PREFER_ARGB_8888)
                Glide.with(this@WeatherActivity).load(photo.data).apply(requestOptions)
                        .transform(RotateTransformation(photo.orientation))
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(iv_slide_weather)
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
            constraintSet.constrainWidth(R.id.cl_current_weather, ConstraintSet.WRAP_CONTENT)
            constraintSet.setMargin(R.id.cl_current_weather, 3, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_current_weather, 6, ConvertUtils.dp2px(80f))
            constraintSet.constrainPercentHeight(R.id.cl_current_weather, 0.4f)
            constraintSet.connect(R.id.cl_current_weather, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.cl_current_weather, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)

            constraintSet.setMargin(R.id.cl_slide_weather, 7, ConvertUtils.dp2px(80f))
            constraintSet.connect(R.id.cl_slide_weather, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.cl_slide_weather, ConstraintSet.TOP, R.id.cl_current_weather, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_slide_weather, ConstraintSet.BOTTOM, R.id.cl_current_weather, ConstraintSet.BOTTOM)
            constraintSet.constrainPercentWidth(R.id.cl_slide_weather, 0.3f)

            constraintSet.constrainHeight(R.id.rv_weather, ConstraintSet.WRAP_CONTENT)
            constraintSet.connect(R.id.rv_weather, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.rv_weather, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.rv_weather, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.constrainPercentWidth(R.id.rv_weather, 0.9f)
            constraintSet.setMargin(R.id.rv_weather, 4, ConvertUtils.dp2px(50f))
            //设置布局管理器
            rv_weather.layoutManager = GridLayoutManager(this, 3, RecyclerView.VERTICAL, false)
        } else {
            constraintSet.constrainWidth(R.id.cl_current_weather, ConstraintSet.WRAP_CONTENT)
            constraintSet.setMargin(R.id.cl_current_weather, 3, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_current_weather, 6, ConvertUtils.dp2px(80f))
            constraintSet.constrainPercentHeight(R.id.cl_current_weather, 0.2f)
            constraintSet.connect(R.id.cl_current_weather, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.cl_current_weather, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)

            constraintSet.connect(R.id.cl_slide_weather, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.cl_slide_weather, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.cl_slide_weather, ConstraintSet.TOP, R.id.rv_weather, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.cl_slide_weather, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.constrainPercentWidth(R.id.cl_slide_weather, 0.6f)
            constraintSet.constrainPercentHeight(R.id.cl_slide_weather, 0.28f)

            constraintSet.constrainHeight(R.id.rv_weather, ConstraintSet.WRAP_CONTENT)
            constraintSet.connect(R.id.rv_weather, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.rv_weather, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.rv_weather, ConstraintSet.TOP, R.id.cl_current_weather, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.rv_weather, ConstraintSet.BOTTOM, R.id.cl_slide_weather, ConstraintSet.TOP)
            constraintSet.constrainPercentWidth(R.id.rv_weather, 1.0f)
            //设置布局管理器
            rv_weather.layoutManager = GridLayoutManager(this, 2, RecyclerView.VERTICAL, false)
        }
        constraintSet.applyTo(cl_weather)
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
        unregisterReceiver(mMediaScannerReceiver)
    }

    companion object {
        private val TAG = WeatherActivity::class.java.simpleName
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