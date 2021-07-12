package com.idwell.cloudframe.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager

import com.blankj.utilcode.util.LogUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.entity.BaseItem
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.util.MyUtils
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : BaseActivity(), BaseQuickAdapter.OnItemClickListener, BaseQuickAdapter.OnItemChildClickListener {

    private var mPaused = false
    private lateinit var mAdapter: BaseQuickAdapter<BaseItem, BaseViewHolder>
    private val mData = mutableListOf<BaseItem>()
    private lateinit var mFragmentManager: FragmentManager
    private var mWifiFragment: WifiFragment? = null
    private var mDeviceInfoFragment: DeviceInfoFragment? = null
    private var mUserManagementFragment: UserManagementFragment? = null
    private var mAlbumSettingsFragment: AlbumSettingsFragment? = null
    private var mAboutFragment: AboutFragment? = null

    private var mPosition = 0

    private val mHandler = Handler()
    private val mAutoplayRunnable = object : Runnable {
        override fun run() {
            if (mPaused) {
                return
            }
            when {
                queryImagesCount("${MediaStore.Images.ImageColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'") > 0 -> {
                    val intent = Intent(this@SettingsActivity, SlideActivity::class.java)
                    intent.putExtra("tab position", 0)
                    startActivity(intent)
                }
                queryImagesCount("${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getSdDir()}/%'") > 0 -> {
                    val intent = Intent(this@SettingsActivity, SlideActivity::class.java)
                    intent.putExtra("tab position", 1)
                    startActivity(intent)
                }
                queryImagesCount("${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getUsbDir()}/%'") > 0 -> {
                    val intent = Intent(this@SettingsActivity, SlideActivity::class.java)
                    intent.putExtra("tab position", 2)
                    startActivity(intent)
                }
            }
        }
    }

    override fun initLayout(): Int {
        return R.layout.activity_settings
    }

    override fun initData() {
        //tv_title_base.setText(R.string.settings)
        iv_title_base.setImageResource(R.drawable.ic_settings_main)
        mPosition = intent.getIntExtra("position", 0)

        mData.add(BaseItem(R.drawable.device_info, R.string.device_info))
        mData.add(BaseItem(R.drawable.ic_user_management, R.string.user_management))
        mData.add(BaseItem(R.drawable.pc_control, R.string.pc_control))
        mData.add(BaseItem(R.drawable.wifi_settings, R.string.wifi_settings))
        mData.add(BaseItem(R.drawable.album_settings, R.string.album_settings))
        mData.add(BaseItem(R.drawable.system_settings, R.string.system_settings))
        mData.add(BaseItem(R.drawable.about, R.string.about))
        mAdapter = object : BaseQuickAdapter<BaseItem, BaseViewHolder>(R.layout.item_content_sidebar, mData) {
            override fun convert(helper: BaseViewHolder, item: BaseItem?) {
                if (item != null) {
                    helper.setImageResource(R.id.iv_icon_item_content_sidebar, item.iconResId)
                    helper.setText(R.id.tv_title_item_content_sidebar, item.titleResId)
                    if (helper.adapterPosition == mPosition) {
                        helper.setBackgroundRes(R.id.cl_item_content_sidebar, R.drawable.bg_item_content_sidebar_selected)
                    } else {
                        helper.setBackgroundRes(R.id.cl_item_content_sidebar, R.drawable.bg_item_content_sidebar)
                    }
                }
            }
        }
        rv_settings.layoutManager = LinearLayoutManager(this)
        rv_settings.addItemDecoration(HorizontalItemDecoration(ContextCompat.getColor(this, R.color.divider)))
        rv_settings.adapter = mAdapter

        mFragmentManager = supportFragmentManager
        showFragment()
    }

    override fun initListener() {
        mAdapter.onItemClickListener = this
        mAdapter.onItemChildClickListener = this
    }

    override fun initMessageEvent(event: MessageEvent) {
        when (event.message) {
            MessageEvent.SLIDE_AUTOPLAY_TIME_CHANGED -> {
                mHandler.removeCallbacks(mAutoplayRunnable)
                if (Device.slideshow > 0) {
                    mHandler.postDelayed(mAutoplayRunnable, Device.slideshow)
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (Device.userActivityListenerMode == 0) {
            when (ev?.action) {
                MotionEvent.ACTION_DOWN -> {
                    mHandler.removeCallbacks(mAutoplayRunnable)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                    if (Device.slideshow > 0) {
                        mHandler.postDelayed(mAutoplayRunnable, Device.slideshow)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        LogUtils.dTag(LOG_TAG, keyCode)
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (--mPosition < 0) {
                    mPosition = mData.size - 1
                }
                if (arrayOf(0, 1, 3, 4, 6).contains(mPosition)) {
                    showFragment()
                } else {
                    mAdapter.notifyDataSetChanged()
                    val transaction = mFragmentManager.beginTransaction()
                    hideFragments(transaction)
                    transaction.commit()
                }
                return true
            }
            KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_DPAD_CENTER  -> {
                if (mPosition == 2) {
                    startActivity(Intent(this, PCControlActivity::class.java))
                } else if (mPosition == 5) {
                    startActivity(Intent(this, SystemActivity::class.java))
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (++mPosition > mData.size - 1) {
                    mPosition = 0
                }
                if (arrayOf(0, 1, 3, 4, 6).contains(mPosition)) {
                    showFragment()
                } else {
                    mAdapter.notifyDataSetChanged()
                    val transaction = mFragmentManager.beginTransaction()
                    hideFragments(transaction)
                    transaction.commit()
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        mPosition = position
        showFragment()
    }

    override fun onItemChildClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        finish()
    }

    private fun showFragment() {
        val transaction: FragmentTransaction
        when (mPosition) {
            0 -> {
                mAdapter.notifyDataSetChanged()
                transaction = mFragmentManager.beginTransaction()
                hideFragments(transaction)
                if (mDeviceInfoFragment == null) {
                    mDeviceInfoFragment = DeviceInfoFragment()
                    mDeviceInfoFragment?.let { transaction.add(R.id.fl_settings, it) }
                } else {
                    mDeviceInfoFragment?.let(transaction::show)
                }
                transaction.commit()
            }
            1 -> {
                mAdapter.notifyDataSetChanged()
                transaction = mFragmentManager.beginTransaction()
                hideFragments(transaction)
                if (mUserManagementFragment == null) {
                    mUserManagementFragment = UserManagementFragment()
                    mUserManagementFragment?.let { transaction.add(R.id.fl_settings, it) }
                } else {
                    mUserManagementFragment?.let(transaction::show)
                }
                transaction.commit()
            }
            2 -> {
                startActivity(Intent(this, PCControlActivity::class.java))
            }
            3 -> {
                mAdapter.notifyDataSetChanged()
                transaction = mFragmentManager.beginTransaction()
                hideFragments(transaction)
                if (mWifiFragment == null) {
                    mWifiFragment = WifiFragment()
                    mWifiFragment?.let { transaction.add(R.id.fl_settings, it) }
                } else {
                    mWifiFragment?.let(transaction::show)
                }
                transaction.commit()
            }
            4 -> {
                mAdapter.notifyDataSetChanged()
                transaction = mFragmentManager.beginTransaction()
                hideFragments(transaction)
                if (mAlbumSettingsFragment == null) {
                    mAlbumSettingsFragment = AlbumSettingsFragment()
                    mAlbumSettingsFragment?.let { transaction.add(R.id.fl_settings, it) }
                } else {
                    mAlbumSettingsFragment?.let(transaction::show)
                }
                transaction.commit()
            }
            5 -> {
                startActivity(Intent(this, SystemActivity::class.java))
            }
            6 -> {
                mAdapter.notifyDataSetChanged()
                transaction = mFragmentManager.beginTransaction()
                hideFragments(transaction)
                if (mAboutFragment == null) {
                    mAboutFragment = AboutFragment()
                    mAboutFragment?.let { transaction.add(R.id.fl_settings, it) }
                } else {
                    mAboutFragment?.let(transaction::show)
                }
                transaction.commit()
            }
        }
    }

    private fun hideFragments(transaction: FragmentTransaction) {
        mDeviceInfoFragment?.let(transaction::hide)
        mUserManagementFragment?.let(transaction::hide)
        mWifiFragment?.let(transaction::hide)
        mAlbumSettingsFragment?.let(transaction::hide)
        mAboutFragment?.let(transaction::hide)
    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {

    }

    private fun queryImagesCount(selection: String): Int {
        val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf("count(*)"), selection, null, null)
        cursor?.moveToFirst()
        val count = cursor?.getInt(0) ?: 0
        cursor?.close()
        return count
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            mPosition = intent.getIntExtra("position", 0)
            showFragment()
        }
    }

    override fun onResume() {
        super.onResume()
        mPaused = false
        if (Device.slideshow > 0) {
            mHandler.postDelayed(mAutoplayRunnable, Device.slideshow)
        }
    }

    override fun onPause() {
        super.onPause()
        mPaused = true
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.dTag(LOG_TAG, "onDestroy")
    }

    companion object {
        private val LOG_TAG = SettingsActivity::class.java.simpleName
    }
}