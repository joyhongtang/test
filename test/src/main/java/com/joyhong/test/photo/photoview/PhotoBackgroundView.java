package com.joyhong.test.photo.photoview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ScreenUtils;

import java.util.Arrays;

public class PhotoBackgroundView extends AppCompatImageView {

    private String TAG = PhotoBackgroundView.class.getSimpleName();
    private int viewWidth, viewHeight;
    private float px, py;

    private Matrix mBaseMatrix = new Matrix();
    private Matrix mDrawableMatrix = new Matrix();
    private Matrix mSupportMatrix = new Matrix();

    private float mDegrees = 0f;
    private float mRotationDegrees = -1f;

    public PhotoBackgroundView(Context context) {
        this(context, null);
    }

    public PhotoBackgroundView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PhotoBackgroundView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        viewWidth = ScreenUtils.getScreenWidth();
        viewHeight = ScreenUtils.getScreenHeight();
        px = viewWidth / 2.0f;
        py = viewHeight / 2.0f;
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        updateBaseMatrix();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        updateBaseMatrix();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        updateBaseMatrix();
    }

    @Override
    public void setImageURI(@Nullable Uri uri) {
        super.setImageURI(uri);
        updateBaseMatrix();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            viewWidth = ScreenUtils.getScreenWidth();
            viewHeight = ScreenUtils.getScreenHeight();
            px = viewWidth / 2.0f;
            py = viewHeight / 2.0f;
            updateBaseMatrix();
        }
    }

    public void setDegrees(float degrees) {
        mDegrees = degrees;
    }

    public void setRotationTo(float degrees) {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            mRotationDegrees = degrees;
            int drawableWidth = drawable.getIntrinsicWidth();
            int drawableHeight = drawable.getIntrinsicHeight();
            RectF rect = new RectF(0, 0, drawableWidth, drawableHeight);
            mDrawableMatrix.mapRect(rect);
            float sx = 1f;
            float sy = 1f;
            if (rect.width() > rect.height()) {
                sx = viewHeight * 1.0f / rect.width();
                sy = viewWidth * 1.0f / rect.height();
            }else if (rect.height() > rect.width()){
                sx = viewHeight * 1.0f / rect.width();
                sy = viewWidth * 1.0f / rect.height();
            }
            LogUtils.dTag(TAG, rect.width() + ", " + rect.height() + ", " + sx + ", " + sy);
            mSupportMatrix.postScale(sx, sy, px, py);
            mSupportMatrix.postRotate(90f, px, py);
            mDrawableMatrix.set(mBaseMatrix);
            mDrawableMatrix.postConcat(mSupportMatrix);
            setImageMatrix(mDrawableMatrix);
        }
    }

    private void updateBaseMatrix() {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            int drawableWidth = drawable.getIntrinsicWidth();
            int drawableHeight = drawable.getIntrinsicHeight();
            mBaseMatrix.reset();
            RectF src = new RectF(0, 0, drawableWidth, drawableHeight);
            RectF dst = new RectF(0, 0, viewWidth, viewHeight);
            mBaseMatrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
            mSupportMatrix.reset();
            if (mRotationDegrees != -1) {
                RectF rect = new RectF(0, 0, drawableWidth, drawableHeight);
                mDrawableMatrix.mapRect(rect);
                float sx = 1.0f;
                float sy = 1.0f;
                if (Arrays.asList(90f, 270f).contains(Math.abs(mRotationDegrees - mDegrees))) {
                    sx = viewWidth * 1.0f / rect.width();
                    sy = viewHeight * 1.0f / rect.height();
                }
                LogUtils.dTag(TAG, rect.width() + ", " + rect.height() + ", " + sx + ", " + sy);
                mSupportMatrix.postRotate(mRotationDegrees - mDegrees, px, py);
                mSupportMatrix.postScale(sx, sy, px, py);
            }
            mDrawableMatrix.set(mBaseMatrix);
            mDrawableMatrix.postConcat(mSupportMatrix);
            setImageMatrix(mDrawableMatrix);
        }
    }
}
