package com.joyhong.test.util;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;

import com.blankj.utilcode.util.SPUtils;
import com.joyhong.test.TestEntity;
import com.joyhong.test.TestResultEnum;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static com.joyhong.test.TestMainActivity.EXIST_EXTERNA_STORAGE;
import static com.joyhong.test.TestMainActivity.EXIST_HEADSET;
import static com.joyhong.test.TestMainActivity.EXIST_USB_STORAGE;

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
//                throw new RuntimeException("create file failed");
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
    public static String saveTestResult2File(File file, ArrayList<TestEntity> testEntities) {
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

    public static void initTestFile(File file,Context mContext) {
        InputStreamReader inputReader = null;
        BufferedReader bufReader = null;
        //对测试文件进行读写
        BufferedWriter bufWriter = null;
        OutputStreamWriter outputStreamWriter = null;
        try {
            inputReader = new InputStreamReader(mContext.getResources().getAssets().open("config.txt"));
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


    /**
     * 初始化所有配置项
     */
    public  static void initConfig(Context mContext,ArrayList<TestEntity> testEntities,View autoTestView)
    {
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
        TestEntity testEntity15 = new TestEntity(0, 2, "com.joyhong.test.interfacedevice.HeadSetDevice", "耳机测试", TestResultEnum.UNKNOW);

        InputStreamReader inputReader = null;
        BufferedReader bufReader = null;
        try {
            inputReader = new InputStreamReader(mContext.getResources().getAssets().open("config.txt"));
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
                    if (!testEntities.contains(testEntity15)) {
                        testEntities.add(testEntity15);
                    }
                } else if (line.contains("Battery_1")) {
                    testEntities.add(testEntity13);
                } else if (line.contains("Rtc_1")) {
                    testEntities.add(testEntity14);
                }else if (line.contains("autotest_1")) {
                    autoTestView.setVisibility(View.VISIBLE);
//                    findViewById(R.id.reset_factory).setVisibility(View.VISIBLE);
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
}