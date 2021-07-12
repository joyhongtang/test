package com.idwell.cloudframe.widget

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.idwell.cloudframe.R

import java.util.Calendar

class AnalogDefaultClock : View {
    /**时钟背景颜色 */
    @ColorInt
    private val colorBackgroundClock = 0xFFF0F0F0.toInt()
    /**时钟圆环颜色 */
    @ColorInt
    private val colorRingClock = ContextCompat.getColor(context, R.color.white)
    /**字体颜色 */
    @ColorInt
    private val colorText = 0xFF141414.toInt()
    /**时钟和分钟的颜色 */
    private val colorMinuteHour = ContextCompat.getColor(context, R.color.white)
    /**秒钟的颜色 */
    @ColorInt
    private val colorSecond = ContextCompat.getColor(context, R.color.white)
    @ColorInt
    private val colorScaleClock = ContextCompat.getColor(context, R.color.white)
    /**时钟最小尺寸 */
    private val sizeMinClock = 200
    /**时钟的宽度 */
    private val widthHour = 6
    /**分钟的宽度 */
    private val widthMinute = 3
    /**秒钟的宽度 */
    private val widthSecond = 2
    /**时钟刻度的宽度 */
    private val widthScale = 4
    //每秒 秒针移动6°
    private val degree = 6
    /**时钟文本 */
    private val textClock = arrayOf("12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")
    /**时 */
    private var hour = 5f
    /**分 */
    private var minute = 30f
    /**秒 */
    private var second = 5f
    /**绘制时钟的Paint */
    private var hourPaint = Paint()
    /**绘制分钟的Paint */
    private var minutePaint = Paint()
    /**绘制秒钟的Paint */
    private var secondPaint = Paint()
    /**圆环的宽度 */
    private val clockRingWidth = 10
    /**时钟大小 */
    private var clockSize: Int = 0
    /**绘制时钟的Paint */
    private var clockPaint = Paint()
    /**绘制时钟圆环的Paint */
    private var clockRingPaint = Paint()
    /**时钟中心外部圆 */
    private var clockCenterOuterCirclePaint = Paint()
    /**时钟中心内部圆 */
    private var clockCenterInnerCirclePaint = Paint()
    /**绘制时钟刻度的Paint */
    private var clockScalePaint = Paint()
    /**绘制时钟文本的Paint */
    private var clockTextPaint = Paint()
    /**获取时间的日历工具 */
    private lateinit var calendar: Calendar

    constructor(context: Context) : super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initView()
    }

    protected fun initView() {
        clockPaint = Paint()
        clockPaint.color = colorBackgroundClock
        clockPaint.isAntiAlias = true
        clockRingPaint = Paint()
        clockRingPaint.color = colorRingClock
        clockRingPaint.strokeWidth = dp2px(clockRingWidth).toFloat()
        clockRingPaint.style = Paint.Style.STROKE
        clockRingPaint.isAntiAlias = true
        //添加阴影 0x80000000
        clockRingPaint.setShadowLayer(4f, 2f, 2f, -0x80000000)
        hourPaint = Paint()
        hourPaint.isAntiAlias = true
        hourPaint.color = colorMinuteHour
        hourPaint.strokeWidth = widthHour.toFloat()
        //设置为圆角
        hourPaint.strokeCap = Paint.Cap.ROUND
        //添加阴影
        hourPaint.setShadowLayer(4f, 0f, 0f, -0x80000000)
        minutePaint = Paint()
        minutePaint.isAntiAlias = true
        minutePaint.color = colorMinuteHour
        minutePaint.strokeWidth = widthMinute.toFloat()
        //设置为圆角
        minutePaint.strokeCap = Paint.Cap.ROUND
        //添加阴影
        minutePaint.setShadowLayer(4f, 0f, 0f, -0x80000000)
        secondPaint = Paint()
        secondPaint.isAntiAlias = true
        secondPaint.color = colorSecond
        secondPaint.strokeWidth = widthSecond.toFloat()
        //设置为圆角
        secondPaint.strokeCap = Paint.Cap.ROUND
        //添加阴影
        secondPaint.setShadowLayer(4f, 3f, 0f, -0x80000000)
        clockCenterOuterCirclePaint = Paint()
        clockCenterOuterCirclePaint.isAntiAlias = true
        clockCenterOuterCirclePaint.color = colorMinuteHour
        //添加阴影
        clockCenterOuterCirclePaint.setShadowLayer(5f, 0f, 0f, -0x80000000)
        clockCenterInnerCirclePaint = Paint()
        clockCenterInnerCirclePaint.isAntiAlias = true
        clockCenterInnerCirclePaint.color = colorSecond
        //添加阴影
        clockCenterInnerCirclePaint.setShadowLayer(5f, 0f, 0f, -0x80000000)
        clockScalePaint = Paint()
        clockScalePaint.isAntiAlias = true
        clockScalePaint.color = colorScaleClock
        //设置为圆角
        clockScalePaint.strokeCap = Paint.Cap.ROUND
        clockScalePaint.strokeWidth = widthScale.toFloat()
        clockTextPaint = Paint()
        clockTextPaint.isAntiAlias = true
        clockTextPaint.strokeWidth = 1f
        clockTextPaint.color = colorText
        clockTextPaint.textSize = sp2px(13).toFloat()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        var width = View.MeasureSpec.getSize(widthMeasureSpec)
        clockSize = dp2px(sizeMinClock)
        if (clockSize > width) {
            width = clockSize
        } else {
            clockSize = width
        }
        setMeasuredDimension(width, width)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            clockSize = w
        }
        val minSize = dp2px(sizeMinClock)
        if (clockSize < minSize) {
            clockSize = minSize
        }
    }

    private fun getTime() {
        calendar = Calendar.getInstance()
        hour = calendar.get(Calendar.HOUR).toFloat()
        minute = calendar.get(Calendar.MINUTE).toFloat()
        second = calendar.get(Calendar.SECOND).toFloat()
        println("$hour:$minute:$second")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        getTime()
        canvas.translate((clockSize / 2).toFloat(), (clockSize / 2).toFloat())
        //drawClock(canvas);
        //drawClockRing(canvas);
        drawClockScale(canvas)
        //drawClockScaleText(canvas);
        drawHourPointer(canvas)
        drawMinutePointer(canvas)
        //drawCenterOuterCircle(canvas);
        drawSecondPointer(canvas, second * degree)
        drawCenterInnerCircle(canvas)
        postInvalidateDelayed(1000)
    }

    /**
     * 画表盘背景
     *
     * @param canvas 画布
     */
    private fun drawClock(canvas: Canvas) {
        canvas.drawCircle(0f, 0f, (clockSize / 2 - 4).toFloat(), clockPaint)
        canvas.save()
    }

    /**
     * 画表盘最外层圆环
     *
     * @param canvas 画布
     */
    private fun drawClockRing(canvas: Canvas) {
        canvas.save()
        val radius = (clockSize / 2 - dp2px(clockRingWidth + 6) / 2).toFloat()
        val rectF = RectF(-radius, -radius, radius, radius)
        clockRingPaint.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(rectF, 0f, 360f, false, clockRingPaint)
        canvas.restore()
    }

    /**
     * 画时针
     *
     * @param canvas 画布
     */
    private fun drawHourPointer(canvas: Canvas) {
        val length = clockSize / 4
        canvas.save()
        //这里没有算秒钟对时钟的影响
        val degree = hour * 5f * degree.toFloat() + minute / 2f
        canvas.rotate(degree, 0f, 0f)
        canvas.drawLine(0f, 0f, 0f, (-length).toFloat(), hourPaint)
        canvas.restore()
    }

    /**
     * 画分针
     *
     * @param canvas 画布
     */
    private fun drawMinutePointer(canvas: Canvas) {
        val length = clockSize / 3 - dp2px(2)
        canvas.save()
        val degree = minute * degree + second / 10f
        canvas.rotate(degree, 0f, 0f)
        canvas.drawLine(0f, 0f, 0f, (-length).toFloat(), minutePaint)
        canvas.restore()
    }

    /**
     * 画秒针
     *
     * @param canvas 画布
     */
    private fun drawSecondPointer(canvas: Canvas, degrees: Float) {
        val length = clockSize / 2 - dp2px(15)
        canvas.save()
        canvas.rotate(degrees)
        canvas.drawLine(0f, (length / 5).toFloat(), 0f, (-length * 4 / 5).toFloat(), secondPaint)
        canvas.restore()
    }

    /**
     * 绘制时钟刻度
     * @param canvas
     */
    private fun drawClockScale(canvas: Canvas) {
        canvas.save()
        val startY = clockSize / 2 - dp2px(clockRingWidth + 6) / 2 - dp2px(clockRingWidth) / 2
        val endY = startY - dp2px(5)
        val endY2 = startY - dp2px(15)
        //canvas.rotate(-180);
        var i = 0
        while (i <= 360) {
            if (i % 5 == 0) {
                canvas.drawLine(0f, startY.toFloat(), 0f, endY2.toFloat(), clockScalePaint)
            } else {
                canvas.drawLine(0f, startY.toFloat(), 0f, endY.toFloat(), clockScalePaint)
            }
            canvas.rotate(degree.toFloat())
            i += degree
        }
        canvas.restore()
    }

    /**
     * 绘制时钟刻度文本
     * @param canvas
     */
    private fun drawClockScaleText(canvas: Canvas) {
        canvas.save()
        //canvas.rotate(-180f);
        val dis = clockTextPaint.measureText(textClock[1]) / 2
        val fontMetrics = clockTextPaint.fontMetrics
        val fontHeight = fontMetrics.descent - fontMetrics.ascent
        val radius = (clockSize / 2).toFloat() - (dp2px(clockRingWidth + 6) / 2).toFloat() - (dp2px(clockRingWidth) / 2).toFloat() - dp2px(10).toFloat() - fontHeight / 2
        for (i in textClock.indices) {
            var x = (Math.sin(Math.PI - Math.PI / 6 * i) * radius - dis).toFloat()
            if (i == 0) {
                x -= dis
            }
            val y = (Math.cos(Math.PI - Math.PI / 6 * i) * radius + dis).toFloat()
            canvas.drawText(textClock[i], x, y, clockTextPaint)
        }
        canvas.restore()
    }

    /**
     * 画中心黑圆
     *
     * @param canvas 画布
     */
    private fun drawCenterOuterCircle(canvas: Canvas) {
        val radius = clockSize / 20
        canvas.save()
        canvas.drawCircle(0f, 0f, radius.toFloat(), clockCenterOuterCirclePaint)
        canvas.restore()
    }

    /**
     * 红色中心圆
     *
     * @param canvas 画布
     */
    private fun drawCenterInnerCircle(canvas: Canvas) {
        val radius = clockSize / 60
        canvas.save()
        canvas.drawCircle(0f, 0f, radius.toFloat(), clockCenterInnerCirclePaint)
        canvas.restore()
    }

    /**
     * 将 dp 转换为 px
     *
     * @param dp 需转换数
     * @return 返回转换结果
     */
    private fun dp2px(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
    }

    private fun sp2px(sp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), resources.displayMetrics).toInt()
    }
}