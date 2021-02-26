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
    public static boolean autoTest = false;

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
        initConfig();
        setContentView(R.layout.activity_test_main);
        main_v = findViewById(R.id.test_result_main);
        clear_result_main = findViewById(R.id.clear_result_main);
        test_info = findViewById(R.id.test_info);
        clear_result_main.setOnClickListener(this);
        findViewById(R.id.auto_test).setOnClickListener(this);
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

    ////////////////////////////////////rtc test////////////////////////////////////////
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

    //////////////////////////human sensor//////////////////////////////////////////////////////
    private boolean humanSensorCloseSuccess, humanSensorOpenSuccess;
    private final String HUMAN_SENSOR_CLOSE = "android.system.voltage.low";
    private final String HUMAN_SENSOR_OPEN = "android.system.voltage.high";
    private String humanSensorTag = "com.joyhong.test.sensor_human";
    private HumanSensorReciver mHumanSensorReciver;

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
            showQRCODE(saveTestResult2Sdcard(false));
        } else if (v.getId() == R.id.reset_factory) {
            resetFactory();
        } else if (v.getId() == R.id.auto_test) {
            autoTest = true;
            rv.getChildAt(0).performClick();

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


    /**
     * 初始化所有配置项
     */
    public void initConfig() {
        TestEntity testEntity = new TestEntity(0, 0, "com.joyhong.test.TouchScreenTestActivity", "触摸测试", TestResultEnum.UNKNOW);
        TestEntity testEntity2 = new TestEntity(0, 1, "com.joyhong.test.RecordActivity", "摄像头测试", TestResultEnum.UNKNOW);
        TestEntity testEntity3 = new TestEntity(0, 2, "com.joyhong.test.androidmediademo.media.MusicSelActivity", "录音测试", TestResultEnum.UNKNOW);
        TestEntity testEntity4 = new TestEntity(0, 2, "com.joyhong.test.photo.SlideTestActivity", "LCD测试", TestResultEnum.UNKNOW);
        TestEntity testEntity5 = new TestEntity(0, 2, "com.joyhong.test.video.VideoViewTestActivity", "视频老化测试", TestResultEnum.UNKNOW);
        TestEntity testEntity6 = new TestEntity(0, 2, "com.joyhong.test.wifi.WifiTestActivity", "Wifi信号强度测试", TestResultEnum.UNKNOW);
        TestEntity testEntity7 = new TestEntity(0, 2, "com.joyhong.test.control.ControlTestActivity", "面板测试", TestResultEnum.UNKNOW);
        TestEntity testEntity8 = new TestEntity(0, 2, "com.joyhong.test.musictest.MusicTestActivity", "喇叭测试", TestResultEnum.UNKNOW);
        TestEntity testEntity9 = new TestEntity(0, 2, "com.joyhong.test.device.DeviceInfoTestActivity", "系统版本信息", TestResultEnum.UNKNOW);
        TestEntity testEntity10 = new TestEntity(0, 2, "com.joyhong.test.gsensor.GsnsorViewAcitvity", "重力感应测试", TestResultEnum.UNKNOW);
        TestEntity testEntity11 = new TestEntity(0, 2, "com.joyhong.test.interfacedevice.InterfaceDevice", "外接设备测试", TestResultEnum.UNKNOW);
        TestEntity testEntity12 = new TestEntity(0, 2, "com.joyhong.test.sensor_human", "人体感应", TestResultEnum.UNKNOW);
        TestEntity testEntity13 = new TestEntity(0, 2, "com.joyhong.test.BatteryInfoActivity", "电池测试", TestResultEnum.UNKNOW);
        TestEntity testEntity14 = new TestEntity(0, 2, "com.joyhong.test.rtc", "RTC测试", TestResultEnum.UNKNOW);

        InputStreamReader inputReader = null;
        BufferedReader bufReader = null;
        try {
            inputReader = new InputStreamReader(getResources().getAssets().open("config.txt"));
            bufReader = new BufferedReader(inputReader);
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
                } else if (line.contains("Speaker_1")) {
                    testEntities.add(testEntity8);
                } else if (line.contains("SystemInfo_1")) {
                    testEntities.add(testEntity9);
                } else if (line.contains("G-sensor_1")) {
                    testEntities.add(testEntity10);
                } else if (line.contains("Human-sensor_1")) {
                    testEntities.add(testEntity12);
                } else if (line.contains("Sdcard_1")) {
                    EXIST_EXTERNA_STORAGE = true;
                    if (!testEntities.contains(testEntity11)) {
                        testEntities.add(testEntity11);
                    }
                } else if (line.contains("USB_1")) {
                    EXIST_USB_STORAGE = true;
                    if (!testEntities.contains(testEntity11)) {
                        testEntities.add(testEntity11);
                    }
                } else if (line.contains("HeadSet_1")) {
                    EXIST_HEADSET = true;
                    if (!testEntities.contains(testEntity11)) {
                        testEntities.add(testEntity11);
                    }
                } else if (line.contains("Battery_1")) {
                    testEntities.add(testEntity13);
                } else if (line.contains("Rtc_1")) {
                    testEntities.add(testEntity14);
                }else if (line.contains("autotest_1")) {
                    findViewById(R.id.auto_test).setVisibility(View.VISIBLE);
                    findViewById(R.id.reset_factory).setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != bufReader) {
                    bufReader.close();
                    bufReader = null;
                }
                if (null != inputReader) {
                    inputReader.close();
                    inputReader = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            }
        }
        if (checkOK) {
            test_info.setText("PASS");
            main_v.setBackgroundResource(R.drawable.shape_actionsheet_green_normal);
        } else {
            test_info.setText("FAIL");
            main_v.setBackgroundResource(R.drawable.shape_actionsheet_top_normal);
        }
        saveTestResult2Sdcard(clear);

    }

    public void initTestFile(File file) {
        InputStreamReader inputReader = null;
        BufferedReader bufReader = null;
        //对测试文件进行读写
        BufferedWriter bufWriter = null;
        OutputStreamWriter outputStreamWriter = null;
        try {
            inputReader = new InputStreamReader(getResources().getAssets().open("config.txt"));
            bufReader = new BufferedReader(inputReader);
            //测试文件
            File testFile = MyTestUtils.INSTANCE.getTestFile();
            outputStreamWriter = new OutputStreamWriter(new FileOutputStream(testFile));
            bufWriter = new BufferedWriter(outputStreamWriter);
            String line = "";
            while ((line = bufReader.readLine()) != null) {
                if (line.contains("Touch_1")) {
                    bufWriter.append("Touch:");
                } else if (line.contains("Camera_1")) {
                    bufWriter.append("Camera:");
                } else if (line.contains("Record_1")) {
                    bufWriter.append("Record:");
                } else if (line.contains("Lcd_1")) {
                    bufWriter.append("Lcd:");
                } else if (line.contains("VideoTest_1")) {
                    bufWriter.append("VideoTest:");
                } else if (line.contains("Wifi_1")) {
                    bufWriter.append("Wifi:");
                } else if (line.contains("RemoteControl_1")) {
                    bufWriter.append("RemoteControl:");
                } else if (line.contains("Speaker_1")) {
                    bufWriter.append("Speaker:");
                } else if (line.contains("SystemInfo_1")) {
                    bufWriter.append("SystemInfo:");
                } else if (line.contains("G-sensor_1")) {
                    bufWriter.append("G-sensor:");
                } else if (line.contains("Human-sensor_1")) {
                    bufWriter.append("Human-sensor:");
                } else if (line.contains("Sdcard_1")) {
                    bufWriter.append("Sdcard:");
                    EXIST_EXTERNA_STORAGE = true;
                } else if (line.contains("USB_1")) {
                    bufWriter.append("USB:");
                    EXIST_USB_STORAGE = true;
                } else if (line.contains("HeadSet_1")) {
                    bufWriter.append("HeadSet:");
                    EXIST_HEADSET = true;
                } else if (line.contains("Battery_1")) {
                    bufWriter.append("Battery:");
                }

                bufWriter.newLine();
                bufWriter.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != bufReader) {
                    bufReader.close();
                    bufReader = null;
                }
                if (null != inputReader) {
                    inputReader.close();
                    inputReader = null;
                }

                if (null != bufWriter) {
                    bufWriter.close();
                    bufWriter = null;
                }

                if (null != outputStreamWriter) {
                    outputStreamWriter.close();
                    outputStreamWriter = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void initTestResult2Sdcard() {
        File file = new File(Environment.getExternalStorageDirectory(), "CloudFrame/Test/test.txt");
        if (!file.exists()) {
            initTestFile(file);
        }
    }

    /**
     * @param clear 清除文件后，还原测试文件
     */
    public String saveTestResult2Sdcard(boolean clear) {
        File file = new File(Environment.getExternalStorageDirectory(), "CloudFrame/Test/test.txt");
        if (clear) {
            file.delete();
            initTestResult2Sdcard();
            return "";
        }
        return saveTestResult2File(MyTestUtils.INSTANCE.getTestFile());
    }


    public String saveTestResult2File(File file) {
        StringBuffer testContent = new StringBuffer();
        //对测试文件进行读写
        BufferedWriter bufWriter = null;
        OutputStreamWriter outputStreamWriter = null;
        try {
            File testFile = null;
            String line = "";
            //测试文件
            try {
                testFile = MyTestUtils.INSTANCE.getTestFile();
                outputStreamWriter = new OutputStreamWriter(new FileOutputStream(testFile));
                bufWriter = new BufferedWriter(outputStreamWriter);
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (TestEntity testEntity : testEntities) {
                int result = SPUtils.getInstance().getInt(testEntity.getTag(), 0);
                String resultDetail = SPUtils.getInstance().getString(testEntity.getTag() + "_detail");
                line = testEntity.getTag();
                if (line.contains("TouchScreenTestActivity")) {
                    if (null != bufWriter)
                        bufWriter.append("Touch:" + result);
                    testContent.append("Touch:" + result);
                    testContent.append(",");
                } else if (line.contains("RecordActivity")) {
                    if (null != bufWriter)
                        bufWriter.append("Camera:" + result);
                    testContent.append("Camera:" + result);
                    testContent.append(",");
                } else if (line.contains("MusicSelActivity")) {
                    if (null != bufWriter)
                        bufWriter.append("Record:" + result);
                    testContent.append("Record:" + result);
                    testContent.append(",");
                } else if (line.contains("SlideTestActivity")) {
                    if (null != bufWriter)
                        bufWriter.append("Lcd:" + result);
                    testContent.append("Lcd:" + result);
                    testContent.append(",");
                } else if (line.contains("VideoViewTestActivity")) {
                    if (null != bufWriter)
                        bufWriter.append("VideoTest:" + result);
                    testContent.append("VideoTest:" + result);
                    testContent.append(",");
                } else if (line.contains("WifiTestActivity")) {
                    if (null != bufWriter)
                        bufWriter.append("Wifi:" + result);
                    testContent.append("Wifi:" + result);
                    testContent.append(",");
                    if (null != bufWriter) {
                        bufWriter.newLine();
                        bufWriter.append(resultDetail);
                    }
                    if (!TextUtils.isEmpty(resultDetail)) {
                        testContent.append(resultDetail);
                        testContent.append(",");
                    }
                } else if (line.contains("ControlTestActivity")) {
                    if (null != bufWriter)
                        bufWriter.append("RemoteControl:" + result);

                    testContent.append("RemoteControl:" + result);
                    testContent.append(",");
                } else if (line.contains("MusicTestActivity")) {
                    if (null != bufWriter)
                        bufWriter.append("Speaker:" + result);
                    testContent.append("Speaker:" + result);
                    testContent.append(",");
                } else if (line.contains("DeviceInfoTestActivity")) {
                    if (null != bufWriter)
                        bufWriter.append("SystemInfo:" + result);
                    testContent.append("SystemInfo:" + result);
                    testContent.append(",");
                    if (null != bufWriter) {
                        bufWriter.newLine();
                        bufWriter.append(resultDetail);
                    }
                    if (!TextUtils.isEmpty(resultDetail)) {
                        testContent.append(resultDetail);
                        testContent.append(",");
                    }
                } else if (line.contains("GsnsorViewAcitvity")) {
                    if (null != bufWriter)
                        bufWriter.append("G-sensor:" + result);
                    testContent.append("G-sensor:" + result);
                    testContent.append(",");
                } else if (line.contains("sensor_human")) {
                    if (null != bufWriter)
                        bufWriter.append("Human-sensor:" + result);
                    testContent.append("Human-sensor:" + result);
                    testContent.append(",");
                } else if (line.contains("BatteryInfoActivity")) {
                    if (null != bufWriter)
                        bufWriter.append("Battery:" + result);
                    testContent.append("Battery:" + result);
                    testContent.append(",");
                    if (null != bufWriter)
                        bufWriter.newLine();
                    if (!TextUtils.isEmpty(resultDetail)) {
                        testContent.append(resultDetail);
                        testContent.append(",");
                    }
                    if (null != bufWriter)
                        bufWriter.append(resultDetail);
                }
                if (null != bufWriter) {
                    bufWriter.newLine();
                    bufWriter.flush();
                }
            }

            if (EXIST_EXTERNA_STORAGE) {
                if (null != bufWriter)
                    bufWriter.append("Sdcard:" + SPUtils.getInstance().getInt("isExternalStorage", 0));
                testContent.append("Sdcard:" + SPUtils.getInstance().getInt("isExternalStorage", 0));
                testContent.append(",");
                if (null != bufWriter) {
                    bufWriter.newLine();
                    bufWriter.flush();
                }
            }
            if (EXIST_USB_STORAGE) {
                if (null != bufWriter) {
                    bufWriter.append("USB:" + SPUtils.getInstance().getInt("isUsbStorage", 0));
                }
                testContent.append("USB:" + SPUtils.getInstance().getInt("isUsbStorage", 0));
                testContent.append(",");
                if (null != bufWriter) {
                    bufWriter.newLine();
                    bufWriter.flush();
                }
            }
            if (EXIST_HEADSET) {
                if (null != bufWriter) {
                    bufWriter.append("HeadSet:" + SPUtils.getInstance().getInt("isHeadSet", 0));
                }
                testContent.append("HeadSet:" + SPUtils.getInstance().getInt("isHeadSet", 0));
                testContent.append(",");
                if (null != bufWriter) {
                    bufWriter.newLine();
                    bufWriter.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != bufWriter) {
                    bufWriter.close();
                    bufWriter = null;
                }

                if (null != outputStreamWriter) {
                    outputStreamWriter.close();
                    outputStreamWriter = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return testContent.toString();
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


    public boolean isRoot(RootResult rootResult) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            if (exitValue == 0) {
                Log.e("GGGGG","ROOT SUCCESS 22");
                rootResult.success();
                return true;
            } else {
                Log.e("GGGGG","ROOT Fail");
                rootResult.fail();
                return false;
            }
        } catch (Exception e) {
            rootResult.fail();
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    // 执行命令并且输出结果
    public static String execRootCmd(String cmd) {
        String result = "";
        DataOutputStream dos = null;
        DataInputStream dis = null;
        try {
            Process p = Runtime.getRuntime().exec("su");// 经过Root处理的android系统即有su命令
            dos = new DataOutputStream(p.getOutputStream());
            dis = new DataInputStream(p.getInputStream());

            dos.writeBytes(cmd + "\n");
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();
            String line = null;
            while ((line = dis.readLine()) != null) {
                Log.d("result", line);
                result += line;
            }
            int exitValue = p.waitFor();
            Log.e("GGGGG","exitValue "+exitValue);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
    interface RootResult {
        void success();

        void fail();
    }
}