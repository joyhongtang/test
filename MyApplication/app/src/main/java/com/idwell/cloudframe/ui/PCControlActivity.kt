package com.idwell.cloudframe.ui

import android.view.View

import com.blankj.utilcode.util.NetworkUtils
import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.util.NetworkUtil

import kotlinx.android.synthetic.main.activity_pc_control.*
import com.idwell.cloudframe.service.FtpService
import android.content.Intent
import com.blankj.utilcode.util.ServiceUtils
import com.blankj.utilcode.util.ToastUtils
import kotlinx.android.synthetic.main.activity_base.*

class PCControlActivity : BaseActivity() {

    override fun initLayout(): Int {
        return R.layout.activity_pc_control
    }

    override fun initData() {
        tv_title_base.setText(R.string.pc_control)
        if (NetworkUtils.getWifiEnabled() && NetworkUtils.isConnected()) {
            tv_wlan.text = getString(R.string.wlan_name, NetworkUtil.getActiveNetworkInfo().extraInfo)
            iv_wlan.setImageResource(R.drawable.ic_wifi_green)
        } else {
            tv_wlan.setText(R.string.please_connect_an_available_wifi)
            iv_wlan.setImageResource(R.drawable.ic_wifi_gray)
        }
        if (ServiceUtils.isServiceRunning(FtpService::class.java)) {
            tv_address.text = "ftp://${NetworkUtils.getIPAddress(true)}:2221"
            tv_address.visibility = View.VISIBLE
            iv_start_stop.setImageResource(R.drawable.ic_stop_white)
            tv_start_stop.text = getString(R.string.stop)
            tv_desc.text = getString(R.string.enter_the_following_address_into_your_ftp_client)
        } else {
            iv_start_stop.setImageResource(R.drawable.ic_start_white)
        }
    }

    override fun initListener() {
        cl_start_stop.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v) {
            cl_start_stop -> if (tv_start_stop.text.toString() == getString(R.string.start)) {
                if (NetworkUtils.isConnected()){
                    startFtpServer()//开启ftp服务器
                    iv_start_stop.setImageResource(R.drawable.ic_stop_white)
                    tv_start_stop.text = getString(R.string.stop)
                    tv_desc.text = getString(R.string.enter_the_following_address_into_your_ftp_client)
                }else{
                    ToastUtils.showShort(R.string.unconnected_network)
                }
            } else {
                stopService(Intent(this, FtpService::class.java))
                iv_start_stop.setImageResource(R.drawable.ic_start_white)
                tv_start_stop.text = getString(R.string.start)
                tv_desc.text = getString(R.string.start_service_to_enable_ftp_server)
                tv_address.text = ""
                tv_address.visibility = View.INVISIBLE
            }
        }
    }

    override fun initMessageEvent(event: MessageEvent) {

    }

    /**
     * 开启FTP服务器
     */
    private fun startFtpServer() {
        startService(Intent(this, FtpService::class.java))
        tv_address.text = "ftp://${NetworkUtils.getIPAddress(true)}:2221"
        tv_address.visibility = View.VISIBLE
    }
}