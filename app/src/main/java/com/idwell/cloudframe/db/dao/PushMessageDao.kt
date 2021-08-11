package com.idwell.cloudframe.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.idwell.cloudframe.db.entity.PushMessage

@Dao
interface PushMessageDao {

    @Query("SELECT * FROM PushMessage ORDER BY errorCode ASC")
    fun loadAll(): LiveData<MutableList<PushMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg pushMessage: PushMessage)

    @Update
    fun update(vararg pushMessage: PushMessage)

    @Delete
    fun delete(vararg pushMessage: PushMessage)
}