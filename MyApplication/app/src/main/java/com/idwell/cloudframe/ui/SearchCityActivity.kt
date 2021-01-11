package com.idwell.cloudframe.ui

import android.content.Context
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseActivity
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.common.MessageEvent
import com.idwell.cloudframe.http.RetrofitManager
import com.idwell.cloudframe.http.service.GetCityListService
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import com.idwell.cloudframe.http.BaseHttpObserver
import com.idwell.cloudframe.http.entity.City
import com.idwell.cloudframe.http.entity.Forecast
import com.idwell.cloudframe.http.service.WeatherService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.activity_search_city.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SearchCityActivity : BaseActivity(), BaseQuickAdapter.OnItemClickListener {

    private lateinit var mCities: MutableList<City>
    private lateinit var mBaseQuickAdapter: BaseQuickAdapter<City, BaseViewHolder>


    override fun initLayout(): Int {
        return R.layout.activity_search_city
    }

    override fun initData() {
        tv_title_base.setText(R.string.search_city)
        et_search_city.imeOptions = EditorInfo.IME_ACTION_DONE
        GlobalScope.launch {
            mCities = MyDatabase.instance.placeDao.queryOrderByAddressAsc()
            mBaseQuickAdapter = object : BaseQuickAdapter<City, BaseViewHolder>(R.layout.item_city, mCities) {
                override fun convert(helper: BaseViewHolder, item: City?) {
                    if (item != null){
                        helper.setText(R.id.tv_city_item_city, item.qualifiedName)
                    }
                }
            }
            rv_search_city.layoutManager = LinearLayoutManager(this@SearchCityActivity)
            rv_search_city.addItemDecoration(
                HorizontalItemDecoration(
                    ContextCompat.getColor(
                        this@SearchCityActivity,
                        R.color.divider
                    )
                )
            )

            rv_search_city.adapter = mBaseQuickAdapter
            mBaseQuickAdapter.onItemClickListener = this@SearchCityActivity
        }
    }

    override fun initListener() {
        iv_search_city.setOnClickListener(this)
        et_search_city.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && !et_search_city.text.toString().trim().isEmpty()) {
                searchCity(et_search_city.text.toString().trim())
            }
            false
        }
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        when (v) {
            iv_search_city -> if (et_search_city.text.toString().trim().isEmpty()) {
                ToastUtils.showShort(R.string.please_enter_the_city_name)
            } else {
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(v?.windowToken, 0)

                if(NetworkUtils.isConnected()){
                    LogUtils.dTag("----","----网络正常----")
                    searchCity(et_search_city.text.toString().trim())
                }else{
                    LogUtils.dTag("----","----没有网络----")
                    ToastUtils.showShort(R.string.unconnected_network)
                }
            }
        }
    }

    override fun initMessageEvent(event: MessageEvent) {

    }

    private fun searchCity(text: String) {
        RetrofitManager
            .getService(GetCityListService::class.java)
            .getCityList(text)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : BaseHttpObserver<MutableList<City>>(this) {
                override fun onSuccess(data: MutableList<City>) {
                    mCities.clear()
                    mCities.addAll(data)
                    mBaseQuickAdapter.notifyDataSetChanged()
                }
            })
    }

    override fun onItemClick(baseQuickAdapter: BaseQuickAdapter<*, *>, view: View, i: Int) {
        getWeather(mCities[i])
    }

    private fun getWeather(city: City) {
        RetrofitManager.getService(WeatherService::class.java)
            .weather(city.lat, city.lon, SimpleDateFormat("ZZZZ", Locale.getDefault()).format(System.currentTimeMillis()))
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : BaseHttpObserver<Forecast>(this) {
                override fun onSuccess(data: Forecast) {
                    GlobalScope.launch {
                        city.id = data.id.toInt()
                        MyDatabase.instance.placeDao.insert(city)
                        MyDatabase.instance.placeDao.deleteLimit()
                        data.name = city.name
                        Device.weather = Gson().toJson(data)
                        Device.weatherState.postValue(1)
                        finish()
                    }
                }
            })
    }

    private fun getWeather(city_id: String) {
        RetrofitManager.getService(WeatherService::class.java)
            .weather(city_id, SimpleDateFormat("ZZZZ", Locale.getDefault()).format(System.currentTimeMillis()))
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : BaseHttpObserver<Forecast>(this) {
                override fun onSuccess(data: Forecast) {
                    Device.weather = Gson().toJson(data)
                    Device.weatherState.postValue(1)
                    finish()
                }
            })
    }
}