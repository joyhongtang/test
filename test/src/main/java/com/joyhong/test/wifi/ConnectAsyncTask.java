package com.joyhong.test.wifi;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

/**
 * 连接指定的wifi
 */
public class ConnectAsyncTask extends AsyncTask<Void, Void, Boolean> {
    private WifiAutoConnectManager mWifiAutoConnectManager;
    private String ssid;
    private String password;
    private WifiAutoConnectManager.WifiCipherType type;
    WifiConfiguration tempConfig;
    boolean isLinked = false;
    public ConnectAsyncTask(String ssid, String password, WifiAutoConnectManager.WifiCipherType type) {
        this.ssid = ssid;
        this.password = password;
        this.type = type;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        // 打开wifi
        mWifiAutoConnectManager.openWifi();
        // 开启wifi功能需要一段时间(我在手机上测试一般需要1-3秒左右)，所以要等到wifi
        // 状态变成WIFI_STATE_ENABLED的时候才能执行下面的语句
        while (mWifiAutoConnectManager.wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
            try {
                // 为了避免程序一直while循环，让它睡个100毫秒检测……
                Thread.sleep(100);

            } catch (InterruptedException ie) {
                Log.e("wifidemo", ie.toString());
            }
        }

        tempConfig = mWifiAutoConnectManager.isExsits(ssid);
        //禁掉所有wifi
        for (WifiConfiguration c : mWifiAutoConnectManager.wifiManager.getConfiguredNetworks()) {
            mWifiAutoConnectManager.wifiManager.disableNetwork(c.networkId);
        }
        if (tempConfig != null) {
            Log.d("wifidemo", ssid + "配置过！");
            boolean result = mWifiAutoConnectManager.wifiManager.enableNetwork(tempConfig.networkId, true);
            if (!isLinked && type != WifiAutoConnectManager.WifiCipherType.WIFICIPHER_NOPASS) {
                try {
                    Thread.sleep(5000);//超过5s提示失败
                    if (!isLinked) {
                        Log.d("wifidemo", ssid + "连接失败！");
                        mWifiAutoConnectManager.wifiManager.disableNetwork(tempConfig.networkId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.d("wifidemo", "result=" + result);
            return result;
        } else {
            Log.d("wifidemo", ssid + "没有配置过！");
            if (type != WifiAutoConnectManager.WifiCipherType.WIFICIPHER_NOPASS) {
            } else {
                WifiConfiguration wifiConfig = mWifiAutoConnectManager.createWifiInfo(ssid, password, type);
                if (wifiConfig == null) {
                    Log.d("wifidemo", "wifiConfig is null!");
                    return false;
                }
                Log.d("wifidemo", wifiConfig.SSID);
                int netID = mWifiAutoConnectManager.wifiManager.addNetwork(wifiConfig);
                boolean enabled = mWifiAutoConnectManager.wifiManager.enableNetwork(netID, true);
                Log.d("wifidemo", "enableNetwork status enable=" + enabled);
//                    Log.d("wifidemo", "enableNetwork connected=" + mWifiAutoConnectManager.wifiManager.reconnect());
//                    return mWifiAutoConnectManager.wifiManager.reconnect();
                return enabled;
            }
            return false;


        }
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
}
}