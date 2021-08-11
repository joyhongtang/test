package com.joyhong.test.control;

import android.util.Log;
import android.view.KeyEvent;

import com.joyhong.test.BaseTestActivity;
import com.joyhong.test.R;
import com.joyhong.test.control.dlroundmenuview.DLRoundMenuView;
import com.joyhong.test.control.dlroundmenuview.Interface.OnMenuClickListener;

import java.util.HashMap;


public class ControlTestActivity extends BaseTestActivity {
    DLRoundMenuView dlRoundMenuView;
    @Override
    public int initLayout() {
        return R.layout.activity_control_test;
    }

    @Override
    public void initData() {
        buildKeyMap();
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

    private HashMap<Integer,Integer> keyMapReflect = new HashMap<>();

    private void buildKeyMap(){
        keyMapReflect.put(KeyEvent.KEYCODE_DPAD_LEFT,25);
        keyMapReflect.put(25,KeyEvent.KEYCODE_DPAD_LEFT);

        keyMapReflect.put(KeyEvent.KEYCODE_DPAD_RIGHT,24);
        keyMapReflect.put(24,KeyEvent.KEYCODE_DPAD_RIGHT);

        keyMapReflect.put(KeyEvent.KEYCODE_DPAD_CENTER,66);
        keyMapReflect.put(66,KeyEvent.KEYCODE_DPAD_CENTER);

    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            int reflect = keyMapReflect.get(keyCode);
            dlRoundMenuView.receiverKeyDown(reflect,false);
        }catch (Exception e){
            e.printStackTrace();
        }
        dlRoundMenuView.receiverKeyDown(keyCode,true);
        return super.onKeyDown(keyCode, event);
    }
}
