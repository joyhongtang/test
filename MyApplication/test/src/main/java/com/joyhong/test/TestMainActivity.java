package com.joyhong.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.HumanSensor;
import android.provider.Settings;
import android.provider.SyncStateContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.joyhong.test.util.MyTestUtils;
import com.joyhong.test.util.TestConstant;
import com.joyhong.test.util.FileUtil;
import com.joyhong.test.widget.MaterialDialog;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import static com.joyhong.test.HomeTvAdapter.lastFocusPos;
import static com.joyhong.test.util.TestConstant.CATEGORY_POP_SELECT_POSITION;

public class TestMainActivity extends AppCompatActivity implements View.OnClickListener {
    private ArrayList<TestEntity> testEntities = new ArrayList<>();
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
    private static final String CHARGER_CURRENT_NOW =
            "/sys/class/power_supply/battery/BatteryAverageCurrent";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SPUtils.getInstance().put("stepinto_test", true);
        TestConstant.isConfigTestMode = true;
        testResult.clear();
        hideBottomUIMenu();  //隐藏底部虚拟按键
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
                }else if (line.contains("Battery_1")) {
                    testEntities.add(testEntity13);
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
        setContentView(R.layout.activity_test_main);
        main_v = findViewById(R.id.test_result_main);
        clear_result_main = findViewById(R.id.clear_result_main);
        test_info = findViewById(R.id.test_info);
        clear_result_main.setOnClickListener(this);
        for (TestEntity testEntityR : testEntities) {
            testResult.put(testEntityR.getTag(), testEntityR);
        }
        rv = findViewById(R.id.rv2);
        int spacing = 32; // 50px
        boolean includeEdge = true;
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
        checkFileExistOrCopy();
//        if (SPUtils.getInstance().getInt(humanSensorTag) != 1) {
        registerHumanSensor();
//        }

        initTestResult2Sdcard();
        try {
            Log.e("TTTTTT","readCurrentFile "+readCurrentFile(new File(CHARGER_CURRENT_NOW)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String readCurrentFile(File file) throws IOException {
        InputStream input = null;
        try {
            input  = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    input));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(null != input)
            input.close();
        }
        return "";
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
                            checkResult(true);
                            refresh();
                        }
                    })
                    .show();
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
            setMotionSensor(true);
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
                        FileUtil.copyAssetsFiles(TestConstant.application, "test", TestConstant.application.getExternalFilesDir(null).getAbsolutePath());
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

    public void initTestResult2Sdcard() {
        File file = new File(Environment.getExternalStorageDirectory(), "CloudFrame/Test/test.txt");
        if (file.exists()) {
            return;
        }
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
                }else if (line.contains("Battery_1")) {
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

    /**
     *
     * @param clear 清除文件后，还原测试文件
     */
    public void saveTestResult2Sdcard(boolean clear) {
        File file = new File(Environment.getExternalStorageDirectory(), "CloudFrame/Test/test.txt");
        if (clear) {
            file.delete();
            initTestResult2Sdcard();
            return;
        }
        //对测试文件进行读写
        BufferedWriter bufWriter = null;
        OutputStreamWriter outputStreamWriter = null;
        try {
            //测试文件
            File testFile = MyTestUtils.INSTANCE.getTestFile();
            outputStreamWriter = new OutputStreamWriter(new FileOutputStream(testFile));
            bufWriter = new BufferedWriter(outputStreamWriter);
            String line = "";
            for (TestEntity testEntity : testEntities) {
                int result = SPUtils.getInstance().getInt(testEntity.getTag(), 0);
                String resultDetail = SPUtils.getInstance().getString(testEntity.getTag()+"_detail");
                line = testEntity.getTag();
                if (line.contains("TouchScreenTestActivity")) {
                    bufWriter.append("Touch:"+result);
                } else if (line.contains("RecordActivity")) {
                    bufWriter.append("Camera:"+result);
                } else if (line.contains("MusicSelActivity")) {
                    bufWriter.append("Record:"+result);
                } else if (line.contains("SlideTestActivity")) {
                    bufWriter.append("Lcd:"+result);
                } else if (line.contains("VideoViewTestActivity")) {
                    bufWriter.append("VideoTest:"+result);
                } else if (line.contains("WifiTestActivity")) {
                    bufWriter.append("Wifi:"+result);
                    bufWriter.newLine();
                    bufWriter.append(resultDetail);
                } else if (line.contains("ControlTestActivity")) {
                    bufWriter.append("RemoteControl:"+result);
                } else if (line.contains("MusicTestActivity")) {
                    bufWriter.append("Speaker:"+result);
                } else if (line.contains("DeviceInfoTestActivity")) {
                    bufWriter.append("SystemInfo:"+result);
                    bufWriter.newLine();
                    bufWriter.append(resultDetail);
                } else if (line.contains("GsnsorViewAcitvity")) {
                    bufWriter.append("G-sensor:"+result);
                } else if (line.contains("sensor_human")) {
                    bufWriter.append("Human-sensor:"+result);
                } else if (line.contains("BatteryInfoActivity")) {
                    bufWriter.append("Battery:"+result);
                    bufWriter.newLine();
                    bufWriter.append(resultDetail);
                }
                bufWriter.newLine();
                bufWriter.flush();
            }

            if (EXIST_EXTERNA_STORAGE) {
                bufWriter.append("Sdcard:"+SPUtils.getInstance().getInt("isExternalStorage", 0));
                bufWriter.newLine();
                bufWriter.flush();
            }
            if (EXIST_USB_STORAGE) {
                bufWriter.append("USB:"+SPUtils.getInstance().getInt("isUsbStorage", 0));
                bufWriter.newLine();
                bufWriter.flush();
            }
            if (EXIST_HEADSET) {
                bufWriter.append("HeadSet:"+SPUtils.getInstance().getInt("isHeadSet", 0));
                bufWriter.newLine();
                bufWriter.flush();
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
    }

    @Override
    protected void onDestroy() {
        TestConstant.isConfigTestMode = false;
        try {
            unRegisterHumanSensor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }



    private boolean openSensor = false;
    private void setMotionSensor(boolean open) {
        openSensor = true;
        try {
            Class.forName("android.os.HumanSensor");
            HumanSensor.setMode(open);
        } catch (Exception e) {
            FileWriter fileWriter = null;
            try {
                File file = new File("/data/data/com.idwell.cloudframe/sleepmode.txt");
                if (!file.exists()) {
                    file.createNewFile();
                }
                fileWriter = new FileWriter(file);
                if (open) {
                    fileWriter.write("300");
                } else {
                    fileWriter.write("200");
                }
                fileWriter.close();
            } catch (Exception e2) {
                try {
                    fileWriter.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (!open) {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, Integer.MAX_VALUE);
        } else
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 15_000);
    }
}