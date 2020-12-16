package com.joyhong.test;

import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.blankj.utilcode.util.SPUtils;
import com.joyhong.test.util.TestConstant;

import java.util.ArrayList;

import static com.joyhong.test.TestMainActivity.testResult;


public class TouchScreenTestActivity extends AppCompatActivity implements View.OnTouchListener {
    private int red, white;
    private MyGridLayout layout;
    private ArrayList<View> testViews =  new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen1);
        hideBottomUIMenu();  //隐藏底部虚拟按键
        red = ContextCompat.getColor(this, R.color.colorAccent);//指定一种颜色
        white = ContextCompat.getColor(this, R.color.white);//指定一种颜色
        layout = (MyGridLayout) findViewById(R.id.SLayout);

        int row = 12;
        int col = 12;
        layout.setColumnCount(col);
        layout.setRowCount(row);
        /*GridLayout的自动填充*/
        for (int i = 0; i < row; i++)
            for (int j = 0; j < col; j++) {
                TextView textview = new TextView(this);
               //每个textview都监听触摸事件
                GridLayout.Spec rowSpec = GridLayout.spec(i, 1.0f); //行坐标和比重rowweight,用float表示的
                GridLayout.Spec columnSpec = GridLayout.spec(j, 1.0f);//列坐标和columnweight
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, columnSpec);
                if (i == 0 || i == row - 1 || j == 0 || j == col - 1) {
                    textview.setBackgroundColor(red);
                    textview.setOnTouchListener(this);
                    testViews.add(textview);
                }
                if((j >= 2 && j<=4) && (i >= 2 && i<=8)){
                    if(i == 3 && j== 3 || (i == 4 && j== 3) || (i == 6 && j==3) || (i == 7 && j==3)){
                        continue;
                    }
                    textview.setBackgroundColor(red);
                    textview.setOnTouchListener(this);
                    testViews.add(textview);
                }

                if((j >= 7 && j<=9) && (i >= 2 && i<=8)){
                    if(i == 3 && j== 8 || (i == 4 && j== 8) || (i == 6 && j==8) || (i == 7 && j==8)){
                        continue;
                    }
                    textview.setBackgroundColor(red);
                    textview.setOnTouchListener(this);
                    testViews.add(textview);
                }
                layout.addView(textview, params);
            }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(testStart) {
            if (testViews.size() > 0) {
                TestEntity testEntity = testResult.get(TestConstant.PACKAGE_NAME+getLocalClassName());
                testEntity.setTestResultEnum(TestResultEnum.FAIL);
                SPUtils.getInstance().put(testEntity.getTag(),2);
            } else {
                TestEntity testEntity = testResult.get(TestConstant.PACKAGE_NAME+getLocalClassName());
                testEntity.setTestResultEnum(TestResultEnum.PASS);
                SPUtils.getInstance().put(testEntity.getTag(),1);
            }
        }
    }
    private boolean testStart = false;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                testStart = true;
                testViews.remove(v);
                v.setBackgroundColor(white);
                if (testViews.size() <= 0) {
                    //com.joyhong.test.com.joyhong.test.TouchScreenTestActivity
                    //com.joyhong.test.TouchScreenTestActivity -> {TestEntity@7254}
                    TestEntity testEntity = testResult.get(TestConstant.PACKAGE_NAME+getLocalClassName());
                    testEntity.setTestResultEnum(TestResultEnum.PASS);
                    SPUtils.getInstance().put(testEntity.getTag(),1);
                    finish();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return false;
    }

    protected void hideBottomUIMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }
}
