package com.idwell.cloudframe.http.entity

data class AcceptBind(
    var deviceFlow: Double = 0.0,
    var device_email: String = "",
    var message: String = "",
    var type: String = ""
)