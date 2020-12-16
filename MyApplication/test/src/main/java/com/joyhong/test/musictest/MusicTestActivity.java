package com.joyhong.test.musictest;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.SPUtils;
import com.joyhong.test.R;
import com.joyhong.test.TestEntity;
import com.joyhong.test.TestResultEnum;
import com.joyhong.test.util.TestConstant;

import java.io.File;
import java.text.SimpleDateFormat;

import static com.joyhong.test.TestMainActivity.testResult;


public class MusicTestActivity extends AppCompatActivity implements View.OnClickListener {

    private MusicService musicService;
    private SeekBar seekBar;
    private TextView musicStatus, musicTime;
    private Button btnPlayOrPause, btnStop, btnQuit;
    private SimpleDateFormat time = new SimpleDateFormat("m:ss");
    public static String[] musicDir = new String[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_test);

        Log.d("hint", "ready to new MusicService");

        File[] files = getExternalFilesDir(null).listFiles();
        for (File f : files) {
            if (f.getAbsolutePath().contains(".mp3")) {
                musicDir[0] = f.getAbsolutePath();
                break;
            }
        }
        musicService = new MusicService();
        Log.d("hint", "finish to new MusicService");

        bindServiceConnection();

        seekBar = (SeekBar) this.findViewById(R.id.MusicSeekBar);
        seekBar.setProgress(musicService.mp.getCurrentPosition());
        seekBar.setMax(musicService.mp.getDuration());
        musicStatus = (TextView) this.findViewById(R.id.MusicStatus);
        musicTime = (TextView) this.findViewById(R.id.MusicTime);
        btnPlayOrPause = (Button) this.findViewById(R.id.BtnPlayorPause);
        findViewById(R.id.pass).setOnClickListener(this);
        findViewById(R.id.fail).setOnClickListener(this);
        Log.d("hint", Environment.getExternalStorageDirectory().getAbsolutePath() + "/You.mp3");
        btnPlayOrPause.postDelayed(new Runnable() {
            @Override
            public void run() {
                musicService.playOrPause();
            }
        }, 500);
        findViewById(R.id.fail).requestFocus();
    }

    @Override
    protected void onResume() {
        if (musicService.mp.isPlaying()) {
            musicStatus.setText(getResources().getString(R.string.playing));
        } else {
            musicStatus.setText(getResources().getString(R.string.pause));
        }

        seekBar.setProgress(musicService.mp.getCurrentPosition());
        seekBar.setMax(musicService.mp.getDuration());
        handler.post(runnable);
        super.onResume();
        Log.d("hint", "handler post runnable");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != musicService) {
            stopMusic();
        }
    }

    private ServiceConnection sc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            musicService = ((MusicService.MyBinder) iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            musicService = null;
        }
    };

    private void bindServiceConnection() {
        Intent intent = new Intent(MusicTestActivity.this, MusicService.class);
        startService(intent);
        bindService(intent, sc, this.BIND_AUTO_CREATE);
    }

    public android.os.Handler handler = new android.os.Handler();
    public Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (musicService.mp.isPlaying()) {
                musicStatus.setText(getResources().getString(R.string.playing));
                btnPlayOrPause.setText(getResources().getString(R.string.pause).toUpperCase());
            } else {
                musicStatus.setText(getResources().getString(R.string.pause));
                btnPlayOrPause.setText(getResources().getString(R.string.play).toUpperCase());
            }
            musicTime.setText(time.format(musicService.mp.getCurrentPosition()) + "/"
                    + time.format(musicService.mp.getDuration()));
            seekBar.setProgress(musicService.mp.getCurrentPosition());
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        musicService.mp.seekTo(seekBar.getProgress());
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            handler.postDelayed(runnable, 100);
        }
    };

    public void stopMusic() {
        if (null != musicService) {
            musicService.stop();
        }
        handler.removeCallbacks(runnable);
        unbindService(sc);
    }

    public void onClick(View view) {
        if (view.getId() == R.id.BtnPlayorPause) {
            musicService.playOrPause();
        } else if (view.getId() == R.id.BtnStop) {
            musicService.stop();
            seekBar.setProgress(0);
        } else if (view.getId() == R.id.BtnQuit) {
            stopMusic();
        } else if (view.getId() == R.id.btnPre) {
            musicService.preMusic();
        } else if (view.getId() == R.id.btnNext) {
            musicService.nextMusic();
        } else if (view.getId() == R.id.pass) {
            TestEntity testEntity = testResult.get(TestConstant.PACKAGE_NAME + getLocalClassName());
            testEntity.setTestResultEnum(TestResultEnum.PASS);
            SPUtils.getInstance().put(testEntity.getTag(), 1);
            finish();
        } else if (view.getId() == R.id.fail) {
            TestEntity testEntity2 = testResult.get(TestConstant.PACKAGE_NAME  + getLocalClassName());
            testEntity2.setTestResultEnum(TestResultEnum.FAIL);
            SPUtils.getInstance().put(testEntity2.getTag(), 2);
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
