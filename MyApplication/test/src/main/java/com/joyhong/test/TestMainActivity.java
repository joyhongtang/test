package com.joyhong.test;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.SPUtils;
import com.joyhong.test.util.MyTestUtils;
import com.joyhong.test.util.ShellUtils;
import com.joyhong.test.util.TestConstant;
import com.joyhong.test.util.FileUtil;
import com.joyhong.test.widget.MaterialDialog;
import com.joyhong.test.widget.MyCreateQRViewDialog;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import static com.joyhong.test.HomeTvAdapter.lastFocusPos;

public class TestMainActivity extends BaseTestActivity implements View.OnClickListener {
    public static ArrayList<TestEntity> testEntities = new ArrayList<>();
    public static HashMap<String, TestEntity> testResult = new HashMap<String, TestEntity>();
    public static boolean EXIST_EXTERNA_STORAGE = false;
    public static boolean EXIST_USB_STORAGE = false;
    public static boolean EXIST_HEADSET = false;
    private CustomRecyclerView rv;
    private HomeTvAdapter homeTvAdapter;
    private RelativeLayout main_v, clear_result_main;
    private TextView test_info;
    private GridLayoutManager mLayoutManager;
    public static int LINE_NUM = 3;  //要显示的行数
    public static final String CHARGER_CURRENT_NOW =
            "/sys/class/power_supply/battery/BatteryAverageCurrent";
    public static final String DEVICE_RTC =
            "/dev/rtc";
    public static final String DEVICE_RTC0 =
            "/dev/rtc0";
    public static final String DEVICE_TEST_RESULT = "/private/";
    private String rtcTag = "com.joyhong.test.rtc";
    public static boolean autoTest = true;
    private View autoTestView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        keepScreenOn = false;
        super.onCreate(savedInstanceState);
        SPUtils.getInstance().put("stepinto_test", true);
        TestConstant.isConfigTestMode = true;
        TestConstant.application = getApplication();
        testResult.clear();
        testEntities.clear();
        hideBottomUIMenu();  //隐藏底部虚拟按键
        setContentView(R.layout.activity_test_main);
        main_v = findViewById(R.id.test_result_main);
        clear_result_main = findViewById(R.id.clear_result_main);
        test_info = findViewById(R.id.test_info);
        clear_result_main.setOnClickListener(this);
        autoTestView = findViewById(R.id.auto_test);
        autoTestView.setOnClickListener(this);
        FileUtil.initConfig(this,testEntities,autoTestView);
        findViewById(R.id.reset_factory).setOnClickListener(this);
        findViewById(R.id.show_qr_code).setOnClickListener(this);
        for (TestEntity testEntityR : testEntities) {
            testResult.put(testEntityR.getTag(), testEntityR);
        }
        rv = findViewById(R.id.rv2);
        //设置布局管理器
        homeTvAdapter = new HomeTvAdapter(TestMainActivity.this, testEntities);
        DecimalFormat df = new DecimalFormat("0.00");//格式化小数
        String num = df.format((float) testEntities.size() / 4);//返回的是String类型
        LINE_NUM = (int) Math.ceil(Double.parseDouble(num));
        mLayoutManager = new GridLayoutManager(this, 4);
        mLayoutManager.setAutoMeasureEnabled(false);
        rv.setLayoutManager(mLayoutManager);
        rv.setAdapter(homeTvAdapter);
        homeTvAdapter.setOnItemClickListener(new MyOnItemClickListener());
        rv.setOnScrollListener(new MyOnScrollListener());
        detectRtc();
        checkFileExistOrCopy();
        registerHumanSensor();
        initTestResult2Sdcard();
    }
    //////////////////////////human sensor//////////////////////////////////////////////////////
    private boolean humanSensorCloseSuccess, humanSensorOpenSuccess;
    private final String HUMAN_SENSOR_CLOSE = "android.system.voltage.low";
    private final String HUMAN_SENSOR_OPEN = "android.system.voltage.high";
    private String humanSensorTag = "com.joyhong.test.sensor_human";
    private HumanSensorReciver mHumanSensorReciver;
    private void detectRtc() {
        try {
            File rtc1 = new File(DEVICE_RTC);
            File rtc0 = new File(DEVICE_RTC0);
            if (rtc1.exists() || rtc0.exists()) {
                TestEntity testEntity = testResult.get(rtcTag);
                testEntity.setTestResultEnum(TestResultEnum.PASS);
                SPUtils.getInstance().put(testEntity.getTag(), 1);
                homeTvAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onClick(View v) {
        if (v == clear_result_main) {
            new MaterialDialog.Builder(this).setTitle("Clear?")
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, new MaterialDialog.OnClickListener() {
                        @Override
                        public void onClick(@NotNull MaterialDialog dialog) {
                            lastFocusPos = -1;
                            for (TestEntity testEntity : testEntities) {
                                SPUtils.getInstance().put(testEntity.getTag(), 0);
                            }
                            SPUtils.getInstance().put("isExternalStorage", 0);
                            SPUtils.getInstance().put("isUsbStorage", 0);
                            SPUtils.getInstance().put("isHeadSet", 0);
                            TestEntity testEntity = testResult.get(humanSensorTag);
                            if (testEntity != null && SPUtils.getInstance().getInt(testEntity.getTag(), -1) != 1) {
                                EventBus.getDefault().post(new MessageEventTest(MessageEventTest.HUMAN_SENSOR_ON));
                            }
                            checkResult(true);
                            refresh();
                        }
                    })
                    .show();
        } else if (v.getId() == R.id.show_qr_code) {
            showQRCODE(saveTestResult2Sdcard(false,false));
        } else if (v.getId() == R.id.reset_factory) {
            resetFactory();
        } else if (v.getId() == R.id.auto_test) {
            autoTest = true;
            int autoCheckPos = 0;
            for(int i=0;i<testEntities.size();i++){
                if (SPUtils.getInstance().getInt(testEntities.get(i).getTag(), 0) != 1){
                    autoCheckPos = i;
                    break;
                }
            }
            rv.getChildAt(autoCheckPos).performClick();
        }
    }

    class HumanSensorReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(HUMAN_SENSOR_CLOSE, intent.getAction())) {
                humanSensorCloseSuccess = true;
                checkHumanSensor();
            } else if (TextUtils.equals(HUMAN_SENSOR_OPEN, intent.getAction())) {
                humanSensorOpenSuccess = true;
                checkHumanSensor();
            }
        }
    }

    private void registerHumanSensor() {
        // 注册广播接收器
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HUMAN_SENSOR_CLOSE);
        intentFilter.addAction(HUMAN_SENSOR_OPEN);
        mHumanSensorReciver = new HumanSensorReciver();
        registerReceiver(mHumanSensorReciver, intentFilter);
    }

    private void checkHumanSensor() {
        //&& (SPUtils.getInstance().getInt(humanSensorTag) != 1)
        if (humanSensorCloseSuccess) {
            TestEntity testEntity = testResult.get(humanSensorTag);
            testEntity.setTestResultEnum(TestResultEnum.PASS);
            SPUtils.getInstance().put(testEntity.getTag(), 1);
            homeTvAdapter.notifyDataSetChanged();
        }
    }

    private void unRegisterHumanSensor() {
        if (null != mHumanSensorReciver)
            unregisterReceiver(mHumanSensorReciver);
    }

    //////////////////////////human sensor//////////////////////////////////////////////////////
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
        refresh();
        checkResult(false);
        TestEntity testEntity = testResult.get(humanSensorTag);
        if (testEntity != null && SPUtils.getInstance().getInt(testEntity.getTag(), -1) != 1) {
            //setMotionSensor(true);
            EventBus.getDefault().post(new MessageEventTest(MessageEventTest.HUMAN_SENSOR_ON));
        }else{
            EventBus.getDefault().post(new MessageEventTest(MessageEventTest.HUMAN_SENSOR_OFF));
        }
    }

    private void refresh() {
        if (-1 == lastFocusPos) {
            homeTvAdapter.notifyDataSetChanged();
        } else {
            homeTvAdapter.notifyItemChanged(lastFocusPos);
        }
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
                        FileUtil.copyAssetsFiles(TestConstant.application, "test", getApplication().getExternalFilesDir(null).getAbsolutePath());
                    }
                }).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checkResult(boolean clear) {
        boolean checkOK = true;
        for (TestEntity testEntity : testEntities) {
            if (SPUtils.getInstance().getInt(testEntity.getTag(), 0) == 2) {
                checkOK = false;
            } else if (SPUtils.getInstance().getInt(testEntity.getTag(), 0) == 1) {
            } else {
                checkOK = false;
                testEntity.testResultEnum = TestResultEnum.PASS;
            }
        }
        if (checkOK) {
            test_info.setText("PASS");
            main_v.setBackgroundResource(R.drawable.shape_actionsheet_green_normal);
        } else {
            test_info.setText("FAIL");
            main_v.setBackgroundResource(R.drawable.shape_actionsheet_top_normal);
        }
        saveTestResult2Sdcard(clear,checkOK);

    }

    public void initTestResult2Sdcard() {
        File file = new File(Environment.getExternalStorageDirectory(), "CloudFrame/Test/test.txt");
        if (!file.exists()) {
            FileUtil.initTestFile(file,this);
        }
    }

    /**
     * @param clear 清除文件后，还原测试文件
     */
    public String saveTestResult2Sdcard(boolean clear,boolean checkSuccess) {
        File file = new File(Environment.getExternalStorageDirectory(), "CloudFrame/Test/test.txt");
        if (clear) {
            file.delete();
            initTestResult2Sdcard();
            return "";
        }
        if(checkSuccess){

        }
        return FileUtil.saveTestResult2File(MyTestUtils.INSTANCE.getTestFile(),testEntities);
    }

    @Override
    protected void onDestroy() {
        TestConstant.isConfigTestMode = false;
        try {
            unRegisterHumanSensor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //退出还原人体感应
        TestEntity humanSensorEntity = testResult.get(humanSensorTag);
        if (humanSensorEntity != null) {
            EventBus.getDefault().post(new MessageEventTest(MessageEventTest.HUMAN_SENSOR_OFF));
        }
        super.onDestroy();
    }


    private MyCreateQRViewDialog myCreateQRViewDialog;

    public void showQRCODE(String connectCode) {
        //二维码页面
        myCreateQRViewDialog = new MyCreateQRViewDialog(this, R.layout.activity_create_qrcode_dialog);
        //显示
        myCreateQRViewDialog.show();
        myCreateQRViewDialog.createQRCode(connectCode);
    }

    public void resetFactory() {
        new MaterialDialog.Builder(this).setTitle("恢复出厂设置?")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new MaterialDialog.OnClickListener() {
                    @Override
                    public void onClick(@NotNull MaterialDialog dialog) {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                            Intent intent = new Intent("android.intent.action.FACTORY_RESET");
                            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                            intent.setPackage("android");
                            intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm");
                            //intent.putExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", true)
                            sendBroadcast(intent);
                        } else {
                            Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
                            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                            intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm");
                            //intent.putExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", true)
                            sendBroadcast(intent);
                        }
                    }
                })
                .show();
    }

    public static boolean stepAutoTestActivity(Activity activity) {
        boolean finished = false;
        int seekPos = 0;
        try {
            for (int i = 0; i < testEntities.size(); i++) {
                TestEntity testEntity = testEntities.get(i);
                if (testEntity.tag.contains(activity.getLocalClassName())) {
                    seekPos = i;
                    break;
                }
            }
            if (seekPos >= testEntities.size() - 1) {
                finished = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        seekPos = seekPos + 1;
        if (seekPos == testEntities.size() - 1) {
        } else {
            int curPos = seekPos;
            for (int j = seekPos; j < testEntities.size(); j++) {
                TestEntity testEntity = testEntities.get(j);
                if (!testEntity.tag.contains("rtc") && !testEntity.tag.contains("human")) {
                    Intent intent = new Intent();
                    intent.setClassName(activity, testEntity.tag);
                    activity.startActivity(intent);
                    return false;
                }
                curPos = j;
            }
            curPos++;
            if (curPos >= testEntities.size() - 1) {
                finished = true;
            }
        }
        return finished;
    }

}