package com.idwell.cloudframe.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder

import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.base.BaseFragment
import com.idwell.cloudframe.widget.MaterialDialog
import kotlinx.android.synthetic.main.fragment_guide_device_info.*

class GuideDeviceInfoFragment : BaseFragment() {

    private val mData = mutableListOf<String>()
    private lateinit var mDeviceInfoAdapter: BaseQuickAdapter<String, BaseViewHolder>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_guide_device_info, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        mDeviceInfoAdapter = object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_device_info, mData) {
            override fun convert(helper: BaseViewHolder, item: String?) {
                if (item != null) {
                    helper.setText(R.id.tv_item_device_info, item)
                }
            }
        }
        rv_content_guide_device_info.layoutManager = LinearLayoutManager(activity)
        rv_content_guide_device_info.adapter = mDeviceInfoAdapter
        if (Device.id != -1) {
            refreshUI()
        } else {
            cl_network_guide_device_info.visibility = View.VISIBLE
        }
        Device.infoState.observe({ this.lifecycle }, { state ->
            LogUtils.dTag(TAG, state)
            when (state) {
                1 -> {
                    refreshUI()
                }
                2 -> {
                    cl_content_guide_device_info.visibility = View.INVISIBLE
                    pb_content_network_guide_device_info.visibility = View.VISIBLE
                    tv_content_network_guide_device_info.setText(R.string.display_device_info)
                    cl_network_guide_device_info.visibility = View.VISIBLE
                }
                3 -> {
                    cl_content_guide_device_info.visibility = View.INVISIBLE
                    pb_content_network_guide_device_info.visibility = View.INVISIBLE
                    tv_content_network_guide_device_info.setText(R.string.refresh_desc)
                    cl_network_guide_device_info.visibility = View.VISIBLE
                }
            }
        })
    }

    override fun initListener() {
        iv_prev_guide_device_info.setOnClickListener(this)
        iv_refresh_network_guide_device_info.setOnClickListener(this)
        tv_skip_guide_device_info.setOnClickListener(this)
        iv_next_guide_device_info.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            iv_prev_guide_device_info -> {
                activity?.let {
                    (it as GuideActivity).showFragment(2)
                }
            }
            tv_skip_guide_device_info -> {
                activity?.onBackPressed()
            }
            iv_next_guide_device_info -> {
                if (mData.isEmpty()) {
                    context?.let {
                        MaterialDialog.Builder(it)
                                .setTitle(R.string.device_information_not_obtained)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.ok, null).show()
                    }
                } else {
                    activity?.onBackPressed()
                }
            }
            iv_refresh_network_guide_device_info -> {
                if (Device.infoState.value == 2) {
                    ToastUtils.showShort(R.string.refreshing)
                } else {
                    LogUtils.dTag(TAG, "context?.sendBroadcast(Intent(Device.ACTION_SIGN_IN))")
                    context?.sendBroadcast(Intent(Device.ACTION_SIGN_IN))
                }
            }
        }
    }

    private fun refreshUI() {
        cl_network_guide_device_info.visibility = View.INVISIBLE
        tv_id_content_guide_device_info.text = getString(R.string.frame_id_desc, Device.token)
        mData.clear()
        if (Device.email.isEmpty()) {
            mData.add(Device.activationDesc)
            mData.add(Device.iosDesc)
            mData.add(Device.androidDesc)
        } else {
            mData.add(Device.iosDesc)
            mData.add(Device.androidDesc)
            //邮箱地址
            when (Device.companyName) {
                "Joyhong" -> {
                    mData.add(Device.emailDesc.replace("xxxx@bsimb.cn", Device.email))
                }
                "Aluratek" -> {
                    mData.add(Device.emailDesc.replace("xxxxxx@wififrame.com", Device.email))
                }
                "Ourphoto" -> {
                    mData.add(Device.emailDesc.replace("xxxxxx@ourphoto.cn", Device.email))
                }
            }
            //mData.add(Device.facebookDesc)
            //mData.add(Device.twitterDesc)
        }
        mDeviceInfoAdapter.setNewData(mData)
        cl_content_guide_device_info.visibility = View.VISIBLE
    }

    companion object {
        private val TAG = GuideDeviceInfoFragment::class.java.simpleName
    }
}
