package com.joyhong.test.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.PermissionUtils;
import com.joyhong.test.util.TestConstant;
import com.joyhong.test.wifi.WifiUtil;

public class TestService extends Service {
    public static final String ACTION_OPEN_WIFI = "com.idwell.action.openwifi";
    public static final String ACTION_CLOSE_WIFI = "com.idwell.action.closewifi";
    public WifiUtil mWifiUtil;
    public static final String WIFI_NAME = "Q-LINK";
    public static final String WIFI_PWD = "1234567890";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (null == mWifiUtil) {
            mWifiUtil = new WifiUtil(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(null == intent){
            return super.onStartCommand(intent, flags, startId);
        }
        String action = intent.getAction();
        if(!TextUtils.isEmpty(TestConstant.deviceToken)){
            return super.onStartCommand(intent, flags, startId);
        }
        if (TextUtils.equals(action, ACTION_OPEN_WIFI)) {
            PermissionUtils.permission(PermissionConstants.LOCATION)
                    .callback(new PermissionUtils.SimpleCallback() {
                        @Override
                        public void onGranted() {
                            mWifiUtil.OpenWifi();
                            boolean connectNet = mWifiUtil.addNetWork(WIFI_NAME, WIFI_PWD, 3);
                        }
                        @Override
                        public void onDenied() {

                        }
                    }).request();

        } else if (TextUtils.equals(action, ACTION_CLOSE_WIFI)) {
            PermissionUtils.permission(PermissionConstants.LOCATION)
                    .callback(new PermissionUtils.SimpleCallback() {
                        @Override
                        public void onGranted() {
                            mWifiUtil.CloseWifi();
                        }
                        @Override
                        public void onDenied() {

                        }
                    }).request();
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
