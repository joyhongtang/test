package com.idwell.cloudframe.ui

import android.content.res.Configuration
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ConvertUtils
import com.google.gson.Gson

import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.db.entity.Alarm
import kotlinx.android.synthetic.main.activity_alarm.*
import kotlinx.android.synthetic.main.activity_base.*

class AlarmActivity : BaseActivity() {

    private lateinit var fragmentManager: FragmentManager
    private var alarmFragment: AlarmFragment? = null
    private var alarmInstanceFragment: AlarmInstanceFragment? = null

    override fun initLayout(): Int {
        return R.layout.activity_alarm
    }

    override fun initData() {
        //tv_title_base.setText(R.string.alarm)
        iv_title_base.setImageResource(R.drawable.ic_alarm)
        onOrientationChanged(resources.configuration.orientation)
        fragmentManager = supportFragmentManager
        alarmFragment = AlarmFragment()
        // 开启一个Fragment事务
        val transaction = fragmentManager.beginTransaction()
        alarmFragment?.let { transaction.add(R.id.fl_alarm, it) }
        transaction.commit()
    }

    override fun initListener() {

    }

    override fun initMessageEvent(event: MessageEvent) {

    }

    fun showFragment(index: Int) {
        val transaction = fragmentManager.beginTransaction()
        hideFragment(transaction)
        when (index) {
            0 -> if (alarmFragment == null) {
                alarmFragment = AlarmFragment()
                alarmFragment?.let { transaction.add(R.id.fl_alarm, it) }
            } else {
                alarmFragment?.let(transaction::show)
            }
            1 -> {
                alarmInstanceFragment = AlarmInstanceFragment()
                transaction.add(R.id.fl_alarm, alarmInstanceFragment!!)
            }
        }
        transaction.commit()
    }

    fun showFragment(alarm: Alarm?) {
        val transaction = fragmentManager.beginTransaction()
        hideFragment(transaction)
        val bundle = Bundle()
        bundle.putString("alarm", Gson().toJson(alarm))
        alarmInstanceFragment = AlarmInstanceFragment()
        alarmInstanceFragment!!.arguments = bundle
        transaction.add(R.id.fl_alarm, alarmInstanceFragment!!)
        transaction.commit()
    }

    private fun hideFragment(transaction: androidx.fragment.app.FragmentTransaction) {
        alarmFragment?.let(transaction::hide)
        alarmInstanceFragment?.let {
            transaction.remove(it)
            alarmInstanceFragment = null
        }
    }

    private fun onOrientationChanged(orientation: Int){
        val constraintSet = ConstraintSet()
        if (orientation == Configuration.ORIENTATION_LANDSCAPE){
            constraintSet.connect(R.id.cl_clock_alarm, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.cl_clock_alarm, ConstraintSet.END, R.id.fl_alarm, ConstraintSet.START)
            constraintSet.connect(R.id.cl_clock_alarm, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_clock_alarm, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.setHorizontalWeight(R.id.cl_clock_alarm, 0.5f)
            constraintSet.constrainPercentHeight(R.id.cl_clock_alarm, 1.0f)
            constraintSet.connect(R.id.fl_alarm, ConstraintSet.START, R.id.cl_clock_alarm, ConstraintSet.END)
            constraintSet.connect(R.id.fl_alarm, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.fl_alarm, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(R.id.fl_alarm, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.setHorizontalWeight(R.id.fl_alarm, 0.5f)
            constraintSet.constrainPercentHeight(R.id.fl_alarm, 1.0f)
            constraintSet.setMargin(R.id.fl_alarm, 3, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.fl_alarm, 4, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.fl_alarm, 6, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.fl_alarm, 7, ConvertUtils.dp2px(50f))
        }else{
            constraintSet.connect(R.id.cl_clock_alarm, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.cl_clock_alarm, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.cl_clock_alarm, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(R.id.cl_clock_alarm, ConstraintSet.BOTTOM, R.id.fl_alarm, ConstraintSet.TOP)
            constraintSet.constrainPercentWidth(R.id.cl_clock_alarm, 1.0f)
            constraintSet.setVerticalWeight(R.id.cl_clock_alarm, 0.5f)
            constraintSet.connect(R.id.fl_alarm, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.fl_alarm, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.fl_alarm, ConstraintSet.TOP, R.id.cl_clock_alarm, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.fl_alarm, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.constrainPercentWidth(R.id.fl_alarm, 1.0f)
            constraintSet.setVerticalWeight(R.id.fl_alarm, 0.5f)
            constraintSet.setMargin(R.id.fl_alarm, 3, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.fl_alarm, 4, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.fl_alarm, 6, ConvertUtils.dp2px(50f))
            constraintSet.setMargin(R.id.fl_alarm, 7, ConvertUtils.dp2px(50f))
        }
        constraintSet.applyTo(cl_alarm)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onOrientationChanged(newConfig.orientation)
    }
}