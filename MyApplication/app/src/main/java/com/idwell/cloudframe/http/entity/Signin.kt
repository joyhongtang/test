package com.idwell.cloudframe.http.entity

class Signin(
    var device_id: Int = 0,
    var deviceName: String = "",
    var deviceEmail: String = "",
    var activation: String = "",
    var email: String = "",
    var android: String = "",
    var ios: String = "",
    var facebook: String = "",
    var twitter: String = "",
    var isAcceptNewUsers: String = "",
    var deviceFlow: Double = 0.0,
    var users: MutableList<User> = mutableListOf()
)