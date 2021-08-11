package com.idwell.cloudframe.http

import com.blankj.utilcode.util.FileIOUtils
import com.idwell.cloudframe.common.Device
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object RetrofitManager {

    const val URL = "https://well.bsimb.cn/"
    //private const val URL = "http://welluat.bsimb.cn/"
    const val WEATHER_ICON_URL = "http://openweathermap.org/img/w/"

    private var mRetrofit: Retrofit
    private var mOkHttpClient: OkHttpClient
    //private lateinit var progressListener: ProgressListener

    private val mSimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    init {
        //SSL证书
        val x509TrustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {

            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf(x509TrustManager), SecureRandom())
        //Log拦截器
        val logger = object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                var content = message
                var index = message.indexOf("\r\nContent-Type: application/octet-stream\r\n");
                if (index != -1) {
                    //LogUtils.dTag("lcs", "index = " + index);
                    for (i in index + 53 until message.length) {
                        if (message[i] == '\r') {
                            index = i;
                            break;
                        }
                    }
                    index += 2;
                    content = message.substring(0, index);
                }
                content = mSimpleDateFormat.format(System.currentTimeMillis()) + " D/OkHttp: " + content + "\r\n";
                val result = FileIOUtils.writeFileFromString(Device.getLogFilePath(), content, true);
                //LogUtils.dTag("lcs", "logPath = " + MyApplication.getLogPath() + ", content = " + content + ", result = " + result);
            }
        }
        val httpLoggingInterceptor = HttpLoggingInterceptor(logger)
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        mOkHttpClient = OkHttpClient.Builder().sslSocketFactory(sslContext.socketFactory, x509TrustManager).addInterceptor(httpLoggingInterceptor)
        //超时时间设置，默认15秒
        .readTimeout(15, TimeUnit.SECONDS)      //全局的读取超时时间
        .writeTimeout(15, TimeUnit.SECONDS)     //全局的写入超时时间
        .connectTimeout(15, TimeUnit.SECONDS)   //全局的连接超时时间
        .build()
        mRetrofit = Retrofit.Builder()
            .client(mOkHttpClient)
            .baseUrl(URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
    }

    /*private val progressRetro: Retrofit
        get() {
            val builder = OkHttpClient.Builder()
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.NONE
            builder.addNetworkInterceptor(loggingInterceptor)
            //进度
            builder.addNetworkInterceptor { chain ->
                val originalResponse = chain.proceed(chain.request())
                originalResponse.newBuilder()
                    .body(originalResponse.body()?.let { ProgressResponseBody(it, progressListener) })
                    .build()
            }
            //全局的读取超时时间，默认15秒
            builder.readTimeout(15, TimeUnit.SECONDS)
            //全局的写入超时时间
            builder.writeTimeout(15, TimeUnit.SECONDS)
            //全局的连接超时时间
            builder.connectTimeout(15, TimeUnit.SECONDS)
            val okHttpClient = builder.build()
            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
        }*/

    fun <T> getService(service: Class<T>): T {
        return mRetrofit.create(service)
    }

    /*fun <T> getProgressService(progressListener: ProgressListener, service: Class<T>): T {
        this.progressListener = progressListener
        return progressRetro.create(service)
    }*/

    fun getOkHttpClient(): OkHttpClient {
        return mOkHttpClient
    }
}