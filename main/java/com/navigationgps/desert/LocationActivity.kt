package com.navigationgps.desert

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.navigationgps.desert.databinding.ActivityLocationBinding

import com.navigationgps.desert.utils.com.example.compassapp.SharedLocationViewModel
import com.google.android.material.tabs.TabLayoutMediator


class LocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationBinding
    private lateinit var viewModel: LocationViewModel
    private lateinit var sharedLocationViewModel: SharedLocationViewModel

    private val tabTitles = arrayOf("المواقع", "مواقعي")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        viewModel = ViewModelProvider(this).get(LocationViewModel::class.java)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as MyApplication
        sharedLocationViewModel = app.sharedLocationViewModel



        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tablayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        binding.addFab.setOnClickListener {
            val dialog = AddLocationDialogFragment { title, lat, lon ->
                val newLocation = LocationEntity(
                    title = title,
                    latitude = lat,
                    longitude = lon,
                    isFav = false
                )
                sharedLocationViewModel.addMyLocation(newLocation)
                Log.d("AddCheck", "تمت إضافة الموقع إلى ViewModel: ${sharedLocationViewModel.myLocations.value?.size}")

                Log.d("AddLocationDebug", "✅ موقع جديد اتضاف: $title ($lat, $lon)")
            }
            dialog.show(supportFragmentManager, "AddLocationDialog")
        }


    }
    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration)
        configuration.fontScale = 1.0f
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

}

class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun createFragment(position: Int) = when (position) {
        0 -> LocationsFragment()
        1 -> MyLocationsFragment()
        else -> LocationsFragment()
    }

    override fun getItemCount() = 2

}
