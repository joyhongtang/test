package com.idwell.cloudframe.http.entity

data class Version(
    val download_link: String,
    val last_version: Int,
    val version_desc: String
)