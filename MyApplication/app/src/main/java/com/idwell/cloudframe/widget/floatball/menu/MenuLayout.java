package com.idwell.cloudframe.widget.floatball.menu;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.blankj.utilcode.util.SizeUtils;
import com.idwell.cloudframe.widget.floatball.runner.ICarrier;
import com.idwell.cloudframe.widget.floatball.runner.ScrollRunner;


/**
 * 子菜单项布局
 *
 * @author 何凌波
 */
public class MenuLayout extends ViewGroup implements ICarrier {
    private int mChildSize;
    private int mChildPadding = 5;
    private float mFromDegrees;
    private float mToDegrees;
    private static int MIN_RADIUS;
    private int mRadius;// 中心菜单圆点到子菜单中心的距离
    private boolean mExpanded = false;
    private boolean isMoving = false;
    private int position = FloatMenu.LEFT_TOP;
    private ScrollRunner mRunner;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            FloatMenu floatMenu = (FloatMenu) getParent();
            floatMenu.remove();
        }
    };

    private int getRadiusAndPadding() {
        return mRadius + (mChildPadding * 2);
    }

    public MenuLayout(Context context) {
        this(context, null);
    }

    public MenuLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        MIN_RADIUS = SizeUtils.dp2px(65);
        mRunner = new ScrollRunner(this);
        setChildrenDrawingOrderEnabled(true);
    }

    /**
     * 计算半径
     */
    private static int computeRadius(final float arcDegrees, final int childCount, final int childSize, final int childPadding, final int minRadius) {
        if (childCount < 2) {
            return minRadius;
        }
//        final float perDegrees = arcDegrees / (childCount - 1);
        final float perDegrees = arcDegrees == 360 ? (arcDegrees) / (childCount) : (arcDegrees) / (childCount - 1);
        final float perHalfDegrees = perDegrees / 2;
        final int perSize = childSize + childPadding;
        final int radius = (int) ((perSize / 2) / Math.sin(Math.toRadians(perHalfDegrees)));
        return Math.max(radius, minRadius);
    }

    /**
     * 计算子菜单项的范围
     */
    private static Rect computeChildFrame(final int centerX, final int centerY, final int radius, final float degrees, final int size) {
        //子菜单项中心点
        final double childCenterX = centerX + radius * Math.cos(Math.toRadians(degrees));
        final double childCenterY = centerY + radius * Math.sin(Math.toRadians(degrees));
        //子菜单项的左上角，右上角，左下角，右下角
        return new Rect((int) (childCenterX - size / 2),
                (int) (childCenterY - size / 2), (int) (childCenterX + size / 2), (int) (childCenterY + size / 2));
    }

    /**
     * 子菜单项大小
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mChildSize * 5, mChildSize);
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(mChildSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(mChildSize, MeasureSpec.EXACTLY));
        }
    }

    private int getLayoutSize() {
        mRadius = computeRadius(Math.abs(mToDegrees - mFromDegrees), getChildCount(),
                mChildSize, mChildPadding, MIN_RADIUS);
        int layoutPadding = 10;
        return mRadius * 2 + mChildSize + mChildPadding + layoutPadding * 2;
    }

    /**
     * 子菜单项位置
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (isMoving) return;
        final int radius = 0;
        layoutItem(radius);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        //当悬浮球在右侧时，使其菜单从上到下的顺序和在左边时一样。
        if (!isLeft()) {
            return childCount - i - 1;
        }
        return i;
    }

    private boolean isLeft() {
        int corner = (int) (mFromDegrees / 90);
        return corner == 0 || corner == 3 ? true : false;
    }

    private void layoutItem(int radius) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            int index = getChildDrawingOrder(childCount, i);
            Rect frame = new Rect(mChildSize * i, 0, mChildSize * (i + 1), mChildSize);
            getChildAt(index).layout(frame.left, frame.top, frame.right, frame.bottom);
        }
    }

    @Override
    public void requestLayout() {
        if (!isMoving) {
            super.requestLayout();
        }
    }

    /**
     * 切换中心按钮的展开缩小
     */
    public void switchState(int position, int duration) {
        this.position = position;
        mExpanded = !mExpanded;
        isMoving = true;
        mRadius = computeRadius(Math.abs(mToDegrees - mFromDegrees), getChildCount(),
                mChildSize, mChildPadding, MIN_RADIUS);
        final int start = mExpanded ? 0 : mRadius;
        final int radius = mExpanded ? mRadius : -mRadius;
        mRunner.start(start, 0, radius, 0, duration);
    }

    public boolean isMoving() {
        return isMoving;
    }

    @Override
    public void onMove(int lastX, int lastY, int curX, int curY) {
        layoutItem(curX);
    }

    public void onDone() {
        isMoving = false;
        if (!mExpanded) {
            FloatMenu floatMenu = (FloatMenu) getParent();
            floatMenu.remove();
        }else {
            postDelayed();
        }
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    /**
     * 设定弧度
     */
    public void setArc(float fromDegrees, float toDegrees, int position) {
        this.position = position;
        if (mFromDegrees == fromDegrees && mToDegrees == toDegrees) {
            return;
        }
        mFromDegrees = fromDegrees;
        mToDegrees = toDegrees;
        requestLayout();
    }

    /**
     * 设定弧度
     */
    public void setArc(float fromDegrees, float toDegrees) {
        setArc(fromDegrees, toDegrees, position);
    }

    /**
     * 设定子菜单项大小
     */
    public void setChildSize(int size) {
        mChildSize = size;
    }

    public int getChildSize() {
        return mChildSize;
    }

    public void setExpand(boolean expand) {
        mExpanded = expand;
    }

    public void postDelayed(){
        removeCallbacks(mRunnable);
        postDelayed(mRunnable, 3000);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mRunnable);
    }
}