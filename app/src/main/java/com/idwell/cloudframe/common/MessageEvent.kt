package com.idwell.cloudframe.common

class MessageEvent {

    companion object {
        const val SD_IN = "sd.in"
        const val SD_OUT = "sd.out"
        const val USB_IN = "usb.in"
        const val USB_OUT = "usb.out"

        const val DEVICE_ACTIVATED = "device.activated"
        const val DEVICE_UPDATE_ACCEPT_NEW_USERS = "device.update_accept_new_users"
        const val DEVICE_UPDATE_DATA_FLOW = "device.update_data_flow"
        const val TIME_12_24 = "time.12_24"
        const val SLIDE_AUTOPLAY_TIME_CHANGED = "slide.autoplay_time_changed"
        const val SLIDE_CUR_IMAGE_DATA = "slide.cur_image_data"
        const val SLIDE_IMAGE_DELETED = "slide.image_deleted"
        const val VIDEOVIEW_CUR_VIDEO_DATA = "videoview.cur_video_data"
        const val START_ACTIVITY_PHOTO = "start.activity.photo"
        const val START_ACTIVITY_WEATHER = "start.activity.weather"
        const val START_ACTIVITY_SEARCH_CITY = "start.activity.search_city"
        const val START_ACTIVITY_ALARM = "start.activity.alarm"
        const val START_ACTIVITY_VIDEO = "start.activity.video"
        const val START_ACTIVITY_CALENDAR = "start.activity.calendar"
        const val START_FRAGMENT_USER_MANAGEMENT = "start.fragment.user_management"
    }

    val message: String
    var number: Int = 0
    var text = ""

    constructor(message: String) {
        this.message = message
    }

    constructor(message: String, number: Int) {
        this.message = message
        this.number = number
    }

    constructor(message: String, text: String) {
        this.message = message
        this.text = text
    }

    constructor(message: String, number: Int, text: String){
        this.message = message
        this.number = number
        this.text = text
    }
}