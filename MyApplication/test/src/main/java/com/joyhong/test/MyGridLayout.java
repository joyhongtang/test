package com.joyhong.test;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.GridLayout;

public class MyGridLayout extends GridLayout {

    /*构造函数的参数个数比较重要，可能引来Android.View.InflateException:
     * Binary XML File Line 异常
     * 最好将构造函数的4种重载方法都加上*/
    public MyGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    /*将所有要分发的MotionEvent的Action都改为MotionEvent.ACTION_DOWN*/
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        MotionEvent e = MotionEvent.obtain(ev);
        e.setAction(MotionEvent.ACTION_DOWN);
        return super.dispatchTouchEvent(e);
    }
}