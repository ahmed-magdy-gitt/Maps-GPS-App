package com.navigationgps.desert

import android.location.Location
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.navigationgps.desert.databinding.ActivityDistanceBtnBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DistanceBtnActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDistanceBtnBinding
    private lateinit var db: RoomDBHelper
    private val gson = Gson()
    private var allLocations = mutableListOf<LocationEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDistanceBtnBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.calcDisBtn.setOnClickListener { calculateDistance()}

        db = RoomDBHelper.getInstance(this)

        lifecycleScope.launch {
            val combinedLocations = mutableListOf<LocationEntity>()

            val dbLocations = withContext(Dispatchers.IO) {
                db.locationDao.getAllLocationsDirect()
            }

            val prefs = getSharedPreferences("my_locations_prefs", MODE_PRIVATE)
            val json = prefs.getString("my_locations_list", null)
            val type = object : TypeToken<MutableList<LocationEntity>>() {}.type
            val sharedLocations: MutableList<LocationEntity> = if (json != null) {
                gson.fromJson(json, type)
            } else mutableListOf()

            // ✅ 3. دمج الاثنين
            combinedLocations.addAll(dbLocations)
            combinedLocations.addAll(sharedLocations)
            allLocations = combinedLocations

            withContext(Dispatchers.Main) {
                val names = allLocations.map { it.title ?: "بدون اسم" }
                val adapter = ArrayAdapter(
                    this@DistanceBtnActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    names
                )
                binding.location1EditText.setAdapter(adapter)
                binding.location2EditText.setAdapter(adapter)
                binding.location1EditText.dropDownVerticalOffset = 0
                binding.location2EditText.dropDownVerticalOffset = 0
                binding.location1EditText.dropDownAnchor = binding.location1EditText.id
                binding.location2EditText.dropDownAnchor = binding.location2EditText.id

                // ✅ افتح القايمة أول ما المستخدم يضغط على الفيلد
                binding.location1EditText.setOnClickListener {
                    binding.location1EditText.showDropDown()
                }
                binding.location2EditText.setOnClickListener {
                    binding.location2EditText.showDropDown()
                }
            }
        }
    }
    fun calculateDistance() {
        val name1 = binding.location1EditText.text.toString().trim()
        val name2 = binding.location2EditText.text.toString().trim()

        if (name1.isEmpty() || name2.isEmpty()) return

        val loc1 = allLocations.find { it.title == name1 }
        val loc2 = allLocations.find { it.title == name2 }

        if (loc1 == null || loc2 == null) {
            Toast.makeText(this, "من فضلك اختر الموقعين أولاً", Toast.LENGTH_SHORT).show()
            return
        }

        val results = FloatArray(1)
        Location.distanceBetween(
            loc1.latitude ?: 0.0, loc1.longitude ?: 0.0,
            loc2.latitude ?: 0.0, loc2.longitude ?: 0.0,
            results
        )
        val km = results[0] / 1000.0
        binding.distanceTextView.text = "المسافة بين الموقعين: %.2f كم".format(km)
    }

}
