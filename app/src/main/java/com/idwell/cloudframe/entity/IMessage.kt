package com.idwell.cloudframe.entity

data class IMessage(
    var sender_id: Int = 0,
    var sender_name: String = "",
    var sender_remarkname: String = "",
    var sender_isReceive: String = "",
    var sender_account: Int = 0,
    var sender_avatar: String = "",
    var sender_platform: String = "",
    var receive_id: Int = 0,
    var receive_name: String = "",
    var to_fcm_token: String = "",
    var text: MutableList<String> = mutableListOf(),
    var file_name: String = "",
    var url: String = "",
    var type: String = "",
    var platform: String = "",
    var ifAccept: String = "",
    var deviceFlow: Double = 0.0,
    var topUpFlow: Double = 0.0,
    var time: Long = 0,
    var isAdmin: String = "",
    var userEmail: String = "",
    var filePath: String = ""
)