package com.joyhong.test;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BaseTestActivity extends Activity implements View.OnClickListener{
    public void initConfig() {
    }

    public int initLayout() {
        return 0;
    }

    public void initData() {
    }

    public void initListener() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initConfig();
        setContentView(initLayout());
        initData();
        initListener();
    }

    @Override
    public void onClick(View v) {

    }
}
