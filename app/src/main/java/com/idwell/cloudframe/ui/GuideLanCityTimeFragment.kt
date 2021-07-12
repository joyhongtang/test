package com.idwell.cloudframe.ui

import android.app.AlarmManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson

import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseFragment
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.entity.MultipleItem
import com.idwell.cloudframe.http.BaseHttpObserver
import com.idwell.cloudframe.http.RetrofitManager
import com.idwell.cloudframe.http.entity.City
import com.idwell.cloudframe.http.service.GetCityListService
import com.idwell.cloudframe.util.TimeUtil
import com.idwell.cloudframe.widget.MaterialDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_guide_lan_city_time.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class GuideLanCityTimeFragment : BaseFragment(), BaseQuickAdapter.OnItemChildClickListener {

    private var cityNameAfterChanged = ""

    private lateinit var mLanguages: List<String>
    private lateinit var mLanguageCodes: List<String>
    private lateinit var mCountryCodes: List<String>
    private val mTimeZoneCities = mutableListOf<String>()
    private val mTimeZoneGmts = mutableListOf<String>()
    private val mTimeZoneIds = mutableListOf<String>()
    private val mLanCityTimeData = mutableListOf<MultipleItem>()
    private lateinit var mLanCityTimeAdapter: BaseMultiItemQuickAdapter<MultipleItem, BaseViewHolder>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_guide_lan_city_time, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        mLanguages = listOf(*resources.getStringArray(R.array.languages))
        mLanguageCodes = listOf(*resources.getStringArray(R.array.system_language_code))
        mCountryCodes = listOf(*resources.getStringArray(R.array.system_country_code))
        val timeZoneList = TimeUtil.timeZoneList
        for (hashMap in timeZoneList) {
            mTimeZoneCities.add(hashMap[TimeUtil.KEY_NAME] as String)
            mTimeZoneGmts.add(hashMap[TimeUtil.KEY_GMT] as String)
            mTimeZoneIds.add(hashMap[TimeUtil.KEY_ID] as String)
        }
        val timeZoneIndex = mTimeZoneIds.indexOf(TimeUtil.timeZone.id)
        val timeZone = mTimeZoneGmts[timeZoneIndex] + ", " + mTimeZoneCities[timeZoneIndex]
        LogUtils.dTag(TAG, Device.getCountry())
        var codeIndex = mCountryCodes.indexOf(Device.getCountry())
        if (codeIndex == -1) {
            codeIndex = 0
        }
        mLanCityTimeData.add(MultipleItem(TYPE_0, getString(R.string.language), mLanguages[codeIndex]))
        mLanCityTimeData.add(MultipleItem(TYPE_1, getString(R.string.city), ""))
        mLanCityTimeData.add(MultipleItem(TYPE_0, getString(R.string.time_zone), timeZone))

        mLanCityTimeAdapter = object : BaseMultiItemQuickAdapter<MultipleItem, BaseViewHolder>(mLanCityTimeData) {
            init {
                addItemType(TYPE_0, R.layout.item_type_0_lan_city_time)
                addItemType(TYPE_1, R.layout.item_type_1_lan_city_time)
            }

            override fun convert(helper: BaseViewHolder, item: MultipleItem?) {
                if (item != null) {
                    when (helper.itemViewType) {
                        TYPE_0 -> {
                            helper.setText(R.id.tv_title_item_type_0_lan_city_time, item.title)
                                    .setText(R.id.tv_content_item_type_0_lan_city_time, item.content)
                                    .addOnClickListener(R.id.tv_content_item_type_0_lan_city_time)
                        }
                        TYPE_1 -> {
                            helper.setText(R.id.tv_title_item_type_1_lan_city_time, item.title)
                                    .setText(R.id.et_content_item_type_1_lan_city_time, item.content)
                            helper.getView<EditText>(R.id.et_content_item_type_1_lan_city_time)
                                    .addTextChangedListener(object : TextWatcher {
                                        override fun afterTextChanged(s: Editable?) {
                                            if (s != null) {
                                                LogUtils.dTag(GuideLanCityTimeFragment::class.java.simpleName, s.toString())
                                                cityNameAfterChanged = s.toString()
                                            }
                                        }

                                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                                        }

                                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                                        }
                                    })
                            helper.getView<EditText>(R.id.et_content_item_type_1_lan_city_time)
                                    .setOnEditorActionListener { v, actionId, event ->
                                        val cityName = v.text.toString().trim()
                                        LogUtils.dTag(GuideLanCityTimeFragment::class.java.simpleName, cityName)
                                        if (actionId == EditorInfo.IME_ACTION_DONE && cityName.isNotEmpty()) {
                                            getCityList(cityName)
                                        }
                                        false
                                    }
                        }
                    }
                }
            }
        }
        rv_content_guide_lan_city_time.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        context?.let {
            rv_content_guide_lan_city_time.addItemDecoration(HorizontalItemDecoration(it, resources.getDimension(R.dimen.dp_10).toInt(), false))
        }
        rv_content_guide_lan_city_time.adapter = mLanCityTimeAdapter
    }

    override fun initListener() {
        iv_prev_guide_lan_city_time.setOnClickListener(this)
        iv_next_guide_lan_city_time.setOnClickListener(this)
        tv_skip_guide_lan_city_time.setOnClickListener(this)
        mLanCityTimeAdapter.onItemChildClickListener = this
    }

    override fun onClick(v: View?) {
        when (v) {
            iv_prev_guide_lan_city_time -> {
                activity?.let {
                    (it as GuideActivity).showFragment(0)
                }
            }
            iv_next_guide_lan_city_time -> {
                if (cityNameAfterChanged.isEmpty()) {
                    context?.let {
                        MaterialDialog.Builder(it).setTitle(R.string.city_name_cannot_be_empty)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.ok, null).show()
                    }
                } else {
                    if (cityNameAfterChanged == mLanCityTimeData[1].content) {
                        activity?.let {
                            (it as GuideActivity).showFragment(2)
                        }
                    } else {
                        context?.let {
                            MaterialDialog.Builder(it).setTitle(R.string.invalid_city_name)
                                    .setNegativeButton(R.string.cancel, null)
                                    .setPositiveButton(R.string.ok, null).show()
                        }
                    }
                }
            }
            tv_skip_guide_lan_city_time -> {
                activity?.let {
                    (it as GuideActivity).showFragment(2)
                }
            }
        }
    }

    override fun onItemChildClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        when (position) {
            0 -> {
                activity?.let {
                    var index = mCountryCodes.indexOf(Device.getCountry())
                    MaterialDialog.Builder(it)
                            .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textsc_imageec_dialog, mLanguages) {
                                override fun convert(helper: BaseViewHolder, item: String) {
                                    val adapterPosition = helper.adapterPosition
                                    helper.setText(R.id.tv_title_item_textsc_imageec_dialog, item)
                                            .setImageResource(R.id.iv_item_textsc_imageec_dialog, if (index == adapterPosition) R.drawable.ic_check_circle_blue else R.drawable.gray_ring_shape)
                                }
                            }, BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
                                index = position
                                adapter.notifyDataSetChanged()
                            }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content)))
                            .setLayoutParamsHeight().setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                                override fun onClick(dialog: MaterialDialog) {
                                    if (index != mCountryCodes.indexOf(Device.getCountry())) {
                                        mLanCityTimeData[0].content = mLanguages[index]
                                        mLanCityTimeAdapter.notifyDataSetChanged()
                                        (it as GuideActivity).removeFragment()
                                        it.changeSystemLanguage(Locale(mLanguageCodes[index], mCountryCodes[index]))
                                    }
                                }
                            }).show().findViewById<RecyclerView>(R.id.rv_content_dialog_material)
                            .scrollToPosition(index)
                }
            }
            1 -> {
            }
            2 -> {
                context?.let {
                    var index = mTimeZoneIds.indexOf(TimeUtil.timeZone.id)
                    MaterialDialog.Builder(it)
                            .setAdapter(object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textst_textsb_imageec_dialog, mTimeZoneCities) {
                                override fun convert(helper: BaseViewHolder, item: String) {
                                    val adapterPosition = helper.adapterPosition
                                    helper.setText(R.id.tv_title_item_textst_textsb_imageec_dialog, mTimeZoneCities[adapterPosition])
                                    helper.setText(R.id.tv_content_item_textst_textsb_imageec_dialog, mTimeZoneGmts[adapterPosition])
                                    Glide.with(mContext)
                                            .load(if (index == adapterPosition) R.drawable.ic_check_circle_blue else R.drawable.gray_ring_shape)
                                            .into(helper.getView<View>(R.id.iv_item_textst_textsb_imageec_dialog) as ImageView)
                                }
                            }, BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
                                index = position
                                adapter.notifyDataSetChanged()
                            }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content)))
                            .setLayoutParamsHeight().setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                                override fun onClick(dialog: MaterialDialog) {
                                    mLanCityTimeData[2].content = mTimeZoneGmts[index] + ", " + mTimeZoneCities[index]
                                    mLanCityTimeAdapter.notifyDataSetChanged()
                                    val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                    alarmManager.setTimeZone(mTimeZoneIds[index])
                                }
                            }).show().findViewById<RecyclerView>(R.id.rv_content_dialog_material)
                            .scrollToPosition(index)
                }
            }
        }
    }

    private fun getCityList(cityName: String) {
        context?.let {
            RetrofitManager.getService(GetCityListService::class.java).getCityList(cityName)
                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : BaseHttpObserver<MutableList<City>>(it) {
                        override fun onSuccess(data: MutableList<City>) {
                            showCityList(data)
                        }
                    })
        }
    }

    private fun showCityList(cities: MutableList<City>) {
        context?.let {
            var index = -1
            MaterialDialog.Builder(it)
                    .setAdapter(object : BaseQuickAdapter<City, BaseViewHolder>(R.layout.item_textsc_imageec_dialog, cities) {
                        override fun convert(helper: BaseViewHolder, item: City) {
                            val adapterPosition = helper.adapterPosition
                            helper.setText(R.id.tv_title_item_textsc_imageec_dialog, item.qualifiedName)
                                    .setImageResource(R.id.iv_item_textsc_imageec_dialog, if (index == adapterPosition) R.drawable.ic_check_circle_blue else R.drawable.gray_ring_shape)
                        }
                    }, BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
                        index = position
                        adapter.notifyDataSetChanged()
                    }, HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider_light_dialog_content)))
                    .setLayoutParamsHeight().setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, object : MaterialDialog.OnClickListener {
                        override fun onClick(dialog: MaterialDialog) {
                            if (index != -1) {
                                val city = cities[index]
                                Device.curCity = Gson().toJson(city)
                                mLanCityTimeData[1].content = city.qualifiedName
                                mLanCityTimeAdapter.notifyDataSetChanged()

                                GlobalScope.launch {
                                    MyDatabase.instance.placeDao.insert(city)
                                    MyDatabase.instance.placeDao.deleteLimit()
                                }
                                Log.e("GGGGG","CR "+Device.curCity)
                            }
                        }
                    }).show()
        }
    }

    companion object {
        private val TAG = GuideLanCityTimeFragment::class.java.simpleName
        private const val TYPE_0 = 0
        private const val TYPE_1 = 1
    }
}
