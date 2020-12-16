package com.joyhong.test;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.joyhong.test.util.TestConstant;
import com.joyhong.test.util.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import static com.joyhong.test.HomeTvAdapter.lastFocusPos;
import static com.joyhong.test.util.TestConstant.CATEGORY_POP_SELECT_POSITION;

public class TestMainActivity extends AppCompatActivity{
    private ArrayList<TestEntity> testEntities = new ArrayList<>();
    public static HashMap<String, TestEntity> testResult = new HashMap<String, TestEntity>();
    public static boolean EXIST_EXTERNA_STORAGE = false;
    public static boolean EXIST_USB_STORAGE = false;
    public static boolean EXIST_HEADSET = false;
    private CustomRecyclerView rv;
    GridAdapter gridAdapter;
    HomeTvAdapter homeTvAdapter;
    private RelativeLayout main_v;
    private TextView test_info;
    private StaggeredGridLayoutManager mLayoutManager;
    public static int LINE_NUM = 3;  //要显示的行数
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TestConstant.isConfigTestMode = true;
        testResult.clear();
        hideBottomUIMenu();  //隐藏底部虚拟按键
        TestEntity testEntity = new TestEntity(0, 0, "com.joyhong.test.TouchScreenTestActivity", "触摸测试", TestResultEnum.UNKNOW);
        TestEntity testEntity2 = new TestEntity(0, 1, "com.joyhong.test.RecordActivity", "摄像头测试", TestResultEnum.UNKNOW);
        TestEntity testEntity3 = new TestEntity(0, 2, "com.joyhong.test.androidmediademo.media.MusicSelActivity", "录音测试", TestResultEnum.UNKNOW);
        TestEntity testEntity4 = new TestEntity(0, 2, "com.joyhong.test.photo.SlideTestActivity", "LCD测试", TestResultEnum.UNKNOW);
        TestEntity testEntity5 = new TestEntity(0, 2, "com.joyhong.test.video.VideoViewTestActivity", "视频老化测试", TestResultEnum.UNKNOW);
        TestEntity testEntity6 = new TestEntity(0, 2, "com.joyhong.test.wifi.WifiTestActivity", "Wifi信号强度测试", TestResultEnum.UNKNOW);
        TestEntity testEntity7 = new TestEntity(0, 2, "com.joyhong.test.control.ControlTestActivity", "遥控器测试", TestResultEnum.UNKNOW);
        TestEntity testEntity8 = new TestEntity(0, 2, "com.joyhong.test.musictest.MusicTestActivity", "喇叭测试", TestResultEnum.UNKNOW);
        TestEntity testEntity9 = new TestEntity(0, 2, "com.joyhong.test.device.DeviceInfoTestActivity", "系统版本信息", TestResultEnum.UNKNOW);
        TestEntity testEntity10 = new TestEntity(0, 2, "com.joyhong.test.gsensor.GsnsorViewAcitvity", "重力感应测试", TestResultEnum.UNKNOW);
        TestEntity testEntity11 = new TestEntity(0, 2, "com.joyhong.test.interfacedevice.InterfaceDevice", "外接设备测试", TestResultEnum.UNKNOW);

        try {
            InputStreamReader inputReader = new InputStreamReader(getResources().getAssets().open("config.txt"));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";
            while ((line = bufReader.readLine()) != null) {
                if (line.contains("Touch_1")) {
                    testEntities.add(testEntity);
                } else if (line.contains("Camera_1")) {
                    testEntities.add(testEntity2);
                } else if (line.contains("Record_1")) {
                    testEntities.add(testEntity3);
                } else if (line.contains("Lcd_1")) {
                    testEntities.add(testEntity4);
                } else if (line.contains("VideoTest_1")) {
                    testEntities.add(testEntity5);
                } else if (line.contains("Wifi_1")) {
                    testEntities.add(testEntity6);
                } else if (line.contains("RemoteControl_1")) {
                    testEntities.add(testEntity7);
                } else if (line.contains("Microphone_1")) {
                    testEntities.add(testEntity8);
                } else if (line.contains("SystemInfo_1")) {
                    testEntities.add(testEntity9);
                } else if (line.contains("G-sensor_1")) {
                    testEntities.add(testEntity10);
                } else if (line.contains("Sdcard_1")) {
                    EXIST_EXTERNA_STORAGE = true;
                    if(!testEntities.contains(testEntity11)){
                        testEntities.add(testEntity11);
                    }
                } else if (line.contains("USB_1")) {
                    EXIST_USB_STORAGE = true;
                    if(!testEntities.contains(testEntity11)){
                        testEntities.add(testEntity11);
                    }
                } else if (line.contains("HeadSet_1")) {
                    EXIST_HEADSET = true;
                    if(!testEntities.contains(testEntity11)){
                        testEntities.add(testEntity11);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        setContentView(R.layout.activity_test_main);
        main_v = findViewById(R.id.test_result_main);
        test_info = findViewById(R.id.test_info);
        for (TestEntity testEntityR : testEntities) {
            testResult.put(testEntityR.getTag(), testEntityR);
        }
        rv = findViewById(R.id.rv2);
//        rv.setLayoutManager(new GridLayoutManager(TestMainActivity.this, 4));
//        gridAdapter = new GridAdapter(TestMainActivity.this, testEntities);
//        rv.setAdapter(gridAdapter);
        int spacing = 32; // 50px
        boolean includeEdge = true;
//        rv.addItemDecoration(new GridSpacingItemDecoration(4, spacing, includeEdge));
        //设置布局管理器

        homeTvAdapter = new HomeTvAdapter(TestMainActivity.this, testEntities);
        LINE_NUM = (int) Math.ceil(testEntities.size()/4);
        mLayoutManager = new StaggeredGridLayoutManager(LINE_NUM, StaggeredGridLayoutManager.HORIZONTAL);
        mLayoutManager.setAutoMeasureEnabled(false);
        rv.setLayoutManager(mLayoutManager);
        rv.setAdapter(homeTvAdapter);
        homeTvAdapter.setOnItemClickListener(new MyOnItemClickListener());
        rv.setOnScrollListener(new MyOnScrollListener());


        checkFileExistOrCopy();
    }
    private class MyOnItemClickListener implements HomeTvAdapter.OnItemClickListener {
        @Override
        public void onItemClick(View view, int position) {
            try {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClassName(TestMainActivity.this, String.valueOf(testEntities.get(position).getTag()));
                TestMainActivity.this.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onItemLongClick(View view, int position) {
        }
    }
    private class MyOnScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            //在滚动的时候处理箭头的状态
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(-1 == lastFocusPos){
            homeTvAdapter.notifyDataSetChanged();
        }else{
//            homeTvAdapter.notifyItemChanged(lastFocusPos);
            homeTvAdapter.notifyDataSetChanged();
        }
        checkResult();
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

    public void checkFileExistOrCopy() {
        File[] files = getExternalFilesDir(null).listFiles();
        try {
            String[] fileNames = getResources().getAssets().list("test");
            boolean allFileExist = true;
            for (String assetFiles : fileNames) {
                boolean exist = false;
                for (File f : files) {
                    if (TextUtils.equals(f.getName(), assetFiles)) {
                        exist = true;
                        break;
                    }
                }
                if (!exist) {
                    allFileExist = false;
                    break;
                }
            }
            if (!allFileExist) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FileUtil.copyAssetsFiles(TestConstant.application, "test", TestConstant.application.getExternalFilesDir(null).getAbsolutePath());
                    }
                }).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checkResult() {
        boolean checkOK = true;
        for (TestEntity testEntity : testEntities) {
            if (SPUtils.getInstance().getInt(testEntity.getTag(), 0) == 2) {
                checkOK = false;
            } else if (SPUtils.getInstance().getInt(testEntity.getTag(), 0) == 1) {
            } else {
                checkOK = false;
            }
        }
        if (checkOK) {
            test_info.setText("PASS");
            main_v.setBackgroundResource(R.drawable.shape_actionsheet_green_normal);
        } else {
            test_info.setText("FAIL");
            main_v.setBackgroundResource(R.drawable.shape_actionsheet_top_normal);
        }

    }

    @Override
    protected void onDestroy() {
        TestConstant.isConfigTestMode = false;
        super.onDestroy();
    }

}