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

class SystemActivity : BaseActivity(), BaseQuickAdapter.OnItemClickListener, BaseQuickAdapter.OnItemChildClickListener {

    private var mPaused = false
    private lateinit var mAdapter: BaseQuickAdapter<BaseItem, BaseViewHolder>
    private val mData = mutableListOf<BaseItem>()
    private lateinit var mFragmentManager: androidx.fragment.app.FragmentManager
    private var mSoundFragment: SoundFragment? = null
    private var mDisplayFragment: DisplayFragment? = null
    private var mDateTimeFragment: DateTimeFragment? = null
    private var mLanguageFragment: LanguageFragment? = null
    private var mAutoOnOffFragment: AutoOnOffFragment? = null
    private var mFactoryDataResetFragment: FactoryDataResetFragment? = null

    private var mPosition: Int = 0

    private val mHandler = Handler()
    private val mAutoplayRunnable = object : Runnable {
        override fun run() {
            if (mPaused) {
                return
            }
            when {
                queryImagesCount("${MediaStore.Images.ImageColumns.DATA} like '${Environment.getExternalStorageDirectory().absolutePath}/%'") > 0 -> {
                    val intent = Intent(this@SystemActivity, SlideActivity::class.java)
                    intent.putExtra("tab position", 0)
                    startActivity(intent)
                }
                queryImagesCount("${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getSdDir()}/%'") > 0 -> {
                    val intent = Intent(this@SystemActivity, SlideActivity::class.java)
                    intent.putExtra("tab position", 1)
                    startActivity(intent)
                }
                queryImagesCount("${MediaStore.Images.ImageColumns.DATA} like '${MyUtils.getUsbDir()}/%'") > 0 -> {
                    val intent = Intent(this@SystemActivity, SlideActivity::class.java)
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
        //tv_title_base.setText(R.string.system)
        iv_title_base.setImageResource(R.drawable.ic_settings_main)

        mData.add(BaseItem(R.drawable.sound, R.string.sound))
        mData.add(BaseItem(R.drawable.display, R.string.display))
        mData.add(BaseItem(R.drawable.date_time, R.string.date_and_time))
        mData.add(BaseItem(R.drawable.language, R.string.language))
        mData.add(BaseItem(R.drawable.auto_on_off, R.string.auto_on_off))
        mData.add(BaseItem(R.drawable.factory_data_reset, R.string.master_clear_title))
        mAdapter = object : BaseQuickAdapter<BaseItem, BaseViewHolder>(R.layout.item_content_sidebar, mData) {
            override fun convert(holder: BaseViewHolder, entity: BaseItem?) {
                if (entity != null) {
                    holder.setImageResource(R.id.iv_icon_item_content_sidebar, entity.iconResId)
                    holder.setText(R.id.tv_title_item_content_sidebar, entity.titleResId)
                    if (holder.adapterPosition == mPosition) {
                        holder.setBackgroundRes(R.id.cl_item_content_sidebar, R.drawable.bg_item_content_sidebar_selected)
                    } else {
                        holder.setBackgroundRes(R.id.cl_item_content_sidebar, R.drawable.bg_item_content_sidebar)
                    }
                }
            }
        }
        rv_settings.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rv_settings.addItemDecoration(HorizontalItemDecoration(ContextCompat.getColor(this, R.color.divider)))
        rv_settings.adapter = mAdapter

        mFragmentManager = supportFragmentManager
        showFragment()
    }

    override fun initListener() {
        mAdapter.onItemClickListener = this
        mAdapter.onItemChildClickListener = this
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (Device.userActivityListenerMode == 0) {
            when (ev?.action) {
                MotionEvent.ACTION_DOWN -> mHandler.removeCallbacks(mAutoplayRunnable)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                    if (Device.slideshow > 0) {
                        mHandler.postDelayed(mAutoplayRunnable, Device.slideshow)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun initMessageEvent(event: MessageEvent) {

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        LogUtils.dTag(LOG_TAG, keyCode)
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (--mPosition < 0) {
                    mPosition = mData.size - 1
                }
                showFragment()
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (++mPosition > mData.size - 1) {
                    mPosition = 0
                }
                showFragment()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onItemClick(baseQuickAdapter: BaseQuickAdapter<*, *>, view: View, i: Int) {
        mPosition = i
        showFragment()
    }

    override fun onItemChildClick(baseQuickAdapter: BaseQuickAdapter<*, *>, view: View, i: Int) {
        finish()
    }

    private fun showFragment() {
        mAdapter.notifyDataSetChanged()
        val transaction = mFragmentManager.beginTransaction()
        hideFragments(transaction)
        when (mPosition) {
            0 -> if (mSoundFragment == null) {
                mSoundFragment = SoundFragment()
                mSoundFragment?.let { transaction.add(R.id.fl_settings, it) }
            } else {
                mSoundFragment?.let(transaction::show)
            }
            1 -> if (mDisplayFragment == null) {
                mDisplayFragment = DisplayFragment()
                mDisplayFragment?.let { transaction.add(R.id.fl_settings, it) }
            } else {
                mDisplayFragment?.let(transaction::show)
            }
            2 -> if (mDateTimeFragment == null) {
                mDateTimeFragment = DateTimeFragment()
                mDateTimeFragment?.let { transaction.add(R.id.fl_settings, it) }
            } else {
                mDateTimeFragment?.let(transaction::show)
            }
            3 -> if (mLanguageFragment == null) {
                mLanguageFragment = LanguageFragment()
                mLanguageFragment?.let { transaction.add(R.id.fl_settings, it) }
            } else {
                mLanguageFragment?.let(transaction::show)
            }
            4 -> if (mAutoOnOffFragment == null) {
                mAutoOnOffFragment = AutoOnOffFragment()
                mAutoOnOffFragment?.let { transaction.add(R.id.fl_settings, it) }
            } else {
                mAutoOnOffFragment?.let(transaction::show)
            }
            5 -> if (mFactoryDataResetFragment == null) {
                mFactoryDataResetFragment = FactoryDataResetFragment()
                mFactoryDataResetFragment?.let { transaction.add(R.id.fl_settings, it) }
            } else {
                mFactoryDataResetFragment?.let(transaction::show)
            }
        }
        transaction.commit()
    }

    private fun hideFragments(transaction: androidx.fragment.app.FragmentTransaction) {
        mSoundFragment?.let(transaction::hide)
        mDisplayFragment?.let(transaction::hide)
        mDateTimeFragment?.let(transaction::hide)
        mLanguageFragment?.let(transaction::hide)
        mAutoOnOffFragment?.let(transaction::hide)
        mFactoryDataResetFragment?.let(transaction::hide)
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
        private val LOG_TAG = SystemActivity::class.java.simpleName
    }
}