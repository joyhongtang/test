package com.idwell.cloudframe.receiver

import android.content.Context
import android.content.Intent
import com.alibaba.sdk.android.push.MessageReceiver
import com.alibaba.sdk.android.push.notification.CPushMessage
import com.blankj.utilcode.util.LogUtils
import com.idwell.cloudframe.service.MessageService
import com.idwell.cloudframe.util.MyLogUtils
import java.net.URLDecoder

class MyMessageReceiver: MessageReceiver() {

    override fun onMessage(context: Context?, cPushMessage: CPushMessage?) {
        super.onMessage(context, cPushMessage)
        LogUtils.dTag(TAG, "${cPushMessage?.title}, ${cPushMessage?.content}")
        if (context != null && cPushMessage != null){
            val json = try {
                URLDecoder.decode(URLDecoder.decode(cPushMessage.content, "UTF-8"), "UTF-8")
            } catch (e: Exception) {
                cPushMessage.content
            }
            MyLogUtils.file(json)
            val intent = Intent(context, MessageService::class.java)
            intent.putExtra("json", json)
            context.startService(intent)
        }
    }

    companion object {
        private val TAG = MyMessageReceiver::class.java.simpleName
    }
}