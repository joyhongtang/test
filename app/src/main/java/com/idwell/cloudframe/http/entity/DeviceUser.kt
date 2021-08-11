package com.idwell.cloudframe.http.entity

data class DeviceUser(
    var device_id: Int = 0,
    var deviceName: String = "",
    var deviceEmail: String = "",
    var isAcceptNewUsers: String = "",
    var deviceFlow: Double = 0.0,
    var users: MutableList<User> = mutableListOf()
)