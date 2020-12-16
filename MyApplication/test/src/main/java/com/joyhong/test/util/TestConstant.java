package com.joyhong.test.util;

import android.app.Application;

public class TestConstant {
    public static final String CATEGORY_POP_SELECT_POSITION = "category_select_position";
    public  static boolean isConfigTestMode = false;
    public static Application application;
    public static String deviceToken,snnumber;
    public static String RSSI_NOT_EXIST =  "android.net.wifi.RSSI_NOT_EXIST";
    //com.joyhong.test
    public static String PACKAGE_NAME = "";
    public static boolean isDualScreen =false;
    public static void initTest(Application mapplication,String mdeviceToken,String msnnumber){
        application = mapplication;
        deviceToken = mdeviceToken;
        snnumber = msnnumber;
    }
}
