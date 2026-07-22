package com.navigationgps.desert

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase



@Database(entities = [LocationEntity::class], version = 1)
 abstract class RoomDBHelper: RoomDatabase() {
    abstract val locationDao: LocationDao

    companion object {
        @Volatile
        private var INSTANCE: RoomDBHelper? = null

        fun getInstance(c: Context): RoomDBHelper{
            return INSTANCE ?: synchronized(this) {

                val instance = Room
                    .databaseBuilder(c, RoomDBHelper::class.java,"app_database")
                    .createFromAsset("locations.db")
                    .build()
                 INSTANCE= instance
                instance

            }
        }
    }
}