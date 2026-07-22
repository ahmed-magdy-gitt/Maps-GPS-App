package com.navigationgps.desert

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.navigationgps.desert.databinding.ItemLocationBinding

class LocationAdapter : ListAdapter<LocationEntity, LocationAdapter.LocationViewHolder>(DIFF_CALLBACK) {

    companion object {
        private const val PAYLOAD_LOCATION_UPDATE = "LOCATION_UPDATE_PAYLOAD" // 💡 تعريف حمولة التحديث

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LocationEntity>() {
            override fun areItemsTheSame(oldItem: LocationEntity, newItem: LocationEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: LocationEntity, newItem: LocationEntity): Boolean {
                return oldItem == newItem
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(location: LocationEntity)
    }

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }
    fun updateFavoriteStatus(updatedLocation: LocationEntity) {
        val currentListCopy = currentList.toMutableList()
        val index = currentListCopy.indexOfFirst {
            it.title == updatedLocation.title &&
                    it.latitude == updatedLocation.latitude &&
                    it.longitude == updatedLocation.longitude
        }
        if (index != -1) {
            currentListCopy[index] = updatedLocation
            submitList(currentListCopy.toList())
            notifyItemChanged(index)
        }
    }


    private var userLat: Double? = null
    private var userLon: Double? = null

    fun updateUserLocation(lat: Double, lon: Double) {
        userLat = lat
        userLon = lon
        Log.d("AdapterDebug", "📍 تم تحديث موقع المستخدم داخل Adapter: ($userLat, $userLon)")
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_LOCATION_UPDATE)
        }
    }

    inner class LocationViewHolder(private val binding: ItemLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(getItem(position))
                }
            }
        }

        fun updateDistance(location: LocationEntity) {
            val distanceKm = if (userLat != null && userLon != null) {
                val results = FloatArray(1)
                android.location.Location.distanceBetween(
                    userLat!!, userLon!!,
                    location.latitude ?: 0.0,
                    location.longitude ?: 0.0,
                    results
                )
                results[0] / 1000.0
            } else {
                0.0
            }

            binding.distanceText.text = "المسافة: %.2f كم".format(distanceKm)
        }

        fun bind(location: LocationEntity) {
            binding.title.text = location.title ?: "بدون عنوان"
            binding.favoriteIcon.setImageResource(
                if (location.isFav == true) R.drawable.favred else R.drawable.favorite_border
            )
            updateDistance(location)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.any { it == PAYLOAD_LOCATION_UPDATE }) {

            holder.updateDistance(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }
}