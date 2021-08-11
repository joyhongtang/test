package com.joyhong.test.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.joyhong.test.service.TestService;

import java.util.List;

/**
 * wifi设置工具类
 *
 */
public class WifiUtil {
    // 定义WifiManager对象
    private WifiManager mWifiManager;
    private DhcpInfo dhcpInfo;
    private List<WifiConfiguration> mWifiConfigurations;
    private ConnectivityManager mConnectivityManager;
    public WifiManager getmWifiManager() {
        return mWifiManager;
    }

    // 定义WifiInfo对象
    private WifiInfo mWifiInfo;
    Context context;
    // 扫描出的网络连接列表
    private List<ScanResult> mWifiList;

    // 构造器
    public WifiUtil(Context context) {
        // 取得WifiManager对象
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        // 取得WifiInfo对象
        mWifiInfo = mWifiManager.getConnectionInfo();
        dhcpInfo  = mWifiManager.getDhcpInfo();
        mWifiConfigurations=mWifiManager.getConfiguredNetworks();
        mConnectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.context = context;

    }
    public void startScan(){
        mWifiManager.startScan();
        mWifiList=mWifiManager.getScanResults();
    }
    /**
     * 获取扫描到的wifi列表
     */
    public List<ScanResult> getWifiList(){
        return mWifiList;
    }
    // 得到MAC地址
    public String GetMacAddress() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getMacAddress();
    }

    // 得到接入点的BSSID
    public String GetBSSID() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getBSSID();
    }

    // 得到接入点的SSID
    public String GetSSID() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getSSID();
    }

    // 得到接入点的IP地址
    public String getIPAddress() {
        return (mWifiInfo == null) ? "NULL" : intToIp(dhcpInfo.ipAddress);
    }

    // 得到接入点的子网掩码
    public String getNetMask() {
        return (mWifiInfo == null) ? "NULL" : intToIp(dhcpInfo.netmask);
    }

    // 关闭WIFI
    public void CloseWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }
    //判断wifi是否连接
    public static boolean isWifiConnect(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context
                .getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.getState() == NetworkInfo.State.CONNECTED ? true : false;
    }

    public String getSSID() {
        if (mWifiInfo != null) {
            String temp = mWifiInfo.getSSID();
            if (temp != null && (temp.startsWith("\"") && temp.endsWith("\""))) {
                temp = temp.substring(1, temp.length() - 1);
                return temp;
            }
            return temp;
        } else {
            return "";
        }
    }

    // 定义几种加密方式，一种是WEP，一种是WPA，还有没有密码的情况
    /*
     * public enum WifiCipherType { NONE,WEP,WPA,EAP }
     */

    // 判断wifi是否加密：
    public static String getSecuritys(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return "WEP";
        } else if (result.capabilities.contains("PSK")) {
            return "WPA";
        } else if (result.capabilities.contains("EAP")) {
            return "EAP";
        }
        return "NONE";
    }
    //获取wifi是那个加密类型
    public String getWifiCipherType(Context context, String ssid) {
        String type = null;
        mWifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        mWifiList = mWifiManager.getScanResults();
        if (mWifiList != null) {
            for (int i = 0; i < mWifiList.size(); i++) {
                ScanResult sr = mWifiList.get(i);
                if (sr.SSID.equals(ssid))
                    type = getSecuritys(sr);
            }
        }
        if (type == null) {
            type = "WPA";
        }
        return type;
    }
    //获取wifi的信号大小
    public int getSignal(Context context, String ssid) {
        int signal = 100;
        mWifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        mWifiList = mWifiManager.getScanResults();
        if (mWifiList != null) {
            for (int i = 0; i < mWifiList.size(); i++) {
                ScanResult sr = mWifiList.get(i);
                if (sr.SSID.equals(ssid))
                    signal = Math.abs(sr.level);
            }
        }

        return signal;
    }
    // 打开WIFI
    public boolean OpenWifi() {
        boolean existWifi = false;
        if (!mWifiManager.isWifiEnabled()) {
             mWifiManager.setWifiEnabled(true);
            while (mWifiList == null || mWifiList.size() == 0) {
                try {
                    mWifiManager.startScan();
                    mWifiList = mWifiManager.getScanResults();
                    for(ScanResult scanResult : mWifiList){
                        if(scanResult.SSID.contains(TestService.WIFI_NAME)){
                            existWifi = true;
                            break;
                        }
                    }
                    Thread.currentThread().sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return existWifi;
    }
    //断判某个wifi是否是连接成功的那个wifi
    public boolean isConnectedWifi(Context context, String myssid){
        mWifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        if (isWifiConnect(context)) {
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            if (ssid != null && ssid.contains(myssid)) {
                return true;
            } else {
                return false;
            }
        }else{
            return false;
        }
    }
    // 查看以前是否也配置过这个网络
    private WifiConfiguration IsExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager
                .getConfiguredNetworks();
        if (existingConfigs != null) {
            for (WifiConfiguration existingConfig : existingConfigs) {
                if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                    return existingConfig;
                }
            }
        }
        return null;
    }
    //删除原来的所有连接
    private void deleteWifiConnect() {
        List<WifiConfiguration> existingConfigs = mWifiManager
                .getConfiguredNetworks();
        if (existingConfigs != null) {
            for (WifiConfiguration existingConfig : existingConfigs) {
                mWifiManager.disableNetwork(existingConfig.networkId);
            }

        }
    }

    // 提供一个外部接口，传入要连接的无线网
    public boolean connectNet(String SSID, String Password, String Type) {
        WifiConfiguration tempConfig = this.IsExsits(SSID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }
//    deleteWifiConnect();
//    forgetWifi(SSID);
        WifiConfiguration wifiConfig = this
                .CreateWifiInfo(SSID, Password, Type);
        int netID = mWifiManager.addNetwork(wifiConfig);
        boolean bRet = mWifiManager.enableNetwork(netID, true);
        mWifiManager.reconnect();
        return bRet;
    }
    public boolean connectNewNet(String SSID, String Password, int Type){
        WifiConfiguration configuration = this
                .createWifiInfo(SSID, Password, Type);
        int wcgId=mWifiManager.addNetwork(configuration);
        mWifiManager.enableNetwork(wcgId, true);
        boolean reconnect = mWifiManager.reconnect();
        return reconnect;
    }

    private WifiConfiguration CreateWifiInfo(String SSID, String Password,
                                             String Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        if (Type.equals("NONE")) {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (Type.equals("WEP")) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (Type.equals("WPA")) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);// 对应wpa2加密方式
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);// 对应wpa加密方式
            config.status = WifiConfiguration.Status.ENABLED;
        } else if (Type.equals("EAP")) {
            config.preSharedKey = "\"" + Password + "\"";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
            config.allowedKeyManagement
                    .set(WifiConfiguration.KeyMgmt.IEEE8021X);// 20120723新增
        }
        return config;
    }
    public WifiConfiguration createWifiInfo(String SSID, String Password, int Type){
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        if(Type == 1) //WIFICIPHER_NOPASS
        {
            config.hiddenSSID = true;
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }

        if(Type == 2) //WIFICIPHER_WEP
        {
            config.hiddenSSID = true;
            config.wepKeys[0]= "\""+Password+"\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if(Type == 3) //WIFICIPHER_WPA
        {
            config.preSharedKey = "\""+Password+"\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }
    public void addNetWork(WifiConfiguration configuration){

        //mWifiManager.saveConfiguration();
    }
    /**
     * 移除wifi，因为权限，无法移除的时候，需要手动去翻wifi列表删除
     * 注意：！！！只能移除自己应用创建的wifi。
     * 删除掉app，再安装的，都不算自己应用，具体看removeNetwork源码
     *
     */
    public boolean forgetWifi(String SSID){
        WifiConfiguration tempConfig = this.IsExsits(SSID);
        if (tempConfig != null) {
            Log.d("howard","tempConfig.networkId="+tempConfig.networkId);
            return  mWifiManager.removeNetwork(tempConfig.networkId);
            //mWifiManager.saveConfiguration();
        }
        return false;
    }

    private String intToIp(int paramInt) {
        return (paramInt & 0xFF) + "." + (0xFF & paramInt >> 8) + "." + (0xFF & paramInt >> 16) + "."
                + (0xFF & paramInt >> 24);
    }
    public boolean addNetWork(String SSID, String password, int Type)
    {
        int netId = -1;
    /*先执行删除wifi操作，1.如果删除的成功说明这个wifi配置是由本APP配置出来的；
                       2.这样可以避免密码错误之后，同名字的wifi配置存在，无法连接；
                       3.wifi直接连接成功过，不删除也能用, netId = getExitsWifiConfig(SSID).networkId;*/
        if (forgetWifi(SSID))
        {
            //移除成功，就新建一个
            netId = mWifiManager.addNetwork(createWifiInfo(SSID, password, Type));
        } else
        {
            //删除不成功，要么这个wifi配置以前就存在过，要么是还没连接过的
            if (getExitsWifiConfig(SSID) != null)
            {
                //这个wifi是连接过的，如果这个wifi在连接之后改了密码，那就只能手动去删除了
                netId = getExitsWifiConfig(SSID).networkId;
            } else
            {
                //没连接过的，新建一个wifi配置
                netId = mWifiManager.addNetwork(createWifiInfo(SSID, password, Type));
            }
        }

        //这个方法的第一个参数是需要连接wifi网络的networkId，第二个参数是指连接当前wifi网络是否需要断开其他网络
        //无论是否连接上，都返回true。。。。
        boolean enableNetwork = mWifiManager.enableNetwork(netId, true);
        return enableNetwork;
    }

    /**
     * 获取配置过的wifiConfiguration
     */
    public WifiConfiguration getExitsWifiConfig(String SSID)
    {
        List<WifiConfiguration> wifiConfigurationList = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration wifiConfiguration : wifiConfigurationList)
        {
            if (wifiConfiguration.SSID.equals("\"" + SSID + "\""))
            {
                return wifiConfiguration;
            }
        }
        return null;
    }

}