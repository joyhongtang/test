package com.idwell.cloudframe.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.Observer;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.ZipUtils;
import com.idwell.cloudframe.R;
import com.idwell.cloudframe.common.Device;
import com.idwell.cloudframe.db.MyDatabase;
import com.idwell.cloudframe.db.entity.DeviceLog;
import com.idwell.cloudframe.http.RetrofitManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UploadFileService extends Service implements LifecycleOwner {

    //最大上传次数
    private static final int MAX_UPLOAD_TIMES = 9;
    //最大上传log文件数量
    private static final int MAX_UPLOAD_LOG_FILE_NUMBER = 18;

    private LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

    //是否正在上传log文件
    private boolean isUploadingLogFile = false;
    //是否正在上传anr,crash文件
    private boolean isUploadingAnrCrashFile = false;
    //anr目录观察者
    private FileObserver mAnrDirObserver;
    private List<DeviceLog> mAnrCrashFiles = new ArrayList<>();
    //anr缓存目录
    private File mAnrFileCacheDir;
    //crash缓存目录
    private File mCrashFileCacheDir;
    //log缓存目录
    private File mLogFileCacheDir;
    //上传log文件路径
    private String mUploadLogFilePath;
    //上传log文件集合
    private List<DeviceLog> mUploadLogFiles = new ArrayList<>();
    private Handler mHandler = new Handler();

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //LogUtils.dTag("lcs", action);
            if (Device.ACTION_SHARE_APP_LOG.equals(action)) {
                if (isUploadingLogFile) {
                    ToastUtils.showShort(R.string.uploading);
                } else {
                    isUploadingLogFile = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //查询数据库中的log文件
                            List<DeviceLog> deviceLogs = MyDatabase.Companion.getInstance().getDeviceLogDao().getLog();
                            //LogUtils.dTag("lcs", deviceLogs.size() + ", " + deviceLogs);
                            //不存在log文件集合
                            List<DeviceLog> emptyDeviceLogs = new ArrayList<>();
                            for (DeviceLog deviceLog : deviceLogs) {
                                if (!new File(deviceLog.getFilePath()).exists()) {
                                    //添加不存在日志文件到集合
                                    emptyDeviceLogs.add(deviceLog);
                                }
                            }
                            //删除数据库中不存在的log文件集合
                            MyDatabase.Companion.getInstance().getDeviceLogDao().delete(emptyDeviceLogs);
                            //删除不存在的日志文件集合
                            deviceLogs.removeAll(emptyDeviceLogs);
                            //LogUtils.dTag("lcs", deviceLogs.size() + ", " + deviceLogs);
                            if (deviceLogs.isEmpty()) {
                                isUploadingLogFile = false;
                                ToastUtils.showShort(R.string.no_log_files);
                            } else {
                                if (mLogFileCacheDir.exists()) {
                                    //清空log文件缓存目录
                                    //LogUtils.dTag("lcs", "清空log文件缓存目录");
                                    FileUtils.deleteAllInDir(mLogFileCacheDir);
                                } else {
                                    //创建log文件缓存目录
                                    //LogUtils.dTag("lcs", "创建log文件缓存目录");
                                    mLogFileCacheDir.mkdirs();
                                }
                                //清空上传log文件集合
                                mUploadLogFiles.clear();
                                //重置上传log文件路径
                                mUploadLogFilePath = null;
                                //源文件集合
                                List<String> srcFilePaths = new ArrayList<>();
                                //压缩文件路径
                                String zipFilePath = null;
                                //上传log文件数量
                                int total = Math.min(deviceLogs.size(), MAX_UPLOAD_LOG_FILE_NUMBER);
                                for (int i = 0; i < total; i++) {
                                    DeviceLog deviceLog = deviceLogs.get(i);
                                    if (i == 0) {
                                        zipFilePath = new File(mLogFileCacheDir, new File(deviceLog.getFilePath()).getName() + ".zip").getAbsolutePath();
                                    }
                                    mUploadLogFiles.add(deviceLog);
                                    srcFilePaths.add(deviceLogs.get(i).getFilePath());
                                }
                                //文件压缩
                                boolean zipResult = false;
                                try {
                                    zipResult = ZipUtils.zipFiles(srcFilePaths, zipFilePath);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                if (zipResult) {
                                    //压缩成功
                                    mUploadLogFilePath = zipFilePath;
                                } else {
                                    //压缩失败
                                    mUploadLogFiles.clear();
                                    mUploadLogFiles.add(deviceLogs.get(0));
                                    mUploadLogFilePath = srcFilePaths.get(0);
                                }
                                //切换到主线程
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (NetworkUtils.isConnected()) {
                                            //联网上传log文件
                                            uploadLog(new File(mUploadLogFilePath));
                                        } else {
                                            //提示无网络
                                            ToastUtils.showShort(R.string.unconnected_network);
                                        }
                                    }
                                });
                            }
                        }
                    }).start();
                }
            }else if (Device.ACTION_SHARE_CRASH_LOG.equals(action)) {
                DeviceLog deviceLog = intent.getParcelableExtra("crashLog");
                if (deviceLog != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            MyDatabase.Companion.getInstance().getDeviceLogDao().add(deviceLog);
                        }
                    }).start();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        //LogUtils.dTag("lcs", "onCreate");
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        mLogFileCacheDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/CloudFrame" + "/cache/log/");
        mAnrFileCacheDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/CloudFrame" + "/cache/anr/");
        mCrashFileCacheDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/CloudFrame" + "/cache/crash/");

        MyDatabase.Companion.getInstance().getDeviceLogDao().liveAnrCrash().observe(this, new Observer<List<DeviceLog>>() {
            @Override
            public void onChanged(List<DeviceLog> deviceLogs) {
                //LogUtils.dTag("lcs", deviceLogs.size() + ", " + deviceLogs);
                mAnrCrashFiles.clear();
                mAnrCrashFiles.addAll(deviceLogs);
                if (!isUploadingAnrCrashFile && deviceLogs.size() > 0) {
                    DeviceLog deviceLog = deviceLogs.get(0);
                    if (new File(deviceLog.getFilePath()).exists()) {
                        if (NetworkUtils.isConnected()) {
                            isUploadingAnrCrashFile = true;
                            uploadAnrCrash(deviceLog);
                        }
                    } else {
                        //文件不存在,删除数据库记录
                        deleteDeviceLog(deviceLog);
                    }
                }
            }
        });

        mAnrDirObserver = new FileObserver("/data/anr", FileObserver.CLOSE_NOWRITE) {
            @Override
            public void onEvent(int event, @Nullable String path) {
                //LogUtils.dTag("lcs", event + ", " + path);
                if (path != null) {
                    DeviceLog deviceLog = new DeviceLog("/data/anr/" + path, "anr");
                    if (!mAnrCrashFiles.contains(deviceLog)) {
                        //LogUtils.dTag("lcs", deviceLog);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                MyDatabase.Companion.getInstance().getDeviceLogDao().add(deviceLog);
                            }
                        }).start();
                    }
                }
            }
        };
        mAnrDirObserver.startWatching();

        //注册广播接收器
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Device.ACTION_SHARE_APP_LOG);
        intentFilter.addAction(Device.ACTION_SHARE_CRASH_LOG);
        registerReceiver(mBroadcastReceiver, intentFilter);

        //log文件数量超出最大限制则删除超出部分
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<DeviceLog> deviceLogs = MyDatabase.Companion.getInstance().getDeviceLogDao().getLog();
                if (deviceLogs.size() > MAX_UPLOAD_LOG_FILE_NUMBER) {
                    for (int i = MAX_UPLOAD_LOG_FILE_NUMBER; i < deviceLogs.size(); i++) {
                        DeviceLog deviceLog = deviceLogs.get(i);
                        //删除log文件
                        FileUtils.delete(deviceLog.getFilePath());
                        //删除数据库记录
                        MyDatabase.Companion.getInstance().getDeviceLogDao().delete(deviceLog);
                    }
                }
            }
        }).start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //LogUtils.dTag("lcs", "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    /**
     * 上传log
     *
     * @param uploadLogFile log文件
     */
    private void uploadLog(File uploadLogFile) {
        //获取密钥
        //String uploadFileSecret = Device.INSTANCE.getUploadFileSecret();
        //第一步,构建HttpUrl
        //第二步,构建RequestBody
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), uploadLogFile);
        //第三步,构建MultipartBody
        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("serial_number", Device.INSTANCE.getSnnumber())
                .addFormDataPart("file_type", "log")
                .addFormDataPart("device_datetime", String.valueOf(System.currentTimeMillis()))
                //在此处添加多个requestBody实现多文件上传
                .addFormDataPart("upload_file", uploadLogFile.getName(), requestBody)
                .build();

        // 第四步,构建Request请求对象
        Request request = new Request.Builder()
                .url(RetrofitManager.URL + "device/uploadFile")
                .post(multipartBody)
                .build();
        // 第五步,构建Request请求对象
        RetrofitManager.INSTANCE.getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                //LogUtils.dTag("lcs", e);
                isUploadingLogFile = false;
                ToastUtils.showShort(R.string.log_upload_failed);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                isUploadingLogFile = false;
                ResponseBody body = response.body();
                if (body != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(body.string());
                        int status = jsonObject.getInt("status");
                        if (status == 200) {
                            for (int i = 0; i < mUploadLogFiles.size(); i++) {
                                FileUtils.delete(mUploadLogFiles.get(i).getFilePath());
                            }
                            deleteDeviceLog(mUploadLogFiles);
                            ToastUtils.showShort(R.string.log_upload_successfully);
                        } else {
                            ToastUtils.showShort(R.string.log_upload_failed);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        ToastUtils.showShort(R.string.log_upload_failed);
                    }
                }
            }
        });
    }

    /**
     * 上传anr,crash
     *
     * @param deviceLog anr,crash文件
     */
    private void uploadAnrCrash(DeviceLog deviceLog) {
        //缓存目录
        File cacheDir;
        if (deviceLog.getFileType().equals("anr")) {
            //anr文件缓存目录
            cacheDir = mAnrFileCacheDir;
        } else {
            //crash文件缓存目录
            cacheDir = mCrashFileCacheDir;
        }
        if (cacheDir.exists()) {
            //清空anr,crash文件缓存目录
            FileUtils.deleteAllInDir(cacheDir);
        } else {
            //创建anr,crash文件缓存目录
            cacheDir.mkdirs();
        }
        //anr和crash上传文件
        File anrCrashUploadFile;
        //源文件
        File srcFile = new File(deviceLog.getFilePath());
        //压缩文件
        File zipFile = new File(cacheDir, new File(deviceLog.getFilePath()).getName() + ".zip");
        //文件压缩
        boolean zipResult = false;
        try {
            zipResult = ZipUtils.zipFile(srcFile, zipFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (zipResult) {
            anrCrashUploadFile = zipFile;
        } else {
            anrCrashUploadFile = srcFile;
        }
        //LogUtils.dTag("lcs", logFile);

        //获取密钥
        //String uploadFileSecret = Device.INSTANCE.getUploadFileSecret();
        //第一步,构建HttpUrl
        //第二步,构建RequestBody
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), anrCrashUploadFile);
        //第三步,构建MultipartBody
        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("serial_number", Device.INSTANCE.getSnnumber())
                .addFormDataPart("file_type", deviceLog.getFileType())
                .addFormDataPart("device_datetime", String.valueOf(System.currentTimeMillis()))
                //在此处添加多个requestBody实现多文件上传
                .addFormDataPart("upload_file", anrCrashUploadFile.getName(), requestBody)
                .build();

        // 第四步,构建Request请求对象
        Request request = new Request.Builder()
                .url(RetrofitManager.URL + "device/uploadFile")
                .post(multipartBody)
                .build();
        // 第五步,构建Request请求对象
        RetrofitManager.INSTANCE.getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                //LogUtils.dTag("lcs", e);
                isUploadingAnrCrashFile = false;
                int uploadTimes = deviceLog.getUploadTimes();
                deviceLog.setUploadTimes(uploadTimes + 1);
                if (e instanceof FileNotFoundException) {
                    deleteDeviceLog(deviceLog);
                } else {
                    updateDeviceLog(deviceLog);
                }
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                isUploadingAnrCrashFile = false;
                int uploadTimes = deviceLog.getUploadTimes();
                deviceLog.setUploadTimes(uploadTimes + 1);
                ResponseBody body = response.body();
                if (body != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(body.string());
                        int status = jsonObject.getInt("status");
                        if (status == 200) {
                            FileUtils.delete(deviceLog.getFilePath());
                            deleteDeviceLog(deviceLog);
                        } else {
                            updateDeviceLog(deviceLog);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        updateDeviceLog(deviceLog);
                    }
                }
            }
        });
    }

    private void deleteDeviceLog(DeviceLog... deviceLog) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MyDatabase.Companion.getInstance().getDeviceLogDao().delete(deviceLog);
            }
        }).start();
    }

    private void deleteDeviceLog(List<DeviceLog> deviceLogs) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MyDatabase.Companion.getInstance().getDeviceLogDao().delete(deviceLogs);
            }
        }).start();
    }

    private void updateDeviceLog(DeviceLog deviceLog) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (deviceLog.getUploadTimes() > MAX_UPLOAD_TIMES) {
                    //超出最大上传次数,删除记录
                    MyDatabase.Companion.getInstance().getDeviceLogDao().delete(deviceLog);
                } else {
                    //更新记录
                    MyDatabase.Companion.getInstance().getDeviceLogDao().update(deviceLog);
                }
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        //LogUtils.dTag("lcs", "onDestroy");
        unregisterReceiver(mBroadcastReceiver);
        mAnrDirObserver.stopWatching();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        super.onDestroy();
    }
}
