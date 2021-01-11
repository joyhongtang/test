package com.idwell.cloudframe.http.progress

interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}
