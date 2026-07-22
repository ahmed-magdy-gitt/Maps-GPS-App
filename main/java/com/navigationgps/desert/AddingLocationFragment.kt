package com.navigationgps.desert

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.navigationgps.desert.databinding.DialogAddLocationBinding

import com.navigationgps.desert.utils.com.example.compassapp.SharedLocationViewModel

class AddLocationDialogFragment(
    private val onLocationAdded: (title: String, lat: Double, lon: Double) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddLocationBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedLocationViewModel: SharedLocationViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddLocationBinding.inflate(requireActivity().layoutInflater)
        val app = requireActivity().application as MyApplication
        sharedLocationViewModel = app.sharedLocationViewModel

        binding.editTextLatitude.setText("")
        binding.editTextLongitude.setText("")
        binding.checkBoxUseCurrentLocation.isChecked = false

        sharedLocationViewModel.currentLocation.observe(this) { location ->
            if (binding.checkBoxUseCurrentLocation.isChecked && location != null) {
                binding.editTextLatitude.setText(location.latitude.toString())
                binding.editTextLongitude.setText(location.longitude.toString())
                binding.editTextLatitude.isEnabled = false
                binding.editTextLongitude.isEnabled = false
            }
        }

        binding.checkBoxUseCurrentLocation.setOnCheckedChangeListener { _, isChecked ->
            binding.editTextLatitude.isEnabled = !isChecked
            binding.editTextLongitude.isEnabled = !isChecked

            if (isChecked) {
                val location = sharedLocationViewModel.currentLocation.value
                if (location != null) {
                    binding.editTextLatitude.setText(location.latitude.toString())
                    binding.editTextLongitude.setText(location.longitude.toString())
                } else {
                    binding.editTextLatitude.setText("يرجي تحديث البوصلة أولا")
                    binding.editTextLongitude.setText("يرجي تحديث البوصلة أولا")
                }
            } else {
                binding.editTextLatitude.setText("")
                binding.editTextLongitude.setText("")
            }
        }

        val dialog = AlertDialog.Builder(requireContext())

            .setView(binding.root)
            .create()
        binding.btnSave.setOnClickListener {
            val title = binding.editTextTitle.text.toString().trim()
            val isUsingCurrent = binding.checkBoxUseCurrentLocation.isChecked
            val latText = binding.editTextLatitude.text.toString().trim()
            val lonText = binding.editTextLongitude.text.toString().trim()

            var isValid = true

            if (title.isEmpty()) {
                binding.editTextTitle.error = "مطلوب إدخال اسم الموقع"
                isValid = false
            }

            if (!isUsingCurrent) {
                if (latText.isEmpty() || latText == "يرجي تحديث البوصلة أولا") {
                    binding.editTextLatitude.error = "مطلوب إدخال خط العرض"
                    isValid = false
                }
                if (lonText.isEmpty() || lonText == "يرجي تحديث البوصلة أولا") {
                    binding.editTextLongitude.error = "مطلوب إدخال خط الطول"
                    isValid = false
                }
            }

            if (!isValid) return@setOnClickListener

            val lat = if (isUsingCurrent)
                sharedLocationViewModel.currentLocation.value?.latitude ?: 0.0
            else
                latText.toDoubleOrNull() ?: 0.0

            val lon = if (isUsingCurrent)
                sharedLocationViewModel.currentLocation.value?.longitude ?: 0.0
            else
                lonText.toDoubleOrNull() ?: 0.0


            sharedLocationViewModel.addManualLocation(
                LocationEntity(title = title, latitude = lat, longitude = lon)
            )
            dialog.dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnDismissListener { _binding = null }
        return dialog
    }

}
