package com.idwell.cloudframe.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.blankj.utilcode.util.Utils
import com.idwell.cloudframe.MyApplication
import com.idwell.cloudframe.db.entity.*

import com.idwell.cloudframe.db.dao.*
import com.idwell.cloudframe.http.entity.City
import com.idwell.cloudframe.http.entity.User

@Database(entities = [Alarm::class, City::class, AutoOnOff::class, PushMessage::class, User::class, DeviceLog::class], version = 2)
@TypeConverters(IntConverter::class)
abstract class MyDatabase : RoomDatabase() {
    abstract val alarmDao: AlarmDao
    abstract val placeDao: CityDao
    abstract val powerDao: PowerDao
    abstract val pushMessageDao: PushMessageDao
    abstract val userDao: UserDao
    abstract val deviceLogDao: DeviceLogDao

    private object DatabaseHolder {
        val MIGRATION_1_2 = object : Migration(1,2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE 'DeviceLog' ('filePath' TEXT NOT NULL, 'fileType' TEXT NOT NULL, 'uploadTimes' INTEGER NOT NULL, PRIMARY KEY('filePath'))")
            }
        }
        val database = Room.databaseBuilder(MyApplication.instance(), MyDatabase::class.java, "cloud_frame").addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()
    }

    companion object {
        val instance = DatabaseHolder.database
    }
}
