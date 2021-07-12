package com.idwell.cloudframe.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import com.blankj.utilcode.util.LogUtils
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.*
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.WritePermission
import org.apache.ftpserver.usermanager.impl.BaseUser
import java.io.File
import org.apache.ftpserver.ftplet.Ftplet

class FtpService : Service() {
    private val tag = FtpService::class.java.simpleName
    private lateinit var ftpService: FtpServer
    private val mDefaultFtplet = object : DefaultFtplet() {
        override fun onDeleteEnd(session: FtpSession?, request: FtpRequest?): FtpletResult {
            LogUtils.dTag(tag, "$session, $request")
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(File("${Environment.getExternalStorageDirectory()}/CloudAlbum/local/${request?.argument}"))
            sendBroadcast(intent)
            return super.onDeleteEnd(session, request)
        }

        override fun onUploadEnd(session: FtpSession?, request: FtpRequest?): FtpletResult {
            LogUtils.dTag(tag, "$session, $request")
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(File("${Environment.getExternalStorageDirectory()}/CloudAlbum/local/${request?.argument}"))
            sendBroadcast(intent)
            return super.onUploadEnd(session, request)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val file = File("${Environment.getExternalStorageDirectory()}/CloudAlbum/local")
        if (!file.exists()) {
            file.mkdirs()
        }
        startFtp()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startFtp() {
        val serverFactory = FtpServerFactory()
        //设置访问用户名和密码还有共享路径
        val baseUser = BaseUser()
        baseUser.name = "anonymous"
        baseUser.password = ""
        baseUser.homeDirectory = "${Environment.getExternalStorageDirectory()}/CloudAlbum/local"
        val authorities = ArrayList<Authority>()
        authorities.add(WritePermission())
        baseUser.authorities = authorities
        serverFactory.userManager.save(baseUser)
        val listenerFactory = ListenerFactory()
        listenerFactory.port = 2221 //设置端口号 非ROOT不可使用1024以下的端口
        serverFactory.addListener("default", listenerFactory.createListener())
        val ftplets = HashMap<String, Ftplet>()
        ftplets["ftplet"] = mDefaultFtplet
        serverFactory.ftplets = ftplets
        ftpService = serverFactory.createServer()
        ftpService.start()
    }

    override fun onDestroy() {
        ftpService.stop()
        super.onDestroy()
    }
}