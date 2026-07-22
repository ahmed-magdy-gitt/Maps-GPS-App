package com.navigationgps.desert

import androidx.recyclerview.widget.DiffUtil

class LocationDiffCallback : DiffUtil.ItemCallback<LocationEntity>() {
    override fun areItemsTheSame(oldItem: LocationEntity, newItem: LocationEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: LocationEntity, newItem: LocationEntity): Boolean {
        return oldItem == newItem
    }
}
