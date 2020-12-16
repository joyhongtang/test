package com.joyhong.test.util

import android.content.Context
import android.graphics.BitmapFactory
import android.hardware.input.InputManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.os.UserHandle
import android.os.storage.StorageManager
import android.view.KeyEvent
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.Utils
import com.joyhong.test.photo.Photo
import java.io.File
import java.util.*

object MyTestUtils {

    /**
     * 刷新媒体库
     */
    fun scanFile(path: String) {
        MediaScannerConnection.scanFile(Utils.getApp(), arrayOf(path), null, null)
    }

    fun getCloudDir(): File {
        val file = File(Environment.getExternalStorageDirectory(), "CloudAlbum/cloud")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }



    fun convertWidthHeight(photo: Photo): IntArray {
        val width: Int
        val height: Int
        val screenWidth = ScreenUtils.getScreenWidth()
        val screenHeight = ScreenUtils.getScreenHeight()
        if (photo.width <= 0 || photo.height <= 0) {
            // 获取Options对象
            val options = BitmapFactory.Options()
            // 仅做解码处理，不加载到内存
            options.inJustDecodeBounds = true
            // 解析文件
            BitmapFactory.decodeFile(photo.data, options)
            // 获取宽高
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                width = 800
                height = 480
            }else {
                width = options.outWidth
                height = options.outHeight
            }
        }else {
            width = photo.width
            height = photo.height
        }
        val widthHeight = intArrayOf(width, height)
        if (width >= height && width >= screenWidth) {
            val ratio = width * 1.0f / screenWidth
            widthHeight[0] = screenWidth
            widthHeight[1] = (height / ratio).toInt()
        }else if (height > width && height > screenHeight) {
            val ratio = height * 1.0f / screenHeight
            widthHeight[0] = (width / ratio).toInt()
            widthHeight[1] = screenHeight
        }
        return widthHeight
    }

    fun convertWidthHeight(photo: Photo, screenWidth: Int, screenHeight: Int): IntArray {
        val width: Int
        val height: Int
        if (photo.width <= 0 || photo.height <= 0) {
            // 获取Options对象
            val options = BitmapFactory.Options()
            // 仅做解码处理，不加载到内存
            options.inJustDecodeBounds = true
            // 解析文件
            BitmapFactory.decodeFile(photo.data, options)
            // 获取宽高
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                width = 800
                height = 480
            }else {
                width = options.outWidth
                height = options.outHeight
            }
        }else {
            width = photo.width
            height = photo.height
        }
        val widthHeight = intArrayOf(width, height)
        if (width >= height && width >= screenWidth) {
            val ratio = width * 1.0f / screenWidth
            widthHeight[0] = screenWidth
            widthHeight[1] = (height / ratio).toInt()
        }else if (height > width && height > screenHeight) {
            val ratio = height * 1.0f / screenHeight
            widthHeight[0] = (width / ratio).toInt()
            widthHeight[1] = screenHeight
        }
        return widthHeight
    }

}