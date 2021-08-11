package com.idwell.cloudframe.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.blankj.utilcode.util.NetworkUtils
import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseFragment
import com.idwell.cloudframe.widget.MaterialDialog
import kotlinx.android.synthetic.main.fragment_guide_wifi.*

class GuideWiFiFragment : BaseFragment(), View.OnClickListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_guide_wifi, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        if (!NetworkUtils.getWifiEnabled()) {
            NetworkUtils.setWifiEnabled(true)
        }
    }

    override fun initListener() {
        iv_next_guide_wifi.setOnClickListener(this)
        tv_skip_guide_wifi.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            tv_skip_guide_wifi -> {
                activity?.let {
                    (it as GuideActivity).showFragment(1)
                }
            }
            iv_next_guide_wifi -> {
                if (NetworkUtils.isConnected()) {
                    activity?.let {
                        (it as GuideActivity).showFragment(1)
                    }
                } else {
                    context?.let {
                        MaterialDialog.Builder(it)
                            .setTitle(R.string.please_connect_to_the_network)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.ok, null)
                            .show()
                    }
                    /*activity?.let {
                        MaterialDialog.Builder(it)
                            .setTitle(R.string.skip_wizard_title)
                            .setContent(R.string.skip_wizard_content)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.ok, object :MaterialDialog.OnClickListener{
                                override fun onClick(dialog: MaterialDialog) {
                                    Device.isFirstIn = false
                                    (it as GuideActivity).removeFragment()
                                }
                            })
                            .show()
                    }*/
                }
            }
        }
    }
}