package com.joyhong.test.util;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Author: luqihua
 * Time: 2017/12/4
 * Description: FileUtil
 */

public class FileUtil {
    private static final String ROOT_DIR = "aaamedia";//以aaa开头容易查找
    private static String sRootPath = "";
    private static boolean hasInitialize = false;

    public static void init(Context context) {
        if (hasInitialize) return;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            sRootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + ROOT_DIR;
        } else {
            sRootPath = context.getCacheDir().getAbsolutePath() + "/" + ROOT_DIR;
        }
        File file = new File(sRootPath);
        if (!file.exists()) {
            boolean success = file.mkdirs();
            if (!success) {
                throw new RuntimeException("create file failed");
            }
        }
        hasInitialize = true;
    }


    public static File newMp4File() {
        SimpleDateFormat format = new SimpleDateFormat("MM_dd_HH_mm_ss", Locale.CHINA);
        return new File(sRootPath, "mp4_" + format.format(new Date()) + ".mp4");
    }

    public static File newAccFile() {
        SimpleDateFormat format = new SimpleDateFormat("MM_dd_HH_mm_ss", Locale.CHINA);
        return new File(sRootPath, "acc_" + format.format(new Date()) + ".acc");
    }
    /**
     * 复制assets目录下所有文件及文件夹到指定路径
     * @param android.app.Activity mActivity 上下文
     * @param java.lang.String mAssetsPath Assets目录的相对路径
     * @param java.lang.String mSavePath 复制文件的保存路径
     * @return void
     */
    public static void copyAssetsFiles(Context mActivity, String mAssetsPath, String mSavePath)
    {
        try
        {
            // 获取assets目录下的所有文件及目录名
            String[] fileNames=mActivity.getResources().getAssets().list(mAssetsPath);
            if(fileNames.length>0)
            {
                // 若是目录
                for(String fileName:fileNames)
                {
                    String newAssetsPath="";
                    // 确保Assets路径后面没有斜杠分隔符，否则将获取不到值
                    if((mAssetsPath==null)||"".equals(mAssetsPath)||"/".equals(mAssetsPath))
                    {
                        newAssetsPath=fileName;
                    }
                    else
                    {
                        if(mAssetsPath.endsWith("/"))
                        {
                            newAssetsPath=mAssetsPath+fileName;
                        }
                        else
                        {
                            newAssetsPath=mAssetsPath+"/"+fileName;
                        }
                    }
                    // 递归调用
                    copyAssetsFiles(mActivity,newAssetsPath,mSavePath+"/"+fileName);
                }
            }
            else
            {
                //只拷贝指定的文件
                if(mAssetsPath.contains("Color")) {
                    // 若是文件
                    File file = new File(mSavePath);
                    // 若文件夹不存在，则递归创建父目录
                    file.getParentFile().mkdirs();
                    java.io.InputStream is = mActivity.getResources().getAssets().open(mAssetsPath);
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(new File(mSavePath));
                    byte[] buffer = new byte[1024];
                    int byteCount = 0;
                    // 循环从输入流读取字节
                    while ((byteCount = is.read(buffer)) != -1) {
                        // 将读取的输入流写入到输出流
                        fos.write(buffer, 0, byteCount);
                    }
                    // 刷新缓冲区
                    fos.flush();
                    fos.close();
                    is.close();
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}