package com.idwell.cloudframe.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiManager;
import android.os.Build;
import androidx.annotation.RequiresPermission;

import com.blankj.utilcode.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;

/**
 * author : chason
 * mailbox : 156874547@qq.com
 * time : 2018/3/25 18:40
 * version : 1.0
 * describe :
 */

public class NetworkUtil {

    private static final int SECURITY_NONE = 0;
    private static final int SECURITY_WEP = 1;
    private static final int SECURITY_PSK = 2;
    private static final int SECURITY_EAP = 3;

    //网络相关
    public static WifiManager getWifiManager() {
        @SuppressLint("WifiManagerLeak")
        WifiManager wifiManager = (WifiManager) Utils.getApp().getSystemService(Context.WIFI_SERVICE);
        return wifiManager;
    }

    public static List<ScanResult> getScanResults() {
        @SuppressLint("WifiManagerLeak")
        WifiManager wifiManager = (WifiManager) Utils.getApp().getSystemService(Context.WIFI_SERVICE);
        //得到扫描结果
        List<ScanResult> results = new ArrayList<>();
        List<ScanResult> mResults = new ArrayList<>();
        results.addAll(wifiManager.getScanResults());
        for (int i = 0; i < results.size(); i++) {
            ScanResult result = results.get(i);
            if (!result.SSID.isEmpty() && !result.capabilities.contains("[IBSS]")) {
                boolean isSame = false;
                for (int j = 0; j < mResults.size(); j++) {
                    if (result.SSID.equals(mResults.get(j).SSID)) {
                        isSame = true;
                        break;
                    }
                }
                if (!isSame) {
                    mResults.add(result);
                }
            }
        }
        sortByLevel(mResults);
        return mResults;
    }

    /**
     * 将搜索到的wifi根据信号强度从强到时弱进行排序
     *
     * @param list 存放周围wifi热点对象的列表
     */
    public static void sortByLevel(List<ScanResult> list) {

        Collections.sort(list, new Comparator<ScanResult>() {

            @Override
            public int compare(ScanResult lhs, ScanResult rhs) {
                return rhs.level - lhs.level;
            }
        });
    }

    @RequiresPermission(ACCESS_NETWORK_STATE)
    public static NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager manager =
                (ConnectivityManager) Utils.getApp().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return null;
        return manager.getActiveNetworkInfo();
    }

    public static WifiConfiguration isExsits(String SSID) {
        @SuppressLint("WifiManagerLeak")
        WifiManager wifiManager = (WifiManager) Utils.getApp().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    //创建wifi热点信息
    public static WifiConfiguration getConfig(ScanResult result, String password, int security) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + result.SSID + "\"";
        switch (security) {
            case SECURITY_NONE:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
            case SECURITY_WEP:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                int length = password.length();
                // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                if ((length == 10 || length == 26 || length == 58) &&
                        password.matches("[0-9A-Fa-f]*")) {
                    config.wepKeys[0] = password;
                } else {
                    config.wepKeys[0] = '"' + password + '"';
                }
                break;
            case SECURITY_PSK:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                if (password.matches("[0-9A-Fa-f]{64}")) {
                    config.preSharedKey = password;
                } else {
                    config.preSharedKey = '"' + password + '"';
                }
                break;
            case SECURITY_EAP:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
                config.enterpriseConfig = new WifiEnterpriseConfig();
                config.enterpriseConfig.setPassword(password);
                break;
            default:
                return null;
        }
        return config;
    }

    // 添加一个网络并连接
    public static void connectWifi(WifiConfiguration config) {
        @SuppressLint("WifiManagerLeak")
        WifiManager wifiManager = (WifiManager) Utils.getApp().getSystemService(Context.WIFI_SERVICE);
        int configID = wifiManager.addNetwork(config);
        wifiManager.enableNetwork(configID, true);
        wifiManager.saveConfiguration();
    }

    /**
     * 通过反射出不同版本的connect方法来连接Wifi
     *
     * @param netId
     * @return Method
     * @since MT 1.0
     */
    public static Method connectWifiByReflectMethod(int netId) {
        @SuppressLint("WifiManagerLeak")
        WifiManager wifiManager = (WifiManager) Utils.getApp().getSystemService(Context.WIFI_SERVICE);
        Method connectMethod = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // 反射方法： connect(int, listener) , 4.2 <= phone's android version
            for (Method methodSub : wifiManager.getClass()
                    .getDeclaredMethods()) {
                if ("connect".equalsIgnoreCase(methodSub.getName())) {
                    Class<?>[] types = methodSub.getParameterTypes();
                    if (types != null && types.length > 0) {
                        if ("int".equalsIgnoreCase(types[0].getName())) {
                            connectMethod = methodSub;
                        }
                    }
                }
            }
            if (connectMethod != null) {
                try {
                    connectMethod.invoke(wifiManager, netId, null);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            // 反射方法: connect(Channel c, int networkId, ActionListener listener)
            // 暂时不处理4.1的情况 , 4.1 == phone's android version
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            // 反射方法：connectWifi(int networkId) ,
            // 4.0 <= phone's android version < 4.1
            for (Method methodSub : wifiManager.getClass()
                    .getDeclaredMethods()) {
                if ("connectWifi".equalsIgnoreCase(methodSub.getName())) {
                    Class<?>[] types = methodSub.getParameterTypes();
                    if (types != null && types.length > 0) {
                        if ("int".equalsIgnoreCase(types[0].getName())) {
                            connectMethod = methodSub;
                        }
                    }
                }
            }
            if (connectMethod != null) {
                try {
                    connectMethod.invoke(wifiManager, netId);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        } else {
            // < android 4.0
            return null;
        }
        return connectMethod;
    }

    /**
     * 向谷歌服务器根据附近wifi请求位置的json
     */
    public static JSONObject getWifiAccessPoints() {
        @SuppressLint("WifiManagerLeak")
        WifiManager manager = (WifiManager) Utils.getApp().getSystemService(Context.WIFI_SERVICE);
        if (manager == null) return null;
        manager.startScan();
        List<ScanResult> scanResults = getScanResults();
        if (scanResults.size() < 2) return null;
        JSONArray jsonArray = new JSONArray();
        for(int i = 0; i < 2; i++) {
            ScanResult result = scanResults.get(i);
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("macAddress",result.BSSID);
                jsonObject.put("signalStrength",result.level);
                int channel = getChannelByFrequency(result.frequency);
                if(channel != -1) {
                    jsonObject.put("channel",channel);
                }
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("wifiAccessPoints",jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * 根据频率获得信道
     * @param frequency 频率
     * @return 信道
     */
    private static int getChannelByFrequency(int frequency) {
        int channel = -1;
        switch (frequency) {
            case 2412:
                channel = 1;
                break;
            case 2417:
                channel = 2;
                break;
            case 2422:
                channel = 3;
                break;
            case 2427:
                channel = 4;
                break;
            case 2432:
                channel = 5;
                break;
            case 2437:
                channel = 6;
                break;
            case 2442:
                channel = 7;
                break;
            case 2447:
                channel = 8;
                break;
            case 2452:
                channel = 9;
                break;
            case 2457:
                channel = 10;
                break;
            case 2462:
                channel = 11;
                break;
            case 2467:
                channel = 12;
                break;
            case 2472:
                channel = 13;
                break;
            case 2484:
                channel = 14;
                break;
            case 5745:
                channel = 149;
                break;
            case 5765:
                channel = 153;
                break;
            case 5785:
                channel = 157;
                break;
            case 5805:
                channel = 161;
                break;
            case 5825:
                channel = 165;
                break;
        }
        return channel;
    }
}
