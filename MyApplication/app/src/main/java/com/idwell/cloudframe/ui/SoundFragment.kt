package com.idwell.cloudframe.ui

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.chad.library.adapter.base.BaseQuickAdapter
import com.idwell.cloudframe.R
import com.idwell.cloudframe.adapter.SoundAdapter
import com.idwell.cloudframe.base.BaseFragment
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.entity.MultipleItem
import kotlinx.android.synthetic.main.fragment_recyclerview.*

class SoundFragment : BaseFragment(), BaseQuickAdapter.OnItemClickListener {

    private lateinit var mSoundAdapter: SoundAdapter
    private var mData = mutableListOf<MultipleItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recyclerview, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        //获取系统最大音量和当前音量
        val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val notificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        val maxNotificationVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)

        mData.add(MultipleItem(SoundAdapter.TEXTST_SEEKBARSB, getString(R.string.media_volume), volume, maxVolume))
        //mData.add(MultipleItem(SoundAdapter.TEXTST_SEEKBARSB, getString(R.string.notification_volume), notificationVolume, maxNotificationVolume))
        //mData.add(MultipleItem(SoundAdapter.TEXTSC_SWITCHEC, getString(R.string.touch_sounds), Device.isSoundEffectsEnabled()))
        mSoundAdapter = SoundAdapter(mData)
        rv_fragment_recyclerview.layoutManager = LinearLayoutManager(context)
        context?.let { rv_fragment_recyclerview.addItemDecoration(
            HorizontalItemDecoration(
                ContextCompat.getColor(it, R.color.divider)
            )
        ) }
        rv_fragment_recyclerview.adapter = mSoundAdapter
    }

    override fun initListener() {
        mSoundAdapter.onItemClickListener = this
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        when (position) {
            2 -> {
                mData[2].isChecked = !mData[2].isChecked
                mSoundAdapter.notifyDataSetChanged()
            }
        }
    }
}
