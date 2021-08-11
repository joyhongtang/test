package com.idwell.cloudframe.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.chad.library.adapter.base.BaseQuickAdapter
import com.idwell.cloudframe.R
import com.idwell.cloudframe.base.BaseFragment
import java.util.*
import android.app.backup.BackupManager
import android.content.res.Configuration
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseViewHolder
import com.idwell.cloudframe.common.Device
import com.idwell.cloudframe.widget.HorizontalItemDecoration
import kotlinx.android.synthetic.main.fragment_recyclerview.*

class LanguageFragment : BaseFragment(), BaseQuickAdapter.OnItemClickListener {

    private lateinit var mLanguageAdapter: BaseQuickAdapter<String, BaseViewHolder>
    private lateinit var languages: List<String>
    private lateinit var countries: List<String>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recyclerview, container, false)
    }

    override fun initData(savedInstanceState: Bundle?) {
        languages = listOf(*resources.getStringArray(R.array.system_language_code))
        countries = listOf(*resources.getStringArray(R.array.system_country_code))
        val data = listOf(*resources.getStringArray(R.array.languages))
        mLanguageAdapter = object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_textsc_imageec, data) {
            override fun convert(helper: BaseViewHolder, item: String?) {
                if (item != null) {
                    helper.setText(R.id.tv_title_item_textsc_imageec, item)
                    var codeIndex = countries.indexOf(Device.getCountry())
                    if (codeIndex == -1) {
                        codeIndex = 0
                    }
                    if (helper.layoutPosition == codeIndex) {
                        helper.getView<View>(R.id.iv_item_textsc_imageec).visibility = View.VISIBLE
                    } else {
                        helper.getView<View>(R.id.iv_item_textsc_imageec).visibility = View.GONE
                    }
                }
            }
        }
        rv_fragment_recyclerview.layoutManager = LinearLayoutManager(context)
        context?.let {
            rv_fragment_recyclerview.addItemDecoration(HorizontalItemDecoration(ContextCompat.getColor(it, R.color.divider)))
        }
        rv_fragment_recyclerview.adapter = mLanguageAdapter
    }

    override fun initListener() {
        mLanguageAdapter.onItemClickListener = this
    }

    override fun onItemClick(baseQuickAdapter: BaseQuickAdapter<*, *>, view: View, i: Int) {
        changeSystemLanguage(Locale(languages[i], countries[i]))
    }

    @SuppressLint("PrivateApi")
    private fun changeSystemLanguage(locale: Locale) {
        val classActivityManagerNative = Class.forName("android.app.ActivityManagerNative")
        val getDefault = classActivityManagerNative.getDeclaredMethod("getDefault")
        val objIActivityManager = getDefault.invoke(classActivityManagerNative)
        val classIActivityManager = Class.forName("android.app.IActivityManager")
        val getConfiguration = classIActivityManager.getDeclaredMethod("getConfiguration")
        val config = getConfiguration.invoke(objIActivityManager) as Configuration
        config.setLocale(locale)
        val clzConfig = Class.forName("android.content.res.Configuration")
        val userSetLocale = clzConfig.getField("userSetLocale")
        userSetLocale.set(config, true)
        val clzParams = arrayOf<Class<*>>(Configuration::class.java)
        val updateConfiguration = classIActivityManager.getDeclaredMethod("updateConfiguration", *clzParams)
        updateConfiguration.invoke(objIActivityManager, config)
        BackupManager.dataChanged("com.android.providers.settings")
    }
}
