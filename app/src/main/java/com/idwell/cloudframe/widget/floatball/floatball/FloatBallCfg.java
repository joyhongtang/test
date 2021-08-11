package com.idwell.cloudframe.widget.floatball.floatball;

public class FloatBallCfg {
    public int mDrawableRes;
    public int mSize;
    /**
     * 标记悬浮球所处于屏幕中的位置
     *
     * @see Gravity#LEFT_TOP
     * @see Gravity#LEFT_CENTER
     * @see Gravity#LEFT_BOTTOM
     * @see Gravity#RIGHT_TOP
     * @see Gravity#RIGHT_CENTER
     * @see Gravity#RIGHT_BOTTOM
     */
    public Gravity mGravity;
    //第一次显示的y坐标偏移量，左上角是原点。
    public int mOffsetY = 0;
    public boolean mHideHalfLater = false;

    public FloatBallCfg(int size, int drawableRes) {
        this(size, drawableRes, Gravity.LEFT_TOP, 0);
    }

    public FloatBallCfg(int size, int drawableRes, Gravity gravity) {
        this(size, drawableRes, gravity, 0);
    }

    public FloatBallCfg(int size, int drawableRes, Gravity gravity, int offsetY) {
        mSize = size;
        mDrawableRes = drawableRes;
        mGravity = gravity;
        mOffsetY = offsetY;
    }

    public FloatBallCfg(int size, int drawableRes, Gravity gravity, boolean hideHalfLater) {
        mSize = size;
        mDrawableRes = drawableRes;
        mGravity = gravity;
        mHideHalfLater = hideHalfLater;
    }

    public FloatBallCfg(int size, int drawableRes, Gravity gravity, int offsetY, boolean hideHalfLater) {
        mSize = size;
        mDrawableRes = drawableRes;
        mGravity = gravity;
        mOffsetY = offsetY;
        mHideHalfLater = hideHalfLater;
    }

    public void setGravity(Gravity gravity) {
        mGravity = gravity;
    }

    public void setHideHalfLater(boolean hideHalfLater) {
        mHideHalfLater = hideHalfLater;
    }

    public enum Gravity {
        LEFT_TOP(android.view.Gravity.LEFT | android.view.Gravity.TOP),
        LEFT_CENTER(android.view.Gravity.LEFT | android.view.Gravity.CENTER),
        LEFT_BOTTOM(android.view.Gravity.LEFT | android.view.Gravity.BOTTOM),
        RIGHT_TOP(android.view.Gravity.RIGHT | android.view.Gravity.TOP),
        RIGHT_CENTER(android.view.Gravity.RIGHT | android.view.Gravity.CENTER),
        RIGHT_BOTTOM(android.view.Gravity.RIGHT | android.view.Gravity.BOTTOM);

        int mValue;

        Gravity(int gravity) {
            mValue = gravity;
        }

        public int getGravity() {
            return mValue;
        }
    }
}
