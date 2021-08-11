package com.idwell.cloudframe.common

import java.io.File
import java.io.FileFilter

class ImageFilter : FileFilter {
    override fun accept(file: File): Boolean {
        val name = file.name.toLowerCase()
        return name.endsWith("jpg") ||
                name.endsWith("jpeg") ||
                name.endsWith("png") ||
                name.endsWith("gif") ||
                name.endsWith("bmp") ||
                name.endsWith("webp")
    }
}
