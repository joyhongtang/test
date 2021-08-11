package com.idwell.cloudframe.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.idwell.cloudframe.common.Device

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_welcome)
        if (Device.isFirstIn) {
            startActivity(Intent(this, GuideActivity::class.java))
        }else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
