package com.idwell.cloudframe.entity

import android.os.Parcel
import android.os.Parcelable

class Music() : Parcelable {
    var id: Long = 0
    var title = ""
    var displayName = ""
    var data = ""
    var size: Long = 0
    var artist = ""
    var album = ""
    var albumID: Long = 0
    var duration: Long = 0
    var isSelected: Boolean = false

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        title = parcel.readString() ?: ""
        displayName = parcel.readString() ?: ""
        data = parcel.readString() ?: ""
        size = parcel.readLong()
        artist = parcel.readString() ?: ""
        album = parcel.readString() ?: ""
        albumID = parcel.readLong()
        duration = parcel.readLong()
        isSelected = parcel.readByte() != 0.toByte()
    }

    constructor(data: String) : this() {
        this.data = data
    }

    constructor(id: Long, title: String, display_name: String, data: String, size: Long, artist: String, album: String, album_id: Long, duration: Long, enabled: Boolean) : this() {
        this.id = id
        this.title = title
        this.displayName = display_name
        this.data = data
        this.size = size
        this.artist = artist
        this.album = album
        this.albumID = album_id
        this.duration = duration
        this.isSelected = enabled
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(title)
        parcel.writeString(displayName)
        parcel.writeString(data)
        parcel.writeLong(size)
        parcel.writeString(artist)
        parcel.writeString(album)
        parcel.writeLong(albumID)
        parcel.writeLong(duration)
        parcel.writeByte(if (isSelected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Music

        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun toString(): String {
        return "Music(id=$id, title=$title, displayName=$displayName, data=$data, size=$size, artist=$artist, album=$album, albumID=$albumID, duration=$duration, isSelected=$isSelected)"
    }

    companion object CREATOR : Parcelable.Creator<Music> {
        override fun createFromParcel(parcel: Parcel): Music {
            return Music(parcel)
        }

        override fun newArray(size: Int): Array<Music?> {
            return arrayOfNulls(size)
        }
    }

}