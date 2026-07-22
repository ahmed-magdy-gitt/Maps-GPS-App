package com.navigationgps.desert

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import com.navigationgps.desert.LocationEntity
import com.navigationgps.desert.utils.com.example.compassapp.SharedLocationViewModel

class MyApplication : Application() {
    lateinit var sharedLocationViewModel: SharedLocationViewModel
    var tempTarget: LocationEntity? = null

    override fun onCreate() {
        super.onCreate()
        sharedLocationViewModel = ViewModelProvider.AndroidViewModelFactory
            .getInstance(this)
            .create(SharedLocationViewModel::class.java)


    }
}
