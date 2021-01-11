package com.idwell.cloudframe.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import com.blankj.utilcode.util.ConvertUtils

import com.idwell.cloudframe.R
import kotlinx.android.synthetic.main.analog_clock0.*

class Clock0Fragment : Fragment() {

    private var imgRes = intArrayOf(
        R.drawable.digital_wheel_0,
        R.drawable.digital_wheel_1,
        R.drawable.digital_wheel_2,
        R.drawable.digital_wheel_3,
        R.drawable.digital_wheel_4,
        R.drawable.digital_wheel_5,
        R.drawable.digital_wheel_6,
        R.drawable.digital_wheel_7,
        R.drawable.digital_wheel_8,
        R.drawable.digital_wheel_9
    )
    private var iv_se0: ImageView? = null
    private var iv_sf0: ImageView? = null
    private var iv_me0: ImageView? = null
    private var iv_mf0: ImageView? = null
    private var iv_he0: ImageView? = null
    private var iv_hf2: ImageView? = null
    private var firstIn = true

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action){
                Intent.ACTION_TIME_TICK -> {
                    tv_week_clock8.text = DateFormat.format("EEEE", System.currentTimeMillis())
                    tv_date_clock8.text = DateFormat.getLongDateFormat(context).format(System.currentTimeMillis())
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.analog_clock0, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        onOrientationChanged(resources.configuration.orientation)

        tv_week_clock8.text = DateFormat.format("EEEE", System.currentTimeMillis())
        tv_date_clock8.text = DateFormat.getLongDateFormat(context).format(System.currentTimeMillis())

        for (i in imgRes.indices) {
            val iv_se = ImageView(context)
            iv_se.scaleType = ImageView.ScaleType.FIT_XY
            iv_se.setImageResource(imgRes[i])
            vf_se_clock_digital_wheel.addView(iv_se)
            val iv_me = ImageView(context)
            iv_me.scaleType = ImageView.ScaleType.FIT_XY
            iv_me.setImageResource(imgRes[i])
            vf_me_clock_digital_wheel.addView(iv_me)
            if (i <= 5) {
                val iv_sf = ImageView(context)
                iv_sf.scaleType = ImageView.ScaleType.FIT_XY
                iv_sf.setImageResource(imgRes[i])
                vf_sf_clock_digital_wheel.addView(iv_sf)
                val iv_mf = ImageView(context)
                iv_mf.scaleType = ImageView.ScaleType.FIT_XY
                iv_mf.setImageResource(imgRes[i])
                vf_mf_clock_digital_wheel.addView(iv_mf)
                if (i <= 2) {
                    val iv_hf = ImageView(context)
                    iv_hf.scaleType = ImageView.ScaleType.FIT_XY
                    iv_hf.setImageResource(imgRes[i])
                    vf_hf_clock_digital_wheel.addView(iv_hf)
                    if (i == 0) {
                        iv_se0 = iv_se
                        iv_sf0 = iv_sf
                        iv_me0 = iv_me
                        iv_mf0 = iv_mf
                    } else if (i == 2) {
                        iv_hf2 = iv_hf
                    }
                }
            }
        }

        val time = DateFormat.format("HHmmss", System.currentTimeMillis())
        val fHour = Integer.parseInt(time.substring(0, 1))
        val eHour = Integer.parseInt(time.substring(1, 2))
        val fMinute = Integer.parseInt(time.substring(2, 3))
        val eMinute = Integer.parseInt(time.substring(3, 4))
        val fSecond = Integer.parseInt(time.substring(4, 5))
        val eSecond = Integer.parseInt(time.substring(5))

        if (fHour == 2) {
            for (i in 0..3) {
                val iv_he = ImageView(context)
                iv_he.scaleType = ImageView.ScaleType.FIT_XY
                iv_he.setImageResource(imgRes[i])
                vf_he_clock_digital_wheel.addView(iv_he)
                if (i == 0) {
                    iv_he0 = iv_he
                }
            }
        } else {
            for (i in imgRes.indices) {
                val iv_he = ImageView(context)
                iv_he.scaleType = ImageView.ScaleType.FIT_XY
                iv_he.setImageResource(imgRes[i])
                vf_he_clock_digital_wheel.addView(iv_he)
                if (i == 0) {
                    iv_he0 = iv_he
                }
            }
        }

        vf_hf_clock_digital_wheel.displayedChild = fHour
        vf_he_clock_digital_wheel.displayedChild = eHour
        vf_mf_clock_digital_wheel.displayedChild = fMinute
        vf_me_clock_digital_wheel.displayedChild = eMinute
        vf_sf_clock_digital_wheel.displayedChild = fSecond
        vf_se_clock_digital_wheel.displayedChild = eSecond
        vf_se_clock_digital_wheel.startFlipping()

        //初始化监听
        initListener()

        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_TIME_TICK)
        context?.registerReceiver(mBroadcastReceiver, intentFilter)
    }

    private fun initListener() {
        vf_se_clock_digital_wheel.inAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                if (!firstIn && vf_se_clock_digital_wheel.currentView == iv_se0) {
                    vf_sf_clock_digital_wheel.showNext()
                }
                if (firstIn) {
                    firstIn = false
                }
            }

            override fun onAnimationEnd(animation: Animation) {

            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        vf_sf_clock_digital_wheel.inAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                if (!firstIn && vf_sf_clock_digital_wheel.currentView == iv_sf0) {
                    vf_me_clock_digital_wheel.showNext()
                }
            }

            override fun onAnimationEnd(animation: Animation) {

            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        vf_me_clock_digital_wheel.inAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                if (!firstIn && vf_me_clock_digital_wheel.currentView == iv_me0) {
                    vf_mf_clock_digital_wheel.showNext()
                }
            }

            override fun onAnimationEnd(animation: Animation) {

            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        vf_mf_clock_digital_wheel.inAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                if (!firstIn && vf_mf_clock_digital_wheel.currentView == iv_mf0) {
                    vf_he_clock_digital_wheel.showNext()
                }
            }

            override fun onAnimationEnd(animation: Animation) {

            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        vf_he_clock_digital_wheel.inAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                if (!firstIn && vf_he_clock_digital_wheel.currentView == iv_he0) {
                    vf_hf_clock_digital_wheel.showNext()
                }
            }

            override fun onAnimationEnd(animation: Animation) {

            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })
        vf_hf_clock_digital_wheel.inAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                if (!firstIn) {
                    vf_he_clock_digital_wheel.removeAllViews()
                    if (vf_hf_clock_digital_wheel.currentView == iv_hf2) {
                        for (i in 0..3) {
                            val iv_he = ImageView(context)
                            iv_he.scaleType = ImageView.ScaleType.FIT_XY
                            iv_he.setImageResource(imgRes[i])
                            vf_he_clock_digital_wheel.addView(iv_he)
                            if (i == 0) {
                                iv_he0 = iv_he
                            }
                        }
                    } else {
                        for (i in imgRes.indices) {
                            val iv_he = ImageView(context)
                            iv_he.scaleType = ImageView.ScaleType.FIT_XY
                            iv_he.setImageResource(imgRes[i])
                            vf_he_clock_digital_wheel.addView(iv_he)
                            if (i == 0) {
                                iv_he0 = iv_he
                            }
                        }
                    }
                }
            }

            override fun onAnimationEnd(animation: Animation) {

            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })
    }

    private fun onOrientationChanged(orientation: Int){
        val constraintSet = ConstraintSet()
        if (orientation == Configuration.ORIENTATION_LANDSCAPE){
            constraintSet.connect(R.id.cl_clock_clock0, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_clock_clock0, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.cl_clock_clock0, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.cl_clock_clock0, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.constrainPercentWidth(R.id.cl_clock_clock0, 0.7f)
            constraintSet.constrainPercentHeight(R.id.cl_clock_clock0, 0.46f)
        }else{
            constraintSet.connect(R.id.cl_clock_clock0, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_clock_clock0, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.cl_clock_clock0, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.cl_clock_clock0, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.constrainPercentWidth(R.id.cl_clock_clock0, 0.9f)
            constraintSet.constrainPercentHeight(R.id.cl_clock_clock0, 0.2f)
        }
        constraintSet.applyTo(cl_clock0)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onOrientationChanged(newConfig.orientation)
    }

    override fun onDestroy() {
        context?.unregisterReceiver(mBroadcastReceiver)
        super.onDestroy()
    }
}