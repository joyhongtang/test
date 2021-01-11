package com.idwell.cloudframe.util

import android.content.Context
import android.graphics.BitmapFactory
import android.hardware.input.InputManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.os.UserHandle
import android.os.storage.StorageManager
import android.os.storage.VolumeInfo
import android.view.KeyEvent
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.Utils
import com.idwell.cloudframe.entity.Photo
import java.io.File
import java.util.*

object MyUtils {

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
    fun getTestFile(): File {
        val file = File(Environment.getExternalStorageDirectory(), "CloudFrame/Test")
        if (!file.exists()) {
            file.mkdirs()
        }
        var fileTest = File(file.absolutePath,"test.txt")
        if(!fileTest.exists()){
            fileTest.createNewFile()
        }
        return fileTest
    }
    fun getSdDir(): String? {
        val storageManager = Utils.getApp().getSystemService(Context.STORAGE_SERVICE) as StorageManager
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            val volumes = storageManager.volumes
            Collections.sort(volumes, VolumeInfo.getDescriptionComparator())
            for (vol in volumes) {
                if (vol.getType() == VolumeInfo.TYPE_PUBLIC && vol.isMountedReadable) {
                    val disk = vol.getDisk()
                    if (disk != null && disk.isSd) {
                        val sv = vol.buildStorageVolume(Utils.getApp(), UserHandle.myUserId(), false)
                        val path = sv.path
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                            return path.replaceFirst("^/storage", "/mnt/media_rw")
                        }else {
                            return path
                        }
                    }
                }
            }
        }else {
            val volumePaths = storageManager.volumePaths
            for (volumePath in volumePaths) {
                if (volumePath.contains("extsd")) {
                    return volumePath
                }
            }
        }
        return null
    }

    fun getUsbDir(): String? {
        val storageManager = Utils.getApp().getSystemService(Context.STORAGE_SERVICE) as StorageManager
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            val volumes = storageManager.volumes
            Collections.sort(volumes, VolumeInfo.getDescriptionComparator())
            for (vol in volumes) {
                if (vol.getType() == VolumeInfo.TYPE_PUBLIC && vol.isMountedReadable) {
                    val disk = vol.getDisk()
                    if (disk != null && disk.isUsb) {
                        val sv = vol.buildStorageVolume(Utils.getApp(), UserHandle.myUserId(), false)
                        val path = sv.path
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                            return path.replaceFirst("^/storage", "/mnt/media_rw")
                        }else {
                            return path
                        }
                    }
                }
            }
        }else {
            val volumePaths = storageManager.volumePaths
            for (volumePath in volumePaths) {
                if (volumePath.contains("usb")) {
                    return volumePath
                }
            }
        }
        return null
    }

    fun isMountSd(): Boolean {
        val storageManager = Utils.getApp().getSystemService(Context.STORAGE_SERVICE) as StorageManager
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            val volumes = storageManager.volumes
            Collections.sort(volumes, VolumeInfo.getDescriptionComparator())
            for (vol in volumes) {
                if (vol.getType() == VolumeInfo.TYPE_PUBLIC && vol.isMountedReadable) {
                    val disk = vol.getDisk()
                    if (disk != null && disk.isSd && vol.getState() == VolumeInfo.STATE_MOUNTED) {
                        return true
                    }
                }
            }
        }else {
            val volumePaths = storageManager.volumePaths
            for (volumePath in volumePaths) {
                if (volumePath.contains("extsd") && storageManager.getVolumeState(volumePath) == Environment.MEDIA_MOUNTED) {
                    return true
                }
            }
        }
        return false
    }

    fun isMountUsb(): Boolean {
        val storageManager = Utils.getApp().getSystemService(Context.STORAGE_SERVICE) as StorageManager
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            val volumes = storageManager.volumes
            Collections.sort(volumes, VolumeInfo.getDescriptionComparator())
            for (vol in volumes) {
                if (vol.getType() == VolumeInfo.TYPE_PUBLIC && vol.isMountedReadable) {
                    val disk = vol.getDisk()
                    if (disk != null && disk.isUsb && vol.getState() == VolumeInfo.STATE_MOUNTED) {
                        return true
                    }
                }
            }
        }else {
            val volumePaths = storageManager.volumePaths
            for (volumePath in volumePaths) {
                if (volumePath.contains("usb") && storageManager.getVolumeState(volumePath) == Environment.MEDIA_MOUNTED) {
                    return true
                }
            }
        }
        return false
    }

    fun isMountSdOrUsb(): Boolean {
        val storageManager = Utils.getApp().getSystemService(Context.STORAGE_SERVICE) as StorageManager
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            val volumes = storageManager.volumes
            Collections.sort(volumes, VolumeInfo.getDescriptionComparator())
            for (vol in volumes) {
                if (vol.getType() == VolumeInfo.TYPE_PUBLIC && vol.isMountedReadable) {
                    val disk = vol.getDisk()
                    if (disk != null && (disk.isSd || disk.isUsb) && vol.getState() == VolumeInfo.STATE_MOUNTED) {
                        return true
                    }
                }
            }
        }else {
            val volumePaths = storageManager.volumePaths
            for (volumePath in volumePaths) {
                if ((volumePath.contains("extsd") || volumePath.contains("usb")) && storageManager.getVolumeState(volumePath) == Environment.MEDIA_MOUNTED) {
                    return true
                }
            }
        }
        return false
    }

    fun injectInputEvent(code: Int) {
        val uptimeMillis = SystemClock.uptimeMillis()
        val down = KeyEvent(uptimeMillis, uptimeMillis, KeyEvent.ACTION_DOWN, code, 0)
        InputManager.getInstance().injectInputEvent(down, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
        val up = KeyEvent(uptimeMillis, uptimeMillis, KeyEvent.ACTION_UP, code, 0)
        InputManager.getInstance().injectInputEvent(up, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
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