package com.joyhong.test.androidmediademo.media;

/**
 * Created by tj on 2018/5/25.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.joyhong.test.R;
import com.joyhong.test.util.DisplayUtils;


public class CirclePgBar extends View {

    private ImageView ctrlView;
    TextView videocp, photocp;
    private Paint mBackPaint;
    private Paint mFrontPaint;
    private Paint mTextPaint;
    private float mStrokeWidth = 10;
    private float mHalfStrokeWidth = mStrokeWidth / 2;
    private float mRadius = DisplayUtils.INSTANCE.dp2px(getContext(),45);
    private RectF mRect;
    private int mProgress = 0;
    public static int progressLevel = 1000;
    private int mWidth;
    private int mHeight;

    public void setCurrentStatus(VideoStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public VideoStatus getCurrentStatus() {
        return currentStatus;
    }

    private VideoStatus currentStatus = VideoStatus.VideoStart;

    public CirclePgBar(Context context) {
        super(context);
        init();
    }

    public CirclePgBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CirclePgBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    //完成相关参数初始化
    private void init() {
        mBackPaint = new Paint();
        mBackPaint.setColor(Color.WHITE);
        mBackPaint.setAntiAlias(true);
        mBackPaint.setStrokeWidth(mStrokeWidth);

        mFrontPaint = new Paint();
        mFrontPaint.setColor(Color.parseColor("#ff5555"));
        mFrontPaint.setAntiAlias(true);
        mFrontPaint.setStyle(Paint.Style.STROKE);
        mFrontPaint.setStrokeWidth(mStrokeWidth);


        mTextPaint = new Paint();
        mTextPaint.setColor(Color.GREEN);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(80);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        initVideStartSCircle();

        initPhototartSCircle();

        initVideoStartArc();

        initPhotoStartArc();
    }


    //重写测量大小的onMeasure方法和绘制View的核心方法onDraw()
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getRealSize(widthMeasureSpec);
        mHeight = getRealSize(heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);

    }

    Paint mVideoStartArcPaint = null;
    Paint mVideoStartCirclePaint = null;
    Paint mPhotoStartArcPaint = null;
    Paint mPhotoStartCirclePaint = null;

    private void initVideoStartArc() {
        mVideoStartArcPaint = new Paint();
        mVideoStartArcPaint.setColor(Color.WHITE);
        mVideoStartArcPaint.setAntiAlias(true);
        mVideoStartArcPaint.setStyle(Paint.Style.STROKE);
        mVideoStartArcPaint.setStrokeWidth(20);
    }

    private void initPhotoStartArc() {
        mPhotoStartArcPaint = new Paint();
        mPhotoStartArcPaint.setARGB(100, 255, 250, 250);
        mPhotoStartArcPaint.setAntiAlias(true);
        mPhotoStartArcPaint.setStyle(Paint.Style.STROKE);
        mPhotoStartArcPaint.setStrokeWidth(20);

    }

    private void initVideStartSCircle() {
        mVideoStartCirclePaint = new Paint();
        mVideoStartCirclePaint.setColor(Color.parseColor("#ff5555"));
        mVideoStartCirclePaint.setAntiAlias(true);
        mVideoStartCirclePaint.setStrokeWidth(mStrokeWidth);
    }

    private void initPhototartSCircle() {
        mPhotoStartCirclePaint = new Paint();
        mPhotoStartCirclePaint.setColor(Color.WHITE);
        mPhotoStartCirclePaint.setAntiAlias(true);
        mPhotoStartCirclePaint.setStrokeWidth(mStrokeWidth);
    }


    public void initVideoCtrl(ImageView ctrlView, TextView videocp, TextView photocp) {
        this.ctrlView = ctrlView;
        this.videocp = videocp;
        this.photocp = photocp;
        if (null != videocp)
            videocp.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentStatus = VideoStatus.VideoStart;
                    invalidate();
                }
            });
        if (null != photocp)
            photocp.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
//                currentStatus = VideoStatus.PhotoStart;
//                invalidate();
                    if (null != surfaceClickCall) {
                        surfaceClickCall.selVideo();
                    }
                }
            });
    }

    private SurfaceClickCall surfaceClickCall;

    public SurfaceClickCall getSurfaceClickCall() {
        return surfaceClickCall;
    }

    public void setSurfaceClickCall(SurfaceClickCall surfaceClickCall) {
        this.surfaceClickCall = surfaceClickCall;
    }

    public interface SurfaceClickCall {
        public void selVideo();

        public void selVideocp();
    }

    private boolean hasSetRecordingView = false;

    @Override
    protected void onDraw(Canvas canvas) {
        float angle = mProgress / (float) progressLevel * 360;
        if (currentStatus == VideoStatus.VideoStart) {
            hasSetRecordingView = false;
            if (null != videocp) {
                videocp.setTextColor(Color.parseColor("#ff5555"));
                photocp.setTextColor(Color.WHITE);
            }

            ctrlView.setVisibility(View.INVISIBLE);
            initVideoStartRect();
            canvas.drawCircle(mWidth / 2, mHeight / 2, mRadius - 5, mVideoStartCirclePaint);
            canvas.drawArc(mRect, -90, 360, false, mVideoStartArcPaint);
        } else if (currentStatus == VideoStatus.VideoIng) {
            if (!hasSetRecordingView) {
                hasSetRecordingView = true;
                ctrlView.setVisibility(View.VISIBLE);
                ctrlView.setImageResource(R.drawable.icon_stop);
            }
            initRect();
            canvas.drawCircle(mWidth / 2, mHeight / 2, mRadius - 5, mBackPaint);
            canvas.drawArc(mRect, -90, angle, false, mFrontPaint);
        } else if (currentStatus == VideoStatus.PhotoStart) {
            if (null != videocp) {
                videocp.setTextColor(Color.WHITE);
                photocp.setTextColor(Color.parseColor("#ff5555"));
            }
            ctrlView.setVisibility(View.INVISIBLE);
            initVideoStartRect();
            canvas.drawCircle(mWidth / 2, mHeight / 2, mRadius - 20, mPhotoStartCirclePaint);
            canvas.drawArc(mRect, -90, 360, false, mPhotoStartArcPaint);
        }
    }

    /**
     * 进度监听。
     */
    public interface OnCountdownProgressListener {

        /**
         * 进度通知。
         *
         * @param progress 进度值。
         */
        void onProgress(int what, int progress);
    }

    /**
     * Listener what。
     */
    private int listenerWhat = 0;
    /**
     * 进度条通知。
     */
    private OnCountdownProgressListener mCountdownProgressListener;

    /**
     * 设置进度监听。
     *
     * @param mCountdownProgressListener 监听器。
     */
    public void setCountdownProgressListener(int what, OnCountdownProgressListener mCountdownProgressListener) {
        this.listenerWhat = what;
        this.mCountdownProgressListener = mCountdownProgressListener;
    }

    /**
     * 进度条类型。
     */
    private ProgressType mProgressType = ProgressType.COUNT;

    /**
     * 进度条类型。
     */
    public enum ProgressType {
        /**
         * 顺数进度条，从0-100；
         */
        COUNT,

        /**
         * 倒数进度条，从100-0；
         */
        COUNT_BACK;
    }

    /**
     * 重置进度。
     */
    private void resetProgress() {
        switch (mProgressType) {
            case COUNT:
                mProgress = 0;
                break;
            case COUNT_BACK:
                mProgress = progressLevel;
                break;
        }
    }

    /**
     * 开始。
     */
    public void start() {
        stop();
        post(progressChangeTask);
    }

    /**
     * 重新开始。
     */
    public void reStart() {
        resetProgress();
        start();
    }

    /**
     * 重新开始。
     */
    public void completeCapture() {
        resetProgress();
        stop();
        invalidate();
    }

    /**
     * 停止。
     */
    public void stop() {
        removeCallbacks(progressChangeTask);
    }

    /**
     * 进度倒计时时间。
     */
    private long timeMillis = 0;

    /**
     * 设置倒计时总时间。
     *
     * @param timeMillis 毫秒。
     */
    public void setTimeMillis(long timeMillis) {
        this.timeMillis = timeMillis;
    }

    /**
     * 进度更新task。
     */
    private Runnable progressChangeTask = new Runnable() {
        @Override
        public void run() {
            removeCallbacks(this);
            switch (mProgressType) {
                case COUNT:
                    mProgress += 1;
                    break;
                case COUNT_BACK:
                    mProgress -= 1;
                    break;
            }
            if (mProgress >= 0 && mProgress <= progressLevel) {
                if (mCountdownProgressListener != null) {

                    mCountdownProgressListener.onProgress(listenerWhat, mProgress);
                    invalidate();
                    postDelayed(progressChangeTask, timeMillis / progressLevel);
                } else
                    mProgress = validateProgress(mProgress);
            }
        }
    };

    /**
     * 验证进度。
     *
     * @param progress 你要验证的进度值。
     * @return 返回真正的进度值。
     */
    private int validateProgress(int progress) {
        if (progress > 1000)
            progress = 1000;
        else if (progress < 0)
            progress = 0;
        return progress;
    }

    public int getRealSize(int measureSpec) {
        int result = 1;
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);

        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.UNSPECIFIED) {
            //自己计算
            result = (int) (mRadius * 2 + mStrokeWidth);
        } else {
            result = size;
        }

        return result;
    }

    private void initVideoStartRect() {
        mRect = new RectF();
        int viewSize = (int) (mRadius * 2);
        int left = (mWidth - viewSize) / 2 + 10;
        int top = (mHeight - viewSize) / 2 + 10;
        int right = left + viewSize - 20;
        int bottom = top + viewSize - 20;
        mRect.set(left, top, right, bottom);
    }

    private void initRect() {
        mRect = new RectF();
        int viewSize = (int) (mRadius * 2);
        int left = (mWidth - viewSize) / 2;
        int top = (mHeight - viewSize) / 2;
        int right = left + viewSize;
        int bottom = top + viewSize;
        mRect.set(left, top, right, bottom);
    }
    public enum VideoStatus {
        VideoStart, VideoIng, VideoPause, VideoRestart, VideoEnd, PhotoStart
    }

}