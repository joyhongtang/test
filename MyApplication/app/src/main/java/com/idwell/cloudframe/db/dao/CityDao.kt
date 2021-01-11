package com.idwell.cloudframe.db.dao

import androidx.room.*
import com.idwell.cloudframe.http.entity.City

@Dao
interface CityDao {

    @Query("SELECT * FROM City ORDER BY name ASC")
    fun queryOrderByAddressAsc(): MutableList<City>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg place: City)

    @Update
    fun update(vararg place: City)

    @Query("DELETE FROM City WHERE (SELECT count(id) FROM City ) > 100 AND id IN (SELECT id FROM City ORDER BY id DESC LIMIT (SELECT count(id) FROM City) OFFSET 100)")
    fun deleteLimit()
}