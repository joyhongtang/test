package com.joyhong.test.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joyhong.test.R

class TestHorizontalItemDecoration : RecyclerView.ItemDecoration {

    private var mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mDividerHeight = 1
    private var mDrawLastDivider = true

    constructor(color: Int) {
        mPaint.color = color
        mPaint.style = Paint.Style.FILL
    }

    constructor(color: Int, drawLastDivider: Boolean) {
        mPaint.color = color
        mPaint.style = Paint.Style.FILL
        mDrawLastDivider = drawLastDivider
    }

    constructor(context: Context, drawLastDivider: Boolean) {
        mPaint.color = ContextCompat.getColor(context, R.color.divider_dialog)
        mPaint.style = Paint.Style.FILL
        mDrawLastDivider = drawLastDivider
    }

    constructor(dividerColor: Int, dividerHeight: Int) {
        mPaint.color = dividerColor
        mPaint.style = Paint.Style.FILL
        mDividerHeight = dividerHeight
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.set(0, 0, 0, mDividerHeight)
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val layoutParams = child.layoutParams as RecyclerView.LayoutParams
            val left = child.left - layoutParams.leftMargin
            val top = child.bottom + layoutParams.bottomMargin
            val right = child.right + layoutParams.rightMargin
            val bottom = top + mDividerHeight
            if (mDrawLastDivider) {
                c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), mPaint)
            } else if (i + 1 != childCount) {
                c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), mPaint)
            }
        }
    }
}