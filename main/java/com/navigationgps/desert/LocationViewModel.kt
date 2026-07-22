package com.navigationgps.desert

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationViewModel(application: Application): AndroidViewModel(application) {
    private val dao = RoomDBHelper.getInstance(application).locationDao
    private val _addedLocation = MutableLiveData<LocationEntity>()
    val addedLocation: LiveData<LocationEntity> get() = _addedLocation

    val favouriteLocations: LiveData<List<LocationEntity>> = dao.getFavoriteLocations()
    val allLocations: LiveData<List<LocationEntity>> = dao.gAllLocations()
    private val _locationsCache = MutableLiveData<List<LocationEntity>>()
    val locationsCache: LiveData<List<LocationEntity>> get() = _locationsCache

    fun loadLocationsFromDB() {

        if (_locationsCache.value != null) return

        viewModelScope.launch(Dispatchers.IO) {
            val allLocations = RoomDBHelper.getInstance(getApplication()).locationDao.gAllLocations().value ?: emptyList()
            _locationsCache.postValue(allLocations)
        }
    }
    fun updateFavouriteStatus(locationId: Int, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateFavorite(locationId, isFavorite)
        }
    }


    fun insertLocation(location: LocationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(location)
            _addedLocation.postValue(location)
        }
    }
    fun deleteLocation(location: LocationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(location)
        }
    }
    fun toggleFavorite(location: LocationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val newStatus = !(location.isFav ?: false)
            dao.updateFavorite(location.id, newStatus)
            val updatedList = dao.getAllLocationsDirect()
            _locationsCache.postValue(updatedList)

            // كمان نحدث الـ LiveData الأساسية عشان الـ Fragment تشوف التغيير
            (allLocations as? MutableLiveData)?.postValue(updatedList)
        }
    }
    fun clearAllLocations() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAll() // دالة تمسح كل المواقع في DAO
        }
    }



    fun getFilteredLocations(maxDistance: Float, userLat: Double, userLon: Double): LiveData<List<LocationEntity>> {
        val filteredLocations = MutableLiveData<List<LocationEntity>>()
        val list = allLocations.value
        if (list == null) {
            filteredLocations.value = emptyList()
        } else {
            viewModelScope.launch(Dispatchers.Default) {
                val filtered = list.filter {
                    calculateDistance(userLat, userLon, it.latitude ?: 0.0, it.longitude ?: 0.0) <= maxDistance
                }
                filteredLocations.postValue(filtered)
            }
        }
        return filteredLocations
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000.0
    }
}
