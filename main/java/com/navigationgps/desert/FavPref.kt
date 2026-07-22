package com.navigationgps.desert.utils.com.example.compassapp

import android.content.Context
import android.content.SharedPreferences

object FavPrefs {
    private const val PREF_NAME = "favorite_prefs"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveFavState(context: Context, locationId: Int, isFav: Boolean) {
        getPrefs(context).edit().putBoolean("fav_$locationId", isFav).apply()
    }

    fun getFavState(context: Context, locationId: Int): Boolean {
        return getPrefs(context).getBoolean("fav_$locationId", false)
    }
}

