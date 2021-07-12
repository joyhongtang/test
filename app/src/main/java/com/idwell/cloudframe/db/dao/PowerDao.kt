package com.idwell.cloudframe.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

import com.idwell.cloudframe.db.entity.AutoOnOff

@Dao
interface PowerDao {

    @Query("SELECT * FROM AutoOnOff")
    fun query(): AutoOnOff

    @Query("SELECT * FROM AutoOnOff")
    fun queryLiveData(): LiveData<AutoOnOff>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPower(vararg powers: AutoOnOff)

    @Update
    fun updatePower(vararg powers: AutoOnOff)

    @Delete
    fun deletePower(vararg powers: AutoOnOff)
}