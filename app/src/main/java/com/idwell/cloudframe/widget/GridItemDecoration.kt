package com.idwell.cloudframe.widget

import android.graphics.Rect
import androidx.recyclerview.widget.RecyclerView
import android.view.View

class GridItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.set(space, space, space, space)
    }
}