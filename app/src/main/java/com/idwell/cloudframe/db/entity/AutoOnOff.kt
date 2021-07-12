package com.idwell.cloudframe.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class AutoOnOff(
    @PrimaryKey
    var id: Int,
    var onHour: Int,
    var onMinute: Int,
    var offHour: Int,
    var offMinute: Int,
    var repeat: MutableList<Int>,
    var isChecked: Boolean
)