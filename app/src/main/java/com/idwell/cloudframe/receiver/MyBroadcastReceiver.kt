package com.idwell.cloudframe.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import com.blankj.utilcode.util.*
import com.google.gson.Gson
import com.idwell.cloudframe.R
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.db.MyDatabase
import com.idwell.cloudframe.http.BaseHttpObserver
import com.idwell.cloudframe.http.RetrofitManager
import com.idwell.cloudframe.http.entity.Base
import com.idwell.cloudframe.http.entity.DeviceToken
import com.idwell.cloudframe.http.entity.Signin
import com.idwell.cloudframe.http.entity.Forecast
import com.idwell.cloudframe.http.service.*
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*

class MyBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            //Log.d("lcs", intent.action)
            when (intent.action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val activeNetworkInfo = connectivityManager.activeNetworkInfo
                    if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                        if (Device.token.isEmpty()) {
                            val macAddress = try {
                                DeviceUtils.getMacAddress()
                            } catch (e: Exception) {
                                ""
                            }
                            if (macAddress.isEmpty()) {
                                //ToastUtils.showLong(R.string.activation_failed_mac_empty)
                            } else {
                                deviceActive(Device.snnumber, macAddress)
                            }
                        } else if (Device.pushToken.isEmpty()) {
                            //ToastUtils.showLong(R.string.activation_failed_push_token_empty)
                        } else if (!Device.isSignined) {
                            signin()
                        }

                        if (Device.weather.isNotEmpty()) {
                            val weather = Gson().fromJson(Device.weather, Forecast::class.java)
                            if (System.currentTimeMillis() - weather.cur_data.dt * 1000L > 10_800_000) {
                                getWeather(weather.name, weather.id)
                            }
                        }
                    }
                }
                Device.ACTION_SIGN_IN -> {
                    if (Device.token.isEmpty()) {
                        val macAddress = try {
                            DeviceUtils.getMacAddress()
                        } catch (e: Exception) {
                            ""
                        }
                        if (macAddress.isEmpty()) {
                            //ToastUtils.showLong(R.string.activation_failed_mac_empty)
                        } else {
                            deviceActive(Device.snnumber, macAddress)
                        }
                    } else if (Device.pushToken.isEmpty()) {
                        //ToastUtils.showLong(R.string.activation_failed_push_token_empty)
                    } else if (!Device.isSignined) {
                        signin()
                    }
                }
            }
        }
    }

    private fun deviceActive(serial_number: String, mac_address: String) {
        RetrofitManager.getService(DeviceActiveService::class.java)
                .deviceActive(serial_number, mac_address).subscribeOn(Schedulers.io())
                //.deviceActive(serial_number, MyApplication.instance().getString(R.string.wlan0)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : BaseHttpObserver<DeviceToken>() {
                    override fun onSubscribe(d: Disposable) {
                        super.onSubscribe(d)
                        if (d.isDisposed) {
                            Device.infoState.postValue(3)
                        } else {
                            Device.infoState.postValue(2)
                        }
                    }

                    override fun onSuccess(data: DeviceToken) {
                        Device.token = data.deviceToken
                        if (Device.pushToken.isEmpty()) {
                            Device.infoState.postValue(3)
                            //ToastUtils.showLong(R.string.activation_failed_push_token_empty)
                        } else {
                            signin()
                        }
                    }

                    override fun onFail(status: Int) {
                        super.onFail(status)
                        Device.infoState.postValue(3)
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                        Device.infoState.postValue(3)
                    }
                })
    }

    private fun signin() {
        RetrofitManager.getService(SignInService::class.java)
                .signin(Device.token, Device.pushToken, Device.companyName)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : BaseHttpObserver<Signin>() {
                    override fun onSubscribe(d: Disposable) {
                        super.onSubscribe(d)
                        if (d.isDisposed) {
                            if (Device.id == -1) {
                                Device.infoState.postValue(3)
                            }
                        } else {
                            if (Device.id == -1) {
                                Device.infoState.postValue(2)
                            }
                        }
                    }

                    override fun onSuccess(data: Signin) {
                        Device.isSignined = true
                        Device.id = data.device_id
                        Device.email = data.deviceEmail
                        Device.flow = data.deviceFlow.toFloat()
                        Device.acceptNewUsers = data.isAcceptNewUsers
                        Device.activationDesc = data.activation
                        Device.emailDesc = data.email
                        Device.androidDesc = data.android
                        Device.iosDesc = data.ios
                        Device.facebookDesc = data.facebook
                        Device.twitterDesc = data.twitter

                        for (user in data.users) {
                            try {
                                user.name = URLDecoder.decode(user.name, "UTF-8")
                                user.remarkname = URLDecoder.decode(user.remarkname, "UTF-8")
                            } catch (e: Exception) {
                            }
                            user.displayName = if (user.remarkname.isNotEmpty()) user.remarkname else if (user.name.isNotEmpty()) user.name else user.account.toString()
                        }
                        GlobalScope.launch {
                            MyDatabase.instance.userDao.deleteAll()
                            MyDatabase.instance.userDao.insert(data.users)
                        }

                        Device.infoState.postValue(1)
                    }

                    override fun onFail(status: Int) {
                        super.onFail(status)
                        if (Device.id == -1) {
                            Device.infoState.postValue(3)
                        }
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                        if (Device.id == -1) {
                            Device.infoState.postValue(3)
                        }
                    }
                })
    }

    private fun getWeather(name: String, city_id: String) {
        RetrofitManager.getService(WeatherService::class.java)
                .weather(city_id, SimpleDateFormat("ZZZZ", Locale.getDefault()).format(System.currentTimeMillis()))
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Base<Forecast>> {
                    override fun onComplete() {

                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onNext(t: Base<Forecast>) {
                        if (t != null) {
                            val status = t.status
                            val forecast = t.data
                            if (status == 200 && forecast != null) {
                                forecast.name = name
                                Device.weather = Gson().toJson(forecast)
                                Device.weatherState.postValue(1)
                            }
                        }
                    }

                    override fun onError(e: Throwable) {

                    }
                })
    }

    companion object {
        private val TAG = MyBroadcastReceiver::class.java.simpleName
    }
}