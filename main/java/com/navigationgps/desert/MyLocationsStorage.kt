package com.navigationgps.desert

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MyLocationsStorage(context: Context) {
    private val prefs = context.getSharedPreferences("my_locations_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(list: List<LocationEntity>) {
        val json = gson.toJson(list)
        prefs.edit().putString("my_locations_list", json).apply()
    }

    fun load(): MutableList<LocationEntity> {
        val json = prefs.getString("my_locations_list", null)
        val type = object : TypeToken<MutableList<LocationEntity>>() {}.type
        return if (json != null) gson.fromJson(json, type) else mutableListOf()
    }
}
