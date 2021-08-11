package com.idwell.cloudframe.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class IntConverter {

    @TypeConverter
    fun revertData(data: String): MutableList<Int>{
        return Gson().fromJson(data, object : TypeToken<MutableList<Int>>() {}.type)
    }

    @TypeConverter
    fun convertData(data: MutableList<Int>): String{
        return Gson().toJson(data, object : TypeToken<MutableList<Int>>() {}.type)
    }
}