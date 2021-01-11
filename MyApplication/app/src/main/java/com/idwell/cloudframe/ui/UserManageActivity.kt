package com.idwell.cloudframe.ui

import android.content.Intent
import android.view.View
import android.widget.EditText

import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.http.RetrofitManager
import com.idwell.cloudframe.http.service.StatusService
import com.idwell.cloudframe.http.BaseHttpObserver
import com.idwell.cloudframe.http.entity.DeviceStatus
import com.idwell.cloudframe.http.entity.User
import com.idwell.cloudframe.widget.MaterialDialog

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_user_manage.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UserManageActivity : BaseActivity() {

    private lateinit var mUser: User

    override fun initLayout(): Int {
        showTopBar = false
        return R.layout.activity_user_manage
    }

    override fun initData() {
        //tv_title_base.setText(R.string.user_management)
        mUser = intent.getParcelableExtra("user")
        Glide.with(this).load(mUser.avatar).placeholder(R.drawable.ic_launcher_round)
                .error(R.drawable.ic_launcher_round).circleCrop().into(iv_avatar_user_manage)
        refreshUI()
    }

    override fun initListener() {
        iv_back_user_manage.setOnClickListener(this)
        tv_cancel_user_manage.setOnClickListener(this)
        tv_view_photo_user_manage.setOnClickListener(this)
        tv_rename_user_manage.setOnClickListener(this)
        tv_block_user_manage.setOnClickListener(this)
        tv_delete_user_manage.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v) {
            iv_back_user_manage -> finish()
            tv_cancel_user_manage -> {
                tv_cancel_user_manage.visibility = View.GONE
                refreshUI()
            }
            tv_view_photo_user_manage -> startActivity(Intent(this, PhotoActivity::class.java).putExtra("user", mUser))
            tv_rename_user_manage -> MaterialDialog.Builder(this).setEditContent(tv_name_user_manage.text.toString()).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                override fun onClick(dialog: MaterialDialog) {
                    val editText = dialog.findViewById<EditText>(R.id.et_content_dialog_material)
                    val name = editText.text.toString().trim()
                    if (name != tv_name_user_manage.text.toString()) {
                        status("rename", name)
                    }
                }
            }).show()
            tv_block_user_manage -> if (tv_cancel_user_manage.visibility == View.VISIBLE) {
                if (mUser.isReceive == "1") {
                    status("lock", null)
                } else {
                    status("unlock", null)
                }
            } else {
                tv_cancel_user_manage.visibility = View.VISIBLE
                refreshUI()
                tv_block_user_manage.visibility = View.VISIBLE
                if (mUser.isReceive == "1") {
                    tv_desc_user_manage.setText(R.string.after_blocking_files_that_the_user_uploads_can_not_be_received)
                } else {
                    tv_desc_user_manage.setText(R.string.after_unblocking_you_can_receive_files_uploaded_by_the_user)
                }
            }
            tv_delete_user_manage -> if (tv_cancel_user_manage.visibility == View.VISIBLE) {
                status("delete", null)
            } else {
                tv_cancel_user_manage.visibility = View.VISIBLE
                refreshUI()
                tv_delete_user_manage.visibility = View.VISIBLE
                tv_desc_user_manage.setText(R.string.after_deleting_the_user_can_not_upload_files_to_the_device)
            }
        }
    }

    override fun initMessageEvent(event: MessageEvent) {

    }

    private fun refreshUI() {
        when {
            mUser.remarkname.isNotEmpty() -> tv_name_user_manage.text = mUser.remarkname
            mUser.name.isNotEmpty() -> tv_name_user_manage.text = mUser.name
            else -> tv_name_user_manage.text = mUser.account.toString()
        }
        if (mUser.isReceive == "1") {
            tv_blocked_user_manage.visibility = View.INVISIBLE
            tv_block_user_manage.setText(R.string.block)
        } else {
            tv_blocked_user_manage.visibility = View.VISIBLE
            tv_block_user_manage.setText(R.string.unblock)
        }
        if (tv_cancel_user_manage.visibility == View.VISIBLE) {
            tv_desc_user_manage.visibility = View.VISIBLE
            tv_view_photo_user_manage.visibility = View.GONE
            tv_rename_user_manage.visibility = View.GONE
            tv_block_user_manage.visibility = View.GONE
            tv_delete_user_manage.visibility = View.GONE
        } else {
            tv_desc_user_manage.visibility = View.INVISIBLE
            tv_view_photo_user_manage.visibility = View.VISIBLE
            tv_rename_user_manage.visibility = View.VISIBLE
            tv_block_user_manage.visibility = View.VISIBLE
            tv_delete_user_manage.visibility = View.VISIBLE
        }
    }

    private fun status(status: String, rename_name: String?) {
        RetrofitManager.getService(StatusService::class.java)
                .status(mUser.id, Device.id, status, "").subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : BaseHttpObserver<DeviceStatus>(this) {
                    override fun onSuccess(t: DeviceStatus) {
                        when (status) {
                            "delete" -> {
                                GlobalScope.launch {
                                    MyDatabase.instance.userDao.delete(mUser)
                                    launch(Dispatchers.Main) {
                                        ToastUtils.showShort(R.string.delete_success)
                                        finish()
                                    }
                                }
                            }
                            "lock" -> {
                                GlobalScope.launch {
                                    mUser.isReceive = "2"
                                    MyDatabase.instance.userDao.update(mUser)
                                    launch(Dispatchers.Main) {
                                        tv_cancel_user_manage.visibility = View.GONE
                                        refreshUI()
                                    }
                                }
                            }
                            "unlock" -> {
                                GlobalScope.launch {
                                    mUser.isReceive = "1"
                                    MyDatabase.instance.userDao.update(mUser)
                                    launch(Dispatchers.Main) {
                                        tv_cancel_user_manage.visibility = View.GONE
                                        refreshUI()
                                    }
                                }
                            }
                            "rename" -> {
                                tv_name_user_manage.text = rename_name
                                GlobalScope.launch {
                                    rename_name?.let { mUser.remarkname = rename_name }
                                    MyDatabase.instance.userDao.update(mUser)
                                }
                            }
                        }
                    }
                })
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.bottom_slide_out)
    }
}
