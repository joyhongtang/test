package com.joyhong.test.device

import android.os.Build
import android.view.View
import androidx.core.content.ContextCompat
import com.blankj.utilcode.BuildConfig
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.DeviceUtils
import com.blankj.utilcode.util.SPUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.ui.test.device.DeviceInfoItem
import com.joyhong.test.BaseTestActivity
import com.joyhong.test.R
import com.joyhong.test.TestMainActivity
import com.joyhong.test.TestResultEnum
import com.joyhong.test.util.TestConstant
import com.joyhong.test.util.TestHorizontalItemDecoration
import kotlinx.android.synthetic.main.activity_test_device.*

class DeviceInfoTestActivity : BaseTestActivity() {

    private lateinit var mMusicAdapter: BaseQuickAdapter<DeviceInfoItem, BaseViewHolder>
    private var mDeviceInfo = mutableListOf<DeviceInfoItem>()
    private lateinit var mLinearLayoutManager: androidx.recyclerview.widget.LinearLayoutManager
    override fun initLayout(): Int {
        return R.layout.activity_test_device
    }

    override fun initData() {
        //tv_title_base.setText(R.string.music)
        mDeviceInfo.add(DeviceInfoItem("APP版本 ： ", AppUtils.getAppVersionName()))
        mDeviceInfo.add(DeviceInfoItem("相框ID ： ", TestConstant.deviceToken))
        mDeviceInfo.add(DeviceInfoItem("SN ： ", TestConstant.snnumber))
        val macAddress = try {
            DeviceUtils.getMacAddress()
        } catch (e: Exception) {
            ""
        }
        mDeviceInfo.add(DeviceInfoItem("MAC 地址 ： ", macAddress))
        mDeviceInfo.add(DeviceInfoItem("Android 系统版本 ： ", Build.VERSION.RELEASE))
        mDeviceInfo.add(DeviceInfoItem("Build 系统版本 ： ", Build.DISPLAY))
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
        try {
            fail.requestFocus()
        }catch (e:Exception){
            e.printStackTrace()
        }

    }

    override fun initListener() {
        findViewById<View>(R.id.pass).setOnClickListener(this)
        findViewById<View>(R.id.fail).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v!!.id) {
            R.id.pass -> {
                val testEntity =
                    TestMainActivity.testResult["${TestConstant.PACKAGE_NAME}$localClassName"]
                testEntity!!.testResultEnum = TestResultEnum.PASS
                SPUtils.getInstance().put(testEntity.getTag(),1)
                finish()
            }
            R.id.fail -> {
                val testEntity2 =
                    TestMainActivity.testResult["${TestConstant.PACKAGE_NAME}$localClassName"]
                testEntity2!!.testResultEnum = TestResultEnum.FAIL
                SPUtils.getInstance().put(testEntity2.getTag(),2)
                finish()
            }
        }
    }


}