package com.joyhong.test.interfacedevice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.SPUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.ui.test.device.DeviceInfoItem
import com.joyhong.test.BaseTestActivity
import com.joyhong.test.R
import com.joyhong.test.TestMainActivity
import com.joyhong.test.TestResultEnum
import com.joyhong.test.interfacedevice.utils.DeviceTestUtils
import com.joyhong.test.util.TestConstant
import com.joyhong.test.util.TestHorizontalItemDecoration
import kotlinx.android.synthetic.main.activity_test_device.*

public class InterfaceDevice : BaseTestActivity(), UsbLis, HeadSetLis {

    private lateinit var mMusicAdapter: BaseQuickAdapter<DeviceInfoItem, BaseViewHolder>
    var mDeviceInfo = mutableListOf<DeviceInfoItem>()
    private lateinit var mLinearLayoutManager: androidx.recyclerview.widget.LinearLayoutManager
    override fun initLayout(): Int {
        return R.layout.activity_test_interface_device
    }

    override fun initData() {
        var sdcadrResule = DeviceInfoItem("外置SDCARD ： ", "")
        if (TestMainActivity.EXIST_EXTERNA_STORAGE) {
            mDeviceInfo.add(sdcadrResule)
        }
        var UResule = DeviceInfoItem("U盘 ： ", "")
        if (TestMainActivity.EXIST_USB_STORAGE) {
            mDeviceInfo.add(UResule);
        }
        var HeadSetResule = DeviceInfoItem("挂载耳机 ： ", "")
        if (TestMainActivity.EXIST_HEADSET) {
            mDeviceInfo.add(HeadSetResule);
        }
        if (SPUtils.getInstance().getInt("isExternalStorage", -1) == 1) {
            sdcadrResule.content = "测试成功"
        }
        if (SPUtils.getInstance().getInt("isUsbStorage", -1) == 1) {
            UResule.content = "测试成功"
        }
        if (SPUtils.getInstance().getInt("isHeadSet", -1) == 1) {
            HeadSetResule.content = "测试成功"
        }
        mMusicAdapter =
            object : BaseQuickAdapter<DeviceInfoItem, BaseViewHolder>(
                R.layout.item_device_test,
                mDeviceInfo
            ) {
                override fun convert(helper: BaseViewHolder, item: DeviceInfoItem?) {
                    if (item != null) {
                        helper.setText(R.id.title, item.title)
                        helper.setText(R.id.desp, item.content)
                    }
                }
            }
        mLinearLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rv_device_info.layoutManager = mLinearLayoutManager
        rv_device_info.addItemDecoration(
            TestHorizontalItemDecoration(
                ContextCompat.getColor(
                    this,
                    R.color.divider
                )
            )
        )
        rv_device_info.adapter = mMusicAdapter
    }

    override fun onDestroy() {
        if (null != usbBroadcast) {
            unregisterReceiver(usbBroadcast)
            unregisterReceiver(headsetReceiver)
        }
        super.onDestroy()
    }

    lateinit var usbBroadcast: UsbBroadcast
    lateinit var headsetReceiver: HeadsetReceiver
    override fun initListener() {
        usbBroadcast = UsbBroadcast()
        usbBroadcast.usbLis = this
        val usbFilter = IntentFilter()
        usbFilter.addAction(Intent.ACTION_MEDIA_MOUNTED)
        usbFilter.addAction(Intent.ACTION_MEDIA_REMOVED)
        usbFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED)
        usbFilter.addAction(Intent.ACTION_HEADSET_PLUG)
        usbFilter.addDataScheme("file")
        registerReceiver(usbBroadcast, usbFilter)


        headsetReceiver =
            HeadsetReceiver()
        headsetReceiver.headSetLis = this
        val headsetFilter =
            IntentFilter(Intent.ACTION_HEADSET_PLUG)
        registerReceiver(headsetReceiver, headsetFilter)

    }

    class UsbBroadcast : BroadcastReceiver() {
        lateinit var usbLis: UsbLis
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            when (intent.action) {
                Intent.ACTION_MEDIA_MOUNTED -> {
                    if (null != usbLis) {
                        usbLis.onMediaStateChanged()
                    }
                }
                Intent.ACTION_MEDIA_UNMOUNTED -> {
                }
                else -> {
                    if (intent.action.equals(Intent.ACTION_HEADSET_PLUG)) {
                        if (intent.hasExtra("state")) {
                            var state = intent.getIntExtra("state", 0);
                        }

                    }
                }
            }
        }
    }

    class HeadsetReceiver : BroadcastReceiver() {
        lateinit var headSetLis: HeadSetLis
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            // 耳机插入状态 0 拔出，1 插入
            val state = if (intent.getIntExtra("state", 0) == 0) false else true
            // 耳机类型
            val name = intent.getStringExtra("name")
            // 耳机是否带有麦克风 0 没有，1 有
            val mic = if (intent.getIntExtra("microphone", 0) == 0) false else true
            val headsetChange =
                String.format("耳机插入: %b, 有麦克风: %b", state, mic)
            if (null != headSetLis) {
                headSetLis.onHeadSetStateChanged(state, mic)
            }
        }
    }


    override fun onMediaStateChanged() {
        var deviceTypeResult = DeviceTestUtils.getAllStorageType()
        if (deviceTypeResult.isExternalStorage) {
            for (deviceinfo in mDeviceInfo) {
                if (deviceinfo.title.contains("外置SDCARD")) {
                    deviceinfo.content = "测试成功"
                }
            }
            SPUtils.getInstance().put("isExternalStorage", 1)
        }
        if (deviceTypeResult.isUsbStorage) {
            for (deviceinfo in mDeviceInfo) {
                if (deviceinfo.title.contains("U盘")) {
                    deviceinfo.content = "测试成功"
                }
            }
            SPUtils.getInstance().put("isUsbStorage", 1)
        }
        mMusicAdapter.notifyDataSetChanged()
        if (checkResult()) {

        }
    }

    override fun onHeadSetStateChanged(headset: Boolean, micPhone: Boolean) {
        if (headset) {
            for (deviceinfo in mDeviceInfo) {
                if (deviceinfo.title.contains("挂载耳机")) {
                    deviceinfo.content = "测试成功"
                }
            }
            SPUtils.getInstance().put("isHeadSet", 1)
            mMusicAdapter.notifyDataSetChanged()
            if (checkResult()) {
            }
        }
    }

    fun checkResult(): Boolean {
        var success: Boolean = true
        for (deviceinfo in mDeviceInfo) {
            if (!TextUtils.equals("测试成功", deviceinfo.content)) {
                success = false
            }
        }
        if (success) {
            val testEntity =
                TestMainActivity.testResult["${TestConstant.PACKAGE_NAME}$localClassName"]
            testEntity!!.testResultEnum = TestResultEnum.PASS
            SPUtils.getInstance().put(testEntity.getTag(), 1)
            finish()
        }
        return success
    }

}