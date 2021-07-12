package com.joyhong.test;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

public class FocusView extends RelativeLayout {
    public FocusView(Context context) {
        super(context);
        init();
    }

    public FocusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FocusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    public FocusView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void init(){
        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){

                    if (Build.VERSION.SDK_INT >= 21) {
                        //抬高Z轴
                        ViewCompat.animate(FocusView.this).scaleX(1.20f).scaleY(1.20f).translationZ(1).start();
                    } else {
                        ViewCompat.animate(FocusView.this).scaleX(1.20f).scaleY(1.20f).start();
                        ViewGroup parent = (ViewGroup) FocusView.this.getParent();
                        parent.requestLayout();
                        parent.invalidate();
                    }
                }else{
                    if (Build.VERSION.SDK_INT >= 21) {
                        ViewCompat.animate(FocusView.this).scaleX(1.0f).scaleY(1.0f).translationZ(0).start();
                    } else {
                        ViewCompat.animate(FocusView.this).scaleX(1.0f).scaleY(1.0f).start();
                        ViewGroup parent = (ViewGroup) FocusView.this.getParent();
                        parent.requestLayout();
                        parent.invalidate();
                    }
                }
            }
        });
    }

}
