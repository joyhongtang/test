package com.idwell.cloudframe.ui

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseFragment
import com.idwell.cloudframe.common.*
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.http.BaseHttpObserver
import com.idwell.cloudframe.http.RetrofitManager
import com.idwell.cloudframe.http.entity.*
import com.idwell.cloudframe.http.service.AcceptBindService
import com.idwell.cloudframe.http.service.DeviceUserService
import com.idwell.cloudframe.http.service.StatusService
import com.idwell.cloudframe.widget.MaterialDialog
import com.idwell.cloudframe.http.service.UpdateDeviceAcceptUserService
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_user_management.*

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.net.URLDecoder

class UserManagementFragment : BaseFragment(), BaseQuickAdapter.OnItemClickListener, BaseQuickAdapter.OnItemChildClickListener {

    private lateinit var mUserAdapter: BaseQuickAdapter<User, BaseViewHolder>
    private var mUsers = mutableListOf<User>()
    private lateinit var mNewUserAdapter: BaseQuickAdapter<User, BaseViewHolder>
    private var mNewUsers = mutableListOf<User>()
    private var mMaterialDialog: MaterialDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_user_management, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        switch_accept_new_users_user_management.isChecked = Device.acceptNewUsers == "1"
        //设置布局管理器
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rv_users_user_management.layoutManager = linearLayoutManager
        //设置适配器
        mUserAdapter = object : BaseQuickAdapter<User, BaseViewHolder>(R.layout.item_fragment_device_info, mUsers) {
            override fun convert(helper: BaseViewHolder, item: User?) {
                if (item != null) {
                    helper.setText(R.id.tv_name_item_fragment_device_info, item.displayName)
                            .setVisible(R.id.tv_blocked_item_fragment_device_info, item.isReceive != "1")
                    Glide.with(mContext).load(item.avatar).placeholder(R.drawable.ic_avatar)
                            .error(R.drawable.ic_avatar).circleCrop()
                            .into(helper.getView(R.id.iv_avatar_item_fragment_device_info))
                }
            }
        }
        rv_users_user_management.adapter = mUserAdapter

        mNewUserAdapter = object : BaseQuickAdapter<User, BaseViewHolder>(R.layout.item_new_user, mNewUsers) {
            override fun convert(helper: BaseViewHolder, item: User?) {
                if (item != null) {
                    helper.setText(R.id.tv_name_item_new_user, item.displayName)
                            .addOnClickListener(R.id.tv_accept_item_new_user)
                            .addOnClickListener(R.id.tv_refuse_item_new_user)
                    Glide.with(mContext).load(item.avatar).placeholder(R.drawable.ic_avatar)
                            .error(R.drawable.ic_avatar).circleCrop()
                            .into(helper.getView(R.id.iv_avatar_item_new_user))
                }
            }
        }
        rv_new_users_user_management.layoutManager = LinearLayoutManager(context)
        context?.let {
            rv_new_users_user_management.addItemDecoration(HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider)))
        }
        rv_new_users_user_management.adapter = mNewUserAdapter

        MyDatabase.instance.userDao.queryAll().observe({ this.lifecycle }, { users ->
            mUsers.clear()
            mNewUsers.clear()
            for (user in users) {
                if (user.isAccepted == "1") {
                    mUsers.add(user)
                } else {
                    mNewUsers.add(user)
                }
            }
            mUserAdapter.notifyDataSetChanged()
            mNewUserAdapter.notifyDataSetChanged()
        })
    }

    override fun initListener() {
        mUserAdapter.onItemClickListener = this
        mNewUserAdapter.onItemChildClickListener = this

        switch_accept_new_users_user_management.setOnClickListener {
            updateDeviceAcceptUser(if (Device.acceptNewUsers == "1") "2" else "1")
        }
    }

    override fun onMessageEvent(event: MessageEvent) {
        when (event.message) {
            MessageEvent.DEVICE_UPDATE_ACCEPT_NEW_USERS -> {
                switch_accept_new_users_user_management.isChecked = Device.acceptNewUsers == "1"
            }
        }
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View, position: Int) {
        val user = mUsers[position]
        val username = if (user.remarkname.isNotEmpty()) user.remarkname else if (user.name.isNotEmpty()) user.name else user.account.toString()
        val data = mutableListOf<String>()
        if (user.isAdmin == "mydevice") {
            data.add(getString(R.string.view_photos))
            data.add(getString(R.string.remark_name))
            data.add(getString(R.string.delete_photos))
            context?.let {
                mMaterialDialog = MaterialDialog.Builder(it).setTitle(username)
                        .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textc_dialog, data) {
                            override fun convert(helper: BaseViewHolder, item: String?) {
                                if (item != null) {
                                    helper.setText(R.id.tv_item_textc_dialog, item)
                                }
                            }
                        }, BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
                            when (position) {
                                0 -> {
                                    GlobalScope.launch {
                                        val count = countImages(user)
                                        if (count > 0) {
                                            val intent = Intent(it, SlideActivity::class.java)
                                            intent.putExtra("tab_position", position + 5)
                                            intent.putExtra("user_id", user.id)
                                            startActivity(intent)
                                        } else {
                                            ToastUtils.showShort(R.string.no_photos_found)
                                        }
                                    }
                                }
                                1 -> {
                                    MaterialDialog.Builder(it).setTitle(R.string.remark_name)
                                            .setEditContent(username)
                                            .setNegativeButton(R.string.cancel, null)
                                            .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                                                override fun onClick(dialog: MaterialDialog) {
                                                    val remarkname = dialog.findViewById<EditText>(R.id.et_content_dialog_material)
                                                            .text.toString().trim()
                                                    if (remarkname != username) {
                                                        status(user, "rename", remarkname)
                                                    }
                                                }
                                            }).show()
                                }
                                2 -> {
                                    GlobalScope.launch {
                                        val result = FileUtils.deleteFilesInDirWithFilter(File(Environment.getExternalStorageDirectory(), "CloudAlbum/cloud/${user.account}"), ImageFilter())
                                        if (result) {
                                            //delete(user)
                                            ToastUtils.showShort(R.string.delete_success)
                                        } else {
                                            ToastUtils.showShort(R.string.delete_failure)
                                        }
                                    }
                                }
                            }
                            mMaterialDialog?.dismiss()
                        }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content), false))
                        .show()
            }
        } else {
            data.add(getString(R.string.view_photos))
            data.add(getString(R.string.remark_name))
            data.add(getString(R.string.delete_user))
            data.add(getString(R.string.delete_user_photos))
            context?.let {
                mMaterialDialog = MaterialDialog.Builder(it).setTitle(username)
                        .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textc_dialog, data) {
                            override fun convert(helper: BaseViewHolder, item: String?) {
                                if (item != null) {
                                    helper.setText(R.id.tv_item_textc_dialog, item)
                                }
                            }
                        }, BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
                            when (position) {
                                0 -> {
                                    GlobalScope.launch {
                                        val count = countImages(user)
                                        if (count > 0) {
                                            val intent = Intent(it, SlideActivity::class.java)
                                            intent.putExtra("tab_position", position + 5)
                                            intent.putExtra("user_id", user.id)
                                            startActivity(intent)
                                        } else {
                                            ToastUtils.showShort(R.string.no_photos_found)
                                        }
                                    }
                                }
                                1 -> {
                                    MaterialDialog.Builder(it).setTitle(R.string.remark_name)
                                            .setEditContent(username)
                                            .setNegativeButton(R.string.cancel, null)
                                            .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                                                override fun onClick(dialog: MaterialDialog) {
                                                    val remarkname = dialog.findViewById<EditText>(R.id.et_content_dialog_material)
                                                            .text.toString().trim()
                                                    if (remarkname != username) {
                                                        status(user, "rename", remarkname)
                                                    }
                                                }
                                            }).show()
                                }
                                2 -> {
                                    status(user, "delete", false)
                                }
                                3 -> {
                                    status(user, "delete", true)
                                }
                            }
                            mMaterialDialog?.dismiss()
                        }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content), false))
                        .show()
            }
        }
    }

    override fun onItemChildClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        when (view?.id) {
            R.id.tv_accept_item_new_user -> {
                acceptBind(mNewUsers[position].id, "yes")
            }
            R.id.tv_refuse_item_new_user -> {
                acceptBind(mNewUsers[position].id, "no")
            }
        }
    }

    private fun status(user: User, status: String, delete: Boolean) {
        RetrofitManager.getService(StatusService::class.java).status(user.id, Device.id, status)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : BaseHttpObserver<DeviceStatus>(context) {
                    override fun onSuccess(data: DeviceStatus) {
                        GlobalScope.launch {
                            MyDatabase.instance.userDao.delete(user)
                            if (delete) {
                                val result = FileUtils.deleteFilesInDirWithFilter(File(Environment.getExternalStorageDirectory(), "CloudAlbum/cloud/${user.account}"), ImageFilter())
                                if (result) {
                                    delete(user)
                                }
                            }
                            ToastUtils.showShort(R.string.delete_success)
                        }
                    }
                })
    }

    private fun status(user: User, status: String, rename_name: String) {
        RetrofitManager.getService(StatusService::class.java)
                .status(user.id, Device.id, status, rename_name).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : BaseHttpObserver<DeviceStatus>(context) {
                    override fun onSuccess(data: DeviceStatus) {
                        when (status) {
                            "rename" -> {
                                user.remarkname = rename_name
                                GlobalScope.launch {
                                    MyDatabase.instance.userDao.update(user)
                                }
                            }
                        }
                    }
                })
    }

    private fun countImages(user: User): Int {
        val selection = "${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"sender_id\":${user.id}%'"
        val cursor = context?.contentResolver?.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf("count(*)"), selection, null, null)
        cursor?.moveToFirst()
        val count = cursor?.getInt(0) ?: 0
        cursor?.close()
        return count
    }

    private fun delete(user: User) {
        context?.contentResolver?.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "${MediaStore.Images.ImageColumns.DESCRIPTION} like '%\"sender_id\":${user.id}%'", null)
    }

    private fun acceptBind(user_id: Int, acceptBind: String) {
        RetrofitManager.getService(AcceptBindService::class.java)
                .acceptBind(user_id, Device.id, acceptBind).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : BaseHttpObserver<AcceptBind>(context) {
                    override fun onSuccess(data: AcceptBind) {
                        when (acceptBind) {
                            "yes" -> {
                                when (data.type) {
                                    "mydevice" -> {
                                        Device.email = data.device_email
                                        Device.flow = data.deviceFlow.toFloat()
                                        EventBus.getDefault()
                                                .post(MessageEvent(MessageEvent.DEVICE_ACTIVATED))
                                        if (Device.companyName != "Aluratek" && !Device.isUnlimitedData) {
                                            EventBus.getDefault()
                                                    .post(MessageEvent(MessageEvent.DEVICE_UPDATE_DATA_FLOW))
                                        }
                                    }
                                }
                            }
                        }
                        deviceUser()
                    }

                    override fun onFail(status: Int) {
                        super.onFail(status)
                        if (status == 0) {
                            deviceUser()
                        }
                    }
                })
    }

    private fun deviceUser() {
        RetrofitManager.getService(DeviceUserService::class.java).deviceUser(Device.id)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : BaseHttpObserver<DeviceUser>() {
                    override fun onSuccess(data: DeviceUser) {
                        for (user in data.users) {
                            try {
                                user.name = URLDecoder.decode(user.name, "UTF-8")
                                user.remarkname = URLDecoder.decode(user.remarkname, "UTF-8")
                            } catch (e: Exception) {
                            }
                            user.displayName = if (user.remarkname.isNotEmpty()) user.remarkname else if (user.name.isNotEmpty()) user.name else user.account.toString()
                        }
                        GlobalScope.launch {
                            MyDatabase.instance.userDao.deleteAll()
                            MyDatabase.instance.userDao.insert(data.users)
                        }
                    }
                })
    }

    private fun updateDeviceAcceptUser(ifAccept: String) {
        RetrofitManager.getService(UpdateDeviceAcceptUserService::class.java)
                .updateDeviceAcceptUser(Device.id, ifAccept).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : BaseHttpObserver<Data>(context) {
                    override fun onSuccess(data: Data) {
                        Device.acceptNewUsers = ifAccept
                        ToastUtils.showShort(data.message)
                    }

                    override fun onFail(status: Int) {
                        super.onFail(status)
                        switch_accept_new_users_user_management.isChecked = Device.acceptNewUsers == "1"
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                        switch_accept_new_users_user_management.isChecked = Device.acceptNewUsers == "1"
                    }

                    override fun onNetworkError() {
                        super.onNetworkError()
                        switch_accept_new_users_user_management.isChecked = Device.acceptNewUsers == "1"
                    }
                })
    }
}