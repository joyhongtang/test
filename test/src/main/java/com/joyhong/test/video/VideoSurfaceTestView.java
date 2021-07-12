package com.joyhong.test.video;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

import com.blankj.utilcode.util.ScreenUtils;
import com.joyhong.test.util.TestConstant;

public class VideoSurfaceTestView extends SurfaceView {

    private int mVideoWidth = 0;
    private int mVideoHeight = 0;

    public VideoSurfaceTestView(Context context) {
        this(context, null);
    }

    public VideoSurfaceTestView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoSurfaceTestView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = mVideoWidth > 0 ? mVideoWidth : MeasureSpec.getSize(widthMeasureSpec);
        int height = mVideoHeight > 0 ? mVideoHeight : MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    /**
     * 调整大小
     * @param videoWidth 视频宽度
     * @param videoHeight 视频高度
     */
    public void adjustSize(int videoWidth, int videoHeight) {
        if (videoWidth == 0 || videoHeight == 0) return;
        int viewWidth, viewHeight;
        if (TestConstant.isDualScreen) {
            viewWidth = ScreenUtils.getScreenWidth() - 195 - 18;
        } else {
            viewWidth = ScreenUtils.getScreenWidth();
        }
        viewHeight = ScreenUtils.getScreenHeight();
        //获取拉伸的宽度比例
        float scaleWidth = viewWidth * 1.0f / videoWidth;
        //获取拉伸高度的比例
        float scaleHeight = viewHeight * 1.0f / videoHeight;
        //保持高度不变进行宽度拉伸
        if (scaleWidth > scaleHeight) {
            mVideoWidth = (int) (videoWidth * scaleHeight);
            mVideoHeight = viewHeight;
        } else {
            mVideoWidth = viewWidth;
            mVideoHeight = (int) (videoHeight * scaleWidth);
        }
        // 设置Holder固定的大小
        getHolder().setFixedSize(mVideoWidth, mVideoHeight);
        // 重新设置自己的大小
        requestLayout();
    }
}
