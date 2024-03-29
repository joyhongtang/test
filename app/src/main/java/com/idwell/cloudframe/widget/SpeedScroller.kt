package com.idwell.cloudframe.widget

import android.content.Context
import android.view.animation.Interpolator
import android.widget.Scroller

class SpeedScroller(context: Context) : Scroller(context) {

    var mDuration = 0

    override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) {
        super.startScroll(startX, startY, dx, dy, mDuration)
    }

    override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
        super.startScroll(startX, startY, dx, dy, mDuration)
    }
}