package com.joyhong.test;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.joyhong.test.service.TestService;

public class BaseTestActivity extends Activity implements View.OnClickListener {
    public int initLayout() {
        return 0;
    }

    public void initData() {
    }

    public void initListener() {
    }
    public boolean keepScreenOn = true;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if(keepScreenOn)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, TestService.class);
        intent.setAction(TestService.ACTION_CLOSE_WIFI);
        startService(intent);

        if (0 != initLayout())
            setContentView(initLayout());
        initData();
        initListener();
    }

    @Override
    public void finish() {
        if(TestMainActivity.autoTest) {
//            boolean finised = TestMainActivity.stepAutoTestActivity(this);
            super.finish();
        } else {
            super.finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode ==  KeyEvent.KEYCODE_BACK){
            super.finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {

    }
}
