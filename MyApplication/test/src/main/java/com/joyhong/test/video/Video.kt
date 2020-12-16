package com.joyhong.test.video

import android.os.Parcel
import android.os.Parcelable

class Video : Parcelable {
    var id = 0L
    var data = ""
    var size = 0L
    var displayName = ""
    var title = ""
    var duration = 0L
    var resolution = ""
    var description = ""
    var isprivate = 0
    var isSelected = false

    constructor(parcel: Parcel) {
        id = parcel.readLong()
        data = parcel.readString() ?: ""
        size = parcel.readLong()
        displayName = parcel.readString() ?: ""
        title = parcel.readString() ?: ""
        duration = parcel.readLong()
        resolution = parcel.readString() ?: ""
        description = parcel.readString() ?: ""
        isprivate = parcel.readInt()
        isSelected = parcel.readByte() != 0.toByte()
    }

    constructor()

    constructor(data: String) {
        this.data = data
    }

    constructor(id: Long, data: String, size: Long, displayName: String, title: String, duration: Long, resolution:String, description: String, isprivate: Int) {
        this.id = id
        this.data = data
        this.size = size
        this.displayName = displayName
        this.title = title
        this.duration = duration
        this.resolution = resolution
        this.description = description
        this.isprivate = isprivate
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Video

        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(data)
        parcel.writeLong(size)
        parcel.writeString(displayName)
        parcel.writeString(title)
        parcel.writeLong(duration)
        parcel.writeString(resolution)
        parcel.writeString(description)
        parcel.writeInt(isprivate)
        parcel.writeByte(if (isSelected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Video> {
        override fun createFromParcel(parcel: Parcel): Video {
            return Video(parcel)
        }

        override fun newArray(size: Int): Array<Video?> {
            return arrayOfNulls(size)
        }
    }
}