package com.joyhong.test.gsensor;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;

import androidx.fragment.app.FragmentActivity;

import com.blankj.utilcode.util.SPUtils;
import com.joyhong.test.R;
import com.joyhong.test.TestEntity;
import com.joyhong.test.TestResultEnum;
import com.joyhong.test.util.TestConstant;

import static com.joyhong.test.TestMainActivity.testResult;


public class GsnsorViewAcitvity extends FragmentActivity implements OnClickListener {
    MyView mAnimView = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
// 全屏显示窗口
// 显示自定义的游戏View
        setContentView(R.layout.activity_test_gensor);
        mAnimView = findViewById(R.id.my_ball);
        findViewById(R.id.pass).setOnClickListener(this);
        findViewById(R.id.fail).setOnClickListener(this);
        findViewById(R.id.fail).requestFocus();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.pass) {
            TestEntity testEntity = testResult.get(TestConstant.PACKAGE_NAME  + getLocalClassName());
            testEntity.setTestResultEnum(TestResultEnum.PASS);
            SPUtils.getInstance().put(testEntity.getTag(), 1);
            finish();
        } else if (v.getId() == R.id.fail) {
            TestEntity testEntity2 = testResult.get(TestConstant.PACKAGE_NAME + getLocalClassName());
            testEntity2.setTestResultEnum(TestResultEnum.FAIL);
            SPUtils.getInstance().put(testEntity2.getTag(), 2);
            finish();
        }
    }


}
