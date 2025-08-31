package com.example.bixi.ui.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.bixi.R
import com.example.bixi.databinding.FragmentTimekeepingBinding
import com.example.bixi.ui.dialogs.LocationDialogFragment
import com.example.bixi.viewModels.TimekeepingViewModel
import com.google.android.gms.maps.model.LatLng

// Importuri pentru locație (Android 10+)
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.bixi.enums.AttendanceType
import com.example.bixi.helper.ApiStatus
import com.example.bixi.helper.ResponseStatusHelper
import com.example.bixi.services.DialogService
import com.example.bixi.ui.activities.MainActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class TimekeepingFragment : Fragment() {

    private var _binding: FragmentTimekeepingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimekeepingViewModel by viewModels()

    // Pentru locație și GPS
    private lateinit var locationRequest: LocationRequest

    // Pentru tracking GPS status
    private var wasGpsEnabledBeforeRequest = false

    companion object {
        fun newInstance() = TimekeepingFragment()
    }

    // Launcher pentru activarea GPS-ului
    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // GPS a fost activat, acum poți deschide dialogul
            showLocationDialog()
        } else {
            // Utilizatorul a refuzat să activeze GPS-ul
            Toast.makeText(
                requireContext(),
                getString(R.string.gps_required_message),
                Toast.LENGTH_LONG
            ).show()
            binding.swAction.isChecked = !binding.swAction.isChecked
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurează cererea de locație pentru verificarea setărilor GPS
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setMinUpdateIntervalMillis(5000L)
            .setMaxUpdateAgeMillis(60000L)
            .setWaitForAccurateLocation(false)
            .build()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimekeepingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        initListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupViewModel(){

        viewModel.sendResponseCode.observe(viewLifecycleOwner) { statusCode ->
            showLoading(false)
            ResponseStatusHelper.showStatusMessage(requireContext(), statusCode)

            if(ApiStatus.fromCode(statusCode) != ApiStatus.SERVER_SUCCESS){
                binding.swAction.isChecked = !binding.swAction.isChecked
                return@observe
            }
        }

        viewModel.attendanceStatus.observe(viewLifecycleOwner) { status ->
            binding.swAction.isChecked = status == AttendanceType.START
        }
    }

    private fun initListeners(){
        binding.swAction.setOnClickListener {
            checkPermissionsGpsAndShowLocationDialog()
        }
    }

    private fun checkPermissionsGpsAndShowLocationDialog() {
        if (!hasLocationPermissions()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.location_permission_required),
                Toast.LENGTH_LONG
            ).show()
            binding.swAction.isChecked = !binding.swAction.isChecked
            return
        }

        // Verifică starea GPS-ului înainte de cerere
        wasGpsEnabledBeforeRequest = isGpsEnabled()

        if (isGpsEnabled()) {
            // GPS este deja activ, deschide direct dialogul
            showLocationDialog()
        } else {
            // GPS nu este activ, încearcă să îl activeze
            checkLocationSettings()
        }
    }

    private fun checkLocationSettings() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val client = LocationServices.getSettingsClient(requireActivity())
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // Setările sunt OK, deschide dialogul
            showLocationDialog()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Cere utilizatorului să activeze GPS-ul
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettingsLauncher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.gps_activation_error),
                        Toast.LENGTH_LONG
                    ).show()
                    binding.swAction.isChecked = !binding.swAction.isChecked
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.gps_activation_failed),
                    Toast.LENGTH_LONG
                ).show()
                binding.swAction.isChecked = !binding.swAction.isChecked
            }
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10+: verifică permisiunile de locație
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
            }
            else -> false // Nu suportăm versiuni mai vechi
        }
    }

    private fun showLocationDialog() {
        showLoading(true)
        val dialog = LocationDialogFragment { confirmedLocation, accuracy ->
            // TODO: Implementează trimiterea la server
             viewModel.sendAction(confirmedLocation)
        }

        // Setează callback pentru cazul când dialogul este închis fără salvare
        dialog.setOnDismissListener {
            binding.swAction.isChecked = !binding.swAction.isChecked
            showLoading(false)
            checkAndAskToDisableGps()
        }

        dialog.show(childFragmentManager, "LocationDialog")
    }

    private fun checkAndAskToDisableGps() {
        // Verifică dacă GPS-ul nu era pornit inițial și acum este pornit
        if (!wasGpsEnabledBeforeRequest && isGpsEnabled()) {
            showGpsDisableDialog()
        }
    }

    private fun showGpsDisableDialog() {
        DialogService.showConfirmationDialog(requireContext(), getString(R.string.gps_activated_title),
            getString(R.string.gps_activate_message), getString(R.string.yes_disable),
        getString(R.string.no_keep_active), R.drawable.ic_gps, {
                openLocationSettings()
            }, {
            })
    }

    private fun openLocationSettings() {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                getString(R.string.cannot_open_location_settings),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun showLoading(isLoading: Boolean, onFinish: (() -> Unit)? = null){
        (activity as MainActivity).showLoading(isLoading, onFinish)
    }
}