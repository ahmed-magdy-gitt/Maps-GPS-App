package com.navigationgps.desert.utils.com.example.compassapp

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.navigationgps.desert.LocationEntity
import com.navigationgps.desert.MyLocationsStorage
import com.google.android.gms.location.*

data class LocationData(val latitude: Double, val longitude: Double, val title: String? = null)

class SharedLocationViewModel(application: Application) : AndroidViewModel(application) {

    val currentLocation = MutableLiveData<LocationData>()
    val myLocations = MutableLiveData<MutableList<LocationEntity>>(mutableListOf())
    val manualLocations = MutableLiveData<MutableList<LocationEntity>>(mutableListOf())
    var cachedLocations: MutableList<LocationEntity>? = null

    fun updateLocation(lat: Double, lon: Double) {
        Log.d("SharedLocationDebug", "📍 updateLocation called with lat=$lat, lon=$lon")
        currentLocation.postValue(LocationData(lat, lon))
    }

    fun addMyLocation(location: LocationEntity) {
        val list = myLocations.value ?: mutableListOf()
        list.add(location)
        myLocations.value = list
        cachedLocations?.add(location)
        Log.d("SharedVM", "➕ تمت إضافة موقع جديد للكاش (${location.title}) — إجمالي الآن ${cachedLocations?.size}")
    }

    fun addManualLocation(location: LocationEntity) {
        val list = manualLocations.value?.toMutableList() ?: mutableListOf()


        val existingIndex = list.indexOfFirst {
            it.title == location.title && it.latitude == location.latitude && it.longitude == location.longitude
        }

        if (existingIndex != -1) {

            list[existingIndex] = location
            Log.d("SharedVM", "✅ تم تحديث الموقع الحالي (موجود مسبقاً): ${location.title}")
        } else {

            list.add(location)
            Log.d("SharedVM", "➕ تمت إضافة موقع جديد: ${location.title}")
        }

        manualLocations.postValue(list)

        try {
            val storage = MyLocationsStorage(getApplication())
            storage.save(list)
            Log.d("SharedVM", "💾 تم حفظ المواقع بعد الإضافة/التحديث (${list.size})")
        } catch (e: Exception) {
            Log.e("SharedVM", "❌ فشل حفظ المواقع: ${e.message}")
        }
    }


    fun toggleFavoriteManual(location: LocationEntity) {
        val list = manualLocations.value ?: return
        val index = list.indexOf(location)
        if (index != -1) {
            list[index] = list[index].copy(isFav = !(list[index].isFav ?: false))
            manualLocations.postValue(list)
        }
    }
    fun updateManualFavoriteStatus(updated: LocationEntity) {
        val currentList = manualLocations.value?.toMutableList() ?: mutableListOf()

        val index = currentList.indexOfFirst { it.id == updated.id }

        if (index != -1) {

            currentList[index] = updated
            Log.d("SharedVM", "✅ تم تحديث حالة المفضلة للموقع: ${updated.title}")
        } else {

            Log.d("SharedVM", "⚠️ الموقع غير موجود في القائمة، لن يتم تكراره: ${updated.title}")
        }

        manualLocations.postValue(currentList)

        try {
            val storage = MyLocationsStorage(getApplication())
            storage.save(currentList)
        } catch (e: Exception) {
            Log.e("SharedVM", "❌ فشل حفظ حالة المفضلة: ${e.message}")
        }
    }

    fun loadManualLocations(loadedList: MutableList<LocationEntity>) {

        if (manualLocations.value.isNullOrEmpty()) {
            manualLocations.value = loadedList
            Log.d("SharedVM", "💾 تم تحميل المواقع اليدوية (${loadedList.size}) من التخزين إلى ViewModel")
        } else {

            Log.d("SharedVM", "⚠️ تجاهل تحميل المواقع اليدوية، الـ ViewModel يحتوي على بيانات مسبقًا.")
        }
    }



    fun toggleFavoritePersistent(location: LocationEntity) {
        val list = manualLocations.value?.toMutableList() ?: mutableListOf()
        val idx = list.indexOfFirst {
            it.title == location.title &&
                    it.latitude == location.latitude &&
                    it.longitude == location.longitude
        }

        if (idx != -1) {
            val updated = list[idx].copy(isFav = !(list[idx].isFav ?: false))
            list[idx] = updated
            manualLocations.postValue(list)
            Log.d("SharedVM", "⭐ toggleFavoritePersistent: updated manualLocations for ${updated.title}")
        } else {
            val added = location.copy(isFav = true)
            list.add(added)
            manualLocations.postValue(list)
            Log.d("SharedVM", "➕ toggleFavoritePersistent: added manual favorite ${added.title}")
        }

        try {
            val storage = MyLocationsStorage(getApplication())
            val savedList = storage.load().toMutableList()
            val savedIdx = savedList.indexOfFirst {
                it.title == location.title &&
                        it.latitude == location.latitude &&
                        it.longitude == location.longitude
            }

            if (savedIdx != -1) {
                savedList[savedIdx] = savedList[savedIdx].copy(isFav = list.first {
                    it.title == savedList[savedIdx].title &&
                            it.latitude == savedList[savedIdx].latitude &&
                            it.longitude == savedList[savedIdx].longitude
                }.isFav)
            } else {
                val toAdd = list.firstOrNull {
                    it.title == location.title &&
                            it.latitude == location.latitude &&
                            it.longitude == location.longitude
                }
                if (toAdd != null) savedList.add(toAdd)
            }

            storage.save(savedList)
            Log.d("SharedVM", "💾 toggleFavoritePersistent: saved changes to MyLocationsStorage (${savedList.size})")
        } catch (e: Exception) {
            Log.e("SharedVM", "Failed saving favorites: ${e.message}", e)
        }
        Log.d("FavoriteDebug", "🔁 ${location.title} → ${list[idx].isFav}")
    }

    fun refreshLocation(context: Context) {
        try {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            val providers = locationManager.getProviders(true)
            var bestLocation: android.location.Location? = null
            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) bestLocation = l
            }
            bestLocation?.let {
                currentLocation.value = LocationData(it.latitude, it.longitude)
                Log.d("SharedVM", "📍 الموقع الحالي تم تحديثه: ${it.latitude}, ${it.longitude}")
            } ?: run {
                Log.d("SharedVM", "⚠️ لم يتم العثور على موقع محدث")
            }
        } catch (e: SecurityException) {
            Log.e("SharedVM", "🚫 فشل تحديث الموقع: ${e.message}")
        }
    }

    fun startLocationUpdates(context: Context) {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setWaitForAccurateLocation(true)
            .build()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation ?: return
                    currentLocation.postValue(
                        LocationData(
                            title = "الموقع الحالي",
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    )

                    val prefs = context.getSharedPreferences("user_location", Context.MODE_PRIVATE)
                    prefs.edit().putFloat("lat", location.latitude.toFloat()).apply()
                    prefs.edit().putFloat("lon", location.longitude.toFloat()).apply()
                }
            },
            Looper.getMainLooper()
        )
    }

    fun cacheLocationsIfNeeded(list: List<LocationEntity>) {
        if (cachedLocations == null) {
            cachedLocations = list.toMutableList()
            Log.d("SharedVM", "✅ تم تخزين المواقع مؤقتًا داخل SharedLocationViewModel (${list.size}) موقع")
        } else {
            Log.d("SharedVM", "⚠️ تم تجاهل التخزين — الكاش موجود مسبقًا (${cachedLocations?.size}) موقع")
        }
    }
}
