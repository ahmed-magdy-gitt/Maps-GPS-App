package com.navigationgps.desert

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
@Dao

interface LocationDao {
    @Query("SELECT * FROM locations ")
    fun gAllLocations(): LiveData<List<LocationEntity>>
    @Query("DELETE FROM locations")
    suspend fun clearAll()


    @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insert(location: LocationEntity)

    @Delete
    suspend fun delete(location: LocationEntity)


    @Query("SELECT * FROM locations WHERE is_fav = 1")
    fun getFavoriteLocations(): LiveData<List<LocationEntity>>
    @Query("UPDATE locations SET is_fav = :isFavorite WHERE _id = :locationId")
     fun updateFavorite(locationId: Int, isFavorite: Boolean)
    @Query("SELECT * FROM locations")
    suspend fun getAllLocationsDirect(): List<LocationEntity>





}