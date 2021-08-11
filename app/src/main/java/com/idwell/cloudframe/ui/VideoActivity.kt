package com.idwell.cloudframe.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ConvertUtils

import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.idwell.cloudframe.R
import com.idwell.cloudframe.adapter.StorageAdapter
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.common.Device.isConfigSdcard
import com.idwell.cloudframe.common.Device.isConfigUSBStorage
import com.idwell.cloudframe.common.MyConstants
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.widget.StorageItemDecoration
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.entity.*
import com.idwell.cloudframe.http.entity.User
import com.idwell.cloudframe.util.MyUtils
import com.idwell.cloudframe.widget.MaterialDialog
import com.idwell.cloudframe.widget.RingView
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.*

class VideoActivity : BaseActivity(), BaseQuickAdapter.OnItemClickListener, BaseQuickAdapter.OnItemLongClickListener {

    private var mPaused = false
    private var mMediaScannerRunnable: MediaScannerRunnable? = null
    private var mMediaScannerReceiver: MediaScannerReceiver? = null

    private var mTempDialog: MaterialDialog? = null

    private val mUsers = mutableListOf<User>()
    private lateinit var mStorageAdapter: StorageAdapter
    private lateinit var mVideoAdapter: BaseQuickAdapter<Video, BaseViewHolder>
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    private var mVideos = mutableListOf<Video>()
    private var mTabPosition: Int = 0
    private var mVideo = Video()
    private val mStorages = mutableListOf<MultipleItem>()
    //是否正在处理文件
    private var isProcessFile = false
    private val mProcessFileObserver = MutableLiveData<Boolean>()

    private val mHandler = Handler()

    override fun initLayout(): Int {
        return R.layout.activity_video
    }

    override fun initData() {
        //tv_title_base.setText(R.string.video)
        iv_more_base.visibility = View.VISIBLE
        iv_title_base.setImageResource(R.drawable.ic_video)
        onOrientationChanged(resources.configuration.orientation)

        var userId = intent.getIntExtra("userId", -1)
        val filePath = intent.getStringExtra("filePath") ?: ""
        mStorages.add(MultipleItem(StorageAdapter.VIDEO_STORAGE, R.drawable.ic_all, getString(R.string.all)))
        mStorages.add(MultipleItem(StorageAdapter.VIDEO_STORAGE, R.drawable.ic_internal_storage, getString(R.string.internal_storage)))
        if(Device.isConfigSdcard)
        mStorages.add(MultipleItem(StorageAdapter.VIDEO_STORAGE, R.drawable.ic_sd, getString(R.string.sd_card)))
        if(Device.isConfigUSBStorage)
        mStorages.add(MultipleItem(StorageAdapter.VIDEO_STORAGE, R.drawable.ic_usb, getString(R.string.usb)))
        mStorageAdapter = StorageAdapter(mStorages)
        rv_storage_video.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        rv_storage_video.addItemDecoration(StorageItemDecoration(2))
        rv_storage_video.adapter = mStorageAdapter
        mVideoAdapter = object : BaseQuickAdapter<Video, BaseViewHolder>(R.layout.item_sidebar_music, mVideos) {
            override fun convert(helper: BaseViewHolder, item: Video?) {
                if (item != null) {
                    helper.setText(R.id.tv_item_sidebar_music, item.displayName)
                    if (isMore) {
                        helper.setBackgroundRes(R.id.cl_item_sidebar_music, R.drawable.transparent)
                        if (item.isSelected) {
                            helper.setImageResource(R.id.iv_item_sidebar_music, R.drawable.ic_checked)
                        } else {
                            helper.setImageResource(R.id.iv_item_sidebar_music, R.drawable.ic_unchecked)
                        }
                        helper.getView<ImageView>(R.id.iv_item_sidebar_music)
                                .visibility = View.VISIBLE
                    } else {
                        if (item == mVideo) {
                            helper.setImageResource(R.id.iv_item_sidebar_music, R.drawable.ic_dot_white_24dp)
                            helper.setBackgroundColor(R.id.cl_item_sidebar_music, ContextCompat.getColor(mContext, R.color.main))
                        } else {
                            helper.setImageResource(R.id.iv_item_sidebar_music, R.drawable.transparent)
                            helper.setBackgroundColor(R.id.cl_item_sidebar_music, ContextCompat.getColor(mContext, R.color.transparent))
                        }
                        helper.getView<ImageView>(R.id.iv_item_sidebar_music).visibility = View.GONE
                    }
                }
            }
        }
        mLinearLayoutManager = LinearLayoutManager(this)
        rv_video.layoutManager = mLinearLayoutManager
        rv_video.addItemDecoration(HorizontalItemDecoration(ContextCompat.getColor(this, R.color.divider)))
        rv_video.adapter = mVideoAdapter

        MyDatabase.instance.userDao.queryAccepted().observe({ this.lifecycle }, { users ->
            mStorages.clear()
            mStorages.add(MultipleItem(StorageAdapter.VIDEO_STORAGE, R.drawable.ic_all, getString(R.string.all)))
            mStorages.add(MultipleItem(StorageAdapter.VIDEO_STORAGE, R.drawable.ic_internal_storage, getString(R.string.internal_storage)))
            if(Device.isConfigSdcard)
            mStorages.add(MultipleItem(StorageAdapter.VIDEO_STORAGE, R.drawable.ic_sd, getString(R.string.sd_card)))
            if(Device.isConfigUSBStorage)
            mStorages.add(MultipleItem(StorageAdapter.VIDEO_STORAGE, R.drawable.ic_usb, getString(R.string.usb)))
            if (users != null) {
                mUsers.clear()
                mUsers.addAll(users)
                for (user in users) {
                    mStorages.add(MultipleItem(StorageAdapter.VIDEO_USER, user))
                }
            }
            if (userId == -1) {
                mStorageAdapter.notifyDataSetChanged()
            } else {
                val user = User()
                user.id = userId
                var totalDir = 4
                if(!isConfigSdcard){
                    totalDir = totalDir - 1
                }
                if(!isConfigUSBStorage){
                    totalDir = totalDir - 1
                }
                var index = mUsers.indexOf(user) + totalDir
                mTabPosition = index
                mStorageAdapter.mPosition = mTabPosition
                mStorageAdapter.notifyDataSetChanged()
                launch(Dispatchers.IO) {
                    val videos = queryVideo()
                    launch(Dispatchers.Main) {
                        mVideos.clear()
                        mVideos.addAll(videos)
                        val video = Video()
                        video.data = filePath
                        val position = mVideos.indexOf(video)
                        mVideo = mVideos[position]
                        setNewVideoList()
                    }
                }
                userId = -1
            }
        })

        launch(Dispatchers.IO) {
            val videos = queryVideo()
            launch(Dispatchers.Main) {
                mVideos.clear()
                mVideos.addAll(videos)
                setNewVideoList()
            }
        }

        mProcessFileObserver.observe({ this.lifecycle }, { processFile ->
            isProcessFile = processFile
        })

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
        mVideoAdapter.onItemClickListener = this
        mVideoAdapter.onItemLongClickListener = this
        iv_play_video.setOnClickListener(this)

        rv_video.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (vsb_video.isTouched) return
                //滑动到顶部
                else if (!rv_video.canScrollVertically(-1)) vsb_video.progress = 0
                //滑动到底部
                else if (!rv_video.canScrollVertically(1)) vsb_video.progress = mVideos.size - 1
                else {
                    //获取第一个可见view的位置
                    val firstItemPosition = mLinearLayoutManager.findFirstVisibleItemPosition()
                    //获取最后一个可见view的位置
                    val lastItemPosition = mLinearLayoutManager.findLastVisibleItemPosition()
                    vsb_video.progress = (firstItemPosition + lastItemPosition) / 2
                }
            }
        })
        vsb_video.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (vsb_video.isTouched) rv_video.scrollToPosition(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }

    override fun initMessageEvent(event: MessageEvent) {
        when (event.message) {
            MessageEvent.VIDEOVIEW_CUR_VIDEO_DATA -> {
                val video = Video()
                video.data = event.text
                var index = mVideos.indexOf(video)
                if (index == -1) index = 0
                mVideo = mVideos[index]
                refreshVideoList()
            }
        }
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v) {
            iv_play_video -> {
                if (mVideos.isNotEmpty()) {
                    startVideoViewActivity()
                }
            }
            iv_more_base -> {
                isMore = !isMore
                for (index in mIndexes) {
                    mVideos[index].isSelected = false
                }
                mIndexes.clear()
                refreshMore()
                mVideoAdapter.notifyDataSetChanged()
            }
            iv_check_base -> {
                val isChecked = mIndexes.size == mVideos.size
                mIndexes.clear()
                for (video in mVideos) {
                    video.isSelected = !isChecked
                    if (video.isSelected) {
                        mIndexes.add(mVideos.indexOf(video))
                    }
                }
                refreshMore()
                mVideoAdapter.notifyDataSetChanged()
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
                                val view = View.inflate(this@VideoActivity, R.layout.view_process, null)
                                dialog.setContentView(view)
                                dialog.setCanceledOnTouchOutside(false)
                                dialog.setCancelable(false)
                                dialog.show()
                                mTempDialog = dialog
                                mProcessFileObserver.postValue(true)
                                val indexes = ArrayList<Int>()
                                indexes.addAll(mIndexes)
                                launch(Dispatchers.IO) {
                                    val videos = mutableListOf<Video>()
                                    for (index in indexes) {
                                        if (mPaused) {
                                            return@launch
                                        }
                                        val video = mVideos[index]
                                        FileUtils.delete(video.data)
                                        delete(video.id)
                                        videos.add(video)
                                    }
                                    launch(Dispatchers.Main) {
                                        isMore = false
                                        for (video in videos) {
                                            val index = mVideos.indexOf(video)
                                            if (index != -1) {
                                                mVideoAdapter.remove(index)
                                                if (index > 0) {
                                                    mVideoAdapter.notifyItemRangeChanged(0, index)
                                                }
                                            }
                                        }
                                        var position = mVideos.indexOf(mVideo)
                                        if (position == -1) {
                                            position = 0
                                        }
                                        if (mVideos.isEmpty()) {
                                            mVideo = Video()
                                        } else {
                                            mVideo = mVideos[position]
                                        }
                                        mIndexes.clear()
                                        refreshMore()
                                        refreshVerticalSeekBar(position)
                                        refreshContent(position)
                                        mProcessFileObserver.postValue(false)
                                        dialog.dismiss()
                                    }
                                }
                            }
                        }).show()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        LogUtils.dTag(LOG_TAG, keyCode)
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (mVideos.isNotEmpty()) {
                    var position = mVideos.indexOf(mVideo)
                    if (--position < 0) {
                        position = mVideos.size - 1
                    }
                    mVideo = mVideos[position]
                    refreshVideoList()
                }
                return true
            }
            KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_DPAD_CENTER  -> {
                if (mVideos.isNotEmpty()) {
                    startVideoViewActivity()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (mVideos.isNotEmpty()) {
                    var position = mVideos.indexOf(mVideo)
                    if (++position >= mVideos.size) {
                        position = 0
                    }
                    mVideo = mVideos[position]
                    refreshVideoList()
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        when (adapter) {
            mStorageAdapter -> {
                mTabPosition = position
                mStorageAdapter.mPosition = position
                mStorageAdapter.notifyDataSetChanged()
                mVideoAdapter.setNewData(null)
                launch(Dispatchers.IO) {
                    val videos = queryVideo()
                    launch(Dispatchers.Main) {
                        mVideos.clear()
                        mVideos.addAll(videos)
                        isMore = false
                        mIndexes.clear()
                        refreshMore()
                        if (mVideos.isNotEmpty()) {
                            mVideo = mVideos[0]
                        }
                        setNewVideoList()
                    }
                }
            }
            mVideoAdapter -> {
                if (isMore) {
                    if (mVideos[position].isSelected) {
                        mVideos[position].isSelected = false
                        mIndexes.remove(position)
                    } else {
                        mVideos[position].isSelected = true
                        mIndexes.add(position)
                    }
                    refreshMore()
                    mVideoAdapter.notifyDataSetChanged()
                } else {
                    if (mVideos[position] == mVideo) {
                        if (File(mVideo.data).exists()) {
                            startVideoViewActivity()
                        } else {
                            fileNotExists()
                        }
                    } else {
                        mVideo = mVideos[position]
                        if (File(mVideo.data).exists()) {
                            refreshVideoList()
                        } else {
                            fileNotExists()
                        }
                    }
                }
            }
        }
    }

    override fun onItemLongClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int): Boolean {
        if (isMore) {
            if (mVideos[position].isSelected) {
                mVideos[position].isSelected = false
                mIndexes.remove(position)
            } else {
                mVideos[position].isSelected = true
                mIndexes.add(position)
            }
        } else {
            isMore = true
            mIndexes.add(position)
            mVideos[position].isSelected = true
        }
        refreshMore()
        mVideoAdapter.notifyDataSetChanged()
        return true
    }

    private fun copyFileToInternalStorage() {
        MaterialDialog.Builder(this).setContent(R.string.import_files_to_memory)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                    override fun onClick(dialog: MaterialDialog) {
                        val view = View.inflate(this@VideoActivity, R.layout.view_ring, null)
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
                            for (video in mVideos) {
                                if (video.isSelected) {
                                    totalSize += video.size
                                }
                            }
                            for (video in mVideos) {
                                if (mPaused) {
                                    return@launch
                                }
                                if (video.isSelected) {
                                    var inputStream: InputStream? = null
                                    var outputStream: OutputStream? = null
                                    try {
                                        val srcFile = File(video.data)
                                        val parent = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
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
                                    } catch (e: FileNotFoundException) {
                                        e.printStackTrace()
                                        writtenSize += video.size
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
                                    mVideos[index].isSelected = false
                                }
                                mIndexes.clear()
                                refreshMore()
                                mVideoAdapter.notifyDataSetChanged()
                                mProcessFileObserver.postValue(false)
                                ToastUtils.showShort(R.string.import_success)
                                dialog.dismiss()
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
                        val view = View.inflate(this@VideoActivity, R.layout.view_ring, null)
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
                            for (music in mVideos) {
                                if (music.isSelected) {
                                    totalSize += music.size
                                }
                            }
                            for (video in mVideos) {
                                if (mPaused) {
                                    return@launch
                                }
                                if (video.isSelected) {
                                    var inputStream: InputStream? = null
                                    var outputStream: OutputStream? = null
                                    try {
                                        val srcFile = File(video.data)
                                        val parent = if (devNames[pos] == R.string.sd_card) MyUtils.getSdDir() else MyUtils.getUsbDir()
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
                                        val sBufferSize = 8192
                                        val data = ByteArray(sBufferSize)
                                        var len: Int
                                        while (inputStream.read(data, 0, sBufferSize).also {
                                                    len = it
                                                } != -1) {
                                            outputStream.write(data, 0, len)
                                            writtenSize += len
                                            ringView.setProgress((writtenSize * 100 / totalSize).toInt())
                                        }
                                        MyUtils.scanFile(destFile.absolutePath)
                                    } catch (e: FileNotFoundException) {
                                        e.printStackTrace()
                                        writtenSize += video.size
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
                                    mVideos[index].isSelected = false
                                }
                                mIndexes.clear()
                                refreshMore()
                                mVideoAdapter.notifyDataSetChanged()
                                mProcessFileObserver.postValue(false)
                                ToastUtils.showShort(R.string.export_success)
                                dialog.dismiss()
                            }
                        }
                    }
                }).show()
    }

    private fun fileNotExists() {
        mVideoAdapter.setNewData(null)
        launch(Dispatchers.IO) {
            val videos = queryVideo()
            launch(Dispatchers.Main) {
                mVideos.clear()
                mVideos.addAll(videos)
                isMore = false
                mIndexes.clear()
                refreshMore()
                setNewVideoList()
            }
        }
    }

    private fun refreshMore() {
        if (mVideos.isEmpty()) {
            cl_more_base.visibility = View.GONE
        } else {
            if (isMore) {
                if (mIndexes.isEmpty()) {
                    iv_check_base.setImageResource(R.drawable.ic_unchecked)
                    iv_delete_base.setImageResource(R.drawable.ic_undelete)
                    iv_delete_base.isClickable = false
                    tv_total_base.visibility = View.GONE
                } else {
                    if (mIndexes.size == mVideos.size) {
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

    private fun refreshVideo() {
        launch(Dispatchers.IO) {
            val videos = queryVideo()
            launch(Dispatchers.Main) {
                mVideos.clear()
                mVideos.addAll(videos)
                if (isMore) {
                    isMore = false
                    mIndexes.clear()
                    refreshMore()
                }
                setNewVideoList()
            }
        }
    }

    private fun refreshVideoList() {
        var position = mVideos.indexOf(mVideo)
        if (position == -1) {
            position = 0
        }
        mVideoAdapter.notifyDataSetChanged()
        rv_video.scrollToPosition(position)
        refreshVerticalSeekBar(position)
        refreshContent(position)
    }

    private fun refreshVerticalSeekBar(position: Int) {
        vsb_video.max = if (mVideos.isNotEmpty()) mVideos.size - 1 else 0
        vsb_video.progress = position
    }

    private fun refreshContent(position: Int) {
        if (mVideos.isEmpty()) {
            iv_thumbnail_video.setImageResource(R.drawable.transparent)
            tv_desc_video.visibility = View.INVISIBLE
            tv_size_video.text = ""
            tv_index_video.text = ""
        } else {
            Glide.with(this).load(mVideo.data).into(iv_thumbnail_video)
            if (mTabPosition > 3) {
                val description = Gson().fromJson(mVideo.description, Description::class.java) ?: Description()
                if (description.text.isEmpty()) {
                    tv_desc_video.visibility = View.INVISIBLE
                } else {
                    tv_desc_video.text = description.text
                    tv_desc_video.visibility = View.VISIBLE
                }
            } else {
                tv_desc_video.visibility = View.INVISIBLE
            }
            tv_size_video.text = FileUtils.getSize(mVideo.data)
            tv_index_video.text = getString(R.string.digital_indicator, position + 1, mVideos.size)
        }
    }

    private fun setNewVideoList() {
        if (mVideos.isEmpty()) {
            mVideo = Video()
            mVideoAdapter.setNewData(mVideos)
            vsb_video.max = 0
            vsb_video.progress = 0
            iv_thumbnail_video.setImageResource(R.drawable.transparent)
            tv_desc_video.visibility = View.INVISIBLE
            tv_size_video.text = ""
            tv_index_video.text = ""
        } else {
            var position = mVideos.indexOf(mVideo)
            if (position == -1) {
                position = 0
                mVideo = mVideos[0]
            }
            vsb_video.max = mVideos.size - 1
            mVideoAdapter.setNewData(mVideos)
            rv_video.scrollToPosition(position)
            LogUtils.dTag(LOG_TAG, "$position, ${mVideo.data}")
            Glide.with(this).load(mVideo.data).into(iv_thumbnail_video)
            if (mTabPosition > 3) {
                val description = Gson().fromJson(mVideo.description, Description::class.java) ?: Description()
                if (description.text.isEmpty()) {
                    tv_desc_video.visibility = View.INVISIBLE
                } else {
                    tv_desc_video.text = description.text
                    tv_desc_video.visibility = View.VISIBLE
                }
            } else {
                tv_desc_video.visibility = View.INVISIBLE
            }
            tv_size_video.text = FileUtils.getSize(mVideo.data)
            tv_index_video.text = getString(R.string.digital_indicator, position + 1, mVideos.size)
        }
    }

    private fun startVideoViewActivity() {
        val intent = Intent(this, VideoViewActivity::class.java)
        intent.putExtra("tabPosition", mTabPosition)
        intent.putExtra("video", mVideo)
        var totalDir = 3
        if(!isConfigSdcard){
            totalDir = totalDir - 1
        }
        if(!isConfigUSBStorage){
            totalDir = totalDir - 1
        }
        if (mTabPosition > totalDir) {
            intent.putExtra("userId", mStorages[mTabPosition].user.id)
        }
        startActivity(intent)
    }

    private fun queryVideo(): MutableList<Video> {
        val videos = mutableListOf<Video>()
        var selection =""
            if(isConfigUSBStorage && !isConfigSdcard){
            selection = when (mTabPosition) {
                //屏蔽.vob格式
                0 -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                1 -> "${MediaStore.Video.VideoColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                2 -> "${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getUsbDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                else -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DESCRIPTION} like '%\"sender_id\":${mStorages[mTabPosition].user.id}%'"
            }
        }else if(!isConfigUSBStorage && isConfigSdcard){
            selection = when (mTabPosition) {
                //屏蔽.vob格式
                0 -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                1 -> "${MediaStore.Video.VideoColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                2 -> "${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getSdDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                else -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DESCRIPTION} like '%\"sender_id\":${mStorages[mTabPosition].user.id}%'"
            }
        }else if(!isConfigUSBStorage && !isConfigSdcard){
                selection = when (mTabPosition) {
                    //屏蔽.vob格式
                    0 -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                    1 -> "${MediaStore.Video.VideoColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                    else -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DESCRIPTION} like '%\"sender_id\":${mStorages[mTabPosition].user.id}%'"
                }
            } else {
            selection = when (mTabPosition) {
                //屏蔽.vob格式
                0 -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                1 -> "${MediaStore.Video.VideoColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                2 -> "${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getSdDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                3 -> "${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getUsbDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                else -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DESCRIPTION} like '%\"sender_id\":${mStorages[mTabPosition].user.id}%'"
            }
        }

        val cursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.VideoColumns._ID, MediaStore.Video.VideoColumns.DATA, MediaStore.Video.VideoColumns.SIZE, MediaStore.Video.VideoColumns.DISPLAY_NAME, MediaStore.Video.VideoColumns.TITLE, MediaStore.Video.VideoColumns.DURATION, MediaStore.Video.VideoColumns.RESOLUTION, MediaStore.Video.VideoColumns.DESCRIPTION, MediaStore.Video.VideoColumns.IS_PRIVATE), selection, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER)
        while (cursor?.moveToNext() == true) {
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns._ID))
            val data = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA))
            val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns.SIZE))
            val displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME)) ?: ""
            val title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.TITLE))
            val duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION))
            val resolution = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.RESOLUTION)) ?: ""
            val description = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DESCRIPTION)) ?: ""
            val isprivate = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.VideoColumns.IS_PRIVATE))
            val video = Video(id, data, size, displayName, title, duration, resolution, description, isprivate)
            videos.add(video)
        }
        cursor?.close()
        return videos
    }

    private fun queryVideo(_data: String) {
        launch(Dispatchers.IO) {
            delay(200)
            var video: Video? = null
            val selection = when (mTabPosition) {
                //屏蔽.vob格式
                0 -> "${MediaStore.Video.VideoColumns.DATA} = '$_data' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                1 -> "${MediaStore.Video.VideoColumns.DATA} = '$_data' and ${MediaStore.Video.VideoColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                2 -> "${MediaStore.Video.VideoColumns.DATA} = '$_data' and ${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getSdDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                3 -> "${MediaStore.Video.VideoColumns.DATA} = '$_data' and ${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getUsbDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
                else -> "${MediaStore.Video.VideoColumns.DATA} = '$_data' and ${MediaStore.Video.VideoColumns.DESCRIPTION} like '%\"sender_id\":${mStorages[mTabPosition].user.id}%'"
            }
            val cursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.VideoColumns._ID, MediaStore.Video.VideoColumns.DATA, MediaStore.Video.VideoColumns.SIZE, MediaStore.Video.VideoColumns.DISPLAY_NAME, MediaStore.Video.VideoColumns.TITLE, MediaStore.Video.VideoColumns.DURATION, MediaStore.Video.VideoColumns.RESOLUTION, MediaStore.Video.VideoColumns.DESCRIPTION, MediaStore.Video.VideoColumns.IS_PRIVATE), selection, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER)
            while (cursor?.moveToNext() == true) {
                val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns._ID))
                val data = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA))
                val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns.SIZE))
                val displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME)) ?: ""
                val title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.TITLE))
                val duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION))
                val resolution = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.RESOLUTION)) ?: ""
                val description = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DESCRIPTION)) ?: ""
                val isprivate = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.VideoColumns.IS_PRIVATE))
                video = Video(id, data, size, displayName, title, duration, resolution, description, isprivate)
            }
            cursor?.close()
            if (video != null) {
                launch(Dispatchers.Main) {
                    if (mVideos.contains(video)) {
                        val index = mVideos.indexOf(video)
                        mVideoAdapter.setData(index, video)
                    } else {
                        mVideoAdapter.addData(video)
                        LogUtils.dTag(LOG_TAG, mVideos.size)
                        var position = mVideos.indexOf(mVideo)
                        if (position == -1) {
                            position = 0
                        }
                        refreshVerticalSeekBar(position)
                        refreshContent(position)
                    }
                }
            }
        }
    }

    private fun countVideo(): Int {
        val selection = when (mTabPosition) {
            //屏蔽.vob格式
            0 -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
            1 -> "${MediaStore.Video.VideoColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
            2 -> "${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getSdDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
            3 -> "${MediaStore.Video.VideoColumns.DATA} like '${MyUtils.getUsbDir()}/%' and ${MediaStore.Video.VideoColumns.DISPLAY_NAME} not like '%.vob'"
            else -> "${MediaStore.Video.VideoColumns.DATA} is not null and ${MediaStore.Video.VideoColumns.DESCRIPTION} like '%\"sender_id\":${mStorages[mTabPosition].user.id}%'"
        }
        val cursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf("count(*)"), selection, null, null)
        cursor?.moveToFirst()
        val count = cursor?.getInt(0) ?: 0
        cursor?.close()
        return count
    }

    fun delete(id: Long) {
        contentResolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "${MediaStore.Video.VideoColumns._ID}=$id", null)
    }

    private fun onOrientationChanged(orientation: Int) {
        val constraintSet = ConstraintSet()
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            constraintSet.connect(R.id.rv_video, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.rv_video, ConstraintSet.END, R.id.vsb_video, ConstraintSet.START)
            constraintSet.connect(R.id.rv_video, ConstraintSet.TOP, R.id.rv_storage_video, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.rv_video, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.setHorizontalWeight(R.id.rv_video, 0.5f)
            constraintSet.constrainHeight(R.id.rv_video, ConstraintSet.MATCH_CONSTRAINT)
            constraintSet.setMargin(R.id.rv_video, 3, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.rv_video, 4, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.rv_video, 6, ConvertUtils.dp2px(20f))
            constraintSet.setMargin(R.id.rv_video, 7, ConvertUtils.dp2px(50f))

            constraintSet.connect(R.id.vsb_video, ConstraintSet.START, R.id.rv_video, ConstraintSet.END)
            constraintSet.connect(R.id.vsb_video, ConstraintSet.END, R.id.cl_preview_video, ConstraintSet.START)
            constraintSet.connect(R.id.vsb_video, ConstraintSet.TOP, R.id.rv_video, ConstraintSet.TOP)
            constraintSet.connect(R.id.vsb_video, ConstraintSet.BOTTOM, R.id.rv_video, ConstraintSet.BOTTOM)
            constraintSet.constrainWidth(R.id.vsb_video, ConstraintSet.WRAP_CONTENT)
            constraintSet.constrainHeight(R.id.vsb_video, ConstraintSet.MATCH_CONSTRAINT)

            constraintSet.connect(R.id.cl_preview_video, ConstraintSet.START, R.id.vsb_video, ConstraintSet.END)
            constraintSet.connect(R.id.cl_preview_video, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.cl_preview_video, ConstraintSet.TOP, R.id.rv_video, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_preview_video, ConstraintSet.BOTTOM, R.id.rv_video, ConstraintSet.BOTTOM)
            constraintSet.setHorizontalWeight(R.id.cl_preview_video, 0.5f)
            constraintSet.constrainPercentHeight(R.id.cl_preview_video, 1.0f)
            constraintSet.setMargin(R.id.cl_preview_video, 3, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_preview_video, 4, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_preview_video, 6, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_preview_video, 7, ConvertUtils.dp2px(50f))
        } else {
            constraintSet.connect(R.id.rv_video, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.rv_video, ConstraintSet.END, R.id.vsb_video, ConstraintSet.START)
            constraintSet.connect(R.id.rv_video, ConstraintSet.TOP, R.id.rv_storage_video, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.rv_video, ConstraintSet.BOTTOM, R.id.cl_preview_video, ConstraintSet.TOP)
            constraintSet.setHorizontalWeight(R.id.rv_video, 1.0f)
            constraintSet.setVerticalWeight(R.id.rv_video, 0.5f)
            constraintSet.setMargin(R.id.rv_video, 3, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.rv_video, 4, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.rv_video, 6, ConvertUtils.dp2px(20f))
            constraintSet.setMargin(R.id.rv_video, 7, ConvertUtils.dp2px(50f))

            constraintSet.connect(R.id.vsb_video, ConstraintSet.START, R.id.rv_video, ConstraintSet.END)
            constraintSet.connect(R.id.vsb_video, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.vsb_video, ConstraintSet.TOP, R.id.rv_video, ConstraintSet.TOP)
            constraintSet.connect(R.id.vsb_video, ConstraintSet.BOTTOM, R.id.rv_video, ConstraintSet.BOTTOM)
            constraintSet.constrainWidth(R.id.vsb_video, ConstraintSet.WRAP_CONTENT)
            constraintSet.constrainHeight(R.id.vsb_video, ConstraintSet.MATCH_CONSTRAINT)
            constraintSet.setMargin(R.id.vsb_video, 7, ConvertUtils.dp2px(20f))

            constraintSet.connect(R.id.cl_preview_video, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.cl_preview_video, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.cl_preview_video, ConstraintSet.TOP, R.id.rv_video, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.cl_preview_video, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.constrainWidth(R.id.cl_preview_video, ConstraintSet.MATCH_CONSTRAINT)
            constraintSet.setVerticalWeight(R.id.cl_preview_video, 0.5f)
            constraintSet.setMargin(R.id.cl_preview_video, 3, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_preview_video, 4, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_preview_video, 6, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.cl_preview_video, 7, ConvertUtils.dp2px(50f))
        }
        constraintSet.applyTo(cl_video)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onOrientationChanged(newConfig.orientation)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        LogUtils.dTag(LOG_TAG, "onNewIntent")
        if (intent != null) {
            val userId = intent.getIntExtra("userId", -1)
            val filePath = intent.getStringExtra("filePath") ?: ""
            val user = User()
            user.id = userId
            var totalDir = 4
            if(!isConfigSdcard){
                totalDir = totalDir - 1
            }
            if(!isConfigUSBStorage){
                totalDir = totalDir - 1
            }
            var index = mUsers.indexOf(user) + totalDir
            mTabPosition = index
            mStorageAdapter.mPosition = mTabPosition
            mStorageAdapter.notifyDataSetChanged()
            launch(Dispatchers.IO) {
                val videos = queryVideo()
                launch(Dispatchers.Main) {
                    mVideos.clear()
                    mVideos.addAll(videos)
                    val video = Video()
                    video.data = filePath
                    val position = mVideos.indexOf(video)
                    LogUtils.dTag(LOG_TAG, "$userId, $filePath, $position")
                    if(-1!=position)
                    mVideo = mVideos[position]
                    setNewVideoList()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mPaused && mVideos.size != countVideo()) {
            mHandler.removeCallbacks(mMediaScannerRunnable)
            mHandler.post(mMediaScannerRunnable)
        }
        mPaused = false
    }

    override fun onPause() {
        super.onPause()
        mPaused = true
        if (isMore && mTempDialog != null) {
            isMore = false
            for (index in mIndexes) {
                mVideos[index].isSelected = false
            }
            mIndexes.clear()
            refreshMore()
            mVideoAdapter.notifyDataSetChanged()
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
        private val LOG_TAG = VideoActivity::class.java.simpleName
    }

    inner class MediaScannerRunnable : Runnable {
        override fun run() {
            //Log.d("lcs", "MediaScannerRunnable: ${mPhotos.size}, ${countImages()}")
            if (mVideos.size != countVideo()) {
                refreshVideo()
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
                    if (mVideos.size != countVideo()) {
                        refreshVideo()
                    }
                } else if (intent.action == Intent.ACTION_MEDIA_SCANNER_SCAN_FILE || intent.action == MyConstants.ACTION_MEDIA_SCANNER_SCAN_FILE) {
                    //Log.d("ooo", "${File(intent.data?.path).exists()}, ${mPhotos.size}, ${countImages()}")
                    val path = intent.data?.path
                    if (path != null) {
                        if (File(path).exists()) {
                            queryVideo(path)
                        } else {
                            val video = Video(path)
                            if (mVideos.contains(video)) {
                                val index = mVideos.indexOf(video)
                                mVideoAdapter.remove(index)
                            }
                        }
                    }
                }
            }
        }
    }

}