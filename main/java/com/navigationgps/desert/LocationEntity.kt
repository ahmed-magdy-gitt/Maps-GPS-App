package com.navigationgps.desert

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id") val id: Int=0,
    val country: String?=null,
    @ColumnInfo(name = "is_fav") val isFav: Boolean?=false,
    val latitude: Double?,
    val longitude: Double?,
    val title: String?
)