package com.idwell.cloudframe.db.entity

import androidx.room.*
import java.io.File

@Entity(indices = [Index(value = ["time"], unique = true)])
class Alarm(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var repeat: MutableList<Int> = mutableListOf(),
    var ringtone: String = "",
    var label: String = "",
    var hour: Int = 0,
    var minute: Int = 0,
    var time: String = "",
    var isChecked: Boolean = true
)