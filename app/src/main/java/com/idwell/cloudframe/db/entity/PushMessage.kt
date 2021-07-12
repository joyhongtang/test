package com.idwell.cloudframe.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class PushMessage {
    @PrimaryKey
    var message: String
    var errorCode: Int = 0

    constructor(message: String){
        this.message = message
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PushMessage

        if (message != other.message) return false

        return true
    }

    override fun hashCode(): Int {
        return message.hashCode()
    }
}