package com.navigationgps.desert

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.navigationgps.desert.databinding.FragmentMyLocationsBinding
import com.navigationgps.desert.utils.com.example.compassapp.FavPrefs
import com.navigationgps.desert.utils.com.example.compassapp.SharedLocationViewModel
import androidx.appcompat.app.AlertDialog as AppCompatAlertDialog // استخدام هذا الاسم لتجنب تضارب الأسماء

// ----------------------------------------------------------------------------
class MyLocationsFragment : Fragment() {

    private var _binding: FragmentMyLocationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: LocationAdapter
    private lateinit var sharedLocationViewModel: SharedLocationViewModel
    private lateinit var locationViewModel: LocationViewModel
    private val myLocationsList = mutableListOf<LocationEntity>()
    private lateinit var storage: MyLocationsStorage

    override fun onAttach(context: Context) {
        val configuration = Configuration(context.resources.configuration)
        configuration.fontScale = 1.0f
        val newContext = context.createConfigurationContext(configuration)
        super.onAttach(newContext)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyLocationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LocationAdapter()
        binding.locationsList.layoutManager = LinearLayoutManager(requireContext())
        binding.locationsList.adapter = adapter

        val app = requireActivity().application as MyApplication
        sharedLocationViewModel = app.sharedLocationViewModel
        locationViewModel = ViewModelProvider(requireActivity())[LocationViewModel::class.java]
        storage = MyLocationsStorage(requireContext().applicationContext)


        myLocationsList.clear()
        val savedList = storage.load()

        Log.d("StorageDebug", "عدد المواقع المحفوظة بعد التحميل = ${storage.load().size}")
        myLocationsList.addAll(savedList)
        adapter.submitList(myLocationsList.toList())
        if (sharedLocationViewModel.myLocations.value.isNullOrEmpty() && savedList.isNotEmpty()) {
            sharedLocationViewModel.myLocations.value = savedList
        }

        sharedLocationViewModel.manualLocations.observe(viewLifecycleOwner) { updatedList ->
            Log.d("SharedVM", "🧠 Observer triggered — عدد المواقع اليدوية = ${updatedList.size}")


            if (!updatedList.isNullOrEmpty()) {
                myLocationsList.clear()
                myLocationsList.addAll(updatedList)
                storage.save(myLocationsList)
            }
            if (updatedList.isNullOrEmpty() && myLocationsList.isEmpty()) {
                myLocationsList.addAll(storage.load())
            }

            adapter.submitList(myLocationsList.toList())

            sharedLocationViewModel.currentLocation.value?.let { current ->
                adapter.updateUserLocation(current.latitude, current.longitude)
            }

            Log.d("MyLocObserver", "✅ Adapter تم تحديثه بعد التغيير (عدد المواقع = ${myLocationsList.size})")
        }


        sharedLocationViewModel.startLocationUpdates(requireContext())

        sharedLocationViewModel.currentLocation.observe(viewLifecycleOwner) { location ->
            if (location != null) {
                Log.d("GPS_Update", "📍 الموقع الحالي اتحدث: ${location.latitude}, ${location.longitude}")
                adapter.updateUserLocation(location.latitude, location.longitude)
                adapter.notifyDataSetChanged()
            }
        }

        adapter.setOnItemClickListener(object : LocationAdapter.OnItemClickListener {
            override fun onItemClick(location: LocationEntity) {
                showOptionsDialog(location)
            }
        })
    }

    @SuppressLint("SuspiciousIndentation")
    private fun showOptionsDialog(location: LocationEntity) {
        val options = arrayOf("إزالة", "مشاركة", "الذهاب إلى")

        val dialog = AppCompatAlertDialog.Builder(requireContext(), R.style.CustomAlertDialogTheme)
            .setTitle(location.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> removeLocation(location)
                    1 -> shareLocation(location)
                    //2 -> addToFavorites(location)
                    2 -> goToLocation(location)
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()

        dialog.getButton(AppCompatAlertDialog.BUTTON_POSITIVE)?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.title_brown)
        )
        dialog.getButton(AppCompatAlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.dark_neutral_gray)
        )
    }

    private fun removeLocation(location: LocationEntity) {
        myLocationsList.remove(location)
        adapter.submitList(myLocationsList.toList())
        storage.save(myLocationsList)
        sharedLocationViewModel.myLocations.postValue(myLocationsList)
        Toast.makeText(requireContext(), "تمت الإزالة من مواقعي", Toast.LENGTH_SHORT).show()
    }

    private fun addToFavorites(location: LocationEntity) {
        val newStatus = !(location.isFav ?: false)
        val updated = location.copy(isFav = newStatus)
        FavPrefs.saveFavState(requireContext(), location.id, newStatus)

        val index = myLocationsList.indexOfFirst { it.id == location.id }
        if (index != -1) {
            myLocationsList[index] = updated
            storage.save(myLocationsList)
        }
        sharedLocationViewModel.updateManualFavoriteStatus(updated)

        adapter.updateFavoriteStatus(updated)

        Toast.makeText(
            requireContext(),
            if (newStatus) "تمت الإضافة إلى المفضلة" else "تمت الإزالة من المفضلة",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun shareLocation(location: LocationEntity) {
        val lat = location.latitude
        val lon = location.longitude
        val title = location.title ?: "موقع بدون اسم"
        val mapsUrl = "https://www.google.com/maps?q=$lat,$lon"

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "📌 $title\nافتح الموقع على الخريطة:\n$mapsUrl")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "شارك الموقع عبر"))
    }

    private fun goToLocation(location: LocationEntity) {
        val options = arrayOf("الذهاب إلى الخريطة", "الذهاب إلى البوصلة")

        AppCompatAlertDialog.Builder(requireContext(), R.style.CustomAlertDialogTheme)
            .setTitle("اختر طريقة الذهاب")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        val gmmIntentUri = Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}(${location.title})")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")

                        try {

                            startActivity(mapIntent)
                        } catch (e: Exception) {

                            val fallbackIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            startActivity(fallbackIntent)
                        }
                    }
                    1 -> {
                        val intent = Intent(requireContext(), CompassActivity::class.java)
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
