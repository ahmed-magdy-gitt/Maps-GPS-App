package com.navigationgps.desert

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.navigationgps.desert.utils.com.example.compassapp.SharedLocationViewModel

object LocationViewModelProvider {
    private var sharedViewModel: SharedLocationViewModel? = null

    fun get(owner: ViewModelStoreOwner): SharedLocationViewModel {
        if (sharedViewModel == null) {
            sharedViewModel = ViewModelProvider(owner)[SharedLocationViewModel::class.java]
        }
        return sharedViewModel!!
    }
}
