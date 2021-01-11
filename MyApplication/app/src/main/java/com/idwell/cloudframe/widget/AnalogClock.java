package com.idwell.cloudframe.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.blankj.utilcode.util.TimeUtils;
import com.idwell.cloudframe.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * author : chason
 * mailbox : 156874547@qq.com
 * time : 2017/11/6 11:41
 * version : 1.0
 * describe :
 */

public class AnalogClock extends View {

    private Context mContext;

    private Drawable mHourHand;// 时针
    private Drawable mMinuteHand;// 分针
    private Drawable mSecondHand;// 秒针
    private Drawable mDisc;// 圆盘
    private Drawable mDial;// 表盘
    private boolean showDate;//是否显示日期

    private String mDay;// 日期
    private String mWeek;// 星期

    private int mDialWidth;// 表盘宽度
    private int mDialHeight;// 表盘高度
    private int centerX;//X轴中心点
    private int centery;//Y轴中心点
    private int mHourWidth;//时钟宽度
    private int mHourHeight;//时钟高度
    private int mHourBoundLeft;//时钟左边框X轴坐标
    private int mMinuteWidth;//分钟宽度
    private int mMinuteHeight;//分钟高度
    private int mMinuteBoundLeft;//分钟左边框X轴坐标
    private int mSecondWidth;//秒钟宽度
    private int mSecondHeight;//秒钟高度
    private int mSecondBoundLeft;//秒钟左边框X轴坐标
    private int mDiscWidth;//圆盘宽度
    private int mDiscHeight;//圆盘高度

    private float mHour;// 时针值
    private float mMinute;// 分针值
    private float mSecond;// 秒针值

    private Paint mPaint;// 画笔

    public AnalogClock(Context context) {
        this(context, null);
    }

    public AnalogClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnalogClock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.AnalogClock);
        mDial = array.getDrawable(R.styleable.AnalogClock_m_dial_id);
        mHourHand = array.getDrawable(R.styleable.AnalogClock_m_hour_id);
        mMinuteHand = array.getDrawable(R.styleable.AnalogClock_m_minute_id);
        mSecondHand = array.getDrawable(R.styleable.AnalogClock_m_second_id);
        mDisc = array.getDrawable(R.styleable.AnalogClock_m_disc_id);
        showDate = array.getBoolean(R.styleable.AnalogClock_show_date, false);
        centerX = (int) array.getDimension(R.styleable.AnalogClock_center_x, 0);
        centery = (int) array.getDimension(R.styleable.AnalogClock_center_y, 0);
        mDialWidth = (int) array.getDimension(R.styleable.AnalogClock_d_width, 0);
        mDialHeight = (int) array.getDimension(R.styleable.AnalogClock_d_height, 0);
        mHourWidth = (int) array.getDimension(R.styleable.AnalogClock_h_width, 0);
        mHourHeight = (int) array.getDimension(R.styleable.AnalogClock_h_height, 0);
        mHourBoundLeft = (int) array.getDimension(R.styleable.AnalogClock_h_bound_left, 0);
        mMinuteWidth = (int) array.getDimension(R.styleable.AnalogClock_m_width, 0);
        mMinuteHeight = (int) array.getDimension(R.styleable.AnalogClock_m_height, 0);
        mMinuteBoundLeft = (int) array.getDimension(R.styleable.AnalogClock_m_bound_left, 0);
        mSecondWidth = (int) array.getDimension(R.styleable.AnalogClock_s_width, 0);
        mSecondHeight = (int) array.getDimension(R.styleable.AnalogClock_s_height, 0);
        mSecondBoundLeft = (int) array.getDimension(R.styleable.AnalogClock_s_bound_left, 0);
        mDiscWidth = (int) array.getDimension(R.styleable.AnalogClock_disc_width, 0);
        mDiscHeight = (int) array.getDimension(R.styleable.AnalogClock_disc_height, 0);
        array.recycle();

        // 初始化画笔
        mPaint = new Paint();
        mPaint.setColor(Color.parseColor("#3399ff"));
        mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mPaint.setFakeBoldText(true);
        mPaint.setAntiAlias(true);// 抗锯齿
        mPaint.setTextSize(30);//设置文字大小
    }

    /**
     * 时间改变时调用此函数，来更新界面的绘制
     */
    private void getTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY); //时
        int minute = calendar.get(Calendar.MINUTE); //分
        int second = calendar.get(Calendar.SECOND); //秒

        mSecond = second;
        mMinute = minute + second / 60.0f;// 分钟值，加上秒，也是为了使效果逼真
        mHour = hour + mMinute / 60.0f + mSecond / 3600.0f;// 小时值，加上分和秒，效果会更加逼真

        if(showDate) {
            mDay = TimeUtils.millis2String(System.currentTimeMillis(),new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()));
            mWeek = TimeUtils.getUSWeek(System.currentTimeMillis());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        getTime();//初始化时间
        canvas.translate(centerX, centery);
        // 设置表盘图片位置。组件在容器X轴上的起点； 组件在容器Y轴上的起点； 组件的宽度；组件的高度
        mDial.setBounds(-centerX, -centery, mDialWidth-centerX, mDialHeight-centery);
        mDial.draw(canvas);// 这里才是真正把表盘图片画在画板上
        canvas.save();
        //再画时针
        mHourHand.setBounds(-mHourBoundLeft, -mHourHeight/2, mHourWidth-mHourBoundLeft, mHourHeight/2);
        canvas.save();
        canvas.rotate(mHour * 30 - 90, 0, 0);// 旋转画板，第一个参数为旋转角度，第二、三个参数为旋转坐标点
        mHourHand.draw(canvas);// 把时针画在画板上
        canvas.restore();
        //然后画分针
        mMinuteHand.setBounds(-mMinuteBoundLeft, -mMinuteHeight/2, mMinuteWidth-mMinuteBoundLeft, mMinuteHeight/2);
        canvas.save();
        canvas.rotate(mMinute * 6 - 90, 0, 0);
        mMinuteHand.draw(canvas);
        canvas.restore();
        //然后画秒针
        mSecondHand.setBounds(-mSecondBoundLeft, -mSecondHeight/2, mSecondWidth-mSecondBoundLeft, mSecondHeight/2);
        canvas.save();
        canvas.rotate(mSecond * 6 - 90, 0, 0);
        mSecondHand.draw(canvas);
        canvas.restore();
        //最后画圆盘
        mDisc.setBounds(-mDiscWidth/2, -mDiscHeight/2, mDiscWidth/2, mDiscHeight/2);
        mDisc.draw(canvas);// 这里才是真正把表盘图片画在画板上
        canvas.save();

        //画日期
        if(showDate) {
            int textWidth = (int) (mPaint.measureText(mWeek));// 计算文字的宽度
            canvas.drawText(mWeek, (textWidth - textWidth / 2), mDialHeight - (mDialHeight / 8), mPaint);// 画文字在画板上，位置为中间两个参数
            textWidth = (int) (mPaint.measureText(mDay));
            canvas.drawText(mDay, (textWidth - textWidth / 2), mDialHeight + (mDialHeight / 8), mPaint);// 同上
        }

        //刷新
        postInvalidateDelayed(1000);
    }
}