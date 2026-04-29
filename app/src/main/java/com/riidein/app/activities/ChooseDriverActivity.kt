package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R

class ChooseDriverActivity : AppCompatActivity() {

    private lateinit var fromMapText: TextView
    private lateinit var toMapText: TextView

    private lateinit var driverCard1: LinearLayout
    private lateinit var driverCard2: LinearLayout

    private lateinit var driver1NameText: TextView
    private lateinit var driver2NameText: TextView
    private lateinit var driver1VehicleText: TextView
    private lateinit var driver2VehicleText: TextView
    private lateinit var driver1PriceText: TextView
    private lateinit var driver2PriceText: TextView
    private lateinit var driver1TimeText: TextView
    private lateinit var driver2TimeText: TextView

    private lateinit var acceptButton1: Button
    private lateinit var acceptButton2: Button
    private lateinit var declineButton1: Button
    private lateinit var declineButton2: Button
    private lateinit var cancelRequestButton: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var fromLocation: String = "Current Location"
    private var toLocation: String = "Destination"
    private var selectedVehicle: String = "Moto"
    private var selectedPrice: String = "Rs 0"

    private val driverIds = mutableListOf<String>()
    private val driverNames = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_driver)

        readIntentData()
        initViews()
        bindMapTexts()
        hideDriverCards()
        loadAvailableDrivers()
        setupButtons()
    }

    private fun readIntentData() {
        fromLocation = intent.getStringExtra("from_location") ?: "Current Location"
        toLocation = intent.getStringExtra("to_location") ?: "Destination"
        selectedVehicle = intent.getStringExtra("selected_vehicle") ?: "Moto"
        selectedPrice = intent.getStringExtra("selected_price") ?: "Rs 0"
    }

    private fun initViews() {
        fromMapText = findViewById(R.id.fromMapText)
        toMapText = findViewById(R.id.toMapText)

        driverCard1 = findViewById(R.id.driverCard1)
        driverCard2 = findViewById(R.id.driverCard2)

        driver1NameText = findViewById(R.id.driver1NameText)
        driver2NameText = findViewById(R.id.driver2NameText)
        driver1VehicleText = findViewById(R.id.driver1VehicleText)
        driver2VehicleText = findViewById(R.id.driver2VehicleText)
        driver1PriceText = findViewById(R.id.driver1PriceText)
        driver2PriceText = findViewById(R.id.driver2PriceText)
        driver1TimeText = findViewById(R.id.driver1TimeText)
        driver2TimeText = findViewById(R.id.driver2TimeText)

        acceptButton1 = findViewById(R.id.acceptButton1)
        acceptButton2 = findViewById(R.id.acceptButton2)
        declineButton1 = findViewById(R.id.declineButton1)
        declineButton2 = findViewById(R.id.declineButton2)
        cancelRequestButton = findViewById(R.id.cancelRequestButton)
    }

    private fun bindMapTexts() {
        fromMapText.text = shortenPlaceName(fromLocation, 18)
        toMapText.text = shortenPlaceName(toLocation, 18)
    }

    private fun hideDriverCards() {
        driverCard1.visibility = View.GONE
        driverCard2.visibility = View.GONE
    }

    private fun loadAvailableDrivers() {
        val vehicleType = when (selectedVehicle.trim().lowercase()) {
            "moto" -> "bike"
            "cab" -> "cab"
            else -> "delivery"
        }

        db.collection("users")
            .whereEqualTo("role", "driver")
            .whereEqualTo("vehicleType", vehicleType)
            .whereEqualTo("isAvailable", true)
            .whereEqualTo("verificationStatus", "approved")
            .limit(2)
            .get()
            .addOnSuccessListener { result ->
                driverIds.clear()
                driverNames.clear()

                if (result.isEmpty) {
                    Toast.makeText(this, "No available $selectedVehicle driver found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val drivers = result.documents

                if (drivers.isNotEmpty()) {
                    val driver = drivers[0]
                    val name = driver.getString("name") ?: "Driver"

                    driverIds.add(driver.id)
                    driverNames.add(name)

                    driver1NameText.text = name
                    driver1VehicleText.text = buildVehicleText(selectedVehicle)
                    driver1PriceText.text = selectedPrice
                    driver1TimeText.text = "1 min"
                    driverCard1.visibility = View.VISIBLE
                }

                if (drivers.size > 1) {
                    val driver = drivers[1]
                    val name = driver.getString("name") ?: "Driver"

                    driverIds.add(driver.id)
                    driverNames.add(name)

                    driver2NameText.text = name
                    driver2VehicleText.text = buildVehicleText(selectedVehicle)
                    driver2PriceText.text = selectedPrice
                    driver2TimeText.text = "2 min"
                    driverCard2.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load drivers", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupButtons() {
        cancelRequestButton.setOnClickListener {
            val intent = Intent(this, CancelRideActivity::class.java)
            intent.putExtra("from_location", fromLocation)
            intent.putExtra("to_location", toLocation)
            startActivity(intent)
        }

        declineButton1.setOnClickListener {
            driverCard1.visibility = View.GONE
        }

        declineButton2.setOnClickListener {
            driverCard2.visibility = View.GONE
        }

        acceptButton1.setOnClickListener {
            if (driverIds.isNotEmpty()) {
                sendRideRequest(driverIds[0], driverNames[0])
            }
        }

        acceptButton2.setOnClickListener {
            if (driverIds.size > 1) {
                sendRideRequest(driverIds[1], driverNames[1])
            }
        }
    }

    private fun sendRideRequest(driverId: String, driverName: String) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { userDoc ->
                val customerName = userDoc.getString("name") ?: "Customer"

                val request = hashMapOf(
                    "customerId" to currentUser.uid,
                    "customerName" to customerName,
                    "driverId" to driverId,
                    "driverName" to driverName,
                    "vehicleType" to selectedVehicle,
                    "pickup" to fromLocation,
                    "drop" to toLocation,
                    "fare" to selectedPrice,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("ride_requests")
                    .add(request)
                    .addOnSuccessListener { document ->
                        Toast.makeText(this, "Ride request sent to $driverName", Toast.LENGTH_SHORT).show()
                        listenForDriverAccept(document.id, driverName)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun listenForDriverAccept(requestId: String, driverName: String) {
        db.collection("ride_requests")
            .document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val status = snapshot.getString("status") ?: ""

                if (status == "accepted") {
                    val intent = Intent(this, RideTrackingActivity::class.java)
                    intent.putExtra("request_id", requestId)
                    intent.putExtra("driver_name", driverName)
                    intent.putExtra("vehicle_name", selectedVehicle)
                    intent.putExtra("selected_price", selectedPrice)
                    intent.putExtra("from_location", fromLocation)
                    intent.putExtra("to_location", toLocation)
                    startActivity(intent)
                    finish()
                }

                if (status == "declined") {
                    Toast.makeText(this, "$driverName declined your request", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun buildVehicleText(vehicleType: String): String {
        return when (vehicleType.trim().lowercase()) {
            "moto" -> "MOTOR-BIKE"
            "cab" -> "CAB"
            "delivery" -> "DELIVERY"
            else -> vehicleType
        }
    }

    private fun shortenPlaceName(place: String, maxLength: Int): String {
        val cleanText = place.trim()
        return if (cleanText.length <= maxLength) cleanText else cleanText.take(maxLength - 3) + "..."
    }
}