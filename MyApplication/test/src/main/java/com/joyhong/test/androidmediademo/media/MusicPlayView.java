package com.joyhong.test.androidmediademo.media;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.joyhong.test.R;


public class MusicPlayView extends RelativeLayout implements IPlayCall {
    private AnimationDrawable voiceAnimation = null;
    private ProgressBar music_loading;
    private ImageView iv_voice;
    private TextView music_dur;

    public void init() {
        music_loading = (ProgressBar) findViewById(R.id.music_loading);
        iv_voice = (ImageView) findViewById(R.id.iv_voice);
    }

    public enum PlayStates {
        INIT, BUFFER, PLAYING, STOP, PAUSE, PERFOME_PLAYING;
    }

    public MusicPlayView(Context context) {
        super(context);
        initView(context);
    }

    public MusicPlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public MusicPlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public MusicPlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(final Context ctx) {
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }


    private int lastProgress = 0;
    private int progress;


    @Override
    public void prepareOK() {

    }

    @Override
    public void buffering() {

    }

    @Override
    public void onPublish(int progress) {

    }

    @Override
    public void onStopMusic() {
        switchViewStatus(PlayStates.STOP);
    }


    public void stopPlayVoiceAnimation() {
        if (voiceAnimation != null && voiceAnimation.isRunning()) {
            Log.i("musiccccccc","stop anim");
            voiceAnimation.stop();
            iv_voice.clearAnimation();
        }
    }

    /**
     * 可播放状态，显示动画
     */
    public void showAnimation() {
//        if (null == voiceAnimation || !(iv_voice.getDrawable() instanceof AnimationDrawable)) {
//            iv_voice.setImageResource(R.drawable.about);
////            voiceAnimation = (AnimationDrawable) iv_voice.getDrawable();
//        }
        // play voice, and start animation
//        if (!voiceAnimation.isRunning()) {
//            voiceAnimation.start();
//        }
    }

    public void switchViewStatus(PlayStates playStates) {
        switch (playStates) {
            case INIT:
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (null != music_dur && music_dur.getVisibility() == View.VISIBLE) {
                            music_dur.setVisibility(View.VISIBLE);
                        }
                        if (music_loading.getVisibility() == View.VISIBLE) {
                            music_loading.setVisibility(INVISIBLE);
                        }
                        if (iv_voice.getVisibility() == View.INVISIBLE) {
                            iv_voice.setVisibility(VISIBLE);
                        }
                    }
                });

                break;
            case STOP:
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (null != music_dur && music_dur.getVisibility() == View.VISIBLE) {
                            music_dur.setVisibility(View.INVISIBLE);
                        }
                        if (music_loading.getVisibility() == View.VISIBLE) {
                            music_loading.setVisibility(INVISIBLE);
                        }
                        if (iv_voice.getVisibility() == View.INVISIBLE) {
                            iv_voice.setVisibility(VISIBLE);
                        }
                        stopPlayVoiceAnimation();
                    }
                });

                break;
            case PAUSE:
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (music_loading.getVisibility() == View.VISIBLE) {
                            music_loading.setVisibility(INVISIBLE);
                        }
                        if (iv_voice.getVisibility() == View.INVISIBLE) {
                            iv_voice.setVisibility(VISIBLE);
                        }
                        stopPlayVoiceAnimation();

                    }
                });
                break;
            case BUFFER:
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (music_loading.getVisibility() == View.INVISIBLE) {
                            music_loading.setVisibility(VISIBLE);
                        }
                        if (iv_voice.getVisibility() == View.VISIBLE) {
                            iv_voice.setVisibility(INVISIBLE);
                        }
                        stopPlayVoiceAnimation();
                    }
                });

                break;
            case PLAYING:
                post(new Runnable() {
                    @Override
                    public void run() {
                        //显示时间
                        if (null != music_dur && music_dur.getVisibility() == View.INVISIBLE) {
                            music_dur.setVisibility(View.VISIBLE);
                        }
                    }
                });
                break;
            case PERFOME_PLAYING:
                post(new Runnable() {
                    @Override
                    public void run() {
                        //dismiss loading dialog
                        if (music_loading.getVisibility() == View.VISIBLE) {
                            music_loading.setVisibility(INVISIBLE);
                        }
                        //显示动画
                        if (iv_voice.getVisibility() == View.INVISIBLE) {
                            iv_voice.setVisibility(VISIBLE);
                        }
                        Log.i("musiccccccc","show anim 1");
                        showAnimation();
                    }
                });
                break;
        }
    }

    //根据秒数转化为时分秒   00:00:00
    public static String getTime(int second) {
        if (second < 10) {
            return "00:0" + second;
        }
        if (second < 60) {
            return "00:" + second;
        }
        if (second < 3600) {
            int minute = second / 60;
            second = second - minute * 60;
            if (minute < 10) {
                if (second < 10) {
                    return "0" + minute + ":0" + second;
                }
                return "0" + minute + ":" + second;
            }
            if (second < 10) {
                return minute + ":0" + second;
            }
            return minute + ":" + second;
        }
        int hour = second / 3600;
        int minute = (second - hour * 3600) / 60;
        second = second - hour * 3600 - minute * 60;
        if (hour < 10) {
            if (minute < 10) {
                if (second < 10) {
                    return "0" + hour + ":0" + minute + ":0" + second;
                }
                return "0" + hour + ":0" + minute + ":" + second;
            }
            if (second < 10) {
                return "0" + hour + minute + ":0" + second;
            }
            return "0" + hour + minute + ":" + second;
        }
        if (minute < 10) {
            if (second < 10) {
                return hour + ":0" + minute + ":0" + second;
            }
            return hour + ":0" + minute + ":" + second;
        }
        if (second < 10) {
            return hour + minute + ":0" + second;
        }
        return hour + minute + ":" + second;
    }

}
