package com.riidein.app.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RideTrackingActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var cancelRideButton: Button
    private lateinit var nextArrow: ImageView
    private lateinit var contactDriverRow: LinearLayout
    private lateinit var sosButton: Button

    private lateinit var arrivalTitle: TextView
    private lateinit var driverNameText: TextView
    private lateinit var driverVehicleText: TextView
    private lateinit var paymentAmountText: TextView
    private lateinit var paymentMethodText: TextView
    private lateinit var pickupLocationText: TextView
    private lateinit var dropLocationText: TextView
    private lateinit var paymentLabel: TextView

    private lateinit var driverProfileImage: ImageView
    private lateinit var vehicleImage: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var locationManager: LocationManager

    private var driverName: String = "Binod"
    private var vehicleName: String = "Moto"
    private var vehicleNumber: String = "BA 01 PA 1234"
    private var selectedPrice: String = "Rs 200"
    private var fromLocation: String = "Boudha"
    private var toLocation: String = "Trade Tower, Thapathali"

    private var requestId: String = ""
    private var driverHasArrived = false
    private enum class RideState {
        ARRIVING,
        ARRIVED,
        COMPLETED
    }

    private var currentRideState = RideState.ARRIVING
    private var currentLocationListener: LocationListener? = null

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                fetchCurrentLocationAndTriggerSos()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.location_permission_denied_sos),
                    Toast.LENGTH_SHORT
                ).show()
                saveSosAlertToFirestore(null, null)
                openPoliceDialer()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_tracking)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        initViews()
        readIntentData()
        bindRideData()
        updateRideStateUI()
        setupClicks()
        disableNextUntilDriverArrives()
        listenForDriverArrival()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        closeButton = findViewById(R.id.closeButton)
        cancelRideButton = findViewById(R.id.cancelRideButton)
        nextArrow = findViewById(R.id.nextArrow)
        contactDriverRow = findViewById(R.id.contactDriverRow)
        sosButton = findViewById(R.id.sosButton)

        arrivalTitle = findViewById(R.id.arrivalTitle)
        driverNameText = findViewById(R.id.driverNameText)
        driverVehicleText = findViewById(R.id.driverVehicleText)
        paymentAmountText = findViewById(R.id.paymentAmountText)
        paymentMethodText = findViewById(R.id.paymentMethodText)
        pickupLocationText = findViewById(R.id.pickupLocationText)
        dropLocationText = findViewById(R.id.dropLocationText)
        paymentLabel = findViewById(R.id.paymentLabel)

        driverProfileImage = findViewById(R.id.driverProfileImage)
        vehicleImage = findViewById(R.id.vehicleImage)
    }

    private fun readIntentData() {

        requestId = intent.getStringExtra("request_id") ?: ""
        driverName = intent.getStringExtra("driver_name") ?: "Binod"
        vehicleName = intent.getStringExtra("vehicle_name") ?: "Moto"
        vehicleNumber = intent.getStringExtra("vehicle_number") ?: when (driverName.lowercase()) {
            "sulav" -> "BA 02 PA 5678"
            else -> "BA 01 PA 1234"
        }
        selectedPrice = intent.getStringExtra("selected_price") ?: "Rs 200"
        fromLocation = intent.getStringExtra("from_location") ?: "Boudha"
        toLocation = intent.getStringExtra("to_location") ?: "Trade Tower, Thapathali"
    }

    private fun bindRideData() {
        driverNameText.text = driverName

        driverVehicleText.text = when (vehicleName.lowercase()) {
            "moto" -> getString(R.string.driver_vehicle_honda)
            "cab" -> getString(R.string.vehicle_type_cab)
            "delivery" -> getString(R.string.vehicle_type_delivery)
            else -> vehicleName
        }

        paymentAmountText.text = selectedPrice
        paymentMethodText.text = getString(R.string.cash)
        pickupLocationText.text = fromLocation
        dropLocationText.text = toLocation

        if (driverName.equals("Sulav", ignoreCase = true)) {
            driverProfileImage.setImageResource(R.drawable.profile2)
        } else {
            driverProfileImage.setImageResource(R.drawable.profile1)
        }

        when (vehicleName.lowercase()) {
            "moto" -> vehicleImage.setImageResource(R.drawable.bike)
            "cab" -> vehicleImage.setImageResource(R.drawable.car)
            "delivery" -> vehicleImage.setImageResource(R.drawable.delivery)
            else -> vehicleImage.setImageResource(R.drawable.bike)
        }
    }

    private fun setupClicks() {
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        closeButton.setOnClickListener {
            finish()
        }

        contactDriverRow.setOnClickListener {
            val intent = Intent(this, MessagesActivity::class.java)
            intent.putExtra("driver_name", driverName)
            startActivity(intent)
        }

        cancelRideButton.setOnClickListener {
            when (currentRideState) {
                RideState.ARRIVING -> openCancelRidePage()

                RideState.ARRIVED -> {
                    currentRideState = RideState.COMPLETED
                    updateRideStateUI()
                    Toast.makeText(
                        this,
                        getString(R.string.ride_completed_demo),
                        Toast.LENGTH_SHORT
                    ).show()
                    saveCompletedRideToFirestore()
                }

                RideState.COMPLETED -> {
                    goToCustomerHome()
                }
            }
        }

        nextArrow.setOnClickListener {
            when (currentRideState) {
                RideState.ARRIVING -> {
                    currentRideState = RideState.ARRIVED
                    updateRideStateUI()
                }

                RideState.ARRIVED -> {
                    currentRideState = RideState.COMPLETED
                    updateRideStateUI()
                    saveCompletedRideToFirestore()
                }

                RideState.COMPLETED -> {
                    goToCustomerHome()
                }
            }
        }

        sosButton.setOnClickListener {
            showSosDialog()
        }
    }

    private fun updateRideStateUI() {
        when (currentRideState) {
            RideState.ARRIVING -> {
                arrivalTitle.text = getString(R.string.ride_state_arriving)
                cancelRideButton.text = getString(R.string.cancel_ride)
                contactDriverRow.visibility = LinearLayout.VISIBLE
                nextArrow.visibility = ImageView.VISIBLE
                sosButton.visibility = Button.VISIBLE
                paymentLabel.visibility = TextView.VISIBLE
                paymentAmountText.visibility = TextView.VISIBLE
                paymentMethodText.visibility = TextView.VISIBLE
            }

            RideState.ARRIVED -> {
                arrivalTitle.text = getString(R.string.ride_state_arrived)
                cancelRideButton.text = getString(R.string.start_ride)
                contactDriverRow.visibility = LinearLayout.VISIBLE
                nextArrow.visibility = ImageView.VISIBLE
                sosButton.visibility = Button.VISIBLE
                paymentLabel.visibility = TextView.VISIBLE
                paymentAmountText.visibility = TextView.VISIBLE
                paymentMethodText.visibility = TextView.VISIBLE
            }

            RideState.COMPLETED -> {
                arrivalTitle.text = getString(R.string.ride_state_completed)
                cancelRideButton.text = getString(R.string.back_to_home)
                contactDriverRow.visibility = LinearLayout.GONE
                nextArrow.visibility = ImageView.GONE
                sosButton.visibility = Button.GONE
                paymentLabel.visibility = TextView.VISIBLE
                paymentAmountText.visibility = TextView.VISIBLE
                paymentMethodText.visibility = TextView.VISIBLE
            }
        }
    }

    private fun saveCompletedRideToFirestore() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.user_not_logged_in), Toast.LENGTH_SHORT).show()
            goToCustomerHome()
            return
        }

        val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            .format(Date(System.currentTimeMillis()))

        val formattedFare = if (selectedPrice.contains("Rs", ignoreCase = true)) {
            val amount = selectedPrice.replace("Rs", "", ignoreCase = true).trim()
            "Rs $amount"
        } else {
            selectedPrice
        }

        val completedRide = hashMapOf(
            "customerId" to currentUser.uid,
            "driverName" to driverName,
            "vehicleName" to vehicleName,
            "vehicleNumber" to vehicleNumber,
            "rideTime" to getString(R.string.wallet_default_ride_time),
            "rideDistance" to getString(R.string.wallet_default_ride_distance),
            "rideFare" to formattedFare,
            "pickupLocation" to fromLocation,
            "dropLocation" to toLocation,
            "rideDate" to getString(R.string.wallet_date_prefix, formattedDate),
            "paymentMethod" to getString(R.string.cash),
            "status" to "completed",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("completed_rides")
            .add(completedRide)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.ride_saved_to_wallet), Toast.LENGTH_SHORT)
                    .show()
                goToCustomerHome()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.failed_to_save_ride), Toast.LENGTH_SHORT)
                    .show()
                goToCustomerHome()
            }
    }

    private fun goToCustomerHome() {
        val intent = Intent(this, CustomerHomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun openCancelRidePage() {
        val intent = Intent(this, CancelRideActivity::class.java)
        intent.putExtra("driver_name", driverName)
        intent.putExtra("vehicle_name", driverVehicleText.text.toString())
        intent.putExtra("vehicle_number", vehicleNumber)
        intent.putExtra("from_location", fromLocation)
        intent.putExtra("to_location", toLocation)
        startActivity(intent)
    }

    private fun showSosDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.sos_title))
            .setMessage(getString(R.string.sos_message_with_location))
            .setPositiveButton(getString(R.string.call_100)) { _, _ ->
                triggerSosFlow()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun triggerSosFlow() {
        if (hasLocationPermission()) {
            fetchCurrentLocationAndTriggerSos()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocationAndTriggerSos() {
        if (!hasLocationPermission()) {
            saveSosAlertToFirestore(null, null)
            openPoliceDialer()
            return
        }

        try {
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }

            if (provider == null) {
                Toast.makeText(
                    this,
                    getString(R.string.no_location_provider),
                    Toast.LENGTH_SHORT
                ).show()
                saveSosAlertToFirestore(null, null)
                openPoliceDialer()
                return
            }

            currentLocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    try {
                        if (hasLocationPermission()) {
                            locationManager.removeUpdates(this)
                        }
                    } catch (_: SecurityException) {
                    }

                    saveSosAlertToFirestore(location.latitude, location.longitude)
                    openPoliceDialer()
                }
            }

            val lastKnown = getLastKnownSafeLocation()
            if (lastKnown != null && lastKnown.latitude != 0.0 && lastKnown.longitude != 0.0) {
                saveSosAlertToFirestore(lastKnown.latitude, lastKnown.longitude)
                openPoliceDialer()
            } else {
                locationManager.requestLocationUpdates(
                    provider,
                    0L,
                    0f,
                    currentLocationListener!!
                )

                Toast.makeText(
                    this,
                    getString(R.string.fetching_location_sos),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (_: SecurityException) {
            saveSosAlertToFirestore(null, null)
            openPoliceDialer()
        } catch (_: Exception) {
            saveSosAlertToFirestore(null, null)
            openPoliceDialer()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownSafeLocation(): Location? {
        if (!hasLocationPermission()) return null

        return try {
            val gpsLocation =
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                } else {
                    null
                }

            val networkLocation =
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } else {
                    null
                }

            when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time >= networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                else -> networkLocation
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun saveSosAlertToFirestore(latitude: Double?, longitude: Double?) {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid ?: "unknown_user"

        val sosData = hashMapOf(
            "userId" to userId,
            "driverName" to driverName,
            "vehicleName" to driverVehicleText.text.toString(),
            "vehicleNumber" to vehicleNumber,
            "fromLocation" to fromLocation,
            "toLocation" to toLocation,
            "latitude" to latitude,
            "longitude" to longitude,
            "phoneNumber" to "100",
            "timestamp" to System.currentTimeMillis(),
            "status" to "triggered"
        )

        db.collection("sos_alerts")
            .add(sosData)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.sos_saved), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.sos_save_failed), Toast.LENGTH_SHORT).show()
            }
    }

    private fun openPoliceDialer() {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:100".toUri()
        }
        startActivity(dialIntent)
    }
    private fun disableNextUntilDriverArrives() {
        nextArrow.isEnabled = false
        nextArrow.alpha = 0.4f
    }

    private fun enableNextAfterDriverArrives() {
        nextArrow.isEnabled = true
        nextArrow.alpha = 1f
    }

    private fun listenForDriverArrival() {
        if (requestId.isEmpty()) return

        db.collection("ride_requests")
            .document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val status = snapshot.getString("status") ?: ""

                if (status == "arrived" && !driverHasArrived) {
                    driverHasArrived = true
                    currentRideState = RideState.ARRIVED
                    updateRideStateUI()
                    enableNextAfterDriverArrives()

                    Toast.makeText(
                        this,
                        "Your driver has arrived. You can start the ride now.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::locationManager.isInitialized && hasLocationPermission()) {
                currentLocationListener?.let { locationManager.removeUpdates(it) }
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }
}