package com.riidein.app.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class VehicleSelectionActivity : AppCompatActivity() {

    private lateinit var motoOption: LinearLayout
    private lateinit var cabOption: LinearLayout
    private lateinit var deliveryOption: LinearLayout

    private lateinit var motoPriceText: TextView
    private lateinit var cabPriceText: TextView
    private lateinit var deliveryPriceText: TextView

    private lateinit var motoTimeText: TextView
    private lateinit var cabTimeText: TextView
    private lateinit var deliveryTimeText: TextView

    private lateinit var motoSubText: TextView
    private lateinit var cabSubText: TextView
    private lateinit var deliverySubText: TextView

    private lateinit var fromMapText: TextView
    private lateinit var toMapText: TextView
    private lateinit var mapWebView: WebView

    private var selectedVehicle = "Moto"

    data class PlacePoint(
        val name: String,
        val lat: Double,
        val lon: Double
    )

    private val knownPlaces = listOf(
        PlacePoint("Boudha", 27.7215, 85.3616),
        PlacePoint("Bouddha", 27.7215, 85.3616),
        PlacePoint("Chabahil", 27.7215, 85.3469),
        PlacePoint("Kapan", 27.7339, 85.3570),
        PlacePoint("Jorpati", 27.7290, 85.3780),
        PlacePoint("Gaushala", 27.7098, 85.3436),
        PlacePoint("Sinamangal", 27.6956, 85.3524),
        PlacePoint("Tinkune", 27.6860, 85.3494),
        PlacePoint("Koteshwor", 27.6781, 85.3498),
        PlacePoint("Baneshwor", 27.6889, 85.3358),
        PlacePoint("New Baneshwor", 27.6915, 85.3420),
        PlacePoint("Old Baneshwor", 27.6900, 85.3340),
        PlacePoint("Maitidevi", 27.7067, 85.3345),
        PlacePoint("Maitighar", 27.6947, 85.3218),
        PlacePoint("Thapathali", 27.6935, 85.3208),
        PlacePoint("Putalisadak", 27.7080, 85.3227),
        PlacePoint("Dillibazar", 27.7078, 85.3279),
        PlacePoint("Ratnapark", 27.7061, 85.3156),
        PlacePoint("New Road", 27.7049, 85.3122),
        PlacePoint("Ason", 27.7076, 85.3100),
        PlacePoint("Basantapur", 27.7048, 85.3075),
        PlacePoint("Thamel", 27.7154, 85.3123),
        PlacePoint("Naxal", 27.7170, 85.3280),
        PlacePoint("Lazimpat", 27.7242, 85.3196),
        PlacePoint("Baluwatar", 27.7304, 85.3311),
        PlacePoint("Maharajgunj", 27.7392, 85.3328),
        PlacePoint("Samakhusi", 27.7305, 85.3128),
        PlacePoint("Gongabu", 27.7390, 85.3144),
        PlacePoint("Kalanki", 27.6932, 85.2815),
        PlacePoint("Balkhu", 27.6846, 85.2964),
        PlacePoint("Teku", 27.6941, 85.3005),
        PlacePoint("Kalimati", 27.6982, 85.3018),
        PlacePoint("Tripureshwor", 27.6930, 85.3145),
        PlacePoint("Lalitpur", 27.6710, 85.3250),
        PlacePoint("Patan", 27.6710, 85.3250),
        PlacePoint("Pulchowk", 27.6780, 85.3168),
        PlacePoint("Jawalakhel", 27.6749, 85.3139),
        PlacePoint("Kupondole", 27.6849, 85.3188),
        PlacePoint("Lagankhel", 27.6667, 85.3245),
        PlacePoint("Satdobato", 27.6574, 85.3240),
        PlacePoint("Gwarko", 27.6663, 85.3337),
        PlacePoint("Imadol", 27.6557, 85.3450),
        PlacePoint("Bhaisepati", 27.6588, 85.3006),
        PlacePoint("Bhaktapur", 27.6710, 85.4298),
        PlacePoint("Suryabinayak", 27.6587, 85.4285),
        PlacePoint("Thimi", 27.6806, 85.3875),
        PlacePoint("Madhyapur Thimi", 27.6806, 85.3875),
        PlacePoint("Kirtipur", 27.6676, 85.2770),
        PlacePoint("Kritipur", 27.6676, 85.2770),
        PlacePoint("Nagarkot", 27.7172, 85.5200),
        PlacePoint("Airport", 27.6966, 85.3591),
        PlacePoint("Tribhuvan Airport", 27.6966, 85.3591),
        PlacePoint("Pashupatinath", 27.7104, 85.3487),
        PlacePoint("Swayambhu", 27.7149, 85.2900)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_selection)

        val fromLocation = intent.getStringExtra("from_location") ?: ""
        val toLocation = intent.getStringExtra("to_location") ?: ""

        val fromLocationText = findViewById<TextView>(R.id.fromLocationText)
        val toLocationText = findViewById<TextView>(R.id.toLocationText)
        val customerNameText = findViewById<TextView>(R.id.customerNameText)
        val findDriverButton = findViewById<Button>(R.id.findDriverButton)

        fromMapText = findViewById(R.id.fromMapText)
        toMapText = findViewById(R.id.toMapText)
        mapWebView = findViewById(R.id.mapWebView)

        motoOption = findViewById(R.id.motoOption)
        cabOption = findViewById(R.id.cabOption)
        deliveryOption = findViewById(R.id.deliveryOption)

        motoPriceText = findViewById(R.id.motoPriceText)
        cabPriceText = findViewById(R.id.cabPriceText)
        deliveryPriceText = findViewById(R.id.deliveryPriceText)

        motoTimeText = findViewById(R.id.motoTimeText)
        cabTimeText = findViewById(R.id.cabTimeText)
        deliveryTimeText = findViewById(R.id.deliveryTimeText)

        motoSubText = findViewById(R.id.motoSubText)
        cabSubText = findViewById(R.id.cabSubText)
        deliverySubText = findViewById(R.id.deliverySubText)

        fromLocationText.text = fromLocation
        toLocationText.text = toLocation

        fromMapText.text = shortenPlaceName(fromLocation, 18)
        toMapText.text = shortenPlaceName(toLocation, 18)

        setupMapWebView()
        loadMapRoute(fromLocation, toLocation)

        loadCustomerName(customerNameText)
        applyDistanceBasedFare(fromLocation, toLocation)
        highlightSelectedVehicle("Moto")

        motoOption.setOnClickListener {
            selectedVehicle = "Moto"
            highlightSelectedVehicle(selectedVehicle)
        }

        cabOption.setOnClickListener {
            selectedVehicle = "Cab"
            highlightSelectedVehicle(selectedVehicle)
        }

        deliveryOption.setOnClickListener {
            selectedVehicle = "Delivery"
            highlightSelectedVehicle(selectedVehicle)
        }

        findDriverButton.setOnClickListener {
            val intent = Intent(this, ChooseDriverActivity::class.java)
            intent.putExtra("from_location", fromLocation)
            intent.putExtra("to_location", toLocation)
            intent.putExtra("selected_vehicle", selectedVehicle)
            intent.putExtra("selected_price", getSelectedPrice())
            startActivity(intent)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupMapWebView() {
        mapWebView.webViewClient = WebViewClient()
        mapWebView.webChromeClient = WebChromeClient()

        val webSettings = mapWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.loadsImagesAutomatically = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true

        mapWebView.loadUrl("file:///android_asset/map.html")
    }

    private fun loadMapRoute(fromLocation: String, toLocation: String) {
        val fromPoint = findPlacePoint(fromLocation)
        val toPoint = findPlacePoint(toLocation)

        if (fromPoint != null && toPoint != null) {
            mapWebView.postDelayed({
                val js = "javascript:setRoute(${fromPoint.lat}, ${fromPoint.lon}, ${toPoint.lat}, ${toPoint.lon})"
                mapWebView.evaluateJavascript(js, null)
            }, 1200)
        }
    }

    private fun loadCustomerName(customerNameText: TextView) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val customerName = document.getString("name") ?: ""
                        customerNameText.text = customerName
                    }
                }
        }
    }

    private fun applyDistanceBasedFare(from: String, to: String) {
        val fromPoint = findPlacePoint(from)
        val toPoint = findPlacePoint(to)

        val distanceKm = if (fromPoint != null && toPoint != null) {
            haversineKm(fromPoint.lat, fromPoint.lon, toPoint.lat, toPoint.lon)
        } else {
            6.0
        }

        val motoFare = calculateFare(distanceKm, 60, 20)
        val cabFare = calculateFare(distanceKm, 140, 36)
        val deliveryFare = calculateFare(distanceKm, 80, 24)

        motoPriceText.text = "Rs $motoFare"
        cabPriceText.text = "Rs $cabFare"
        deliveryPriceText.text = "Rs $deliveryFare"

        motoTimeText.text = estimateTime(distanceKm, 20.0)
        cabTimeText.text = estimateTime(distanceKm, 24.0)
        deliveryTimeText.text = estimateTime(distanceKm, 18.0)

        motoSubText.text = getString(R.string.near_by_you)
        cabSubText.text = getString(R.string.near_by_you)
        deliverySubText.text = getString(R.string.parcel_nearby)
    }

    private fun calculateFare(distanceKm: Double, baseFare: Int, perKmRate: Int): Int {
        val rawFare = baseFare + (distanceKm * perKmRate)
        val roundedFare = ((rawFare + 9) / 10).toInt() * 10
        return maxOf(baseFare, roundedFare)
    }

    private fun estimateTime(distanceKm: Double, avgSpeedKmPerHour: Double): String {
        val minutes = ((distanceKm / avgSpeedKmPerHour) * 60).toInt().coerceAtLeast(2)
        return "$minutes min"
    }

    private fun findPlacePoint(locationText: String): PlacePoint? {
        val lowerText = locationText.trim().lowercase()

        for (place in knownPlaces) {
            if (lowerText.contains(place.name.lowercase())) {
                return place
            }
        }

        for (place in knownPlaces) {
            if (place.name.lowercase().contains(lowerText)) {
                return place
            }
        }

        return null
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    private fun shortenPlaceName(place: String, maxLength: Int): String {
        val cleanText = place.trim()
        return if (cleanText.length <= maxLength) cleanText else cleanText.take(maxLength - 3) + "..."
    }

    private fun highlightSelectedVehicle(selected: String) {
        val selectedBg = R.drawable.route_top_card_bg
        val normalBg = R.drawable.vehicle_item_bg

        motoOption.setBackgroundResource(if (selected == "Moto") selectedBg else normalBg)
        cabOption.setBackgroundResource(if (selected == "Cab") selectedBg else normalBg)
        deliveryOption.setBackgroundResource(if (selected == "Delivery") selectedBg else normalBg)
    }

    private fun getSelectedPrice(): String {
        return when (selectedVehicle) {
            "Moto" -> motoPriceText.text.toString()
            "Cab" -> cabPriceText.text.toString()
            else -> deliveryPriceText.text.toString()
        }
    }
}