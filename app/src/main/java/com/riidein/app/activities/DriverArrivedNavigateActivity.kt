package com.riidein.app.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R

class DriverArrivedNavigateActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private var requestId = ""
    private var customerName = ""
    private var pickupLocation = ""
    private var dropLocation = ""
    private var fare = ""
    private var vehicleType = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_arrived_navigate)

        readIntentData()
        bindData()
        setupClicks()
    }

    private fun readIntentData() {
        requestId = intent.getStringExtra("request_id") ?: ""
        customerName = intent.getStringExtra("customer_name") ?: "Customer"
        pickupLocation = intent.getStringExtra("pickup") ?: "Pickup"
        dropLocation = intent.getStringExtra("drop") ?: "Drop"
        fare = intent.getStringExtra("fare") ?: "Rs 0"
        vehicleType = intent.getStringExtra("vehicle_type") ?: "Moto"
    }

    private fun bindData() {
        findViewById<TextView>(R.id.riderName).text = customerName
        findViewById<TextView>(R.id.pickupText).text = pickupLocation
        findViewById<TextView>(R.id.dropText).text = dropLocation
        findViewById<TextView>(R.id.fareText).text = fare

        val vehicleInfoTop = findViewById<TextView>(R.id.vehicleInfoTop)
        val bikeImage = findViewById<ImageView>(R.id.bikeImage)

        when (vehicleType.trim().lowercase()) {
            "cab" -> {
                vehicleInfoTop.text = "CAB"
                bikeImage.setImageResource(R.drawable.car)
            }
            "delivery" -> {
                vehicleInfoTop.text = "DELIVERY"
                bikeImage.setImageResource(R.drawable.delivery)
            }
            else -> {
                vehicleInfoTop.text = "MOTOR-BIKE"
                bikeImage.setImageResource(R.drawable.bike)
            }
        }
    }

    private fun setupClicks() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.closeButton).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.navigateButton).setOnClickListener {
            openNavigationToCustomer()
        }

        findViewById<Button>(R.id.arrivedButton).setOnClickListener {
            markDriverArrived()
        }
    }

    private fun openNavigationToCustomer() {
        if (pickupLocation.isBlank()) {
            Toast.makeText(this, "Pickup location not found", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = Uri.parse("google.navigation:q=${Uri.encode(pickupLocation)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            val browserUri = Uri.parse(
                "https://www.google.com/maps/search/?api=1&query=${Uri.encode(pickupLocation)}"
            )
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }

    private fun markDriverArrived() {
        if (requestId.isBlank()) {
            Toast.makeText(this, "Ride request not found", Toast.LENGTH_SHORT).show()

            return
        }

        db.collection("ride_requests")
            .document(requestId)
            .update("status", "arrived")
            .addOnSuccessListener {
                Toast.makeText(this, "Customer has been notified", Toast.LENGTH_SHORT).show()
                val arrivedButton = findViewById<Button>(R.id.arrivedButton)
                arrivedButton.isEnabled = false
                arrivedButton.alpha = 0.5f
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update arrival status", Toast.LENGTH_SHORT).show()
            }
    }
}