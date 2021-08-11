package com.idwell.cloudframe.ui

import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText

import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.TimeUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseFragment
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.db.entity.Alarm

import java.io.File
import java.text.SimpleDateFormat

import com.idwell.cloudframe.entity.BaseItem
import com.idwell.cloudframe.util.AlarmUtil
import com.idwell.cloudframe.util.TimeUtil
import com.idwell.cloudframe.widget.MaterialDialog
import com.idwell.cloudframe.widget.MyTimePickerDialog
import kotlinx.android.synthetic.main.fragment_alarm_instance.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

class AlarmInstanceFragment : BaseFragment(), MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener, BaseQuickAdapter.OnItemClickListener {

    private val mTag = AlarmInstanceFragment::class.java.simpleName

    private lateinit var mMediaPlayer: MediaPlayer
    private var mState = PlaybackStateCompat.STATE_NONE
    private var mAudioManager: AudioManager? = null

    private var mAlarmActivity: AlarmActivity? = null
    private lateinit var mAlarm: Alarm
    private var mRingtone = ""
    private lateinit var mAdapter: BaseQuickAdapter<BaseItem, BaseViewHolder>
    private lateinit var data: MutableList<BaseItem>

    private lateinit var repeatKeys: List<String>
    private var weeks: MutableList<String>? = null
    private lateinit var mRingtones: MutableList<File>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_alarm_instance, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        mAlarmActivity = context as AlarmActivity
        mMediaPlayer = MediaPlayer()

        repeatKeys = Arrays.asList(*resources.getStringArray(R.array.repeats))
        mRingtones = FileUtils.listFilesInDir("/system/media/audio/alarms")
        weeks = context?.resources?.getStringArray(R.array.weeks_abbr)?.toMutableList()
        val calendar = Calendar.getInstance()
        val parcelable = Gson().fromJson<Alarm>(arguments?.getString("alarm"), Alarm::class.java)
        if (parcelable == null) {
            mAlarm = Alarm(0, mutableListOf(), mRingtones[0].absolutePath, getString(R.string.alarm), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.timeInMillis), true)
        } else {
            mAlarm = parcelable
            calendar.set(Calendar.HOUR_OF_DAY, mAlarm.hour)
            calendar.set(Calendar.MINUTE, mAlarm.minute)
        }
        mRingtone = mAlarm.ringtone
        val name = File(mAlarm.ringtone).name
        data = mutableListOf(BaseItem(R.string.repeat, convertRepeat()), BaseItem(R.string.ringtone, name.substring(0, name.lastIndexOf('.'))), BaseItem(R.string.label, mAlarm.label), BaseItem(R.string.set_time, mAlarm.time))
        mAdapter = object : BaseQuickAdapter<BaseItem, BaseViewHolder>(R.layout.item_fragment_alarm_instance, data) {
            override fun convert(helper: BaseViewHolder, item: BaseItem?) {
                if (item != null) {
                    helper.setText(R.id.tv_title_item_fragment_alarm_instance, item.titleResId)
                            .setText(R.id.tv_content_item_fragment_alarm_instance, item.content)
                }
            }
        }
        rv_alarm_instance.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        context?.let {
            rv_alarm_instance.addItemDecoration(HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider)))
        }
        rv_alarm_instance.adapter = mAdapter

        mAudioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mAudioManager?.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    }

    override fun initListener() {
        mAdapter.onItemClickListener = this
        mMediaPlayer.setOnPreparedListener(this)
        tv_ok_alarm_instance.setOnClickListener(this)
        tv_cancel_alarm_instance.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            tv_ok_alarm_instance -> {
                GlobalScope.launch {
                    MyDatabase.instance.alarmDao.insert(mAlarm)
                    val alarm = MyDatabase.instance.alarmDao.queryByTime(mAlarm.time)
                    LogUtils.dTag(mTag, "alarm.id = " + alarm.id)
                    AlarmUtil.startAlarm(alarm)
                    mAlarmActivity?.showFragment(0)
                }
            }
            tv_cancel_alarm_instance -> mAlarmActivity?.showFragment(0)
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mp?.start()
        mState = PlaybackStateCompat.STATE_PLAYING
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        when (position) {
            0 -> context?.let {
                val repeat = mutableListOf<Int>()
                repeat.addAll(mAlarm.repeat)
                MaterialDialog.Builder(it)
                        .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textsc_imageec_dialog, weeks) {
                            override fun convert(helper: BaseViewHolder, item: String) {
                                helper.setText(R.id.tv_title_item_textsc_imageec_dialog, item)
                                        .setImageResource(R.id.iv_item_textsc_imageec_dialog, if (repeat.contains(helper.layoutPosition)) R.drawable.ic_check_circle_blue else R.drawable.gray_ring_shape)
                            }
                        }, BaseQuickAdapter.OnItemClickListener { adapter1, _, position1 ->
                            if (repeat.contains(position1)) {
                                repeat.remove(position1)
                            } else {
                                repeat.add(position1)
                                repeat.sort()
                            }
                            adapter1.notifyDataSetChanged()
                        }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content)))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                            override fun onClick(dialog: MaterialDialog) {
                                mAlarm.repeat.clear()
                                mAlarm.repeat.addAll(repeat)
                                data[0].content = convertRepeat()
                                mAdapter.notifyDataSetChanged()
                            }
                        }).show()
            }
            1 -> {
                context?.let {
                    MaterialDialog.Builder(it).setTitle(R.string.ringtone)
                            .setAdapter(object : BaseQuickAdapter<File, BaseViewHolder>(R.layout.item_textsc_imageec_dialog, mRingtones) {
                                override fun convert(helper: BaseViewHolder, item: File) {
                                    val name = mRingtones[helper.adapterPosition].name
                                    helper.setText(R.id.tv_title_item_textsc_imageec_dialog, name.substring(0, name.lastIndexOf(".")))
                                            .setImageResource(R.id.iv_item_textsc_imageec_dialog, if (helper.adapterPosition == mRingtones.indexOf(File(mRingtone))) R.drawable.ic_check_circle_blue else R.drawable.gray_ring_shape)
                                }
                            }, BaseQuickAdapter.OnItemClickListener { adapter1, _, position1 ->
                                if (position1 == mRingtones.indexOf(File(mRingtone))) {
                                    when {
                                        mState == PlaybackStateCompat.STATE_NONE -> setDataSource()
                                        mMediaPlayer.isPlaying -> {
                                            mMediaPlayer.pause()
                                            mState = PlaybackStateCompat.STATE_PAUSED
                                        }
                                        else -> {
                                            mMediaPlayer.start()
                                            mState = PlaybackStateCompat.STATE_PLAYING
                                        }
                                    }
                                } else {
                                    mRingtone = mRingtones[position1].absolutePath
                                    adapter1.notifyDataSetChanged()
                                    setDataSource()
                                }
                            }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content)))
                            .setLayoutParamsHeight()
                            .setNegativeButton(R.string.cancel, object : MaterialDialog.OnClickListener {
                                override fun onClick(dialog: MaterialDialog) {
                                    mRingtone = mAlarm.ringtone
                                    mMediaPlayer.pause()
                                }
                            })
                            .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                                override fun onClick(dialog: MaterialDialog) {
                                    mAlarm.ringtone = mRingtone
                                    val name = File(mRingtone).name
                                    data[1].content = name.substring(0, name.lastIndexOf("."))
                                    mAdapter.notifyDataSetChanged()
                                    mMediaPlayer.pause()
                                }
                            }).setOnCancelListener(object : MaterialDialog.OnCancelListener {
                                override fun onCancel(dialog: DialogInterface) {
                                    mRingtone = mAlarm.ringtone
                                    mMediaPlayer.pause()
                                }
                            }).show()
                }
            }
            2 -> context?.let {
                MaterialDialog.Builder(it).setEditContent(mAlarm.label)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                            override fun onClick(dialog: MaterialDialog) {
                                val editText = dialog.findViewById<EditText>(R.id.et_content_dialog_material)
                                mAlarm.label = editText.text.toString().trim()
                                data[2].content = editText.text.toString().trim()
                                mAdapter.notifyDataSetChanged()
                            }
                        }).show()
            }
            3 -> {
                val calendar = Calendar.getInstance()
                val timePickerDialog = MyTimePickerDialog(context, R.style.DateTimePicker, TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    mAlarm.hour = hour
                    mAlarm.minute = minute
                    mAlarm.time = TimeUtils.date2String(calendar.time, SimpleDateFormat("HH:mm", Locale.getDefault()))
                    data[3].content = mAlarm.time
                    mAdapter.notifyDataSetChanged()
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), TimeUtil.isTime24)
                timePickerDialog.show()
                timePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE).textSize = 24f
                timePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE).textSize = 24f
            }
        }
    }

    private fun convertRepeat(): String {
        return when (mAlarm.repeat.size) {
            0 -> getString(R.string.off)
            7 -> getString(R.string.everyday)
            else -> {
                val weeks = resources.getStringArray(R.array.weeks_abbr)
                val repeat = mutableListOf<String>()
                for (i in mAlarm.repeat) {
                    repeat.add(weeks[i])
                }
                Gson().toJson(repeat).replace("[\"", "").replace("\",\"", ", ").replace("\"]", "")
            }
        }
    }

    private fun setDataSource() {
        try {
            mMediaPlayer.reset()
            mMediaPlayer.setDataSource(mRingtone)
            mMediaPlayer.prepareAsync()
            mState = PlaybackStateCompat.STATE_NONE
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            // 永久丢失焦点，如被其他播放器抢占
            // 短暂丢失焦点，如来电
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (mState == PlaybackStateCompat.STATE_PLAYING) mMediaPlayer.pause()
            // 瞬间丢失焦点，如通知
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (mState == PlaybackStateCompat.STATE_PLAYING) mMediaPlayer.setVolume(0.5f, 0.5f)
        }
    }

    override fun onDestroy() {
        mAudioManager?.abandonAudioFocus(this)
        mMediaPlayer.release()
        super.onDestroy()
    }
}