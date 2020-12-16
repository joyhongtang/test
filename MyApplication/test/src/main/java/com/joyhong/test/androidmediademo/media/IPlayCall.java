package com.joyhong.test.androidmediademo.media;

public interface IPlayCall {
    void prepareOK();
    void buffering();
    void onPublish(int progress);
    void onStopMusic();
}
