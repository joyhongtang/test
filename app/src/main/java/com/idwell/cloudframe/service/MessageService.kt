package com.idwell.cloudframe.service

import android.app.Service
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import android.content.*
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.*
import android.provider.MediaStore
import android.text.format.Formatter
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.FileUtils

import com.google.gson.Gson
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.MyConstants
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.db.entity.PushMessage
import com.idwell.cloudframe.entity.IMessage
import com.idwell.cloudframe.entity.Description
import com.idwell.cloudframe.http.entity.User
import com.idwell.cloudframe.http.service.MessageService
import com.idwell.cloudframe.ui.*
import com.idwell.cloudframe.util.MyLogUtils
import com.idwell.cloudframe.util.MyUtils
import com.idwell.cloudframe.widget.MaterialDialog
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener1
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist

import java.io.File

import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import java.lang.Exception

class MessageService : Service(), LifecycleOwner, View.OnTouchListener, View.OnClickListener {

    private val mLifecycleRegistry = LifecycleRegistry(this)
    private lateinit var mConnectivityManager: ConnectivityManager

    private var mDownloadTask: DownloadTask? = null

    private var mMaterialDialog: MaterialDialog? = null

    private val mPushMessages = mutableListOf<PushMessage>()
    private lateinit var mWindowManager: WindowManager
    private lateinit var mLayoutParams: WindowManager.LayoutParams
    //通知栏
    private lateinit var mNotificationBar: View
    //通知内容
    private lateinit var mContent: TextView
    private lateinit var mIMessage: IMessage
    //下载中
    private var isDownload = false

    private val mHandler = Handler()
    private val mBarRunnable = Runnable {
        if (mNotificationBar.isAttachedToWindow) {
            mWindowManager.removeViewImmediate(mNotificationBar)
        }
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null) {
                when (intent.action) {
                    Intent.ACTION_TIME_TICK -> {
                        if (mPushMessages.isNotEmpty() && !isDownload && isStorageAvailable() && isNetworkConnected()) {
                            LogUtils.dTag(TAG, intent.action)
                            isDownload = true
                            val pushMessage = mPushMessages[0]
                            handleMessage(pushMessage)
                            mPushMessages.clear()
                        }
                    }
                    WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                        val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                        if (networkInfo != null && networkInfo.isConnected && mPushMessages.isNotEmpty() && !isDownload && isStorageAvailable()) {
                            LogUtils.dTag(TAG, intent.action)
                            isDownload = true
                            val pushMessage = mPushMessages[0]
                            handleMessage(pushMessage)
                            mPushMessages.clear()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        LogUtils.dTag(TAG, "onCreate")
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        mConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        //初始化窗口
        mLayoutParams = WindowManager.LayoutParams()
        //前面有SYSTEM才可以遮挡状态栏，不然的话只能在状态栏下显示通知栏
        val sdkInt = Build.VERSION.SDK_INT
        when {
            sdkInt < Build.VERSION_CODES.KITKAT -> mLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
            sdkInt < Build.VERSION_CODES.N_MR1 -> mLayoutParams.type = WindowManager.LayoutParams.TYPE_TOAST
            sdkInt < Build.VERSION_CODES.O -> mLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
            else -> mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        mLayoutParams.format = PixelFormat.TRANSLUCENT
        //设置必须触摸通知栏才可以关掉
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        // 设置通知栏的长和宽
        if (Device.isDualScreen) {
            mLayoutParams.width = ScreenUtils.getScreenWidth() - 18 - 195
        } else {
            mLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        }
        mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        mLayoutParams.windowAnimations = R.style.FloatWindow
        mLayoutParams.gravity = Gravity.TOP or Gravity.START

        mNotificationBar = View.inflate(this, R.layout.notification, null)
        mContent = mNotificationBar.findViewById(R.id.tv_notification)
        mNotificationBar.setOnTouchListener(this)
        mNotificationBar.setOnClickListener(this)

        MyDatabase.instance.pushMessageDao.loadAll()
                .observe(this, Observer<MutableList<PushMessage>> { pushMessages ->
                    if (pushMessages != null && pushMessages.isNotEmpty()) {
                        mPushMessages.clear()
                        if (isStorageAvailable()) {
                            if (!isDownload && isNetworkConnected()) {
                                LogUtils.dTag(TAG, "Query")
                                isDownload = true
                                val pushMessage = pushMessages[0]
                                handleMessage(pushMessage)
                                pushMessages.remove(pushMessage)
                            }
                        } else {
                            if (mMaterialDialog == null) {
                                MaterialDialog.Builder(this@MessageService)
                                        .setContent(R.string.out_of_memory)
                                        .setPositiveButton(R.string.ok, null).show()
                            } else if (mMaterialDialog?.isShowing == false) {
                                mMaterialDialog?.show()
                            }
                        }
                        mPushMessages.addAll(pushMessages)
                    }
                })

        //注册广播接收器
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_TIME_TICK)
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        registerReceiver(mBroadcastReceiver, intentFilter)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val json = intent.getStringExtra("json")
            if (json != null) {
                LogUtils.dTag(TAG, json)
                val iMessage = Gson().fromJson(json, IMessage::class.java)
                when (iMessage.type) {
                    "updateDeviceAccept" -> {
                        Device.acceptNewUsers = iMessage.ifAccept
                        EventBus.getDefault()
                                .post(MessageEvent(MessageEvent.DEVICE_UPDATE_ACCEPT_NEW_USERS))
                    }
                    "updateDeviceFlow" -> {
//                        val topUpFlow = Formatter.formatFileSize(this, (if (iMessage.topUpFlow >= 1000) iMessage.topUpFlow * 1.024F * 1024 * 1024 else iMessage.topUpFlow * 1024 * 1024).toLong())
//                        showMessage(getString(R.string.message_recharged, topUpFlow), iMessage)
                        if (!Device.isUnlimitedData) {
                            val topUpFlow = Formatter.formatFileSize(this, (if (iMessage.topUpFlow >= 1000) iMessage.topUpFlow * 1.024F * 1024 * 1024 else iMessage.topUpFlow * 1024 * 1024).toLong())
                            showMessage(getString(R.string.message_recharged, topUpFlow), iMessage)
                        }
                    }
                    "new user" -> {
                        val user = User(iMessage.sender_id, iMessage.sender_name, iMessage.sender_account, iMessage.sender_remarkname, "", iMessage.sender_avatar, iMessage.sender_platform, iMessage.sender_isReceive, 0L, 0L, "2")
                        user.displayName = if (user.remarkname.isNotEmpty()) user.remarkname else if (user.name.isNotEmpty()) user.name else user.account.toString()
                        GlobalScope.launch {
                            MyDatabase.instance.userDao.insert(user)
                        }
                        val name = if (user.name.isNotEmpty()) user.name else user.account.toString()
                        showMessage(getString(R.string.message_bind, name), iMessage)
                    }
                    "unbind user" -> {
                        GlobalScope.launch {
                            MyDatabase.instance.userDao.deleteId(iMessage.sender_id)
                        }
                        val name = if (iMessage.sender_name.isNotEmpty()) iMessage.sender_name else iMessage.sender_account.toString()
                        showMessage(getString(R.string.message_unbind, name), iMessage)
                    }
                    "update_user_name" -> {
                        val user = User(iMessage.sender_id, iMessage.sender_name, iMessage.sender_account, iMessage.sender_remarkname, "", iMessage.sender_avatar, iMessage.sender_platform, iMessage.sender_isReceive, 0L, 0L, "2")
                        GlobalScope.launch {
                            MyDatabase.instance.userDao.update(user)
                        }
                    }
                    "updateUserProImg" -> { //更新用户头像
                        GlobalScope.launch {
                            val user = MyDatabase.instance.userDao.queryId(iMessage.sender_id)
                            user.avatar = iMessage.sender_avatar
                            MyDatabase.instance.userDao.update(user)
                        }
                    }
                    "deviceUserRename" -> {
                        GlobalScope.launch {
                            val user = MyDatabase.instance.userDao.queryId(iMessage.sender_id)
                            user.remarkname = iMessage.sender_remarkname
                            user.displayName = iMessage.sender_remarkname
                            MyDatabase.instance.userDao.update(user)
                        }
                    }
                    "refuseBind" -> {
                        GlobalScope.launch {
                            MyDatabase.instance.userDao.deleteId(iMessage.sender_id)
                        }
                    }
                    "acceptBind" -> {
                        val user = User(iMessage.sender_id, iMessage.sender_name, iMessage.sender_account, iMessage.sender_remarkname, "", iMessage.sender_avatar, iMessage.sender_platform, iMessage.sender_isReceive, 0L, 0L, "1")
                        user.displayName = if (user.remarkname.isNotEmpty()) user.remarkname else if (user.name.isNotEmpty()) user.name else user.account.toString()
                        GlobalScope.launch {
                            MyDatabase.instance.userDao.insert(user)
                        }
                    }
                    "updateUserName" -> { //更新用户名
                        GlobalScope.launch {
                            val user = MyDatabase.instance.userDao.queryId(iMessage.sender_id)
                            user.name = iMessage.sender_name
                            user.displayName = if (user.remarkname.isNotEmpty()) user.remarkname else if (user.name.isNotEmpty()) user.name else user.account.toString()
                            MyDatabase.instance.userDao.update(user)
                        }
                    }
                    "deleteDeviceUser" -> { //删除设备用户
                        GlobalScope.launch {
                            MyDatabase.instance.userDao.deleteId(iMessage.sender_id)
                        }
                    }
                    "deleteMyDevice" -> {
                        GlobalScope.launch {
                            Device.deleteMyDevice()
                            MyDatabase.instance.userDao.deleteAll()
                            Device.infoState.postValue(3)
                            sendBroadcast(Intent(Device.ACTION_SIGN_IN))
                        }
                    }
                    else -> {
                        GlobalScope.launch {
                            MyDatabase.instance.pushMessageDao.insert(PushMessage(json))
                        }
                    }
                }
                if (iMessage.deviceFlow != 0.0 && Device.companyName != "Aluratek"&& !Device.isUnlimitedData) {
                    Device.flow = iMessage.deviceFlow.toFloat()
                    EventBus.getDefault().post(MessageEvent(MessageEvent.DEVICE_UPDATE_DATA_FLOW))
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun getLifecycle(): Lifecycle {
        return mLifecycleRegistry
    }

    override fun onClick(view: View) {
        mHandler.removeCallbacks(mBarRunnable)
        if (view.isAttachedToWindow) {
            mWindowManager.removeViewImmediate(view)
        }
        when (mIMessage.type) {
            "image" -> {
                val topActivity = ActivityUtils.getTopActivity()
                if (topActivity !is MainActivity) {
                    if (topActivity is SlideActivity) {
                        ActivityUtils.finishActivity(SlideActivity::class.java)
                    } else if (topActivity !is PhotoActivity) {
                        ActivityUtils.finishToActivity(MainActivity::class.java, false)
                    }
                }
                EventBus.getDefault()
                        .post(MessageEvent(MessageEvent.START_ACTIVITY_PHOTO, mIMessage.sender_id))
            }
            "video" -> {
                val topActivity = ActivityUtils.getTopActivity()
                if (topActivity !is MainActivity) {
                    if (topActivity is VideoViewActivity) {
                        ActivityUtils.finishActivity(VideoViewActivity::class.java)
                    } else if (topActivity !is VideoActivity) {
                        ActivityUtils.finishToActivity(MainActivity::class.java, false)
                    }
                }
                EventBus.getDefault()
                        .post(MessageEvent(MessageEvent.START_ACTIVITY_VIDEO, mIMessage.sender_id, mIMessage.filePath))
            }
            "new user" -> {
                val topActivity = ActivityUtils.getTopActivity()
                if (topActivity !is MainActivity) {
                    if (topActivity is SystemActivity) {
                        ActivityUtils.finishActivity(SystemActivity::class.java)
                    } else if (topActivity !is SettingsActivity) {
                        ActivityUtils.finishToActivity(MainActivity::class.java, false)
                    }
                }
                EventBus.getDefault()
                        .post(MessageEvent(MessageEvent.START_FRAGMENT_USER_MANAGEMENT))
            }
        }
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        LogUtils.dTag(TAG, "onTouch")
        when (event.action) {
            MotionEvent.ACTION_OUTSIDE -> {
                mHandler.removeCallbacks(mBarRunnable)
                if (view.isAttachedToWindow) {
                    mWindowManager.removeViewImmediate(view)
                }
            }
            MotionEvent.ACTION_UP -> view.performClick()
        }
        return true
    }

    private fun handleMessage(pushMessage: PushMessage) {
        val iMessage = Gson().fromJson(pushMessage.message, IMessage::class.java)
        LogUtils.dTag(TAG, iMessage.type)
        when (iMessage.type) {
            "image" -> if ("app" == iMessage.platform) {
                downloadPhotoFromApp(pushMessage, iMessage)
            } else {
                downloadPhotoFromPlatform(pushMessage, iMessage)
            }
            "video" -> downloadVideo(pushMessage, iMessage)
            else -> {
                isDownload = false
                GlobalScope.launch {
                    MyDatabase.instance.pushMessageDao.delete(pushMessage)
                }
            }
        }
    }

    /**
     * 下载App照片
     */
    private fun downloadPhotoFromApp(pushMessage: PushMessage, iMessage: IMessage) {
        /*RetrofitManager.getService(MessageService::class.java).download(iMessage.url)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        isDownload = false
                        GlobalScope.launch {
                            if (++pushMessage.errorCode > 3) {
                                MyDatabase.instance.pushMessageDao.delete(pushMessage)
                            } else {
                                MyDatabase.instance.pushMessageDao.update(pushMessage)
                            }
                        }
                    }

                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        GlobalScope.launch {
                            try {//保存压缩文件
                                val zipFilePath = File(File(MyUtils.getCloudDir(), iMessage.sender_account.toString()), iMessage.file_name).absolutePath
                                FileIOUtils.writeFileFromIS(zipFilePath, response.body()?.byteStream())
                                //解压压缩文件
                                val files = ZipUtils.unzipFile(File(zipFilePath), File(zipFilePath).parentFile)
                                //删除压缩文件
                                FileUtils.deleteFile(zipFilePath)
                                //描述信息
                                val texts = iMessage.text
                                //照片路径数组
                                val paths = arrayOfNulls<String>(files.size)
                                for (i in files.indices) {
                                    val contentValues = ContentValues()
                                    contentValues.put(MediaStore.Images.ImageColumns.DATA, files[i].absolutePath)
                                    val options = BitmapFactory.Options()
                                    options.inJustDecodeBounds = true
                                    BitmapFactory.decodeFile(files[i].absolutePath, options)
                                    contentValues.put(MediaStore.Images.ImageColumns.WIDTH, options.outWidth)
                                    contentValues.put(MediaStore.Images.ImageColumns.HEIGHT, options.outHeight)
                                    contentValues.put(MediaStore.Images.ImageColumns.DESCRIPTION, Gson().toJson(Description(iMessage.sender_id, iMessage.sender_name, iMessage.sender_remarkname, iMessage.sender_isReceive, iMessage.sender_account, iMessage.sender_avatar, iMessage.sender_platform, texts[i], true, false, 0, "cloud")))
                                    contentValues.put(MediaStore.Images.ImageColumns.IS_PRIVATE, false)
                                    Utils.getApp().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                                }
                                isDownload = false
                                MyDatabase.instance.pushMessageDao.delete(pushMessage)
                                launch(Dispatchers.Main) {
                                    val message = if (paths.size == 1) getString(R.string.message_photo) else getString(R.string.message_photos, paths.size)
                                    iMessage.filePath = files.last().absolutePath
                                    showMessage(message, iMessage)
                                }
                            } catch (e: Exception) {
                                isDownload = false
                                if (++pushMessage.errorCode > 3) {
                                    MyDatabase.instance.pushMessageDao.delete(pushMessage)
                                } else {
                                    MyDatabase.instance.pushMessageDao.update(pushMessage)
                                }
                            }
                        }
                    }
                })*/
        mDownloadTask = DownloadTask.Builder(iMessage.url, File(MyUtils.getCloudDir(), iMessage.sender_account.toString()))
                .setFilename(iMessage.file_name).setPassIfAlreadyCompleted(false).build()
        mDownloadTask?.enqueue(object : DownloadListener1() {
            override fun taskStart(task: DownloadTask, model: Listener1Assist.Listener1Model) {
                MyLogUtils.file("$task")
            }

            override fun taskEnd(task: DownloadTask, cause: EndCause, realCause: Exception?, model: Listener1Assist.Listener1Model) {
                //LogUtils.dTag(TAG, "$task, $cause, $realCause, $model")
                MyLogUtils.file("$task, cause = $cause, realCause = $realCause")
                when (cause) {
                    EndCause.COMPLETED -> {
                        try {
                            val zipFilePath = File(File(MyUtils.getCloudDir(), iMessage.sender_account.toString()), iMessage.file_name).absolutePath
                            //解压压缩文件
                            val files = ZipUtils.unzipFile(File(zipFilePath), File(zipFilePath).parentFile)
                            //删除压缩文件
                            FileUtils.delete(zipFilePath)
                            //描述信息
                            val texts = iMessage.text
                            //照片路径数组
                            val paths = arrayOfNulls<String>(files.size)
                            for (i in files.indices) {
                                val contentValues = ContentValues()
                                contentValues.put(MediaStore.Images.ImageColumns.DATA, files[i].absolutePath)
                                val options = BitmapFactory.Options()
                                options.inJustDecodeBounds = true
                                BitmapFactory.decodeFile(files[i].absolutePath, options)
                                contentValues.put(MediaStore.Images.ImageColumns.WIDTH, options.outWidth)
                                contentValues.put(MediaStore.Images.ImageColumns.HEIGHT, options.outHeight)
                                contentValues.put(MediaStore.Images.ImageColumns.DESCRIPTION, Gson().toJson(Description(iMessage.sender_id, iMessage.sender_name, iMessage.sender_remarkname, iMessage.sender_isReceive, iMessage.sender_account, iMessage.sender_avatar, iMessage.sender_platform, texts[i], true, false, 0f, "cloud")))
                                contentValues.put(MediaStore.Images.ImageColumns.IS_PRIVATE, false)
                                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                                //发送广播
                                val intent = Intent(MyConstants.ACTION_MEDIA_SCANNER_SCAN_FILE)
                                intent.data = Uri.fromFile(files[i])
                                sendBroadcast(intent)
                            }
                            isDownload = false
                            GlobalScope.launch {
                                MyDatabase.instance.pushMessageDao.delete(pushMessage)
                                launch(Dispatchers.Main) {
                                    val message = if (paths.size == 1) getString(R.string.message_photo) else getString(R.string.message_photos, paths.size)
                                    iMessage.filePath = files.last().absolutePath
                                    showMessage(message, iMessage)
                                }
                            }
                        } catch (e: Exception) {
                            isDownload = false
                            GlobalScope.launch {
                                if (++pushMessage.errorCode > 3) {
                                    MyDatabase.instance.pushMessageDao.delete(pushMessage)
                                } else {
                                    MyDatabase.instance.pushMessageDao.update(pushMessage)
                                }
                            }
                        }
                    }
                    else -> {
                        isDownload = false
                        GlobalScope.launch {
                            if (++pushMessage.errorCode > 3) {
                                MyDatabase.instance.pushMessageDao.delete(pushMessage)
                            } else {
                                MyDatabase.instance.pushMessageDao.update(pushMessage)
                            }
                        }
                    }
                }
            }

            override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
                LogUtils.dTag(TAG, "${currentOffset * 100f / totalLength}%")
            }

            override fun connected(task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long) {
                MyLogUtils.file("$task, blockCount = $blockCount, currentOffset = $currentOffset, totalLength = $totalLength")
            }

            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
                MyLogUtils.file("$task, cause = $cause")
            }
        })
    }

    /**
     * 下载Facebook,Twitter照片
     */
    private fun downloadPhotoFromPlatform(pushMessage: PushMessage, iMessage: IMessage) {
        /*RetrofitManager.getService(MessageService::class.java).download(iMessage.url)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        isDownload = false
                        GlobalScope.launch {
                            if (++pushMessage.errorCode > 3) {
                                MyDatabase.instance.pushMessageDao.delete(pushMessage)
                            } else {
                                MyDatabase.instance.pushMessageDao.update(pushMessage)
                            }
                        }
                    }

                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        GlobalScope.launch {
                            //保存文件
                            val filePath = File(File(MyUtils.getCloudDir(), iMessage.sender_account.toString()), iMessage.file_name).absolutePath
                            FileIOUtils.writeFileFromIS(filePath, response.body()?.byteStream())
                            //描述信息
                            var text = ""
                            if (iMessage.text.isNotEmpty()) text = iMessage.text[0]
                            val contentValues = ContentValues()
                            contentValues.put(MediaStore.Images.ImageColumns.DATA, filePath)
                            val options = BitmapFactory.Options()
                            options.inJustDecodeBounds = true
                            BitmapFactory.decodeFile(filePath, options)
                            contentValues.put(MediaStore.Images.ImageColumns.WIDTH, options.outWidth)
                            contentValues.put(MediaStore.Images.ImageColumns.HEIGHT, options.outHeight)
                            contentValues.put(MediaStore.Images.ImageColumns.DESCRIPTION, Gson().toJson(Description(iMessage.sender_id, iMessage.sender_name, iMessage.sender_remarkname, iMessage.sender_isReceive, iMessage.sender_account, iMessage.sender_avatar, iMessage.sender_platform, text, true, false, 0, "cloud")))
                            contentValues.put(MediaStore.Images.ImageColumns.IS_PRIVATE, false)
                            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                            isDownload = false
                            MyDatabase.instance.pushMessageDao.delete(pushMessage)
                            launch(Dispatchers.Main) {
                                iMessage.filePath = filePath
                                showMessage(getString(R.string.message_photo), iMessage)
                            }
                        }
                    }
                })*/
        mDownloadTask = DownloadTask.Builder(iMessage.url, File(MyUtils.getCloudDir(), iMessage.sender_account.toString()))
                .setFilename(iMessage.file_name).build()
        mDownloadTask?.enqueue(object : DownloadListener1() {
            override fun taskStart(task: DownloadTask, model: Listener1Assist.Listener1Model) {
                MyLogUtils.file("$task")
            }

            override fun taskEnd(task: DownloadTask, cause: EndCause, realCause: Exception?, model: Listener1Assist.Listener1Model) {
                //LogUtils.dTag(TAG, "$task, $cause, $realCause, $model")
                MyLogUtils.file("$task, cause = $cause, realCause = $realCause")
                when (cause) {
                    EndCause.COMPLETED -> {
                        val filePath = File(File(MyUtils.getCloudDir(), iMessage.sender_account.toString()), iMessage.file_name).absolutePath
                        //描述信息
                        var text = ""
                        if (iMessage.text.isNotEmpty()) text = iMessage.text[0]
                        val contentValues = ContentValues()
                        contentValues.put(MediaStore.Images.ImageColumns.DATA, filePath)
                        val options = BitmapFactory.Options()
                        options.inJustDecodeBounds = true
                        BitmapFactory.decodeFile(filePath, options)
                        contentValues.put(MediaStore.Images.ImageColumns.WIDTH, options.outWidth)
                        contentValues.put(MediaStore.Images.ImageColumns.HEIGHT, options.outHeight)
                        contentValues.put(MediaStore.Images.ImageColumns.DESCRIPTION, Gson().toJson(Description(iMessage.sender_id, iMessage.sender_name, iMessage.sender_remarkname, iMessage.sender_isReceive, iMessage.sender_account, iMessage.sender_avatar, iMessage.sender_platform, text, true, false, 0f, "cloud")))
                        contentValues.put(MediaStore.Images.ImageColumns.IS_PRIVATE, false)
                        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        //发送广播
                        val intent = Intent(MyConstants.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        intent.data = Uri.fromFile(File(filePath))
                        sendBroadcast(intent)
                        isDownload = false
                        GlobalScope.launch {
                            MyDatabase.instance.pushMessageDao.delete(pushMessage)
                            launch(Dispatchers.Main) {
                                iMessage.filePath = filePath
                                showMessage(getString(R.string.message_photo), iMessage)
                            }
                        }
                    }
                    else -> {
                        isDownload = false
                        GlobalScope.launch {
                            if (++pushMessage.errorCode > 3) {
                                MyDatabase.instance.pushMessageDao.delete(pushMessage)
                            } else {
                                MyDatabase.instance.pushMessageDao.update(pushMessage)
                            }
                        }
                    }
                }
            }

            override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
                LogUtils.dTag(TAG, "${currentOffset * 100f / totalLength}%")
            }

            override fun connected(task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long) {
                MyLogUtils.file("$task, blockCount = $blockCount, currentOffset = $currentOffset, totalLength = $totalLength")
            }

            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
                MyLogUtils.file("$task, cause = $cause")
            }
        })
    }

    /**
     * 下载视频
     */
    private fun downloadVideo(pushMessage: PushMessage, iMessage: IMessage) {
        /*RetrofitManager.getService(MessageService::class.java).download(iMessage.url)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        isDownload = false
                        GlobalScope.launch {
                            if (++pushMessage.errorCode > 3) {
                                MyDatabase.instance.pushMessageDao.delete(pushMessage)
                            } else {
                                MyDatabase.instance.pushMessageDao.update(pushMessage)
                            }
                        }
                    }

                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        GlobalScope.launch {
                            //保存文件
                            val filePath = File(File(MyUtils.getCloudDir(), iMessage.sender_account.toString()), iMessage.file_name).absolutePath
                            FileIOUtils.writeFileFromIS(filePath, response.body()?.byteStream())
                            var text = ""
                            if (iMessage.text.isNotEmpty()) text = iMessage.text[0]
                            val contentValues = ContentValues()
                            contentValues.put(MediaStore.Video.VideoColumns.DATA, filePath)
                            contentValues.put(MediaStore.Video.VideoColumns.DESCRIPTION, Gson().toJson(Description(iMessage.sender_id, iMessage.sender_name, iMessage.sender_remarkname, iMessage.sender_isReceive, iMessage.sender_account, iMessage.sender_avatar, iMessage.sender_platform, text, true, false, 0, "cloud")))
                            contentValues.put(MediaStore.Video.VideoColumns.IS_PRIVATE, false)
                            contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                            isDownload = false
                            MyDatabase.instance.pushMessageDao.delete(pushMessage)
                            launch(Dispatchers.Main) {
                                iMessage.filePath = filePath
                                showMessage(getString(R.string.message_video), iMessage)
                            }
                        }
                    }
                })*/
        mDownloadTask = DownloadTask.Builder(iMessage.url, File(MyUtils.getCloudDir(), iMessage.sender_account.toString()))
                .setFilename(iMessage.file_name).build()
        mDownloadTask?.enqueue(object : DownloadListener1() {
            override fun taskStart(task: DownloadTask, model: Listener1Assist.Listener1Model) {
                MyLogUtils.file("$task")
            }

            override fun taskEnd(task: DownloadTask, cause: EndCause, realCause: Exception?, model: Listener1Assist.Listener1Model) {
                //LogUtils.dTag(TAG, "$task, $cause, $realCause, $model")
                MyLogUtils.file("$task, cause = $cause, realCause = $realCause")
                when (cause) {
                    EndCause.COMPLETED -> {
                        val filePath = File(File(MyUtils.getCloudDir(), iMessage.sender_account.toString()), iMessage.file_name).absolutePath
                        var text = ""
                        if (iMessage.text.isNotEmpty()) text = iMessage.text[0]
                        val contentValues = ContentValues()
                        contentValues.put(MediaStore.Video.VideoColumns.DATA, filePath)
                        contentValues.put(MediaStore.Video.VideoColumns.DESCRIPTION, Gson().toJson(Description(iMessage.sender_id, iMessage.sender_name, iMessage.sender_remarkname, iMessage.sender_isReceive, iMessage.sender_account, iMessage.sender_avatar, iMessage.sender_platform, text, true, false, 0f, "cloud")))
                        contentValues.put(MediaStore.Video.VideoColumns.IS_PRIVATE, false)
                        contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                        //发送广播
                        val intent = Intent(MyConstants.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        intent.data = Uri.fromFile(File(filePath))
                        sendBroadcast(intent)
                        isDownload = false
                        GlobalScope.launch {
                            MyDatabase.instance.pushMessageDao.delete(pushMessage)
                            launch(Dispatchers.Main) {
                                iMessage.filePath = filePath
                                showMessage(getString(R.string.message_video), iMessage)
                            }
                        }
                    }
                    else -> {
                        isDownload = false
                        GlobalScope.launch {
                            if (++pushMessage.errorCode > 3) {
                                MyDatabase.instance.pushMessageDao.delete(pushMessage)
                            } else {
                                MyDatabase.instance.pushMessageDao.update(pushMessage)
                            }
                        }
                    }
                }
            }

            override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
                LogUtils.dTag(TAG, "${currentOffset * 100f / totalLength}%")
            }

            override fun connected(task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long) {
                MyLogUtils.file("$task, blockCount = $blockCount, currentOffset = $currentOffset, totalLength = $totalLength")
            }

            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
                MyLogUtils.file("$task, cause = $cause")
            }
        })
    }

    private fun showMessage(message: String, iMessage: IMessage) {
        mHandler.removeCallbacks(mBarRunnable)
        if (mNotificationBar.isAttachedToWindow) {
            mWindowManager.removeViewImmediate(mNotificationBar)
        }
        mContent.text = message
        mWindowManager.addView(mNotificationBar, mLayoutParams)
        mIMessage = iMessage
        mHandler.postDelayed(mBarRunnable, 8000)
    }

    private fun isNetworkConnected(): Boolean {
        if (NetworkUtils.isConnected()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkCapabilities = mConnectivityManager.getNetworkCapabilities(mConnectivityManager.activeNetwork)
                if (networkCapabilities != null) {
                    val hasCapability = mConnectivityManager.getNetworkCapabilities(mConnectivityManager.activeNetwork)
                            .hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    if (!hasCapability) {
                        ToastUtils.showShort(R.string.network_unavailable)
                    }
                    return hasCapability
                }
            }
            return true
        }
        ToastUtils.showShort(R.string.unconnected_network)
        return false
    }

    private fun isStorageAvailable(): Boolean {
        return StatFs(Environment.getExternalStorageDirectory().absolutePath).availableBytes > 100 * 1024 * 1024
    }

    override fun onDestroy() {
        super.onDestroy()
        mDownloadTask?.cancel()
        if (mMaterialDialog != null) {
            mMaterialDialog?.dismiss()
            mMaterialDialog = null
        }
        mHandler.removeCallbacks(mBarRunnable)
        unregisterReceiver(mBroadcastReceiver)
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    companion object {
        private val TAG = MessageService::class.java.simpleName
    }
}
