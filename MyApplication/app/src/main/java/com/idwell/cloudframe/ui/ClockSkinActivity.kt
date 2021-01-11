package com.idwell.cloudframe.ui

import android.content.Intent
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.MessageEvent
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.activity_clock_skin.*

class ClockSkinActivity : BaseActivity(), BaseQuickAdapter.OnItemClickListener {

    private lateinit var mAdapter: BaseQuickAdapter<Int, BaseViewHolder>
    private val selects = intArrayOf(R.drawable.transparent, R.drawable.transparent, R.drawable.clock0_enabled, R.drawable.clock1_enabled, R.drawable.clock2_enabled, R.drawable.clock3_enabled, R.drawable.clock4_enabled, R.drawable.clock5_enabled, R.drawable.clock6_enabled, R.drawable.clock7_enabled, R.drawable.clock8_enabled, R.drawable.transparent, R.drawable.transparent)

    override fun initLayout(): Int {
        return R.layout.activity_clock_skin
    }

    override fun initData() {
        tv_title_base.setText(R.string.choose_clock_skin)
        val data = mutableListOf(R.drawable.transparent, R.drawable.transparent, R.drawable.clock0_normal, R.drawable.clock1_normal, R.drawable.clock2_normal,
                R.drawable.clock3_normal, R.drawable.clock4_normal, R.drawable.clock5_normal, R.drawable.clock6_normal,
                R.drawable.clock7_normal, R.drawable.clock8_normal, R.drawable.transparent, R.drawable.transparent)
        //设置布局管理器
        val layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            this,
            androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
            false
        )
        rv_clock_skin.layoutManager = layoutManager
        //设置适配器
        mAdapter = object : BaseQuickAdapter<Int, BaseViewHolder>(R.layout.item_clock_skin, data) {
            override fun convert(helper: BaseViewHolder, item: Int?) {
                if (item != null) {
                    val imageView = helper.getView<ImageView>(R.id.iv_item_clock_skin)
                    val options = RequestOptions().skipMemoryCache(true).override(300, 300)
                    if (helper.layoutPosition == Device.clockMode + 2) {
                        Glide.with(mContext).load(selects[Device.clockMode + 2]).apply(options).into(imageView)
                    } else {
                        Glide.with(mContext).load(item).apply(options).into(imageView)
                    }
                }
            }
        }
        rv_clock_skin.adapter = mAdapter
        rv_clock_skin.scrollToPosition(Device.clockMode)
    }

    override fun initListener() {
        mAdapter.onItemClickListener = this
    }

    override fun onClick(v: View?) {
        when(v){
            iv_back_base -> {
                startActivity(Intent(this, ClockActivity::class.java))
                finish()
            }
        }
    }

    override fun initMessageEvent(event: MessageEvent) {

    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, ClockActivity::class.java))
    }

    override fun onItemClick(baseQuickAdapter: BaseQuickAdapter<*, *>, view: View, i: Int) {
        if (i == 0 || i == 1 || i == 11 || i == 12) return
        Device.clockMode = i - 2
        startActivity(Intent(this, ClockActivity::class.java))
        finish()
    }
}