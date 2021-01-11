package com.idwell.cloudframe.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.idwell.cloudframe.db.entity.Alarm

@Dao
interface AlarmDao {

    @Query("SELECT * FROM Alarm")
    fun query(): MutableList<Alarm>

    @Query("SELECT * FROM Alarm WHERE time = :time")
    fun queryByTime(time: String?): Alarm

    @Query("SELECT * FROM Alarm ORDER BY hour, minute ASC")
    fun queryOrderAsc(): LiveData<MutableList<Alarm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg alarm: Alarm)

    @Update
    fun update(vararg alarm: Alarm)

    @Delete
    fun delete(vararg alarm: Alarm)
}