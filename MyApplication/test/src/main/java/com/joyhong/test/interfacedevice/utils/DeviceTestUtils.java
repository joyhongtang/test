package com.joyhong.test.interfacedevice.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.Log;

import com.joyhong.test.interfacedevice.InterfaceDeviceExist;
import com.joyhong.test.util.TestConstant;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class DeviceTestUtils {
    /***
     * 获取所有的存储设备
     * @return
     */
    public static List<String> getAllExternalStorage() {
        List<String> storagePath = new ArrayList<>();
        StorageManager storageManager = (StorageManager) TestConstant.application.getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] storageVolumes;
        try {
            Method getVolumeList = StorageManager.class.getDeclaredMethod("getVolumeList");
            storageVolumes = (StorageVolume[]) getVolumeList.invoke(storageManager);
            Method getVolumeState = StorageManager.class.getDeclaredMethod("getVolumeState", String.class);
            for (StorageVolume storageVolume : storageVolumes) {
                Method getPath = null;
                getPath = StorageVolume.class.getMethod("getPath");
                String path = (String) getPath.invoke(storageVolume);
                String state = (String) getVolumeState.invoke(storageManager, path);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    storagePath.add(path);
                }
            }
        } catch (Exception e) {
            Log.e("cdl", e.getMessage());
        }
        return storagePath;
    }
    public static InterfaceDeviceExist getAllStorageType(){
        List<String> allpaths = getAllExternalStorage();
        InterfaceDeviceExist interfaceDeviceExist = new InterfaceDeviceExist();
        for(String path : allpaths){
            if (path.contains("emulated") || path.contains("internal")) {
                //内置存储
            }else if(TextUtils.equals(getSDcardPath(TestConstant.application),path)){
                //Sdcard
                interfaceDeviceExist.setExternalStorage(true);
            }else{
                //U盘
                interfaceDeviceExist.setUsbStorage(true);
            }
        }
        return interfaceDeviceExist;
    }
    /**
     * 获取SD卡路径
     *
     * @param context
     * @return
     */
    public static String getSDcardPath(Context context) {
        String pathBack = null;
        if (Build.VERSION.SDK_INT < 23) {
            pathBack = "/mnt/external_sd/";
            File file = new File(pathBack);
            if (!file.exists()) {
                pathBack = null;
            }
        } else {
            pathBack = getSDcardDir(context);
        }
        if (pathBack == null || pathBack.contains("null") || pathBack.length() < 6) {
            return null;
        }
        if (pathBack.endsWith("/")) {
            pathBack = pathBack.substring(0, pathBack.length() - 1);
        }
        return pathBack;
    }
    /**
     * 获取外置SD卡根目录
     *
     * @param context
     * @return
     */
    private static String getSDcardDir(Context context) {
        String sdcardDir = null;
        StorageManager storageManager = (StorageManager) TestConstant.application.getSystemService(Context.STORAGE_SERVICE);
        Class<?> volumeInfoClazz = null;
        Class<?> diskInfoClazz = null;
        try {
            diskInfoClazz = Class.forName("android.os.storage.DiskInfo");
            Method isSd = diskInfoClazz.getMethod("isSd");
            volumeInfoClazz = Class.forName("android.os.storage.VolumeInfo");
            Method getType = volumeInfoClazz.getMethod("getType");
            Method getDisk = volumeInfoClazz.getMethod("getDisk");
            Field path = volumeInfoClazz.getDeclaredField("path");
            Method getVolumes = storageManager.getClass().getMethod("getVolumes");
            List<Class<?>> result = (List<Class<?>>) getVolumes.invoke(storageManager);
            for (int i = 0; i < result.size(); i++) {
                Object volumeInfo = result.get(i);
                if ((int) getType.invoke(volumeInfo) == 0) {
                    Object disk = getDisk.invoke(volumeInfo);
                    if (disk != null) {
                        if ((boolean) isSd.invoke(disk)) {
                            sdcardDir = (String) path.get(volumeInfo);
                            break;
                        }
                    }
                }
            }
            return sdcardDir + File.separator;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
