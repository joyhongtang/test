package com.idwell.cloudframe.http

import android.app.Dialog
import android.content.Context
import android.view.View

import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.Utils
import com.idwell.cloudframe.R
import com.idwell.cloudframe.http.entity.Base
import com.idwell.cloudframe.util.MyLogUtils

import io.reactivex.Observer
import io.reactivex.disposables.Disposable

abstract class BaseHttpObserver<T> : Observer<Base<T>> {

    private var mDialog: Dialog? = null

    protected constructor()

    protected constructor(context: Context?) {
        context?.apply {
            val view = View.inflate(context, R.layout.view_process, null)
            mDialog = Dialog(context, R.style.LoadingDialog)
            mDialog?.setContentView(view)
        }
    }

    override fun onSubscribe(d: Disposable) {
        if (!NetworkUtils.isConnected()) {
            d.dispose()
            onNetworkError()
            ToastUtils.showShort(R.string.unconnected_network)
        } else {
            mDialog?.show()
            mDialog?.setOnCancelListener {
                d.dispose()
            }
        }
    }

    override fun onNext(base: Base<T>) {
        mDialog?.dismiss()
        val status = base.status
        if (status == 200) {
            base.data?.let { onSuccess(it) }
        } else {
            onFail(status)
        }
    }

    override fun onError(e: Throwable) {
        mDialog?.dismiss()
        //ToastUtils.showLong(e.message)
        //LogUtils.dTag("lcs", e.message)
        MyLogUtils.file(e.message);
    }

    override fun onComplete() {

    }

    abstract fun onSuccess(data: T)

    open fun onNetworkError() {}

    open fun onFail(status: Int) {
        when (status) {
            101 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e101)}")
            102 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e102)}")
            103 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e103)}")
            104 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e104)}")
            105 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e105)}")
            106 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e106)}")
            107 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e107)}")
            108 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e108)}")
            109 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e109)}")
            110 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e110)}")
            111 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e111)}")
            112 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e112)}")
            113 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e113)}")
            114 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e114)}")
            115 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e115)}")
            116 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e116)}")
            117 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e117)}")
            118 -> ToastUtils.showShort("${Utils.getApp().getString(R.string.e118)}")
            119 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e119)}")
            120 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e120)}")
            121 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e121)}")
            122 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e122)}")
            123 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e123)}")
            124 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e124)}")
            125 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e125)}")
            126 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e126)}")
            127 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e127)}")
            128 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e128)}")
            129 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e129)}")
            130 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e130)}")
            131 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e131)}")
            132 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e132)}")
            133 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e133)}")
            134 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e134)}")
            135 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e135)}")
            313 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e313)}")
            318 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e318)}")
            327 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e327)}")
            404 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e404)}")
            405 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e405)}")
            406 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e406)}")
            407 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e407)}")
            408 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e408)}")
            409 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e409)}")
            410 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e410)}")
            411 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e411)}")
            412 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e412)}")
            413 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e413)}")
            414 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e414)}")
            415 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e415)}")
            416 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e416)}")
            417 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e417)}")
            418 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e418)}")
            901 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e901)}")
            902 -> ToastUtils.showShort("$status, ${Utils.getApp().getString(R.string.e902)}")
            else -> ToastUtils.showShort(R.string.error_code, status)
        }
    }
}