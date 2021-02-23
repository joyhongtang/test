package com.joyhong.test.device

import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.DeviceUtils
import com.blankj.utilcode.util.SPUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
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
        Log.e("KKKKKK", "1")
        mDeviceInfo.add(DeviceInfoItem("相框ID ： ", TestConstant.deviceToken))
        Log.e("KKKKKK", "2")
        mDeviceInfo.add(DeviceInfoItem("SN ： ", TestConstant.snnumber))
        Log.e("KKKKKK", "3")
        val macAddress = try {
            DeviceUtils.getMacAddress()
        } catch (e: Exception) {
            ""
        }
        mDeviceInfo.add(DeviceInfoItem("MAC:", macAddress))
        Log.e("KKKKKK", "5")
        mDeviceInfo.add(DeviceInfoItem("Android sdk:", Build.VERSION.RELEASE))
        mDeviceInfo.add(DeviceInfoItem("Build version", Build.DISPLAY))

        val system_info = "\nSystemInfo_appversion:" +
                AppUtils.getAppVersionName() +
                "\nSystemInfo_frameid:" + TestConstant.deviceToken +
                "\nSystemInfo_sn:" + TestConstant.snnumber +
                "\nSystemInfo_mac:" + macAddress +
                "\nSystemInfo_systemversion:" + Build.VERSION.RELEASE +
                "\nSystemInfo_machine:" + Build.DISPLAY
        val testEntity =
            TestMainActivity.testResult["${TestConstant.PACKAGE_NAME}$localClassName"]
        SPUtils.getInstance().put(testEntity!!.tag + "_detail", system_info)

        Log.e("KKKKKK", "6")
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
        Log.e("KKKKKK", "7")
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
        Log.e("KKKKKK", "8")
        rv_device_info.adapter = mMusicAdapter
//        fail.requestFocus()
        Log.e("KKKKKK", "9")

        fail.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        Log.e("KKKKKK", "10")
        if (checkDeviceInfoAllExist()) {
            Log.e("KKKKKK", "12")
//            test_result.visibility = View.VISIBLE
        } else {
            Log.e("KKKKKK", "13")
//            test_result.visibility = View.INVISIBLE
        }
        Log.e("KKKKKK", "11")
    }

    fun checkDeviceInfoAllExist(): Boolean {
        var success = true
        var macAddress = ""
         macAddress = try {
            DeviceUtils.getMacAddress()
        } catch (e: Exception) {
            ""
        }
        if (TextUtils.isEmpty(AppUtils.getAppVersionName()) || TextUtils.isEmpty(TestConstant.deviceToken) ||
            TextUtils.isEmpty(TestConstant.snnumber) || TextUtils.isEmpty(macAddress) || TextUtils.isEmpty(
                Build.VERSION.RELEASE
            )
            || TextUtils.isEmpty(Build.DISPLAY)
        ) {
            success = false
        }
        return success
    }

    override fun initListener() {
        findViewById<View>(R.id.pass).setOnClickListener(this)
        findViewById<View>(R.id.fail).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v!!.id) {
            R.id.pass -> {
                if (!TestConstant.deviceToken.isNullOrEmpty()) {
                    val testEntity =
                        TestMainActivity.testResult["${TestConstant.PACKAGE_NAME}$localClassName"]
                    testEntity!!.testResultEnum = TestResultEnum.PASS
                    SPUtils.getInstance().put(testEntity.getTag(), 1)
                    finish()
                }
            }
            R.id.fail -> {
                val testEntity2 =
                    TestMainActivity.testResult["${TestConstant.PACKAGE_NAME}$localClassName"]
                testEntity2!!.testResultEnum = TestResultEnum.FAIL
                SPUtils.getInstance().put(testEntity2.getTag(), 2)
                finish()
            }
        }
    }


}