package com.joyhong.test.control;

import android.util.Log;
import android.view.KeyEvent;

import com.joyhong.test.BaseTestActivity;
import com.joyhong.test.R;
import com.joyhong.test.control.dlroundmenuview.DLRoundMenuView;
import com.joyhong.test.control.dlroundmenuview.Interface.OnMenuClickListener;


public class ControlTestActivity extends BaseTestActivity {
    DLRoundMenuView dlRoundMenuView;
    @Override
    public int initLayout() {
        return R.layout.activity_control_test;
    }

    @Override
    public void initData() {
        dlRoundMenuView = findViewById(R.id.dl_rmv);
        dlRoundMenuView.setOnMenuClickListener(new OnMenuClickListener() {
            @Override
            public void OnMenuClick(int position) {
                Log.e("TAG", "点击了："+position);
            }
        });
    }

    @Override
    public void initListener() {

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        dlRoundMenuView.receiverKeyDown(keyCode);
        return super.onKeyDown(keyCode, event);
    }
}
