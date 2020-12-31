package com.joyhong.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.SPUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.joyhong.test.device.DeviceInfoItem;
import com.joyhong.test.util.TestConstant;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static com.joyhong.test.TestMainActivity.testResult;

public class BatteryInfoActivity extends AppCompatActivity implements OnClickListener {
    // 定义电池信息的按钮
    private Button btnBattery;
    // 定义显示电池信息的textview
    private TextView tvBattery;

    private BaseQuickAdapter<DeviceInfoItem, BaseViewHolder> mMusicAdapter;
    private ArrayList mDeviceInfo = new ArrayList();
    private androidx.recyclerview.widget.LinearLayoutManager mLinearLayoutManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.battery_info);
        // 得到布局中的所有对象
        findView();
        // 设置对象的监听器
//        setListener();
        findViewById(R.id.pass).setOnClickListener(this);
        findViewById(R.id.fail).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);
    }

    private void findView() {
        // 得到布局中的所有对象
        btnBattery = (Button) findViewById(R.id.btn_battery);
        tvBattery = (TextView) findViewById(R.id.tv_battery);
    }

    // 设置对象的监听器
    private void setListener() {
        btnBattery.setOnClickListener(listener);
    }

    OnClickListener listener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            // 当前的音量
            if (v.getId() == R.id.btn_battery) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_BATTERY_CHANGED);
                registerReceiver(mBroadcastReceiver, filter);
            }
        }
    };
    // 声明广播接受者对象
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                // 得到电池状态：
                // BatteryManager.BATTERY_STATUS_CHARGING：充电状态。
                // BatteryManager.BATTERY_STATUS_DISCHARGING：放电状态。
                // BatteryManager.BATTERY_STATUS_NOT_CHARGING：未充满。
                // BatteryManager.BATTERY_STATUS_FULL：充满电。
                // BatteryManager.BATTERY_STATUS_UNKNOWN：未知状态。
                int status = intent.getIntExtra("status", 0);
                // 得到健康状态：
                // BatteryManager.BATTERY_HEALTH_GOOD：状态良好。
                // BatteryManager.BATTERY_HEALTH_DEAD：电池没有电。
                // BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE：电池电压过高。
                // BatteryManager.BATTERY_HEALTH_OVERHEAT：电池过热。
                // BatteryManager.BATTERY_HEALTH_UNKNOWN：未知状态。
                int health = intent.getIntExtra("health", 0);
                // boolean类型
                boolean present = intent.getBooleanExtra("present", false);
                // 得到电池剩余容量
                int level = intent.getIntExtra("level", 0);
                // 得到电池最大值。通常为100。
                int scale = intent.getIntExtra("scale", 0);
                // 得到图标ID
                int icon_small = intent.getIntExtra("icon-small", 0);
                // 充电方式：　BatteryManager.BATTERY_PLUGGED_AC：AC充电。　BatteryManager.BATTERY_PLUGGED_USB：USB充电。
                int plugged = intent.getIntExtra("plugged", 0);
                String pluggedInfo = "UNKNOW";
                if (plugged == BatteryManager.BATTERY_PLUGGED_AC) {
                    pluggedInfo = "AC";
                } else if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
                    pluggedInfo = "USB";
                }
                // 得到电池的电压
                int voltage = intent.getIntExtra("voltage", 0);
                // 得到电池的温度,0.1度单位。例如 表示197的时候，意思为19.7度
                int temperature = intent.getIntExtra("temperature", 0);
                // 得到电池的类型
                String technology = intent.getStringExtra("technology");
                // 得到电池状态
                String statusString = "";
                // 根据状态id，得到状态字符串
                switch (status) {
                    case BatteryManager.BATTERY_STATUS_UNKNOWN:
                        statusString = "UNKNOWN";
                        break;
                    case BatteryManager.BATTERY_STATUS_CHARGING:
                        statusString = "CHARGING";
                        break;
                    case BatteryManager.BATTERY_STATUS_DISCHARGING:
                        statusString = "DISCHARGING";
                        break;
                    case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                        statusString = "NOT_CHARGING";
                        break;
                    case BatteryManager.BATTERY_STATUS_FULL:
                        statusString = "FULL";
                        break;
                }
                //得到电池的寿命状态
                String healthString = "";
                //根据状态id，得到电池寿命
                switch (health) {
                    case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                        healthString = "UNKNOWN";
                        break;
                    case BatteryManager.BATTERY_HEALTH_GOOD:
                        healthString = "GOOD";
                        break;
                    case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                        healthString = "OVERHEAT";
                        break;
                    case BatteryManager.BATTERY_HEALTH_DEAD:
                        healthString = "HEALTH_DEAD";
                        break;
                    case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                        healthString = "OVER_VOLTAGE";
                        break;
                    case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                        healthString = "UNSPECIFIED_FAILURE";
                        break;
                }
                //得到充电模式
                String acString = "";
                //根据充电状态id，得到充电模式
                switch (plugged) {
                    case BatteryManager.BATTERY_PLUGGED_AC:
                        acString = "AC 充电";
                        break;
                    case BatteryManager.BATTERY_PLUGGED_USB:
                        acString = "USB 充电";
                        break;
                }
                String batteryInfo = "\n电池的状态：" + statusString
                        + "\n健康值:" + healthString
                        + "\n电池剩余容量： " + level
                        + "\n电池的最大值：" + scale
                        + "\n充电方式：" + pluggedInfo
                        + "\n电池的电压：" + voltage
                        + "\n电池的温度：" + (float) temperature * 0.1
                        + "\n电池的类型：" + technology;
                String batteryInfoDetail = "\nBattery_status:" + statusString
                        + "\nBattery_health:" + healthString
                        + "\nBattery_level:" + level
                        + "\nBattery_max:" + scale
                        + "\nBattery_plugged:" + pluggedInfo
                        + "\nBattery_voltage:" + voltage
                        + "\nBattery_temperature:" + (float) temperature * 0.1
                        + "\nBattery_technology:" + technology;

                TestEntity testEntity2 = testResult.get(TestConstant.PACKAGE_NAME + getLocalClassName());
                SPUtils.getInstance().put(testEntity2.getTag() + "_detail", batteryInfoDetail);
//                try {
//                    String mode1 = readTextFile(new File(CHARGER_CURRENT_NOW), 0, null).trim();
//                    batteryInfo += "\n电池的充电类型：" + mode1;
//                } catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }

                if (health == BatteryManager.BATTERY_HEALTH_GOOD && level >= 80 && scale >= 95 && plugged == BatteryManager.BATTERY_PLUGGED_AC) {
                    findViewById(R.id.pass).setBackgroundColor(Color.parseColor("#1AAD19"));
                    findViewById(R.id.pass).setEnabled(true);
                }else{
                    findViewById(R.id.pass).setBackgroundColor(Color.parseColor("#1AFFFFFF"));
                    findViewById(R.id.pass).setEnabled(false);
                }
                //显示电池信息
                tvBattery.setText(batteryInfo);
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        // 解除注册监听
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.pass) {
            TestEntity testEntity = testResult.get(TestConstant.PACKAGE_NAME + getLocalClassName());
            testEntity.setTestResultEnum(TestResultEnum.PASS);
            SPUtils.getInstance().put(testEntity.getTag(), 1);
            finish();
        } else if (view.getId() == R.id.fail) {
            TestEntity testEntity2 = testResult.get(TestConstant.PACKAGE_NAME + getLocalClassName());
            testEntity2.setTestResultEnum(TestResultEnum.FAIL);
            SPUtils.getInstance().put(testEntity2.getTag(), 0);
            finish();
        }
    }

    private int TIME = 2000;
    static final String TAG = "readsysclass";
    private static final String CHARGER_CURRENT_NOW =
            "/sys/class/power_supply/battery/current_now";

    /**
     * Read a text file into a String, optionally limiting the length.
     *
     * @param file     to read (will not seek, so things like /proc files are OK)
     * @param max      length (positive for head, negative of tail, 0 for no limit)
     * @param ellipsis to add of the file was truncated (can be null)
     * @return the contents of the file, possibly truncated
     * @throws IOException if something goes wrong reading the file
     */
    public static String readTextFile(File file, int max, String ellipsis) throws IOException {
        InputStream input = new FileInputStream(file);
        try {
            long size = file.length();
            if (max > 0 || (size > 0 && max == 0)) {  // "head" mode: read the first N bytes
                if (size > 0 && (max == 0 || size < max)) max = (int) size;
                byte[] data = new byte[max + 1];
                int length = input.read(data);
                if (length <= 0) return "";
                if (length <= max) return new String(data, 0, length);
                if (ellipsis == null) return new String(data, 0, max);
                return new String(data, 0, max) + ellipsis;
            } else if (max < 0) {  // "tail" mode: keep the last N
                int len;
                boolean rolled = false;
                byte[] last = null, data = null;
                do {
                    if (last != null) rolled = true;
                    byte[] tmp = last;
                    last = data;
                    data = tmp;
                    if (data == null) data = new byte[-max];
                    len = input.read(data);
                } while (len == data.length);

                if (last == null && len <= 0) return "";
                if (last == null) return new String(data, 0, len);
                if (len > 0) {
                    rolled = true;
                    System.arraycopy(last, len, last, 0, last.length - len);
                    System.arraycopy(data, 0, last, last.length - len, len);
                }
                if (ellipsis == null || !rolled) return new String(last);
                return ellipsis + new String(last);
            } else {  // "cat" mode: size unknown, read it all in streaming fashion
                ByteArrayOutputStream contents = new ByteArrayOutputStream();
                int len;
                byte[] data = new byte[1024];
                do {
                    len = input.read(data);
                    if (len > 0) contents.write(data, 0, len);
                } while (len == data.length);
                return contents.toString();
            }
        } finally {
            input.close();
        }
    }
}

