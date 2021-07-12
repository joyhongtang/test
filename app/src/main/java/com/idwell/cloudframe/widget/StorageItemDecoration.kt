package com.idwell.cloudframe.widget

import android.graphics.Rect
import androidx.recyclerview.widget.RecyclerView
import android.view.View

class StorageItemDecoration(val left:Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.getChildLayoutPosition(view) != 0){
            outRect.left = left
        }
    }
}