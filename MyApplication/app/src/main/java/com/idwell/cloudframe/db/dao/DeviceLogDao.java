package com.idwell.cloudframe.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.idwell.cloudframe.db.entity.DeviceLog;

import java.util.List;

@Dao
public interface DeviceLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(DeviceLog... deviceLogs);

    @Delete
    void delete(DeviceLog... deviceLogs);

    @Delete
    void delete(List<DeviceLog> deviceLogs);

    @Update
    void update(DeviceLog... deviceLogs);

    @Query("SELECT * FROM DeviceLog ORDER BY uploadTimes ASC")
    List<DeviceLog> getAll();

    @Query("SELECT * FROM DeviceLog WHERE fileType='log' ORDER BY rowid DESC")
    List<DeviceLog> getLog();

    @Query("SELECT * FROM DeviceLog WHERE fileType IN ('anr','crash') ORDER BY uploadTimes ASC")
    LiveData<List<DeviceLog>> liveAnrCrash();
}
