package com.idwell.cloudframe.base

import android.app.Dialog
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.view.Surface
import android.view.View
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.LogUtils
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.common.MessageEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Logger
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


abstract class BaseFragment : Fragment(), View.OnClickListener {

    var isMore = false
    var mDialog: Dialog? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initData(savedInstanceState)
        initListener()
        context?.let {
            mDialog = Dialog(it, R.style.LoadingDialog).apply {
                setContentView(View.inflate(it, R.layout.view_process, null))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //绑定事件接受
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        //注销事件接受
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onMessageEvent(event: MessageEvent) {

    }

    abstract fun initData(savedInstanceState: Bundle?)

    abstract fun initListener()

    override fun onClick(v: View?) {

    }
}