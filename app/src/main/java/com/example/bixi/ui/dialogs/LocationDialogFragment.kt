package com.example.bixi.ui.dialogs

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.example.bixi.R
import com.example.bixi.helper.BackgroundStylerService
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LocationDialogFragment(
    private val onSave: (LatLng, Float) -> Unit
) : BottomSheetDialogFragment(), OnMapReadyCallback {

    val LOCATION_RANGE_LOW: Int = 300
    val LOCATION_RANGE_MEDIUM: Int = 200
    val LOCATION_RANGE_HIGH: Int = 100

    private lateinit var mapContainer: View
    private lateinit var googleMap: GoogleMap
    private lateinit var saveButton: Button
    private lateinit var accuracyText: TextView
    private lateinit var closeButton: ImageView

    // Pentru locația în timp real
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var locationCallback: LocationCallback? = null

    // Date curente despre locație
    private var currentLatLng: LatLng? = null
    private var currentAccuracy: Float = Float.MAX_VALUE

    // Configurări
    private val locationWaitTimeSeconds = 15
    private val accuracyThreshold = 300f // 50 metri
    private var countdownHandler: Handler? = null
    private var countdownRunnable: Runnable? = null
    private var isLocationUpdatesActive = false

    // Callback pentru când dialogul este închis
    private var onDismissCallback: (() -> Unit)? = null

    fun setOnDismissListener(callback: () -> Unit) {
        onDismissCallback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_location, container, false)

        view.post {
            val parent = view.parent as? View
            parent?.let {
                val layoutParams = it.layoutParams
                if (layoutParams is CoordinatorLayout.LayoutParams) {
                    val behavior = layoutParams.behavior
                    if (behavior is BottomSheetBehavior<*>) {
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                        behavior.isDraggable = false
                        behavior.isHideable = false
                    }
                }
            }
        }

        initViews(view)
        initMap()
        setListeners()
        setStyles()
        initStartUI()

        return view
    }

    private fun initStartUI(){
        // Inițializează UI-ul
        updateAccuracyDisplay(Float.MAX_VALUE)
        saveButton.isEnabled = false
        saveButton.text = getString(R.string.searching_location)
    }

    private fun initLocationRequest(){
        // Inițializează clientul pentru locație
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Configurează cererea de locație pentru acuratețe maximă
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L) // Actualizare la 2 secunde
            .setMinUpdateIntervalMillis(4000L) // Minim 1 secundă între actualizări
            .setMaxUpdateAgeMillis(5000L) // Locațiile mai vechi de 5 secunde sunt considerate învechite
            .setWaitForAccurateLocation(false) // Nu aștepta locații foarte precise
            .build()
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            updateUI(false, getString(R.string.location_permission_required))
            return
        }

        isLocationUpdatesActive = true

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                val location = locationResult.lastLocation
                if (location != null && isLocationUpdatesActive) {
                    updateLocationData(location)
                }
            }
        }

        try {
            // Pornește actualizările continue
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )

            // Pornește countdown-ul pentru timpul maxim de așteptare
            startWaitCountdown()

        } catch (e: SecurityException) {
            updateUI(false, getString(R.string.security_error_location))
        }
    }

    private fun updateLocationData(location: Location) {
        currentLatLng = LatLng(location.latitude, location.longitude)
        currentAccuracy = location.accuracy

        // Actualizează UI-ul
        updateAccuracyDisplay(currentAccuracy)

        // Actualizează harta dacă este gata
        if (::googleMap.isInitialized) {
            updateMapLocation(currentLatLng!!)
        }

        // Verifică dacă acuratețea este suficient de bună
        if (currentAccuracy <= accuracyThreshold) {
            enableSaveButton()
        }
    }

    private fun updateMapLocation(latLng: LatLng) {

        // Centrează harta pe locația nouă
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
    }

    private fun updateAccuracyDisplay(accuracy: Float) {
        if (accuracy == Float.MAX_VALUE) {
            accuracyText.text = getString(R.string.searching_gps_signal)
            accuracyText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            return
        }

        accuracyText.text = getString(R.string.gps_accuracy_display, accuracy.toInt())

        // Colorează textul în funcție de acuratețe
        val color = when {
            accuracy <= LOCATION_RANGE_HIGH -> android.R.color.holo_green_dark  // Foarte bună
            accuracy <= LOCATION_RANGE_MEDIUM -> android.R.color.holo_green_light // Bună
            accuracy <= LOCATION_RANGE_LOW -> android.R.color.holo_orange_light // Acceptabilă
            else -> android.R.color.holo_red_light // Slabă
        }
        accuracyText.setTextColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun startWaitCountdown() {
        var remainingSeconds = locationWaitTimeSeconds

        countdownHandler = Handler(Looper.getMainLooper())
        countdownRunnable = object : Runnable {
            override fun run() {
                if (remainingSeconds > 0 && isLocationUpdatesActive) {
                    // Verifică din nou acuratețea
                    if (currentAccuracy <= accuracyThreshold) {
                        enableSaveButton()
                        return
                    }

                    // Actualizează textul butonului cu timpul rămas
                    if (currentLatLng != null) {
                        saveButton.text = getString(R.string.improving_accuracy_countdown, remainingSeconds)
                    } else {
                        saveButton.text = getString(R.string.searching_location_countdown, remainingSeconds)
                    }

                    remainingSeconds--
                    countdownHandler?.postDelayed(this, 1000)
                } else {
                    // Countdown-ul s-a terminat, activează butonul oricum
                    enableSaveButton()
                }
            }
        }

        countdownRunnable?.let { countdownHandler?.post(it) }
    }

    private fun enableSaveButton() {
        // Oprește countdown-ul
        countdownRunnable?.let { countdownHandler?.removeCallbacks(it) }

        saveButton.isEnabled = true

        if (currentLatLng != null) {
            if (currentAccuracy <= accuracyThreshold) {
                saveButton.text = getString(R.string.confirm_location)
            } else {
                saveButton.text = getString(R.string.confirm_location_low_accuracy)
            }
        } else {
            saveButton.text = getString(R.string.confirm_without_location)
        }
    }

    private fun updateUI(success: Boolean, message: String) {
        if (!success) {
            accuracyText.text = message
            accuracyText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
            saveButton.isEnabled = true
            saveButton.text = getString(R.string.confirm)
        }
    }

    private fun stopLocationUpdates() {
        isLocationUpdatesActive = false
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        countdownRunnable?.let { countdownHandler?.removeCallbacks(it) }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Activează layer-ul "My Location" dacă avem permisiunile necesare
        if (hasLocationPermission()) {
            try {
                googleMap.isMyLocationEnabled = true
                googleMap.uiSettings.isMyLocationButtonEnabled = true // Dezactivăm butonul default
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        // Configurează setările hărții
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = false

        initLocationRequest()
        // Pornește căutarea locației - presupune că GPS este deja activ
        startLocationUpdates()

        animateMapOnStart()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun initMap(){
        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
    }

    private fun initViews(view: View){
        mapContainer = view.findViewById(R.id.v_map_overlay)
        saveButton = view.findViewById(R.id.saveButton)
        accuracyText = view.findViewById(R.id.accuracyText)
        closeButton = view.findViewById(R.id.iv_close)
    }

    private fun animateMapOnStart(){
        mapContainer.animate()?.alpha(0f)?.setDuration(1000)?.setStartDelay(100)?.setInterpolator(android.view.animation.AccelerateInterpolator())
            ?.start()
    }

    private fun setListeners(){
        closeButton.setOnClickListener {
            dismiss()
        }

        saveButton.setOnClickListener {
            if (currentLatLng != null) {
                onSave(currentLatLng!!, currentAccuracy)
            }
            dismiss()
        }
    }

    private fun setStyles(){
        BackgroundStylerService.setRoundedBackground(
            view = closeButton,
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.md_theme_surfaceVariant),
            cornerRadius = 20f * requireContext().resources.displayMetrics.density,
            withRipple = true,
            rippleColor = ContextCompat.getColor(requireContext(), R.color.md_theme_surfaceContainerLow),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLocationUpdates()
        countdownHandler = null
        countdownRunnable = null
    }

    override fun onDestroy() {
        super.onDestroy()
        onDismissCallback?.invoke()
    }
}