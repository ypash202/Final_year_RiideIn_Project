package com.riidein.app.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.riidein.app.R
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.pow

class EnterRouteActivity : AppCompatActivity() {

    private lateinit var fromEditText: AutoCompleteTextView
    private lateinit var toEditText: AutoCompleteTextView
    private lateinit var locationManager: LocationManager

    private val locationPermissionRequestCode = 1001
    private var locationListener: LocationListener? = null

    data class KnownPlace(
        val name: String,
        val lat: Double,
        val lon: Double
    )

    private val knownPlaces = listOf(
        KnownPlace("Boudha", 27.7215, 85.3616),
        KnownPlace("Chabahil", 27.7215, 85.3469),
        KnownPlace("Kapan", 27.7339, 85.3570),
        KnownPlace("Thapathali", 27.6935, 85.3208),
        KnownPlace("Maitighar", 27.6947, 85.3218),
        KnownPlace("Baneshwor", 27.6889, 85.3358),
        KnownPlace("Koteshwor", 27.6781, 85.3498),
        KnownPlace("Kalanki", 27.6932, 85.2815),
        KnownPlace("Thamel", 27.7154, 85.3123),
        KnownPlace("New Road", 27.7049, 85.3122),
        KnownPlace("Lalitpur", 27.6710, 85.3250),
        KnownPlace("Bhaktapur", 27.6710, 85.4298)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_route)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        fromEditText = findViewById(R.id.fromEditText)
        toEditText = findViewById(R.id.toEditText)
        val nextButton = findViewById<Button>(R.id.nextButton)

        backButton.setOnClickListener { finish() }
        closeButton.setOnClickListener { finish() }

        fromEditText.setText("Current Location", false)

        setupAutocomplete(fromEditText)
        setupAutocomplete(toEditText)

        loadCurrentLocation()

        nextButton.setOnClickListener {
            val from = fromEditText.text.toString().trim()
            val to = toEditText.text.toString().trim()

            if (from.isEmpty() || to.isEmpty()) {
                Toast.makeText(this, "Please enter both locations", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, VehicleSelectionActivity::class.java)
                intent.putExtra("from_location", from)
                intent.putExtra("to_location", to)
                startActivity(intent)
            }
        }
    }

    private fun setupAutocomplete(field: AutoCompleteTextView) {
        field.threshold = 1

        field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                val query = text?.toString()?.trim().orEmpty()
                if (query.length >= 2) {
                    fetchSuggestions(field, query)
                }
            }
        })

        field.setOnItemClickListener { _, _, _, _ ->
            field.dismissDropDown()
        }
    }

    private fun loadCurrentLocation() {
        if (hasLocationPermission()) {
            requestFreshLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            locationPermissionRequestCode
        )
    }

    private fun requestFreshLocation() {
        try {
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }

            if (provider == null) {
                fromEditText.setText("Current Location", false)
                return
            }

            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val fallbackPlace = findNearestKnownPlace(location.latitude, location.longitude)
                    fromEditText.setText(fallbackPlace, false)
                    reverseGeocodeWithPhoton(location.latitude, location.longitude, fallbackPlace)

                    try {
                        locationManager.removeUpdates(this)
                    } catch (_: SecurityException) {
                    }
                }
            }

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                fromEditText.setText("Current Location", false)
                return
            }

            locationManager.requestLocationUpdates(provider, 0L, 0f, locationListener!!)
        } catch (_: Exception) {
            fromEditText.setText("Current Location", false)
        }
    }

    private fun reverseGeocodeWithPhoton(lat: Double, lon: Double, fallbackPlace: String) {
        thread {
            try {
                val urlString = "https://photon.komoot.io/reverse?lat=$lat&lon=$lon"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }

                val json = JSONObject(response)
                val features = json.getJSONArray("features")

                val locationName = if (features.length() > 0) {
                    val properties = features.getJSONObject(0).getJSONObject("properties")
                    when {
                        properties.has("suburb") -> properties.getString("suburb")
                        properties.has("district") -> properties.getString("district")
                        properties.has("city_district") -> properties.getString("city_district")
                        properties.has("locality") -> properties.getString("locality")
                        properties.has("name") -> properties.getString("name")
                        properties.has("street") -> properties.getString("street")
                        properties.has("city") -> properties.getString("city")
                        else -> fallbackPlace
                    }
                } else {
                    fallbackPlace
                }

                runOnUiThread {
                    fromEditText.setText(locationName, false)
                }
            } catch (_: Exception) {
                runOnUiThread {
                    fromEditText.setText(fallbackPlace, false)
                }
            }
        }
    }

    private fun findNearestKnownPlace(lat: Double, lon: Double): String {
        var nearestPlace = "Current Location"
        var smallestDistance = Double.MAX_VALUE

        for (place in knownPlaces) {
            val distance = distanceSquared(lat, lon, place.lat, place.lon)
            if (distance < smallestDistance) {
                smallestDistance = distance
                nearestPlace = place.name
            }
        }

        return nearestPlace
    }

    private fun distanceSquared(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return (lat1 - lat2).pow(2) + (lon1 - lon2).pow(2)
    }

    private fun fetchSuggestions(targetField: AutoCompleteTextView, query: String) {
        thread {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val urlString = "https://photon.komoot.io/api/?q=$encodedQuery&limit=6"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }

                val json = JSONObject(response)
                val features = json.getJSONArray("features")
                val suggestions = mutableListOf<String>()

                for (i in 0 until features.length()) {
                    val properties = features.getJSONObject(i).getJSONObject("properties")
                    val parts = mutableListOf<String>()

                    if (properties.has("name")) parts.add(properties.getString("name"))
                    if (properties.has("street")) parts.add(properties.getString("street"))
                    if (properties.has("district")) parts.add(properties.getString("district"))
                    if (properties.has("city")) parts.add(properties.getString("city"))

                    val suggestion = parts.distinct().joinToString(", ")
                    if (suggestion.isNotBlank()) {
                        suggestions.add(suggestion)
                    }
                }

                runOnUiThread {
                    val adapter = ArrayAdapter(
                        this@EnterRouteActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        suggestions.distinct()
                    )
                    targetField.setAdapter(adapter)
                    if (suggestions.isNotEmpty()) {
                        targetField.showDropDown()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationListener?.let { locationManager.removeUpdates(it) }
        } catch (_: Exception) {
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                requestFreshLocation()
            } else {
                Toast.makeText(
                    this,
                    "Location permission denied. Using default current location.",
                    Toast.LENGTH_SHORT
                ).show()
                fromEditText.setText("Current Location", false)
            }
        }
    }
}