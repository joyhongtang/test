package com.joyhong.test.androidmediademo.media;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.joyhong.test.R;
import com.joyhong.test.TestEntity;
import com.joyhong.test.TestResultEnum;
import com.joyhong.test.util.AudioFileFunc;
import com.joyhong.test.util.TestConstant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.joyhong.test.TestMainActivity.testResult;


public class MusicSelActivity extends AppCompatActivity implements CirclePgBar.OnCountdownProgressListener, View.OnClickListener {
    private CirclePgBar circlePgBar;
    private TextView count_s;
    private boolean isRecording = false;
    //线程操作
    private ExecutorService mExecutorService;
    //录音开始时间与结束时间
    private long startTime, endTime;
    //录音所保存的文件
    private File mAudioFile;
    //文件列表数据
    private String mFilePath = "";
    private static final int BUFFER_SIZE = 2048;
    private byte[] mBuffer;
    private FileOutputStream mFileOutPutStream;
    //文件流录音API
    private AudioRecord mAudioRecord;
    int read = 0;
    private View main_music_p;
    public MusicPlayView main_music;
    /**
     * 默认音乐背景图片
     */
    private String coverPath = "https://flowerft2.oss-cn-shenzhen.aliyuncs.com/videos/music_cover/music_default_2.png";
    private Timer mTimer = null;
    private TimerTask mTimerTask = null;
    private static int count = 0;
    private boolean isPause = false;
    private static int delay = 0; //1s
    private static int period = 1000; //1s
    private static final int UPDATE_VIEW = 0;
    private static final int UPDATE_VIEW_TIME = 2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFilePath = getFilesDir().getAbsolutePath() + File.separator;
        setContentView(R.layout.activity_take_music);
        initData();
    }

    protected void initData() {
        findViewById(R.id.left_back).setOnClickListener(this);
        findViewById(R.id.fail).setOnClickListener(this);
        findViewById(R.id.pass).setOnClickListener(this);
        circlePgBar = (CirclePgBar) findViewById(R.id.video_status);
        count_s = (TextView) findViewById(R.id.count_s);
        video_cancle = findViewById(R.id.video_cancle);
        video_cancle.setVisibility(View.VISIBLE);
        video_ok = findViewById(R.id.video_ok);
//        video_cancle.setOnClickListener(this);
//        video_ok.setOnClickListener(this);
        main_music = findViewById(R.id.main_music);
        main_music_p = findViewById(R.id.main_music_p);
        main_music.init();
//        findViewById(R.id.video_publish).setOnClickListener(this);
//        findViewById(R.id.iv_del).setOnClickListener(this);
        circlePgBar.setCountdownProgressListener(1, this);
        circlePgBar.initVideoCtrl((ImageView) findViewById(R.id.video_ctrl), null, null);
        circlePgBar.setOnClickListener(this);
        mExecutorService = Executors.newSingleThreadExecutor();
        mBuffer = new byte[BUFFER_SIZE];
        readIntent();
        findViewById(R.id.play_music).setOnClickListener(this);
//        findViewById(R.id.play_music).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                mExecutorService.execute(new Runnable() {
////                    @Override
////                    public void run() {
//                startPlay(mAudioFile);
////                    }
////                });
//            }
//        });
    }

    private void readIntent() {
    }

    private void stopmRecord() {
        //停止录音
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                stopRecord();
                circlePgBar.post(new Runnable() {
                    @Override
                    public void run() {
                        //点击停止录像
                        circlePgBar.setCurrentStatus(CirclePgBar.VideoStatus.VideoStart);
                        circlePgBar.completeCapture();
                    }
                });
            }
        });
    }

    protected View video_cancle, video_ok;

    private void initVideoStartView() {
//        video_cancle.setVisibility(View.GONE);
        video_ok.setVisibility(View.GONE);
    }

    private void initVideoDoneView() {
//        video_cancle.setVisibility(View.VISIBLE);
        video_ok.setVisibility(View.VISIBLE);
    }


    @Override
    public void onProgress(int what, int progress) {
    }

    /**
     * 显示播放音乐控制条
     */
    private void visibleMusicPlayer() {
        main_music_p.setVisibility(View.VISIBLE);
//        main_music.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                mExecutorService.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        startPlay(mAudioFile);
//                    }
//                });
//
//            }
//        });
    }

    /**
     * 保存录制的视频数据
     */
    private void saveVideo() {
        stopmRecord();
        initVideoDoneView();
        String filepath_n = mFilePath + currenttime + "a" + ".mp3";
        AudioFileFunc.copyWaveFile(filepath, filepath_n, minBufferSize);
        mAudioFile = new File(filepath_n);
        //点击停止录像
        circlePgBar.setCurrentStatus(CirclePgBar.VideoStatus.VideoStart);
        circlePgBar.completeCapture();
        visibleMusicPlayer();

    }

    //更新UI线程的Handler
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_VIEW_TIME:
                    count_s.setText(count + "S");
                    break;
                case UPDATE_VIEW:
                    if (count >= 10) {
                        count_s.setText("10S");
                        saveVideo();
                        stopRecordByTimeTask(false);
                    } else {
                        count_s.setText(count + "S");
                        if (!isRecording) {
                            stopRecordByTimeTask(false);
                        }
                    }
                    break;

            }
        }
    };

    int minBufferSize;
    String filepath, lastFilePath = null;
    long currenttime = 0;


    /**
     * @description 开始录音操作
     * @author ldm
     * @time 2017/2/9 16:29
     */
    private void startRecord() {
        try {
            if (hasRecord) {
                ToastUtils.showLong("你已经录制过了，请确认");
                return;
            }
            if (null != audioTrack) {
                audioTrack.flush();
                audioTrack.stop();
            }

            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(true);
            //创建录音文件,.m4a为MPEG-4音频标准的文件的扩展名
            currenttime = System.currentTimeMillis();
            if (null != lastFilePath) {
                new File(lastFilePath).delete();
            }
            filepath = mFilePath + currenttime + "__tempdel.pcm";
            lastFilePath = filepath;
            mAudioFile = new File(filepath);
            //创建父文件夹
            mAudioFile.getParentFile().mkdirs();
            //创建文件
            mAudioFile.createNewFile();
            //创建文件输出流
            mFileOutPutStream = new FileOutputStream(mAudioFile);
            //配置AudioRecord
            //从麦克风采集数据
            int audioSource = MediaRecorder.AudioSource.MIC;
            //设置采样频率
            int sampleRate = 44100;
            //设置单声道输入
            int channelConfig = AudioFormat.CHANNEL_IN_MONO;
            //设置格式，安卓手机都支持的是PCM16
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            //计算AudioRecord内部buffer大小
            minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
            //根据上面的设置参数初始化AudioRecord
            mAudioRecord = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, Math.max(minBufferSize, BUFFER_SIZE));
            //开始录音
            mAudioRecord.startRecording();
            resetRecord = false;
            //记录开始时间
            startTime = System.currentTimeMillis();
            //写入数据到文件
            while (isRecording) {
                read = mAudioRecord.read(mBuffer, 0, BUFFER_SIZE);
                if (read > 0) {
                    mFileOutPutStream.write(mBuffer, 0, read);
                    if (!resetRecord) {
                        resetRecord = true;
                        circlePgBar.post(new Runnable() {
                            @Override
                            public void run() {
                                circlePgBar.setCurrentStatus(CirclePgBar.VideoStatus.VideoIng);
                                circlePgBar.setTimeMillis(10 * 1000);
                                circlePgBar.reStart();
                                startTimer();
                            }
                        });
                    }
                } else {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showLong("录音设备出现问题或不存在");
        } finally {
//            if (null != mAudioRecord) {
//                //释放资源
//                mAudioRecord.release();
//            }
        }
    }

    private boolean resetRecord = false;

    @Override
    protected void onStop() {
        super.onStop();
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
        main_music.switchViewStatus(MusicPlayView.PlayStates.STOP);
    }

    /**
     * @description 停止录音
     * @author ldm
     * @time 2017/2/9 16:45
     */
    private void stopRecord() {
        try {
            //停止录音
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
            mFileOutPutStream.close();
            //记录时长
            endTime = System.currentTimeMillis();
            //录音时间处理，比如只有大于2秒的录音才算成功
            int time = (int) ((endTime - startTime) / 1000);
            //录音成功,添加数据
//                mAudioFile
            //录音成功,发Message
        } catch (Exception e) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mExecutorService) {
            mExecutorService.shutdownNow();
        }
        if (null != audioTrack) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
        try {
            //停止录音
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopTimer();
    }
    /*******6.0以上版本手机权限处理***************************/
    /**
     * @description 兼容手机6.0权限管理
     * @author ldm
     * @time 2016/5/24 14:59
     */
    private void permissionForM() {
        startRecord();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        startRecord();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private AudioTrack audioTrack;

    /**
     * @description 播放音频流文件声音
     * @author ldm
     * @time 2017/2/10 8:13
     */
    private void startPlay(File audioFile) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isRecording) {
                        ToastUtils.showLong("正在录音中...");
                        return;
                    }
                    FileInputStream fis = null;
                    //配置播放器
                    //首先设备播放器声音类型
                    int streamType = AudioManager.STREAM_MUSIC;

                    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 10, 0);

                    //设置播放的频率，和录音时的频率一致
                    int sampleRate = 44100;
                    //播放输出声道
                    int channelConfig = AudioFormat.CHANNEL_OUT_FRONT_LEFT;
                    //播放格式
                    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                    //设置流模式
                    int mode = AudioTrack.MODE_STREAM;
                    //计算buffer大小
                    int minSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                    //初始化播放器
                    audioTrack = new AudioTrack(streamType, sampleRate, channelConfig, audioFormat, Math.max(minSize, BUFFER_SIZE), mode);
                    //从音频文件中读取文件流数据
                    fis = openFileInput(mAudioFile.getName());
                    audioTrack.play();
                    //读取数据到播放器中
                    int read = -1;
                    resetPlayStatus = false;
                    while ((read = fis.read(mBuffer)) > 0) {
                        int ret = audioTrack.write(mBuffer, 0, read);
                        switch (ret) {
                            case AudioTrack.ERROR_INVALID_OPERATION:
                            case AudioTrack.ERROR_BAD_VALUE:
                            case AudioTrack.ERROR:
                            case AudioTrack.ERROR_DEAD_OBJECT:
                            case AudioTrack.PLAYSTATE_PAUSED:
                            case AudioTrack.PLAYSTATE_STOPPED:
                                playFail(audioTrack);
                                return;
                            default:
                                if (!resetPlayStatus) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            main_music.switchViewStatus(MusicPlayView.PlayStates.PERFOME_PLAYING);
                                        }
                                    });
                                    resetPlayStatus = true;
                                }
                                break;
                        }
                    }
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            playFail(audioTrack);
                        }
                    });
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private boolean resetPlayStatus = false;

    private void clearRecord() {
        if (null != mAudioFile && mAudioFile.exists()) {
            mAudioFile.delete();
            mAudioFile = null;
        }
        playFail(audioTrack);
        main_music.switchViewStatus(MusicPlayView.PlayStates.STOP);
        main_music_p.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * @description
     * @author ldm
     * @time 2017/2/10 播放失败处理
     */
    private void playFail(AudioTrack audioTrack) {
        mAudioFile = null;
        main_music.switchViewStatus(MusicPlayView.PlayStates.STOP);
        if (null != audioTrack) {
            try {
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.audioTrack = null;
        }
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.video_status) {
            if (!isRecording) {
                initVideoStartView();
                isRecording = true;
                count_s.setText("0S");
                //录音操作
                mExecutorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT > 22) {
                            //6.0以上权限管理
                            permissionForM();
                        } else {
                            //开始录音
                            startRecord();
                        }

                    }
                });
            } else {
                isRecording = false;
                sendMessage(UPDATE_VIEW);
            }
        } else if (v.getId() == R.id.play_music) {
            startPlay(mAudioFile);
        } else if (v.getId() == R.id.pass) {
            if (isRecording) {
                ToastUtils.showLong("正在录音中...");
                return;
            }
            TestEntity testEntity = testResult.get(TestConstant.PACKAGE_NAME + getLocalClassName());
            testEntity.setTestResultEnum(TestResultEnum.PASS);
            SPUtils.getInstance().put(testEntity.getTag(), 1);
            finish();
        } else if (v.getId() == R.id.fail) {
            if (isRecording) {
                ToastUtils.showLong("正在录音中...");
                return;
            }
            TestEntity testEntity2 = testResult.get(TestConstant.PACKAGE_NAME + getLocalClassName());
            testEntity2.setTestResultEnum(TestResultEnum.FAIL);
            SPUtils.getInstance().put(testEntity2.getTag(), 2);
            finish();
        }
    }

    private boolean hasRecord = false;

    private void stopRecordByTimeTask(boolean needupdatatime) {
        circlePgBar.setCurrentStatus(CirclePgBar.VideoStatus.VideoIng);
        circlePgBar.setTimeMillis(10 * 1000);
        circlePgBar.reStart();
        stopTimer();
        saveVideo();
        hasRecord = true;
        isRecording = false;
        startPlay(null);
    }

    private static final int REQUEST_CODE_CHOOSE = 23;//定义请求码常量


    private boolean isUpload = false;


    private void startTimer() {
        count = 0;
        stopTimer();
        if (mTimer == null) {
            mTimer = new Timer();
        }
        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    count++;
                    if (count >= 10) {
                        mTimerTask.cancel();
                        mTimer.cancel();
                    }
                    sendMessage(UPDATE_VIEW);
                }
            };
        }

        if (mTimer != null && mTimerTask != null)
            mTimer.schedule(mTimerTask, delay, period);

    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        count = 0;
    }

    public void sendMessage(int id) {
        if (mHandler != null) {
            mHandler.removeMessages(id);
            Message message = Message.obtain(mHandler, id);
            mHandler.sendMessage(message);
        }
    }
}
