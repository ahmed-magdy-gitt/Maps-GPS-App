package com.navigationgps.desert

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import com.navigationgps.desert.databinding.ActivityCompassBinding
import com.navigationgps.desert.utils.com.example.compassapp.SharedLocationViewModel
import com.google.android.gms.location.*
import com.github.javiersantos.piracychecker.PiracyChecker
import com.github.javiersantos.piracychecker.callbacks.PiracyCheckerCallback
import com.github.javiersantos.piracychecker.enums.PiracyCheckerError
import com.github.javiersantos.piracychecker.enums.InstallerID
import com.github.javiersantos.piracychecker.enums.PirateApp
import com.github.javiersantos.piracychecker.enums.Display
import com.navigationgps.desert.BuildConfig

class CompassActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityCompassBinding
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private var accelSensor: Sensor? = null
    private var magnetSensor: Sensor? = null
    private var useRotationVector = false

    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sharedLocationViewModel: SharedLocationViewModel
    private var currentDegree = 0f
    private var targetLat: Double? = null
    private var targetLon: Double? = null
    private var targetTitle: String? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var arrowImage: ImageView

    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setContentView(binding.root)

        // GOOGLE PLAY LICENSING CHECK
        // RSA Key from client
        val licensingKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnLnUEhc5iINbczxXBcvfKDa3AsEAd6Lt1TPZhEg+4pQLyyLzD/s7LCmlte3B8TZ81PQg3v2DOXEllX7E4NbUR1Lx+YUpryOSVEnAtk3exm7qpMO55LDvCkTzVhWnTTcUAPcN6sKr78wiuH+yah468IAzO+VLV3ei6CxCx6J6mkU4QAuiNmiSVlx7OyIEv4FYyFK+OLGYMn7OTGx6fGWl2kMt9Uz8G6X5Wl1ne4lOHF++o277T49CYbs+Va/GelmUWXdOjPoCmSPZqdYorFIE/syh9x092Icmdj4CcN5aYS2qw0vQYxOwIxYwB7y5zQBUwksYgLUTQedFJOYev+WeTQIDAQAB"

        if (licensingKey.isNotEmpty() && !BuildConfig.DEBUG) {
             PiracyChecker(this)
                .enableGooglePlayLicensing(licensingKey)
                .enableInstallerId(InstallerID.GOOGLE_PLAY)
                .callback(object : PiracyCheckerCallback() {
                    override fun allow() {
                        // License Valid: Do nothing, let app run
                    }

                    override fun doNotAllow(error: PiracyCheckerError, appit: PirateApp?) {
                        // License Invalid: Show Dialog then close (Must be on Main Thread)
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                androidx.appcompat.app.AlertDialog.Builder(this@CompassActivity)
                                    .setTitle("التطبيق مدفوع")
                                    .setMessage("يرجى تفعيل التطبيق من Google Play لاستخدام")
                                    .setCancelable(false)
                                    .setPositiveButton("إغلاق التطبيق") { _, _ ->
                                        finish()
                                    }
                                    .show()
                            }
                        }
                    }

                    override fun onError(error: PiracyCheckerError) {
                         // Check error (e.g. network), usually allow or retry. For now, allow.
                    }
                })
                .start()
        }

        binding.newButton.setOnClickListener {
            val intent = Intent(this@CompassActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
        val callback = object : OnBackPressedCallback(true /* enabled */) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@CompassActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }

                startActivity(intent)

                finish()
            }
        }

        onBackPressedDispatcher.addCallback(this, callback)

        binding.clearTargetButton?.setOnClickListener {

            val app = application as MyApplication
            app.tempTarget = null

            val prefs = getSharedPreferences("LastTarget", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            targetLat = null
            targetLon = null
            targetTitle = null

            binding.targetLocationLayout.visibility = View.GONE
            binding.arrowImage.visibility = View.GONE
            binding.clearTargetButton!!.visibility = View.GONE

            android.util.Log.d("CompassTarget", "🗑️ تم حذف الهدف بنجاح")
        }

        markCompassVisited()
        val app = application as MyApplication
        sharedLocationViewModel = app.sharedLocationViewModel
        val prefs = getSharedPreferences("LastTarget", Context.MODE_PRIVATE)
        if (prefs.contains("lat") && prefs.all["lat"] is Float) {
            prefs.edit().clear().apply()
            android.util.Log.d("CompassTarget", "🧹 تم حذف القيم القديمة من LastTarget (Float)")
        }
        val hasIntentTarget = intent.hasExtra("latitude") && intent.hasExtra("longitude")

        if (hasIntentTarget) {
            targetLat = intent.getDoubleExtra("latitude", 0.0)
            targetLon = intent.getDoubleExtra("longitude", 0.0)
            targetTitle = intent.getStringExtra("title")

            if (targetLat != 0.0 || targetLon != 0.0) {
                prefs.edit()
                    .putString("lat", targetLat.toString())
                    .putString("lon", targetLon.toString())
                    .putString("title", targetTitle)
                    .apply()
            }

            android.util.Log.d("CompassTarget", "📥 تم استقبال هدف عبر Intent: $targetTitle ($targetLat, $targetLon)")
        } else {
            val latStr = prefs.getString("lat", null)
            val lonStr = prefs.getString("lon", null)

            if (!latStr.isNullOrEmpty() && !lonStr.isNullOrEmpty()) {
                targetLat = latStr.toDoubleOrNull()
                targetLon = lonStr.toDoubleOrNull()
                targetTitle = prefs.getString("title", "وجهة محفوظة")
                android.util.Log.d("CompassTarget", "📂 استرجاع هدف محفوظ: $targetTitle ($targetLat, $targetLon)")
            } else {
                android.util.Log.d("CompassTarget", "⚠️ لا يوجد هدف محفوظ ولا Intent يحمل هدف")
            }
        }

        arrowImage = binding.arrowImage
        arrowImage = binding.arrowImage
        arrowImage.post {
            arrowImage.pivotX = arrowImage.width / 2f
            arrowImage.pivotY = arrowImage.height.toFloat()
        }

        if (targetLat != null && targetLon != null && targetLat!! != 0.0 && targetLon!! != 0.0) {
            binding.targetLocationLayout.visibility = View.VISIBLE
            binding.targetTitle.text = targetTitle ?: "وجهة غير معروفة"
            binding.targetLat.text = targetLat.toString()
            binding.targetLong.text = targetLon.toString()
            arrowImage.visibility = ImageView.VISIBLE
            binding.clearTargetButton!!.visibility = View.VISIBLE
        } else {
            binding.targetLocationLayout.visibility = View.GONE
            arrowImage.visibility = ImageView.GONE
            binding.clearTargetButton!!.visibility = View.GONE
        }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (rotationSensor != null) {
            useRotationVector = true
        } else {
            accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {

            startLocationUpdates()
            getLastLocation()
        }
    }
    private fun markCompassVisited() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putBoolean("has_visited_compass", true)
        editor.apply()
    }

    override fun onResume() {
        super.onResume()
        try {
            if (useRotationVector) {
                rotationSensor?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
            } else {
                accelSensor?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
                magnetSensor?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)

        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            } catch (_: Exception) {}
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        try {
            if (useRotationVector && event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val degree = Math.toDegrees(orientation[0].toDouble()).toInt()
                updateCompass(degree)
            } else {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        gravity = event.values.clone()
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        geomagnetic = event.values.clone()
                        hasGeomagnetic = true
                    }
                }
                if (hasGravity && hasGeomagnetic) {
                    val R = FloatArray(9)
                    val I = FloatArray(9)
                    if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(R, orientation)
                        val degree = Math.toDegrees(orientation[0].toDouble()).toInt()
                        updateCompass(degree)
                    }
                }
            }
        } catch (_: Exception) {}
    }
    private fun updateCompass(degree: Int) {
        binding.locationText.text = "°$degree"
        binding.directionText.text = getCompassDirection(degree)

        val rotationAnimation = RotateAnimation(
            currentDegree,
            (-degree).toFloat(),
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        rotationAnimation.duration = 210
        rotationAnimation.fillAfter = true
        binding.compassImage.startAnimation(rotationAnimation)
        currentDegree = (-degree).toFloat()

        if (targetLat != null && targetLon != null && targetLat != 0.0 && targetLon != 0.0) {
            val targetAzimuth = calculateAzimuth(latitude, longitude, targetLat!!, targetLon!!)
            val arrowDegree = targetAzimuth - degree
            arrowImage.rotation = arrowDegree
        }
    }

    private fun getCompassDirection(degree: Int): String {
        return when (degree) {
            in 337..360, in 0..22 -> "شمال"
            in 23..67 -> "شمال شرق"
            in 68..112 -> "شرق"
            in 113..157 -> "جنوب شرق"
            in 158..202 -> "جنوب"
            in 203..247 -> "جنوب غرب"
            in 248..292 -> "غرب"
            in 293..336 -> "شمال غرب"
            else -> "شمال"
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateLocationUI(location)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                updateLocationUI(it)
                sharedLocationViewModel.updateLocation(it.latitude, it.longitude)
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
                getLastLocation()
            } else {
                android.util.Log.w("CompassActivity", "🚨 لم يتم منح إذن الموقع. قد لا تعمل بعض ميزات التطبيق.")
            }
        }
    }

    private fun updateLocationUI(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
        val altitude = location.altitude
        val speedKmh = location.speed * 3.6f
        val displayedSpeed = if (speedKmh < 1.0f) 0f else speedKmh

        sharedLocationViewModel.updateLocation(latitude, longitude)

        binding.currentLat.text = "$latitude"
        binding.currentLong.text = "$longitude"
        binding.altitudeText.text = "${altitude.toInt()} م"
        binding.speedText.text = "السرعة\n${String.format("%.2f", displayedSpeed)}\n كم/س"

        val qibla = calculateQiblaDirection(latitude, longitude)
        binding.qiblaDirectionText.text = "\nاتجاه القبلة: ${String.format("%.1f", qibla)}°"

        if (targetLat != null && targetLon != null && targetLat!! != 0.0 && targetLon!! != 0.0) {
            binding.arrowImage.visibility = ImageView.VISIBLE
            binding.targetLocationLayout.visibility = ImageView.VISIBLE

            val currentLocation = Location("current").apply { latitude = this@CompassActivity.latitude; longitude = this@CompassActivity.longitude }
            val targetLocation = Location("target").apply { latitude = targetLat!!; longitude = targetLon!! }

            val distanceInMeters = currentLocation.distanceTo(targetLocation)
            val distanceInKm = distanceInMeters / 1000
            binding.horizontalDistance.text = String.format("%.2f كم", distanceInKm)
        } else {
            binding.horizontalDistance.text = "--"
            binding.arrowImage.visibility = ImageView.GONE
        }
        val prefs = getSharedPreferences("CompassPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("notification_shown", true).apply()

    }

    private fun calculateAzimuth(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val loc1 = Location("loc1").apply { latitude = lat1; longitude = lon1 }
        val loc2 = Location("loc2").apply { latitude = lat2; longitude = lon2 }
        return loc1.bearingTo(loc2)
    }

    fun calculateQiblaDirection(lat: Double, lon: Double): Double {
        val kaabaLat = Math.toRadians(21.422487)
        val kaabaLon = Math.toRadians(39.826206)
        val userLat = Math.toRadians(lat)
        val userLon = Math.toRadians(lon)
        val deltaLon = kaabaLon - userLon
        val y = Math.sin(deltaLon)
        val x = Math.cos(userLat) * Math.tan(kaabaLat) - Math.sin(userLat) * Math.cos(deltaLon)
        var qibla = Math.toDegrees(Math.atan2(y, x))
        if (qibla < 0) qibla += 360.0
        return qibla
    }
    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration)
        configuration.fontScale = 1.0f
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }
    override fun onDestroy() {
        super.onDestroy()

    }
}