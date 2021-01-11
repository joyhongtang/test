package com.idwell.cloudframe.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.SharedElementCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.MediaStoreSignature
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.idwell.cloudframe.R
import com.idwell.cloudframe.adapter.StorageAdapter
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.common.Device.isConfigSdcard
import com.idwell.cloudframe.common.Device.isConfigUSBStorage
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.common.MyConstants
import com.idwell.cloudframe.common.RotateTransformation
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.entity.Description
import com.idwell.cloudframe.entity.MultipleItem
import com.idwell.cloudframe.entity.Photo
import com.idwell.cloudframe.http.entity.User
import com.idwell.cloudframe.util.MyUtils
import com.idwell.cloudframe.util.PicUtils
import com.idwell.cloudframe.widget.GridItemDecoration
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.widget.MaterialDialog
import com.idwell.cloudframe.widget.RingView
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.activity_photo.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.*

class PhotoActivity : BaseActivity(), BaseQuickAdapter.OnItemClickListener {

    private var mPaused = false
    private var mMediaScannerRunnable: MediaScannerRunnable? = null
    private var mMediaScannerReceiver: MediaScannerReceiver? = null

    private var mTempDialog: MaterialDialog? = null

    private val mUsers = mutableListOf<User>()
    //是否正在处理文件
    private var isProcessFile = false
    private val mProcessFileObserver = MutableLiveData<Boolean>()
    //图片适配器
    private val mStorages = ArrayList<MultipleItem>()
    private lateinit var mStorageAdapter: StorageAdapter
    //是否点击存储条目
    private var isStorageItemClicked = false
    private var mPhotos = mutableListOf<Photo>()
    private var mPhotosAll = mutableListOf<Photo>()
    private lateinit var mPhotoAdapter: BaseQuickAdapter<Photo, BaseViewHolder>
    private var mTabPosition: Int = 0
    private var mPosition: Int = 0

    private val mHandler = Handler()
    private var mSlideRunnable: SlideRunnable? = null

    override fun initLayout(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
        }
        return R.layout.activity_photo
    }

    override fun initData() {
        //tv_title_base.setText(R.string.photo)
        iv_more_base.visibility = View.VISIBLE
        iv_title_base.setImageResource(R.drawable.ic_photo)
        onOrientationChanged(resources.configuration.orientation)
        var userId = intent.getIntExtra("userId", -1)
        mStorages.add(MultipleItem(StorageAdapter.PHOTO_STORAGE, R.drawable.ic_all, getString(R.string.all)))
        mStorages.add(MultipleItem(StorageAdapter.PHOTO_STORAGE, R.drawable.ic_internal_storage, getString(R.string.internal_storage)))
        if(isConfigSdcard)
        mStorages.add(MultipleItem(StorageAdapter.PHOTO_STORAGE, R.drawable.ic_sd, getString(R.string.sd_card)))
        if(isConfigUSBStorage)
        mStorages.add(MultipleItem(StorageAdapter.PHOTO_STORAGE, R.drawable.ic_usb, getString(R.string.usb)))
        mStorages.add(MultipleItem(StorageAdapter.PHOTO_STORAGE, R.drawable.ic_favorite, getString(R.string.favorites)))
        mStorageAdapter = StorageAdapter(mStorages)
        rv_storage_photo.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rv_storage_photo.addItemDecoration(HorizontalItemDecoration(ContextCompat.getColor(this, R.color.divider)))
        rv_storage_photo.adapter = mStorageAdapter

        //设置Adapter
        mPhotoAdapter = object : BaseQuickAdapter<Photo, BaseViewHolder>(R.layout.item_photo, mPhotos) {
            override fun convert(helper: BaseViewHolder, item: Photo?) {
                if (item != null) {
                    val imageView = helper.getView<ImageView>(R.id.iv_image_item_photo)
                    if (isMore) {
                        helper.setVisible(R.id.iv_new_item_photo, false)
                        if (item.isSelected) {
                            helper.setImageResource(R.id.iv_selected_item_photo, R.drawable.ic_checked)
                        } else {
                            helper.setImageResource(R.id.iv_selected_item_photo, R.drawable.ic_unchecked)
                        }
                    } else {
                        if (mTabPosition > 4) {
                            val description = Gson().fromJson(item.description, Description::class.java) ?: Description()
                            if (description.isNew) {
                                helper.setVisible(R.id.iv_new_item_photo, true)
                            } else {
                                helper.setVisible(R.id.iv_new_item_photo, false)
                            }
                        } else {
                            helper.setVisible(R.id.iv_new_item_photo, false)
                        }
                        helper.setImageResource(R.id.iv_selected_item_photo, R.drawable.transparent)
                    }

                    //val description = Gson().fromJson(item.description, Description::class.java) ?: Description()

                    val widthHeight = MyUtils.convertWidthHeight(item, imageView.width, imageView.height)
                    val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .override(widthHeight[0], widthHeight[1])
                            .signature(MediaStoreSignature(item.mime_type, item.date_modified, item.orientation))
                            .format(DecodeFormat.PREFER_ARGB_8888)
                    Glide.with(mContext).load(item.data).apply(requestOptions)
                            //.override(imageView.width, imageView.height)
                            .error(R.drawable.ic_broken_image_black_24dp)
                            .transform(RotateTransformation(item.orientation)).into(imageView)
                }
            }

            override fun convertPayloads(helper: BaseViewHolder, item: Photo?, payloads: MutableList<Any>) {
                super.convertPayloads(helper, item, payloads)
                if (item != null) {
                    if (isMore) {
                        helper.setVisible(R.id.iv_new_item_photo, false)
                        if (item.isSelected) {
                            helper.setImageResource(R.id.iv_selected_item_photo, R.drawable.ic_checked)
                        } else {
                            helper.setImageResource(R.id.iv_selected_item_photo, R.drawable.ic_unchecked)
                        }
                    } else {
                        if (mTabPosition > 4) {
                            val description = Gson().fromJson(item.description, Description::class.java) ?: Description()
                            if (description.isNew) {
                                helper.setVisible(R.id.iv_new_item_photo, true)
                            } else {
                                helper.setVisible(R.id.iv_new_item_photo, false)
                            }
                        } else {
                            helper.setVisible(R.id.iv_new_item_photo, false)
                        }
                        helper.setImageResource(R.id.iv_selected_item_photo, R.drawable.transparent)
                    }
                }
            }
        }
        rv_photo.layoutManager = GridLayoutManager(this, 4, RecyclerView.VERTICAL, false)
        rv_photo.addItemDecoration(GridItemDecoration(10))
        rv_photo.adapter = mPhotoAdapter

        if (userId == -1) {
            refreshImages()
        }

        MyDatabase.instance.userDao.queryAccepted().observe({ this.lifecycle }, { users ->
            mStorages.clear()
            mStorages.add(MultipleItem(StorageAdapter.PHOTO_STORAGE, R.drawable.ic_all, getString(R.string.all)))
            mStorages.add(MultipleItem(StorageAdapter.PHOTO_STORAGE, R.drawable.ic_internal_storage, getString(R.string.internal_storage)))
            if(isConfigSdcard)
            mStorages.add(MultipleItem(StorageAdapter.PHOTO_STORAGE, R.drawable.ic_sd, getString(R.string.sd_card)))
            if(isConfigUSBStorage)
            mStorages.add(MultipleItem(StorageAdapter.PHOTO_STORAGE, R.drawable.ic_usb, getString(R.string.usb)))
            mStorages.add(MultipleItem(StorageAdapter.PHOTO_STORAGE, R.drawable.ic_favorite, getString(R.string.favorites)))
            if (users != null) {
                mUsers.clear()
                mUsers.addAll(users)
                for (user in users) {
                    mStorages.add(MultipleItem(StorageAdapter.PHOTO_USER, user))
                }
            }
            if (userId == -1) {
                mStorageAdapter.notifyDataSetChanged()
            } else {
                val user = User()
                user.id = userId
                var totalDir = 5
                if(!isConfigSdcard){
                    totalDir = totalDir - 1
                }
                if(!isConfigUSBStorage){
                    totalDir = totalDir - 1
                }
                val index = mUsers.indexOf(user) + totalDir
                mTabPosition = index
                mStorageAdapter.mPosition = mTabPosition
                mStorageAdapter.notifyDataSetChanged()
                refreshImages()
                userId = -1
            }
        })

        mProcessFileObserver.observe({ this.lifecycle }, { processFile ->
            when (processFile) {
                true -> {
                    isProcessFile = true
                    if (Device.slideshow > 0) {
                        mHandler.removeCallbacks(mSlideRunnable)
                    }
                }
                false -> {
                    isProcessFile = false
                    if (Device.slideshow > 0) {
                        mPosition = 0
                        mHandler.removeCallbacks(mSlideRunnable)
                        mHandler.postDelayed(mSlideRunnable, Device.slideshow)
                    }
                }
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setExitSharedElementCallback(object : SharedElementCallback() {
                override fun onMapSharedElements(names: MutableList<String>?, sharedElements: MutableMap<String, View>?) {
                    super.onMapSharedElements(names, sharedElements)
                    val imageView = mPhotoAdapter.getViewByPosition(rv_photo, mPosition, R.id.iv_image_item_photo)
                    if (imageView != null) {
                        sharedElements?.put("image", imageView)
                    }
                }
            })
        }

        mSlideRunnable = SlideRunnable()
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
        mStorageAdapter.onItemClickListener = this
        mPhotoAdapter.onItemClickListener = this
    }

    override fun initMessageEvent(event: MessageEvent) {
        when (event.message) {
            MessageEvent.SLIDE_CUR_IMAGE_DATA -> {
                val photo = Photo()
                photo.data = event.text
                mPosition = mPhotos.indexOf(photo)
                rv_photo.scrollToPosition(mPosition)
            }
            MessageEvent.SLIDE_IMAGE_DELETED -> {
                val photo = Photo()
                photo.data = event.text
                val index = mPhotos.indexOf(photo)
                mPhotoAdapter.remove(index)
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                if (Device.slideshow > 0) {
                    mHandler.removeCallbacks(mSlideRunnable)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (Device.slideshow > 0) {
                    mHandler.removeCallbacks(mSlideRunnable)
                    mHandler.postDelayed(mSlideRunnable, Device.slideshow)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v) {
            iv_more_base -> {
                isMore = !isMore
                for (index in mIndexes) {
                    mPhotos[index].isSelected = false
                }
                mIndexes.clear()
                refreshMore()
                mPhotoAdapter.notifyItemRangeChanged(0, mPhotos.size, "1")
            }
            iv_check_base -> {
                val isChecked = mIndexes.size == mPhotos.size
                mIndexes.clear()
                for (index in mPhotos.indices) {
                    val photo = mPhotos[index]
                    photo.isSelected = !isChecked
                    if (photo.isSelected) {
                        mIndexes.add(index)
                    }
                }
                refreshMore()
                mPhotoAdapter.notifyItemRangeChanged(0, mPhotos.size, "1")
            }
            iv_copy_base -> {
                when (mTabPosition) {
                    2, 3 -> {
                        copyFileToInternalStorage()
                    }
                    else -> {
                        copyFileToExternalStorage()
                    }
                }
            }
            iv_delete_base -> {
                MaterialDialog.Builder(this).setTitle(R.string.delete_the_selected_files)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                            override fun onClick(dialog: MaterialDialog) {
                                val view = View.inflate(this@PhotoActivity, R.layout.view_process, null)
                                dialog.setContentView(view)
                                dialog.setCanceledOnTouchOutside(false)
                                dialog.setCancelable(false)
                                dialog.show()
                                mTempDialog = dialog
                                mProcessFileObserver.postValue(true)
                                val indexes = ArrayList<Int>()
                                indexes.addAll(mIndexes)
                                launch(Dispatchers.IO) {
                                    val photos = mutableListOf<Photo>()
                                    for (index in indexes) {
                                        if (mPaused) {
                                            return@launch
                                        }
                                        val photo = mPhotos[index]
                                        FileUtils.delete(photo.data)
                                        if(Device.configBmpConvertJpg) {
                                            var bmpFile = photo.data.substring(
                                                0,
                                                photo.data.lastIndexOf('.') + 1
                                            ) + PicUtils.BMP

                                            var bmpUpderCaseFile = photo.data.substring(
                                                0,
                                                photo.data.lastIndexOf('.') + 1
                                            ) + PicUtils.BMP_UPPER_CASE
                                            FileUtils.delete(bmpFile)
                                            FileUtils.delete(bmpUpderCaseFile)
                                            MyUtils.scanFile(bmpFile)
                                            MyUtils.scanFile(bmpUpderCaseFile)
                                        }

                                        delete(photo.id)
                                        photos.add(photo)
                                    }
                                    launch(Dispatchers.Main) {
                                        isMore = false
                                        for (photo in photos) {
                                            val index = mPhotos.indexOf(photo)
                                            if (index != -1) {
                                                mPhotoAdapter.remove(index)
                                                if (index > 0) {
                                                    mPhotoAdapter.notifyItemRangeChanged(0, index)
                                                }
                                            }
                                        }
                                        mIndexes.clear()
                                        refreshMore()
                                        mProcessFileObserver.postValue(false)
                                    }
                                    dialog.dismiss()
                                }
                            }
                        }).show()
            }
        }
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        when (adapter) {
            mStorageAdapter -> {
                if (!isStorageItemClicked) {
                    isStorageItemClicked = true
                    mTabPosition = position
                    mIndexes.clear()
                    mStorageAdapter.mPosition = position
                    mStorageAdapter.notifyDataSetChanged()
                    mPhotoAdapter.setNewData(null)
                    //Log.d("lcs", "onItemClick: 111")
                    launch(Dispatchers.IO) {
                        //Log.d("lcs", "onItemClick: 222")
                        val images = queryImages()
                        launch(Dispatchers.Main) {
                            //Log.d("lcs", "onItemClick: 333")
                            mPhotos.clear()
                            mPhotos.addAll(images)
                            isMore = false
                            mIndexes.clear()
                            refreshMore()
                            mPhotoAdapter.setNewData(mPhotos)
                            isStorageItemClicked = false
                        }
                    }
                }
            }
            mPhotoAdapter -> {
                if (isMore) {
                    if (mPhotos[position].isSelected) {
                        mPhotos[position].isSelected = false
                        mIndexes.remove(position)
                    } else {
                        mPhotos[position].isSelected = true
                        mIndexes.add(position)
                    }
                    refreshMore()
                    mPhotoAdapter.notifyItemChanged(position)
                } else {
                    if (File(mPhotos[position].data).exists()) {
                        mPosition = position
                        val imageView = view?.findViewById<ImageView>(R.id.iv_image_item_photo)
                        val intent = Intent(this, SlideActivity::class.java)
                        intent.putParcelableArrayListExtra("tab_adapter",mStorages)
                        intent.putExtra("tab_position", mTabPosition)
                        //intent.putExtra("position", position)
                        intent.putExtra("photo_path", mPhotos[position].data)
                        var totalDir = 4
                        if(!isConfigSdcard){
                            totalDir = totalDir - 1
                        }
                        if(!isConfigUSBStorage){
                            totalDir = totalDir - 1
                        }
                        if (mTabPosition > totalDir) {
                            intent.putExtra("user_id", mStorages[mTabPosition].user.id)
                        }
                        //Log.d("lcs", "onItemClick: $position, ${mPhotos[position]}")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && imageView != null) {
                            startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(this, imageView, "image").toBundle())
                        } else {
                            startActivity(intent)
                        }
                    } else {
                        mPosition = 0
                        launch(Dispatchers.IO) {
                            val images = queryImages()
                            launch(Dispatchers.Main) {
                                mPhotos.clear()
                                mPhotos.addAll(images)
                                mPhotoAdapter.setNewData(mPhotos)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun copyFileToInternalStorage() {
        MaterialDialog.Builder(this).setContent(R.string.import_files_to_memory)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                    override fun onClick(dialog: MaterialDialog) {
                        val view = View.inflate(this@PhotoActivity, R.layout.view_ring, null)
                        val ringView = view.findViewById<RingView>(R.id.rv_view_ring)
                        dialog.setContentView(view)
                        dialog.setCanceledOnTouchOutside(false)
                        dialog.setCancelable(false)
                        dialog.show()
                        mTempDialog = dialog
                        mProcessFileObserver.postValue(true)
                        launch(Dispatchers.IO) {
                            var totalSize = 0L
                            var writtenSize = 0L
                            for (music in mPhotos) {
                                if (music.isSelected) {
                                    totalSize += music.size
                                }
                            }
                            for (photo in mPhotos) {
                                if (mPaused) {
                                    return@launch
                                }
                                if (photo.isSelected) {
                                    var inputStream: InputStream? = null
                                    var outputStream: OutputStream? = null
                                    try {
                                        val srcFile = File(photo.data)
                                        val parent = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                                        var destFile = File(parent, srcFile.name)

                                        if (destFile.exists()) {
                                            var suffix = 1
                                            val endIndex = srcFile.name.lastIndexOf('.')
                                            val fileName = if (endIndex == -1) srcFile.name else srcFile.name.substring(0, endIndex)
                                            val fileType = if (endIndex == -1) "" else srcFile.name.substring(endIndex)
                                            while (destFile.exists()) {
                                                destFile = File(parent, "$fileName-$suffix$fileType")
                                                suffix++
                                            }
                                        }

                                        inputStream = FileInputStream(srcFile)
                                        outputStream = BufferedOutputStream(FileOutputStream(destFile))
                                        val byteSize = 8192
                                        val data = ByteArray(byteSize)
                                        var len: Int
                                        while (inputStream.read(data, 0, byteSize).also {
                                                    len = it
                                                } != -1) {
                                            outputStream.write(data, 0, len)
                                            writtenSize += len.toLong()
                                            ringView.setProgress((writtenSize * 100 / totalSize).toInt())
                                        }
                                        MyUtils.scanFile(destFile.absolutePath)
                                        //Log.d("lcs", "copyFileToInternalStorage: mPaused = ${mPaused}, progress = ${writtenSize * 100f / totalSize}")
                                    } catch (e: FileNotFoundException) {
                                        e.printStackTrace()
                                        writtenSize += photo.size
                                        ringView.setProgress((writtenSize * 100 / totalSize).toInt())
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    } finally {
                                        try {
                                            inputStream?.close()
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                        try {
                                            outputStream?.close()
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }

                            launch(Dispatchers.Main) {
                                isMore = false
                                for (index in mIndexes) {
                                    mPhotos[index].isSelected = false
                                }
                                mIndexes.clear()
                                refreshMore()
                                mPhotoAdapter.notifyItemRangeChanged(0, mPhotos.size, "1")
                                mProcessFileObserver.postValue(false)
                                ToastUtils.showShort(R.string.import_success)
                                dialog.dismiss()
                                //Log.d("lcs", "copyFileToInternalStorage: dialog.dismiss()")
                            }
                        }
                    }
                }).show()
    }

    private fun copyFileToExternalStorage() {
        val devNames = mutableListOf<Int>()
        var pos = 0
        if (MyUtils.isMountSd()) {
            devNames.add(R.string.sd_card)
        }
        if (MyUtils.isMountUsb()) {
            devNames.add(R.string.usb)
        }
        MaterialDialog.Builder(this).setTitle(R.string.export_files_to_external_storage)
                .setAdapter(object : BaseQuickAdapter<Int, BaseViewHolder>(R.layout.item_textsc_imageec_dialog, devNames) {
                    override fun convert(helper: BaseViewHolder, item: Int?) {
                        if (item != null) {
                            val position = helper.layoutPosition
                            helper.setText(R.id.tv_title_item_textsc_imageec_dialog, item)
                            if (pos == position) Glide.with(mContext).load(R.drawable.ic_check_circle_blue).into(helper.getView(R.id.iv_item_textsc_imageec_dialog))
                            else Glide.with(mContext).load(R.drawable.gray_ring_shape).into(helper.getView(R.id.iv_item_textsc_imageec_dialog))
                        }
                    }
                }, BaseQuickAdapter.OnItemClickListener { adapter, _, position ->
                    pos = position
                    adapter.notifyDataSetChanged()
                }).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                    override fun onClick(dialog: MaterialDialog) {
                        val view = View.inflate(this@PhotoActivity, R.layout.view_ring, null)
                        val ringView = view.findViewById<RingView>(R.id.rv_view_ring)
                        dialog.setContentView(view)
                        dialog.setCanceledOnTouchOutside(false)
                        dialog.setCancelable(false)
                        dialog.show()
                        mTempDialog = dialog
                        mProcessFileObserver.postValue(true)
                        launch(Dispatchers.IO) {
                            var totalSize = 0L
                            var writtenSize = 0L
                            for (photo in mPhotos) {
                                if (photo.isSelected) {
                                    totalSize += photo.size
                                }
                            }
                            for (photo in mPhotos) {
                                if (mPaused) {
                                    return@launch
                                }
                                if (photo.isSelected) {
                                    var inputStream: InputStream? = null
                                    var outputStream: OutputStream? = null
                                    try {
                                        val srcFile = File(photo.data)
                                        val parent = if (devNames[pos] == R.string.sd_card) MyUtils.getSdDir() else MyUtils.getUsbDir()
                                        var destFile = File(parent, srcFile.name)
                                        //LogUtils.dTag("lcs", destFile.canWrite())

                                        if (destFile.exists()) {
                                            var suffix = 1
                                            val endIndex = srcFile.name.lastIndexOf('.')
                                            val fileName = if (endIndex == -1) srcFile.name else srcFile.name.substring(0, endIndex)
                                            val fileType = if (endIndex == -1) "" else srcFile.name.substring(endIndex)
                                            while (destFile.exists()) {
                                                destFile = File(parent, "$fileName-$suffix$fileType")
                                                suffix++
                                            }
                                        }

                                        inputStream = FileInputStream(srcFile)
                                        outputStream = BufferedOutputStream(FileOutputStream(destFile))
                                        val byteSize = 8192
                                        val data = ByteArray(byteSize)
                                        var len: Int
                                        while (inputStream.read(data, 0, byteSize).also {
                                                    len = it
                                                } != -1) {
                                            outputStream.write(data, 0, len)
                                            writtenSize += len
                                            ringView.setProgress((writtenSize * 100 / totalSize).toInt())
                                        }
                                        MyUtils.scanFile(destFile.absolutePath)
                                    } catch (e: FileNotFoundException) {
                                        e.printStackTrace()
                                        writtenSize += photo.size
                                        ringView.setProgress((writtenSize * 100 / totalSize).toInt())
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    } finally {
                                        try {
                                            inputStream?.close()
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                        try {
                                            outputStream?.close()
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }

                            launch(Dispatchers.Main) {
                                isMore = false
                                for (index in mIndexes) {
                                    mPhotos[index].isSelected = false
                                }
                                mIndexes.clear()
                                refreshMore()
                                mPhotoAdapter.notifyItemRangeChanged(0, mPhotos.size, "1")
                                mProcessFileObserver.postValue(false)
                                ToastUtils.showShort(R.string.export_success)
                                dialog.dismiss()
                            }
                        }
                    }
                }).show()
    }

    private fun refreshMore() {
        if (mPhotos.isEmpty()) {
            cl_more_base.visibility = View.GONE
        } else {
            if (isMore) {
                if (mIndexes.isEmpty()) {
                    iv_check_base.setImageResource(R.drawable.ic_unchecked)
                    iv_delete_base.setImageResource(R.drawable.ic_undelete)
                    iv_delete_base.isClickable = false
                    tv_total_base.visibility = View.GONE
                } else {
                    if (mIndexes.size == mPhotos.size) {
                        iv_check_base.setImageResource(R.drawable.ic_checked)
                    } else {
                        iv_check_base.setImageResource(R.drawable.ic_unchecked)
                    }
                    iv_delete_base.setImageResource(R.drawable.ic_delete)
                    iv_delete_base.isClickable = true
                    tv_total_base.text = "${mIndexes.size}"
                    tv_total_base.visibility = View.VISIBLE
                }
                iv_check_base.visibility = View.VISIBLE
                iv_delete_base.visibility = View.VISIBLE
                if (mTabPosition == 0 || !MyUtils.isMountSdOrUsb()) {
                    iv_copy_base.visibility = View.GONE
                } else {
                    if (mIndexes.isEmpty()) {
                        iv_copy_base.setImageResource(R.drawable.ic_uncopy)
                        iv_copy_base.isClickable = false
                    } else {
                        iv_copy_base.setImageResource(R.drawable.ic_copy)
                        iv_copy_base.isClickable = true
                    }
                    iv_copy_base.visibility = View.VISIBLE
                }
                iv_more_base.setImageResource(R.drawable.ic_cancel)
            } else {
                tv_total_base.visibility = View.GONE
                iv_check_base.visibility = View.GONE
                iv_delete_base.visibility = View.GONE
                iv_copy_base.visibility = View.GONE
                iv_more_base.setImageResource(R.drawable.ic_more)
            }
            cl_more_base.visibility = View.VISIBLE
        }
    }

    private fun refreshImages() {
        launch(Dispatchers.IO) {
            val images = queryImages()
            launch(Dispatchers.Main) {
                mPhotos.clear()
                mPhotos.addAll(images)
                if (isMore) {
                    isMore = false
                    mIndexes.clear()
                    refreshMore()
                }
                mPhotoAdapter.setNewData(mPhotos)
                if (Device.slideshow > 0) {
                    mHandler.removeCallbacks(mSlideRunnable)
                    mHandler.postDelayed(mSlideRunnable, Device.slideshow)
                }
            }
        }
    }

    private fun queryImages(): MutableList<Photo> {
        mPhotosAll.clear()
        val images = mutableListOf<Photo>()
        var title = mStorages.get(mTabPosition).title
        val selection = when (title) {
            getString(R.string.all) -> "${MediaStore.Images.ImageColumns.DATA} is not null"
            getString(R.string.internal_storage) -> "${MediaStore.Images.ImageColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'"
            getString(R.string.sd_card) -> "${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getSdDir()}/%'"
            getString(R.string.usb) -> "${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getUsbDir()}/%'"
            getString(R.string.favorites) -> "${MediaStore.Images.ImageColumns.DATA} is not null and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"favorite\":true%'"
            else -> "${MediaStore.Images.ImageColumns.DATA} is not null and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"sender_id\":${mStorages[mTabPosition].user.id}%'"
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
            if(Device.configBmpConvertJpg) {
                try {
                    if (photo.data.toLowerCase().contains(".bmp")) {
                        var file = PicUtils.ImgToJPG(File(photo.data))
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
            mPhotosAll.add(photo)
        }
        cursor?.close()
        return images
    }

    private fun queryImage(_data: String) {
        launch(Dispatchers.IO) {
            delay(200)
            var photo: Photo? = null

            var selection =""
            if(isConfigUSBStorage && !isConfigSdcard){
                selection = when (mTabPosition) {
                    0 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data'"
                    1 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'"
                    2 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getUsbDir()}/%'"
                    3 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"favorite\":true%'"
                    else -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"sender_id\":${mStorages[mTabPosition].user.id}%'"
                }
            }else if(!isConfigUSBStorage && isConfigSdcard){
                selection = when (mTabPosition) {
                    0 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data'"
                    1 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'"
                    2 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getSdDir()}/%'"
                    3 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"favorite\":true%'"
                    else -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"sender_id\":${mStorages[mTabPosition].user.id}%'"
                }
            }else if(!isConfigUSBStorage && !isConfigSdcard){
                selection = when (mTabPosition) {
                    0 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data'"
                    1 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'"
                    2 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"favorite\":true%'"
                    else -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"sender_id\":${mStorages[mTabPosition].user.id}%'"
                }
            }  else {
                selection = when (mTabPosition) {
                    0 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data'"
                    1 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'"
                    2 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getSdDir()}/%'"
                    3 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getUsbDir()}/%'"
                    4 -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"favorite\":true%'"
                    else -> "${MediaStore.Images.ImageColumns.DATA} = '$_data' and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"sender_id\":${mStorages[mTabPosition].user.id}%'"
                }
            }

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
                        mPhotoAdapter.setData(index, photo)
                    } else {
                        mPhotos.add(photo)
                        mPhotos.sort()
                        if (mPhotos.size == 1) {
                            //Log.d("ooo", "notifyDataSetChanged: ${mPhotos.size}")
                            mPhotoAdapter.notifyDataSetChanged()
                        } else {
                            val index = mPhotos.indexOf(photo)
                            mPhotoAdapter.notifyItemInserted(index)
                            //Log.d("ooo", "notifyItemInserted: $index")
                        }
                    }
                }
            }
        }
    }

    private fun countImages(): Int {
        val selection = when (mTabPosition) {
            0 -> "${MediaStore.Images.ImageColumns.DATA} is not null"
            1 -> "${MediaStore.Images.ImageColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'"
            2 -> "${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getSdDir()}/%'"
            3 -> "${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getUsbDir()}/%'"
            4 -> "${MediaStore.Images.ImageColumns.DATA} is not null and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"favorite\":true%'"
            else -> "${MediaStore.Images.ImageColumns.DATA} is not null and ${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"sender_id\":${mStorages[mTabPosition].user.id}%'"
        }
        val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf("count(*)"), selection, null, null)
        cursor?.moveToFirst()
        val count = cursor?.getInt(0) ?: 0
        cursor?.close()
        return count
    }

    /**
     * 删除Images表中指定ID数据
     */
    private fun delete(id: Long) {
        contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "${MediaStore.Images.ImageColumns._ID}=$id", null)
    }

    private fun onOrientationChanged(orientation: Int) {
        val constraintSet = ConstraintSet()
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            constraintSet.connect(R.id.rv_storage_photo, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.rv_storage_photo, ConstraintSet.END, R.id.rv_photo, ConstraintSet.START)
            constraintSet.connect(R.id.rv_storage_photo, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(R.id.rv_storage_photo, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.setHorizontalWeight(R.id.rv_storage_photo, 0.3f)
            constraintSet.setVerticalWeight(R.id.rv_storage_photo, 1.0f)
            constraintSet.setMargin(R.id.rv_storage_photo, 3, ConvertUtils.dp2px(2f))
            constraintSet.connect(R.id.rv_photo, ConstraintSet.START, R.id.rv_storage_photo, ConstraintSet.END)
            constraintSet.connect(R.id.rv_photo, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.rv_photo, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(R.id.rv_photo, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.setHorizontalWeight(R.id.rv_photo, 0.7f)
            constraintSet.setVerticalWeight(R.id.rv_photo, 1.0f)
            constraintSet.setMargin(R.id.rv_photo, 6, ConvertUtils.dp2px(10f))
            constraintSet.setMargin(R.id.rv_photo, 7, ConvertUtils.dp2px(10f))
        } else {
            constraintSet.connect(R.id.rv_storage_photo, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.rv_storage_photo, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.rv_storage_photo, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(R.id.rv_storage_photo, ConstraintSet.BOTTOM, R.id.rv_photo, ConstraintSet.TOP)
            constraintSet.constrainPercentWidth(R.id.rv_storage_photo, 1.0f)
            constraintSet.setVerticalWeight(R.id.rv_storage_photo, 0.4f)
            constraintSet.setMargin(R.id.rv_storage_photo, 3, ConvertUtils.dp2px(2f))
            constraintSet.connect(R.id.rv_photo, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.rv_photo, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.rv_photo, ConstraintSet.TOP, R.id.rv_storage_photo, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.rv_photo, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.constrainPercentWidth(R.id.rv_photo, 1.0f)
            constraintSet.setVerticalWeight(R.id.rv_photo, 0.6f)
            constraintSet.setMargin(R.id.rv_storage_photo, 6, ConvertUtils.dp2px(10f))
            constraintSet.setMargin(R.id.rv_storage_photo, 7, ConvertUtils.dp2px(10f))
        }
        constraintSet.applyTo(cl_photo)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LogUtils.dTag(LOG_TAG, "onConfigurationChanged")
        onOrientationChanged(newConfig.orientation)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null && !isProcessFile) {
            val userId = intent.getIntExtra("userId", -1)
            val user = User()
            user.id = userId
            var totalDir = 5
            if(!isConfigSdcard){
                totalDir = totalDir - 1
            }
            if(!isConfigUSBStorage){
                totalDir = totalDir - 1
            }
            var index = mUsers.indexOf(user) + totalDir
            if (mTabPosition != index) {
                mTabPosition = index
                mStorageAdapter.mPosition = mTabPosition
                mStorageAdapter.notifyDataSetChanged()
                launch(Dispatchers.IO) {
                    val images = queryImages()
                    launch(Dispatchers.Main) {
                        mPhotos.clear()
                        mPhotos.addAll(images)
                        isMore = false
                        refreshMore()
                        mPhotoAdapter.setNewData(mPhotos)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mPaused) {
            if (mPhotos.size == countImages()) {
                if (Device.slideshow > 0) {
                    mHandler.removeCallbacks(mSlideRunnable)
                    mHandler.postDelayed(mSlideRunnable, Device.slideshow)
                }
            } else {
                mHandler.removeCallbacks(mMediaScannerRunnable)
                mHandler.post(mMediaScannerRunnable)
            }
        }
        if(mTabPosition == 4){
            onItemClick(mStorageAdapter, null, 4)
        }
        mPaused = false
    }

    override fun onPause() {
        super.onPause()
        mPaused = true
        if (isMore && mTempDialog != null) {
            isMore = false
            for (index in mIndexes) {
                mPhotos[index].isSelected = false
            }
            mIndexes.clear()
            refreshMore()
            mPhotoAdapter.notifyDataSetChanged()
            mProcessFileObserver.postValue(false)

            mTempDialog?.dismiss()
            mTempDialog = null
        }
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mMediaScannerReceiver)
    }

    companion object {
        private val LOG_TAG = PhotoActivity::class.java.simpleName
    }

    inner class SlideRunnable : Runnable {
        override fun run() {
            //Log.d("lcs", "SlideRunnable: mTabPosition = $mTabPosition, mPhotos.size = ${mPhotos.size}")
            if (mPaused) {
                return
            }
            if (mPhotos.size > 0) {
                val imageView = mPhotoAdapter.getViewByPosition(rv_photo, 0, R.id.iv_image_item_photo)
                val intent = Intent(this@PhotoActivity, SlideActivity::class.java)
                intent.putParcelableArrayListExtra("tab_adapter",mStorages)
                intent.putExtra("tab_position", mTabPosition)
                //intent.putExtra("position", 0)
                var totalDir = 4
                if(!isConfigSdcard){
                    totalDir = totalDir - 1
                }
                if(!isConfigUSBStorage){
                    totalDir = totalDir - 1
                }
                if (mTabPosition > totalDir) {
                    intent.putExtra("user_id", mStorages[mTabPosition].user.id)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && imageView != null) {
                    startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(this@PhotoActivity, imageView, "image").toBundle())
                } else {
                    startActivity(intent)
                }
            }
        }
    }

    inner class MediaScannerRunnable : Runnable {
        override fun run() {
            //Log.d("lcs", "MediaScannerRunnable: ${mPhotos.size}, ${countImages()}")
            if (mPhotosAll.size < countImages()) {
                refreshImages()
                mHandler.postDelayed(mMediaScannerRunnable, 3000)
            }
        }
    }

    inner class MediaScannerReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //Log.d("lcs", "${intent?.action}, ${intent?.data}, ${intent?.data?.path}, ${intent?.dataString}, ${intent?.extras}")
            if (intent != null && !isMore) {
                if (intent.action == Intent.ACTION_MEDIA_SCANNER_STARTED) {
                    mHandler.removeCallbacks(mMediaScannerRunnable)
                    mHandler.postDelayed(mMediaScannerRunnable, 3000)
                } else if (intent.action == Intent.ACTION_MEDIA_SCANNER_FINISHED) {
                    mHandler.removeCallbacks(mMediaScannerRunnable)
                    if (mPhotosAll.size != countImages()) {
                        refreshImages()
                    }
                } else if (intent.action == Intent.ACTION_MEDIA_SCANNER_SCAN_FILE || intent.action == MyConstants.ACTION_MEDIA_SCANNER_SCAN_FILE) {
                    //Log.d("lcs", "MediaScannerReceiver: ${File(intent.data?.path).exists()}, ${mPhotos.size}, ${countImages()}")
                    val path = intent.data?.path
                    if (path != null) {
                        if (File(path).exists()) {
                            queryImage(path)
                        } else {
                            val photo = Photo(path)
                            if (mPhotos.contains(photo)) {
                                val index = mPhotos.indexOf(photo)
                                mPhotoAdapter.remove(index)
                            }
                        }
                    }
                }
            }
        }
    }

}