package com.joyhong.test.photo.photoview

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class MyViewPagerTest : ViewPager {

    private lateinit var mSpeedScroller: SpeedScrollerTest

    constructor(context: Context) : super(context){
        init(context)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet){
        init(context)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return try {
            super.dispatchTouchEvent(ev)
        } catch (e: Exception) {
            false
        }
    }

    override fun setCurrentItem(item: Int) {
        mSpeedScroller.mDuration = 0
        super.setCurrentItem(item)
    }

    override fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        if (smoothScroll){
            mSpeedScroller.mDuration = 1200
        }
        super.setCurrentItem(item, smoothScroll)
    }

    private fun init(context: Context){
        mSpeedScroller = SpeedScrollerTest(context)
        val field = ViewPager::class.java.getDeclaredField("mScroller")
        field.isAccessible = true
        field.set(this, mSpeedScroller)
    }
}