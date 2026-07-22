package com.navigationgps.desert

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.navigationgps.desert.databinding.ActivityFavoritesBinding
import com.navigationgps.desert.utils.com.example.compassapp.SharedLocationViewModel

class FavoritesActivity : AppCompatActivity() {
    private lateinit var viewModel: LocationViewModel
    private lateinit var sharedLocationViewModel: SharedLocationViewModel
    private lateinit var adapter: LocationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(LocationViewModel::class.java)
        val app = application as MyApplication
        sharedLocationViewModel = app.sharedLocationViewModel

        adapter = LocationsAdapter(
            onFavoriteClick = { location -> toggleFavorite(location) },
            onItemClick = { location -> showOptionsDialog(location) }
        )
        sharedLocationViewModel.currentLocation.observe(this) { location ->
            if (location != null) {
                adapter.updateUserLocation(location.latitude, location.longitude)
                Log.d("FavoritesDebug", "📍 تم تحديث موقع المستخدم داخل FavoritesActivity: ${location.latitude}, ${location.longitude}")
            }
        }

        binding.favoritesList.layoutManager = LinearLayoutManager(this)
        binding.favoritesList.adapter = adapter

        viewModel.favouriteLocations.observe(this) { updateFavoritesList() }
        sharedLocationViewModel.manualLocations.observe(this) { updateFavoritesList() }
    }
    fun updateFavoritesList() {
        val dbFavs = viewModel.favouriteLocations.value ?: emptyList()
        val manualFavs = sharedLocationViewModel.manualLocations.value?.filter { it.isFav == true } ?: emptyList()

        val combined = (dbFavs + manualFavs)
            .distinctBy { Triple(it.title, it.latitude, it.longitude) }

        adapter.submitList(combined)
    }
    private fun toggleFavorite(location: LocationEntity) {
        if (sharedLocationViewModel.manualLocations.value?.contains(location) == true) {
            sharedLocationViewModel.toggleFavoriteManual(location)
        } else {
            viewModel.toggleFavorite(location)
        }
        Toast.makeText(this, "تم تحديث حالة المفضلة", Toast.LENGTH_SHORT).show()
    }
    private fun showOptionsDialog(location: LocationEntity) {
        val options = arrayOf("إزالة من المفضلة", "مشاركة", "الذهاب إلى")
        AlertDialog.Builder(this,R.style.CustomAlertDialogTheme)
            .setTitle(location.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> toggleFavorite(location)
                    1 -> shareLocation(location)
                    2 -> goToLocation(location)
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
    private fun shareLocation(location: LocationEntity) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "موقعي: ${location.title}\nخط العرض: ${location.latitude}\nخط الطول: ${location.longitude}")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "شارك عبر"))
    }
    private fun goToLocation(location: LocationEntity) {
        val options = arrayOf("الذهاب إلى الخريطة", "الذهاب إلى البوصلة")

        // 🎨 استخدام ثيم مخصص للتحكم في الألوان (سيحتاج لإضافة الثيم في styles.xml)
        AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            .setTitle("اختر طريقة الذهاب")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, MapActivity::class.java)
                        intent.putExtra("latitude", location.latitude)
                        intent.putExtra("longitude", location.longitude)
                        intent.putExtra("title", location.title)
                        startActivity(intent)
                    }
                    1 -> {
                        val intent = Intent(this, CompassActivity::class.java)
                        intent.putExtra("latitude", location.latitude)
                        intent.putExtra("longitude", location.longitude)
                        intent.putExtra("title", location.title)
                        startActivity(intent)
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("إلغاء") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration)
        configuration.fontScale = 1.0f
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }
}
