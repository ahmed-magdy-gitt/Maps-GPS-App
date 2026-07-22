package com.navigationgps.desert

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.navigationgps.desert.databinding.ItemLocationBinding
import com.navigationgps.desert.utils.com.example.compassapp.FavPrefs

class LocationsAdapter(
    private val onFavoriteClick: (LocationEntity) -> Unit,
    private val onItemClick: (LocationEntity) -> Unit
) : ListAdapter<LocationEntity, LocationsAdapter.LocationViewHolder>(DiffCallback) {

    private var userLat: Double = 0.0
    private var userLon: Double = 0.0

    fun updateUserLocation(lat: Double, lon: Double) {
        userLat = lat
        userLon = lon
        notifyDataSetChanged()
    }

    inner class LocationViewHolder(private val binding: ItemLocationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(location: LocationEntity) {
            binding.title.text = location.title

            val lat = location.latitude ?: 0.0
            val lon = location.longitude ?: 0.0
            val distanceKm = calculateDistance(userLat, userLon, lat, lon)
            binding.distanceText.text = "المسافة: %.2f كم".format(distanceKm)

            binding.favoriteIcon.setImageResource(
                if (location.isFav == true) R.drawable.favred else R.drawable.favorite_border
            )
            binding.favoriteIcon.setOnClickListener { onFavoriteClick(location) }
            binding.root.setOnClickListener { onItemClick(location) }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {

        val location = getItem(position)
        val context = holder.itemView.context

        val isFav = FavPrefs.getFavState(context, location.id)


        val updatedLocation = location.copy(isFav = isFav)

        holder.bind(updatedLocation)

    }
    fun updateFavoriteStatus(updatedLocation: LocationEntity) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedLocation.id }
        if (index != -1) {
            currentList[index] = updatedLocation
            submitList(currentList.toList())

        }
    }
    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<LocationEntity>() {
            override fun areItemsTheSame(oldItem: LocationEntity, newItem: LocationEntity) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: LocationEntity, newItem: LocationEntity) = oldItem == newItem
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000.0
    }
}

