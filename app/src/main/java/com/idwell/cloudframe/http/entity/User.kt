package com.idwell.cloudframe.http.entity

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
class User(
    @PrimaryKey
    var id: Int = 0,
    var name: String = "",
    var account: Int = 0,
    var remarkname: String = "",
    var displayName: String = "",
    var avatar: String = "",
    var platform: String = "",
    var isReceive: String = "",
    var create_date: Long = 0,
    var modify_date: Long = 0,
    var isAccepted: String = "",
    var isAdmin: String = "",
    var userEmail: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeInt(account)
        parcel.writeString(remarkname)
        parcel.writeString(displayName)
        parcel.writeString(avatar)
        parcel.writeString(platform)
        parcel.writeString(isReceive)
        parcel.writeLong(create_date)
        parcel.writeLong(modify_date)
        parcel.writeString(isAccepted)
        parcel.writeString(isAdmin)
        parcel.writeString(userEmail)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}