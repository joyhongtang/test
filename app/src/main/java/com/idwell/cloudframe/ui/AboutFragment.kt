package com.idwell.cloudframe.ui

import android.app.Dialog
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blankj.utilcode.util.*

import com.chad.library.adapter.base.BaseQuickAdapter
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.adapter.AboutAdapter
import com.idwell.cloudframe.base.BaseFragment
import com.idwell.cloudframe.http.RetrofitManager
import com.idwell.cloudframe.http.service.VersionService
import com.idwell.cloudframe.http.entity.Version
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.entity.MultipleItem
import com.idwell.cloudframe.http.BaseHttpObserver
import com.idwell.cloudframe.widget.MaterialDialog
import com.idwell.cloudframe.widget.RingView

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_recyclerview.*
import java.io.File
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import com.blankj.utilcode.util.FileUtils
import com.idwell.cloudframe.MyApplication
import com.idwell.cloudframe.util.MyLogUtils
import com.joyhong.test.TestMainActivity
import com.joyhong.test.util.TestConstant
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener1
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist

class AboutFragment : BaseFragment(), BaseQuickAdapter.OnItemClickListener {

    private val mData = mutableListOf<MultipleItem>()
    private lateinit var mAboutAdapter: AboutAdapter

    private var mRingDialog: Dialog? = null
    private var mRingView: RingView? = null
    private var mDownloadTask: DownloadTask? = null

    //点击次数
    private var mHits = LongArray(COUNTS)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recyclerview, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        val macAddress = try {
            DeviceUtils.getMacAddress()
        } catch (e: Exception) {
            ""
        }
        val availableFlow = ConvertUtils.byte2FitMemorySize((if (Device.flow >= 1000) Device.flow * 1.024F * 1024 * 1024 else Device.flow * 1024 * 1024).toLong())
        mData.add(MultipleItem(AboutAdapter.TEXTST_TEXTSB_TEXTEB, getString(R.string.version), AppUtils.getAppVersionName(), getString(R.string.check_for_updates)))
        mData.add(MultipleItem(AboutAdapter.TEXTST_TEXTSB, getString(R.string.frame_id), Device.token))
        mData.add(MultipleItem(AboutAdapter.TEXTST_TEXTSB, getString(R.string.serial_number), Device.snnumber))
        mData.add(MultipleItem(AboutAdapter.TEXTST_TEXTSB, getString(R.string.status_wifi_mac_address), macAddress))
        mData.add(MultipleItem(AboutAdapter.TEXTST_TEXTSB, getString(R.string.model), Build.MODEL))
        mData.add(MultipleItem(AboutAdapter.TEXTST_TEXTSB, getString(R.string.build_number), Build.DISPLAY))
        mData.add(MultipleItem(AboutAdapter.TEXTST_TEXTSB_TEXTEB, getString(R.string.internal_storage), getString(R.string.total, Device.getTotalBytes()), getString(R.string.available, Device.getAvailableBytes())))
        if (Device.companyName != "Aluratek" && !Device.isUnlimitedData) {
            mData.add(MultipleItem(AboutAdapter.TEXTST_TEXTSB_TEXTET_TEXTEB, getString(R.string.data_flow), getString(R.string.available, availableFlow), "", getString(R.string.top_up)))
        }
        mAboutAdapter = AboutAdapter(mData)
        rv_fragment_recyclerview.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        context?.let { rv_fragment_recyclerview.addItemDecoration(
            HorizontalItemDecoration(
                ContextCompat.getColor(it, R.color.divider)
            )
        ) }
        rv_fragment_recyclerview.adapter = mAboutAdapter

        Device.infoState.observe({ this.lifecycle }, { state ->
            LogUtils.dTag(TAG, state)
            when (state) {
                1 -> {
                    mData[1].content = Device.token
                    mAboutAdapter.notifyItemChanged(1)
                }
            }
        })
    }

    override fun initListener() {
        mAboutAdapter.onItemClickListener = this
    }

    override fun onMessageEvent(event: MessageEvent) {
        when (event.message) {
            MessageEvent.DEVICE_UPDATE_DATA_FLOW -> {
                val availableFlow = ConvertUtils.byte2FitMemorySize((if (Device.flow >= 1000) Device.flow * 1.024F * 1024 * 1024 else Device.flow * 1024 * 1024).toLong())
                mData.last().content = availableFlow
                mAboutAdapter.notifyItemChanged(mData.lastIndex)
            }
        }
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        when (position) {
            0 -> {
                checkNewVersion()
            }
            4 -> {
                startShareAppLog()
            }
            5 -> {
                startSettings()
            }
            7 -> {
                context?.let {
                    MaterialDialog.Builder(it).setTitle(R.string.data_top_up)
                            .setContent(getString(R.string.top_up_link, "https://well.bsimb.cn/recharge/index"))
                            .show()
                }
            }
            6 ->{
                startTest()
            }
        }
    }

    private fun startShareAppLog() {
        System.arraycopy(mHits, 1, mHits, 0, mHits.size - 1)
        mHits[mHits.size - 1] = SystemClock.uptimeMillis()
        if (SystemClock.uptimeMillis() - mHits[0] <= DURATION) {
            mHits = LongArray(COUNTS)
            context?.sendBroadcast(Intent(Device.ACTION_SHARE_APP_LOG))
        }
    }

    /**
     * FOR TEST
     */
    private fun startTest() {
        //每次点击时，数组向前移动一位
        System.arraycopy(mHits, 1, mHits, 0, mHits.size - 1)
        //为数组最后一位赋值
        mHits[mHits.size - 1] = SystemClock.uptimeMillis()
        if (SystemClock.uptimeMillis() - mHits[0] <= DURATION) {
            mHits = LongArray(COUNTS) //重新初始化数组
            TestConstant.initTest(MyApplication.instance(), Device.token, Device.snnumber)
            startActivity(Intent(activity, TestMainActivity::class.java))
        }
    }

    private fun startSettings() {
        if (Device.openSystemSettings) {
            System.arraycopy(mHits, 1, mHits, 0, mHits.size - 1)
            mHits[mHits.size - 1] = SystemClock.uptimeMillis()
            if (SystemClock.uptimeMillis() - mHits[0] <= DURATION) {
                mHits = LongArray(COUNTS)
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
            }
        }
    }

    /**
     * 检测新版本
     */
    private fun checkNewVersion() {
        RetrofitManager.getService(VersionService::class.java)
                .version(Device.id, AppUtils.getAppVersionCode()).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : BaseHttpObserver<Version>(context) {
                    override fun onSuccess(data: Version) {
                        if (Device.versionCode == data.last_version && Device.versionCode > AppUtils.getAppVersionCode() && File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CloudPhotoFrame.apk").exists()) {
                            context?.let {
                                MaterialDialog.Builder(it).setTitle(R.string.install_new_version)
                                        .setContent(data.version_desc)
                                        .setNegativeButton(R.string.cancel, null)
                                        .setPositiveButton(R.string.install, object : MaterialDialog.OnClickListener {
                                            override fun onClick(dialog: MaterialDialog) {
                                                installApp()
                                            }
                                        }).show()
                            }
                        } else {
                            if (AppUtils.getAppVersionCode() < data.last_version) {
                                context?.let {
                                    MaterialDialog.Builder(it).setTitle(R.string.find_new_version)
                                            .setContent(data.version_desc)
                                            .setNegativeButton(R.string.cancel, null)
                                            .setPositiveButton(R.string.download, object : MaterialDialog.OnClickListener {
                                                override fun onClick(dialog: MaterialDialog) {
                                                    FileUtils.delete(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CloudPhotoFrame.apk"))
                                                    downloadApk(data)
                                                }
                                            }).show()
                                }
                            } else {
                                ToastUtils.showShort(R.string.its_the_latest_version)
                            }
                        }
                    }
                })
    }

    private fun showRingDialog() {
        if (mRingDialog == null) {
            context?.let {
                val view = View.inflate(it, R.layout.view_ring, null)
                mRingView = view.findViewById(R.id.rv_view_ring)
                mRingDialog = Dialog(it, R.style.LoadingDialog)
                mRingDialog?.setContentView(view)
                mRingDialog?.setCanceledOnTouchOutside(false)
            }
        } else {
            mRingView?.setProgress(0)
        }
        mRingDialog?.show()
    }

    /**
     * 下载APK
     */
    private fun downloadApk(data: Version) {
        /*RetrofitManager.getProgressService(object : ProgressListener {
            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                mRingView.setProgress((bytesRead * 100 / contentLength).toInt())
            }
        }, DownloadService::class.java).download(data.download_link).map { body ->
            //保存文件
            FileIOUtils.writeFileFromBytesByStream(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CloudPhotoFrame.apk"), body.bytes())
            ""
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : BaseObserver<Any>() {
                    override fun onSubscribe(d: Disposable) {
                        showRingDialog()
                    }

                    override fun onNext(t: Any) {
                        Device.versionCode = data.last_version
                        Device.versionDesc = data.version_desc
                        dismissRingDialog()
                        installApp()
                    }

                    override fun onError(e: Throwable) {
                        dismissRingDialog()
                    }
                })*/
        mDownloadTask = DownloadTask.Builder(data.download_link, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)).setFilename("CloudPhotoFrame.apk").setMinIntervalMillisCallbackProcess(200).setPassIfAlreadyCompleted(false).build()
        mDownloadTask?.enqueue(object : DownloadListener1() {
            override fun taskStart(task: DownloadTask, model: Listener1Assist.Listener1Model) {
                MyLogUtils.file("$task")
                showRingDialog()
            }

            override fun taskEnd(task: DownloadTask, cause: EndCause, realCause: Exception?, model: Listener1Assist.Listener1Model) {
                MyLogUtils.file("$task, cause = $cause, realCause = $realCause")
                when(cause) {
                    EndCause.COMPLETED -> {
                        Device.versionCode = data.last_version
                        Device.versionDesc = data.version_desc
                        dismissRingDialog()
                        installApp()
                    }
                    else -> {
                        dismissRingDialog()
                    }
                }
            }

            override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
                mRingView?.setProgress((currentOffset * 100f / totalLength).toInt())
            }

            override fun connected(task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long) {
                MyLogUtils.file("$task, blockCount = $blockCount, currentOffset = $currentOffset, totalLength = $totalLength")
            }

            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
                MyLogUtils.file("$task, cause = $cause")
            }
        })
    }

    private fun installApp() {
        //普通安装
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.fromFile(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CloudPhotoFrame.apk")), "application/vnd.android.package-archive")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context?.startActivity(intent)
        //静默安装
        /*GlobalScope.launch {
            val command = "pm install -r -i com.idwell.cloudframe --user 0 ${context?.filesDir}/CloudPhotoFrame.apk"
            Runtime.getRuntime().exec(command)
        }*/
    }

    private fun dismissRingDialog() {
        if (mRingDialog == null) return
        mRingDialog?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        mDownloadTask?.cancel()
    }

    companion object {
        private val TAG = AboutFragment::class.java.simpleName

        const val COUNTS = 10
        const val DURATION = 10_000L
    }
}
