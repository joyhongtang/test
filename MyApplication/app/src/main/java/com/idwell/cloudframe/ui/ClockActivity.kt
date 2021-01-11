package com.idwell.cloudframe.ui

import android.content.Intent
import android.os.Handler
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

import com.blankj.utilcode.util.LogUtils
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.MessageEvent

import kotlinx.android.synthetic.main.activity_clock.*

class ClockActivity : BaseActivity(), View.OnTouchListener {
    private val tag = ClockActivity::class.java.simpleName
    private val mHandler = Handler()
    private val mRunnable = Runnable {
        cl_top_bar_clock.visibility = View.GONE
    }

    private lateinit var detector: GestureDetector

    private val onGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            if (cl_top_bar_clock.visibility == View.VISIBLE) {
                mHandler.removeCallbacks(mRunnable)
                cl_top_bar_clock.visibility = View.GONE
            } else {
                cl_top_bar_clock.visibility = View.VISIBLE
                mHandler.postDelayed(mRunnable, 5000)
            }
            return super.onSingleTapConfirmed(e)
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            startActivity(Intent(this@ClockActivity, ClockSkinActivity::class.java))
            finish()
            return super.onDoubleTap(e)
        }
    }

    private val onPageChangeListener = object : androidx.viewpager.widget.ViewPager.OnPageChangeListener {
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            LogUtils.dTag(tag, "position = $position ,positionOffset = $positionOffset ,positionOffsetPixels = $positionOffsetPixels")
        }

        override fun onPageSelected(position: Int) {
            LogUtils.dTag(tag, position)
            Device.clockMode = position
        }

        override fun onPageScrollStateChanged(state: Int) {

        }
    }

    override fun initLayout(): Int {
        showTopBar = false
        return R.layout.activity_clock
    }

    override fun initData() {
        detector = GestureDetector(this, onGestureListener)
        vp_clock.offscreenPageLimit = 9
        vp_clock.adapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): androidx.fragment.app.Fragment {
                return getFragment(position)
            }

            override fun getCount(): Int {
                return 9
            }
        }
        vp_clock.currentItem = Device.clockMode
    }

    override fun initListener() {
        iv_back_clock.setOnClickListener(this)
        vp_clock.setOnTouchListener(this)
        vp_clock.addOnPageChangeListener(onPageChangeListener)
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        detector.onTouchEvent(motionEvent)
        return false
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v) {
            iv_back_clock -> finish()
        }
    }

    override fun initMessageEvent(event: MessageEvent) {

    }

    private fun getFragment(position: Int): androidx.fragment.app.Fragment {
        return when (position) {
            0 -> Clock0Fragment()
            1 -> Clock1Fragment()
            2 -> Clock2Fragment()
            3 -> Clock3Fragment()
            4 -> Clock4Fragment()
            5 -> Clock5Fragment()
            6 -> Clock6Fragment()
            7 -> Clock7Fragment()
            8 -> Clock8Fragment()
            else -> Clock0Fragment()
        }
    }

    override fun onResume() {
        super.onResume()
        mHandler.postDelayed(mRunnable, 5000)
    }

    override fun onStop() {
        super.onStop()
        mHandler.removeCallbacks(mRunnable)
    }
}