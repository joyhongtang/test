package com.idwell.cloudframe.widget.floatball.menu;

public abstract class MenuItem {

    public int mDrawableRes;
    public int mStringRes;

    public MenuItem(int drawableRes, int stringRes) {
        mDrawableRes = drawableRes;
        mStringRes = stringRes;
    }

    /**
     * 点击次菜单执行的操作
     */
    public abstract void action();
}
