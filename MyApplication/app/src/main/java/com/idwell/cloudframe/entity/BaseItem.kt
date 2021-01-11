package com.idwell.cloudframe.entity

class BaseItem {

    var iconResId: Int = 0
    var title = ""
    var titleResId: Int = 0
    var content = ""

    constructor(iconResId: Int, titleResId: Int) {
        this.iconResId = iconResId
        this.titleResId = titleResId
    }

    constructor(titleResId: Int, content: String) {
        this.titleResId = titleResId
        this.content = content
    }
}