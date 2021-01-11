package com.idwell.cloudframe.entity

import android.os.Parcel
import android.os.Parcelable
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.idwell.cloudframe.http.entity.User
import java.io.Serializable

class MultipleItem() : MultiItemEntity,Parcelable {

    private var itemType = 0
    var iconResId: Int = 0
    var title = ""
    var content = ""
    var desc = ""
    var ext = ""
    var max = 0
    var progress = 0
    var isChecked = false
    var user = User()

    constructor(parcel: Parcel) : this() {
        itemType = parcel.readInt()
        iconResId = parcel.readInt()
        title = parcel.readString()!!
        content = parcel.readString()!!
        desc = parcel.readString()!!
        ext = parcel.readString()!!
        max = parcel.readInt()
        progress = parcel.readInt()
        isChecked = parcel.readByte() != 0.toByte()
        user = parcel.readParcelable(User::class.java.classLoader)!!
    }

    constructor(itemType: Int, iconResId: Int) : this() {
        this.itemType = itemType
        this.iconResId = iconResId
    }

    constructor(itemType: Int, iconResId: Int, title: String) : this() {
        this.itemType = itemType
        this.iconResId = iconResId
        this.title = title
    }

    constructor(itemType: Int, title: String) : this() {
        this.itemType = itemType
        this.title = title
    }

    constructor(itemType: Int, title: String, isChecked: Boolean) : this() {
        this.itemType = itemType
        this.title = title
        this.isChecked = isChecked
    }

    constructor(itemType: Int, title: String, content: String) : this() {
        this.itemType = itemType
        this.title = title
        this.content = content
    }

    constructor(itemType: Int, title: String, content: String, isChecked: Boolean) : this() {
        this.itemType = itemType
        this.title = title
        this.content = content
        this.isChecked = isChecked
    }

    constructor(itemType: Int, title: String, content: String, desc: String) : this() {
        this.itemType = itemType
        this.title = title
        this.content = content
        this.desc = desc
    }

    constructor(itemType: Int, title: String, content: String, desc: String, ext: String) : this() {
        this.itemType = itemType
        this.title = title
        this.content = content
        this.desc = desc
        this.ext = ext
    }

    constructor(itemType: Int, title: String, progress: Int, max: Int) : this() {
        this.itemType = itemType
        this.title = title
        this.progress = progress
        this.max = max
    }

    constructor(itemType: Int, user: User) : this() {
        this.itemType = itemType
        this.user = user
    }

    override fun getItemType(): Int {
        return itemType
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(itemType)
        dest?.writeInt(iconResId)
        dest?.writeString(title)
        dest?.writeString(content)
        dest?.writeString(desc)
        dest?.writeString(ext)
        dest?.writeInt(max)
        dest?.writeInt(progress)
        dest?.writeByte(if (isChecked) 1 else 0)
        dest?.writeParcelable(user, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MultipleItem> {
        override fun createFromParcel(parcel: Parcel): MultipleItem {
            return MultipleItem(parcel)
        }

        override fun newArray(size: Int): Array<MultipleItem?> {
            return arrayOfNulls(size)
        }
    }
}