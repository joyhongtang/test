package com.idwell.cloudframe.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.blankj.utilcode.util.NetworkUtils
import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseFragment
import com.idwell.cloudframe.common.MessageEvent
import kotlinx.android.synthetic.main.fragment_wifi.*

class WifiFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wifi, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        sw_fragment_wifi.isChecked = NetworkUtils.getWifiEnabled()
    }

    override fun initListener() {
        sw_fragment_wifi.setOnCheckedChangeListener { _, isChecked -> NetworkUtils.setWifiEnabled(isChecked) }
    }
}