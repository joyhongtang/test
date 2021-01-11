package com.idwell.cloudframe.widget.floatball;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.blankj.utilcode.util.ScreenUtils;
import com.idwell.cloudframe.common.Device;
import com.idwell.cloudframe.widget.floatball.floatball.FloatBall;
import com.idwell.cloudframe.widget.floatball.floatball.FloatBallCfg;
import com.idwell.cloudframe.widget.floatball.floatball.StatusBarView;
import com.idwell.cloudframe.widget.floatball.menu.FloatMenu;
import com.idwell.cloudframe.widget.floatball.menu.FloatMenuCfg;
import com.idwell.cloudframe.widget.floatball.menu.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class FloatBallManager {
    public int mScreenWidth, mScreenHeight;

    private IFloatBallPermission mPermission;
    private OnFloatBallClickListener mFloatballClickListener;
    private WindowManager mWindowManager;
    private Context mContext;
    private FloatBall floatBall;
    private FloatMenu floatMenu;
    private StatusBarView statusBarView;
    public int floatballX, floatballY;
    private boolean isShowing = false;
    private List<MenuItem> menuItems = new ArrayList<>();
    private Activity mActivity;

    public FloatBallManager(Context application, FloatBallCfg ballCfg) {
        this(application, ballCfg, null);
    }

    public FloatBallManager(Context context, FloatBallCfg ballCfg, FloatMenuCfg menuCfg) {
        mContext = context;
        FloatBallUtil.inSingleActivity = false;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        computeScreenSize();
        floatBall = new FloatBall(context, this, ballCfg);
        floatMenu = new FloatMenu(context, this, menuCfg);
        floatMenu.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        statusBarView = new StatusBarView(context, this);
    }

    public FloatBallManager(Activity activity, FloatBallCfg ballCfg) {
        this(activity, ballCfg, null);
    }

    public FloatBallManager(Activity activity, FloatBallCfg ballCfg, FloatMenuCfg menuCfg) {
        mActivity = activity;
        FloatBallUtil.inSingleActivity = true;
        mWindowManager = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
        computeScreenSize();
        floatBall = new FloatBall(mActivity, this, ballCfg);
        floatMenu = new FloatMenu(mActivity, this, menuCfg);
        statusBarView = new StatusBarView(mActivity, this);
    }

    public void buildMenu() {
        inflateMenuItem();
    }

    /**
     * 添加一个菜单条目
     *
     * @param item
     */
    public FloatBallManager addMenuItem(MenuItem item) {
        menuItems.add(item);
        return this;
    }

    public int getMenuItemSize() {
        return menuItems != null ? menuItems.size() : 0;
    }

    /**
     * 设置菜单
     *
     * @param items
     */
    public FloatBallManager setMenu(List<MenuItem> items) {
        menuItems = items;
        return this;
    }

    private void inflateMenuItem() {
        floatMenu.removeAllItemViews();
        for (MenuItem item : menuItems) {
            floatMenu.addItem(item);
        }
    }

    public int getBallSize() {
        return floatBall.getSize();
    }

    public void computeScreenSize() {
        if (Device.isDualScreen){
            mScreenWidth = ScreenUtils.getScreenWidth() - 18 - 195;
        }else {
            mScreenWidth = ScreenUtils.getScreenWidth();
        }
        mScreenHeight = ScreenUtils.getScreenHeight();
    }

    public int getStatusBarHeight() {
        return statusBarView.getStatusBarHeight();
    }

    public void onStatusBarHeightChange() {
        floatBall.onLayoutChange();
    }

    public void show() {
        if (mActivity == null) {
            if (mPermission == null) {
                return;
            }
            if (!mPermission.hasFloatBallPermission(mContext)) {
                mPermission.onRequestFloatBallPermission();
                return;
            }
        }
        if (isShowing) return;
        isShowing = true;
        floatBall.setVisibility(View.VISIBLE);
        statusBarView.attachToWindow(mWindowManager);
        floatBall.attachToWindow(mWindowManager);
        floatMenu.detachFromWindow(mWindowManager);
    }

    public void closeMenu() {
        floatMenu.closeMenu();
    }

    public void reset() {
        floatBall.setVisibility(View.VISIBLE);
        floatBall.postSleepRunnable();
        floatMenu.detachFromWindow(mWindowManager);
    }

    public void onFloatBallClick() {
        if (menuItems != null && menuItems.size() > 0) {
            floatMenu.attachToWindow(mWindowManager);
        } else {
            if (mFloatballClickListener != null) {
                mFloatballClickListener.onFloatBallClick();
            }
        }
    }

    public void hide() {
        if (!isShowing) return;
        isShowing = false;
        floatBall.detachFromWindow(mWindowManager);
        floatMenu.detachFromWindow(mWindowManager);
        statusBarView.detachFromWindow(mWindowManager);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        computeScreenSize();
        reset();
    }

    public void setPermission(IFloatBallPermission iPermission) {
        this.mPermission = iPermission;
    }

    public void setOnFloatBallClickListener(OnFloatBallClickListener listener) {
        mFloatballClickListener = listener;
    }

    public interface OnFloatBallClickListener {
        void onFloatBallClick();
    }

    public interface IFloatBallPermission {
        /**
         * request the permission of floatball,just use {@link #requestFloatBallPermission(Activity)},
         * or use your custom method.
         *
         * @return return true if requested the permission
         * @see #requestFloatBallPermission(Activity)
         */
        boolean onRequestFloatBallPermission();

        /**
         * detect whether allow  using floatball here or not.
         *
         * @return
         */
        boolean hasFloatBallPermission(Context context);

        /**
         * request floatball permission
         */
        void requestFloatBallPermission(Activity activity);
    }
}
