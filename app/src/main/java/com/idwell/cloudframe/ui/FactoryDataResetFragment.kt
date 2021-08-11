package com.idwell.cloudframe.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder

import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseFragment
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.http.BaseHttpObserver
import com.idwell.cloudframe.http.RetrofitManager
import com.idwell.cloudframe.http.entity.Data
import com.idwell.cloudframe.http.service.RestoreFactoryService
import com.idwell.cloudframe.widget.MaterialDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_factory_data_reset.*

class FactoryDataResetFragment : BaseFragment(), View.OnClickListener {

    private val mOptions = mutableListOf<Int>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_factory_data_reset, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        mOptions.add(R.string.delete_all_users_and_restore_the_device_to_inactive)
    }

    override fun initListener() {
        tv_reset_factory_data_reset.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            tv_reset_factory_data_reset -> {
                context?.let {
                    var isChecked = true
                    MaterialDialog.Builder(it).setTitle(R.string.master_clear_confirm_title)
                            .setAdapter(object : BaseQuickAdapter<Int, BaseViewHolder>(R.layout.item_master_clear, mOptions) {
                                override fun convert(helper: BaseViewHolder, item: Int?) {
                                    if (item != null) {
                                        helper.setText(R.id.tv_title_item_master_clear, item)
                                                .setImageResource(R.id.iv_item_master_clear, if (isChecked) R.drawable.ic_check_circle_blue else R.drawable.gray_ring_shape)
                                    }
                                }
                            }, BaseQuickAdapter.OnItemClickListener { adapter, _, _ ->
                                isChecked = !isChecked
                                adapter.notifyDataSetChanged()
                            }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content)))
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                                override fun onClick(dialog: MaterialDialog) {
                                    if (isChecked) {
                                        RetrofitManager.getService(RestoreFactoryService::class.java)
                                                .restoreFactory(Device.id, Device.pushToken)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(object : BaseHttpObserver<Data>(context) {
                                                    override fun onSuccess(data: Data) {
                                                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                                                            val intent = Intent("android.intent.action.FACTORY_RESET")
                                                            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                                                            intent.setPackage("android")
                                                            intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm")
                                                            //intent.putExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", true)
                                                            it.sendBroadcast(intent)
                                                        } else {
                                                            val intent = Intent("android.intent.action.MASTER_CLEAR")
                                                            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                                                            intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm")
                                                            //intent.putExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", true)
                                                            it.sendBroadcast(intent)
                                                        }
                                                    }
                                                })
                                    } else {
                                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                                            val intent = Intent("android.intent.action.FACTORY_RESET")
                                            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                                            intent.setPackage("android")
                                            intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm")
                                            //intent.putExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", true)
                                            it.sendBroadcast(intent)
                                        } else {
                                            val intent = Intent("android.intent.action.MASTER_CLEAR")
                                            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                                            intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm")
                                            //intent.putExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", true)
                                            it.sendBroadcast(intent)
                                        }
                                    }
                                }
                            }).show()
                }
            }
        }
    }
}
