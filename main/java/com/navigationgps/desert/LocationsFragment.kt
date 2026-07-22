package com.navigationgps.desert

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.navigationgps.desert.databinding.FragmentLocationsBinding
import com.navigationgps.desert.utils.com.example.compassapp.FavPrefs
import com.navigationgps.desert.utils.com.example.compassapp.SharedLocationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*

class LocationsFragment : Fragment() {

    private var _binding: FragmentLocationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedLocationViewModel: SharedLocationViewModel
    private lateinit var viewModel: LocationViewModel
    private lateinit var adapter: LocationsAdapter
    private var allLocationsCache: List<LocationEntity> = emptyList()
    private var searchQuery: String = ""
    private var filterJob: Job? = null
    private var searchJob: Job? = null


    private val distances = listOf(100.0, 200.0, 500.0, 4000.0)
    private var userLat: Double = 29.04434880
    private var userLon: Double = 31.10688340
    override fun onAttach(context: Context) {
        val configuration = Configuration(context.resources.configuration)
        configuration.fontScale = 1.0f
        val newContext = context.createConfigurationContext(configuration)
        super.onAttach(newContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireActivity().application as MyApplication
        sharedLocationViewModel = app.sharedLocationViewModel
        setupRecyclerView()
        setupSearch()
        setupSpinner()
        observeLocation()
        observeAllLocations()
    }

    private fun setupSearch() {
        binding.searchIcon.setOnClickListener {
            if (binding.searchEditText.visibility == View.GONE) {
                binding.searchEditText.visibility = View.VISIBLE
                binding.searchEditText.requestFocus()
            } else {
                binding.searchEditText.visibility = View.GONE
                binding.searchEditText.text.clear()
                searchQuery = ""
                updateFilteredList(distances[binding.distanceSpinner.selectedItemPosition])
            }
        }

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(400) // debounce
                    searchQuery = s.toString()
                    updateFilteredList(distances[binding.distanceSpinner.selectedItemPosition])
                }
            }
        })
    }
    private fun setupSpinner() {
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            distances
        )

        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)

        binding.distanceSpinner.adapter = spinnerAdapter

        binding.distanceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateFilteredList(distances[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    private fun observeLocation() {
        sharedLocationViewModel.currentLocation.observe(viewLifecycleOwner) { locationData ->
            locationData?.let {
                userLat = it.latitude
                userLon = it.longitude
                adapter.updateUserLocation(userLat, userLon)

            }
        }
    }
    private fun observeAllLocations() {
        viewModel = ViewModelProvider(requireActivity())[LocationViewModel::class.java]

        sharedLocationViewModel.cachedLocations?.let { cached ->
            Log.d("LocationsFragment", "🧠 تم استخدام البيانات من الكاش (عدد = ${cached.size})")
            allLocationsCache = cached
            updateFilteredList(distances[binding.distanceSpinner.selectedItemPosition])
            return
        }

        Log.d("LocationsFragment", "📥 لا يوجد كاش، جاري التحميل من قاعدة البيانات...")
        viewModel.allLocations.observe(viewLifecycleOwner) { locations ->
            Log.d("LocationsFragment", "✅ تم تحميل البيانات من Room (${locations.size}) موقع")
            allLocationsCache = locations
            sharedLocationViewModel.cacheLocationsIfNeeded(locations)
            updateFilteredList(distances[binding.distanceSpinner.selectedItemPosition])
        }
        viewModel.locationsCache.observe(viewLifecycleOwner) { locations ->
            adapter.submitList(locations)
        }
    }


    private var filteredList: List<LocationEntity> = emptyList()
    private var displayedCount = 0
    private val batchSize = 100
    private var isLoadingMore = false
    private fun updateFilteredList(maxDistance: Double) {
        if (!isAdded || _binding == null) return

        binding.progressContainer.visibility = View.VISIBLE
        filterJob?.cancel()
        filterJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val filtered = allLocationsCache.filter { loc ->
                val lat = loc.latitude ?: 0.0
                val lon = loc.longitude ?: 0.0
                val distanceKm = calculateDistance(userLat, userLon, lat, lon)
                distanceKm <= maxDistance && (loc.title ?: "").contains(searchQuery, ignoreCase = true)
            }
                .sortedBy {
                    calculateDistance(userLat, userLon, it.latitude ?: 0.0, it.longitude ?: 0.0)
                }

            withContext(Dispatchers.Main) {
                filteredList = filtered
                displayedCount = 0
                adapter.updateUserLocation(userLat, userLon)
                loadNextBatch() // عرض أول دفعة
                binding.progressContainer.visibility = View.GONE
            }
        }
    }

    private fun loadNextBatch() {
        if (isLoadingMore) return
        isLoadingMore = true

        val nextBatch = filteredList.drop(displayedCount).take(batchSize)
        if (nextBatch.isNotEmpty()) {
            displayedCount += nextBatch.size
            val updatedList = filteredList.take(displayedCount)
            adapter.submitList(updatedList)
        }

        isLoadingMore = false
    }
    private fun setupRecyclerView() {
        adapter = LocationsAdapter(
            onFavoriteClick = { toggleFavorite(it) },
            onItemClick = { showOptionsDialog(it) }
        )

        val layoutManager = LinearLayoutManager(requireContext())
        binding.locationsRecyclerView.layoutManager = layoutManager
        binding.locationsRecyclerView.adapter = adapter
        binding.locationsRecyclerView.addOnScrollListener(object :
            androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoadingMore && (visibleItemCount + firstVisibleItemPosition >= totalItemCount - 10)) {
                    loadNextBatch()
                }
            }
        })

    }


    private fun refreshLocationsData() {

        viewModel .allLocations

        Toast.makeText(requireContext(), "جارٍ تحديث المواقع...", Toast.LENGTH_SHORT).show()
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
    private fun toggleFavorite(location: LocationEntity) {
        viewModel.toggleFavorite(location)
        val newStatus = !(location.isFav ?: false)
        FavPrefs.saveFavState(requireContext(), location.id, newStatus)
        val updated = location.copy(isFav = newStatus)
        adapter.updateFavoriteStatus(updated)
    }
    private fun showOptionsDialog(location: LocationEntity) {
        val options = arrayOf("مفضلة", "مشاركة", "الذهاب إلى")
        AlertDialog.Builder(requireContext(),R.style.CustomAlertDialogTheme)
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
        val lat = location.latitude
        val lon = location.longitude
        val title = location.title ?: "موقع بدون اسم"


        val mapsUrl = "https://www.google.com/maps?q=$lat,$lon"

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "📌 $title\nافتح الموقع هنا:\n$mapsUrl"
            )
            type = "text/plain"
        }

        startActivity(Intent.createChooser(shareIntent, "شارك الموقع عبر"))
    }




    private fun goToLocation(location: LocationEntity) {
        val options = arrayOf("الذهاب إلى الخريطة", "الذهاب إلى البوصلة")

        AlertDialog.Builder(requireContext(),R.style.CustomAlertDialogTheme)
            .setTitle("اختر طريقة الذهاب")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        val gmmIntentUri = Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}(${location.title})")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps") // لو فيه Google Maps

                        try {

                            startActivity(mapIntent)
                        } catch (e: Exception) {
                            val fallbackIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            startActivity(fallbackIntent)
                        }
                    }
                    1 -> {
                        val app = requireActivity().application as MyApplication
                        app.tempTarget = location


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
