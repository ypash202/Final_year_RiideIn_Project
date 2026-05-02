package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.riidein.app.R
import com.riidein.app.utils.ProfileImageHelper

class DriverRideRequestActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var requestCard: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var riderName: TextView
    private lateinit var pickupText: TextView
    private lateinit var dropoffText: TextView
    private lateinit var fareText: TextView
    private lateinit var acceptButton: Button
    private lateinit var declineButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var profileImage: ImageView

    private var currentRequestId: String = ""
    private var currentCustomerId: String = ""
    private var currentCustomerName: String = ""
    private var currentCustomerProfileImageUri: String = ""
    private var currentCustomerProfileImageBase64: String = ""
    private var currentPickup: String = ""
    private var currentDrop: String = ""
    private var currentFare: String = ""
    private var currentVehicleType: String = ""

    private var requestListListener: ListenerRegistration? = null
    private var currentRequestListener: ListenerRegistration? = null

    private var customerCancelHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_ride_request)

        initViews()
        hideRequest()
        setupClicks()
        listenForRideRequest()
    }

    private fun initViews() {
        requestCard = findViewById(R.id.requestCard)
        statusText = findViewById(R.id.statusText)
        riderName = findViewById(R.id.riderName)
        pickupText = findViewById(R.id.pickupText)
        dropoffText = findViewById(R.id.dropoffText)
        fareText = findViewById(R.id.fareText)
        acceptButton = findViewById(R.id.acceptButton)
        declineButton = findViewById(R.id.declineButton)
        backButton = findViewById(R.id.backButton)
        closeButton = findViewById(R.id.closeButton)
        profileImage = findViewById(R.id.profileImage)
    }

    private fun hideRequest() {
        statusText.text = "Waiting for request..."
        requestCard.visibility = View.GONE
        profileImage.setImageResource(R.drawable.profile1)
    }

    private fun setupClicks() {
        backButton.setOnClickListener {
            finish()
        }

        closeButton.setOnClickListener {
            goOfflineAndClose()
        }

        acceptButton.setOnClickListener {
            acceptRide()
        }

        declineButton.setOnClickListener {
            declineRide()
        }
    }

    private fun listenForRideRequest() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        requestListListener?.remove()

        requestListListener = db.collection("ride_requests")
            .whereEqualTo("driverId", currentUser.uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load request", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (value == null || value.isEmpty) {
                    if (currentRequestId.isBlank()) {
                        hideRequest()
                    }
                    return@addSnapshotListener
                }

                val doc = value.documents[0]

                currentRequestId = doc.id
                currentCustomerId = doc.getString("customerId") ?: ""
                currentCustomerName = doc.getString("customerName") ?: "Customer"
                currentCustomerProfileImageUri = doc.getString("customerProfileImageUri") ?: ""
                currentCustomerProfileImageBase64 = doc.getString("customerProfileImageBase64") ?: ""
                currentPickup = doc.getString("pickup") ?: "Pickup"
                currentDrop = doc.getString("drop") ?: "Drop"
                currentFare = doc.getString("fare") ?: "Rs 0"
                currentVehicleType = doc.getString("vehicleType") ?: "Moto"

                riderName.text = currentCustomerName
                pickupText.text = currentPickup
                dropoffText.text = currentDrop
                fareText.text = currentFare

                loadCustomerImage()

                statusText.text = "You’re online"
                requestCard.visibility = View.VISIBLE

                listenToCurrentRequestDocument(currentRequestId)
            }
    }

    private fun listenToCurrentRequestDocument(requestId: String) {
        if (requestId.isBlank()) return

        customerCancelHandled = false
        currentRequestListener?.remove()

        currentRequestListener = db.collection("ride_requests")
            .document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    return@addSnapshotListener
                }

                val status = snapshot.getString("status") ?: ""

                if (status == "cancelled_by_customer" && !customerCancelHandled) {
                    customerCancelHandled = true

                    Toast.makeText(
                        this,
                        "Customer cancelled the request",
                        Toast.LENGTH_LONG
                    ).show()

                    db.collection("ride_requests")
                        .document(requestId)
                        .update("driverNotified", true)

                    goBackToDriverHomeAfterCustomerCancel()
                }
            }
    }

    private fun loadCustomerImage() {
        if (currentCustomerProfileImageBase64.isNotBlank()) {
            ProfileImageHelper.loadBase64IntoImageView(
                imageView = profileImage,
                base64Image = currentCustomerProfileImageBase64,
                fallbackRes = R.drawable.profile1
            )
        } else {
            ProfileImageHelper.loadUriIntoImageView(
                imageView = profileImage,
                uriString = currentCustomerProfileImageUri,
                fallbackRes = R.drawable.profile1
            )
        }
    }

    private fun acceptRide() {
        if (currentRequestId.isBlank()) {
            Toast.makeText(this, "No ride request selected", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("ride_requests")
            .document(currentRequestId)
            .update("status", "accepted")
            .addOnSuccessListener {
                Toast.makeText(this, "Ride accepted", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, DriverArrivedNavigateActivity::class.java)
                intent.putExtra("request_id", currentRequestId)
                intent.putExtra("customer_id", currentCustomerId)
                intent.putExtra("customer_name", currentCustomerName)
                intent.putExtra("customer_profile_image_uri", currentCustomerProfileImageUri)
                intent.putExtra("customer_profile_image_base64", currentCustomerProfileImageBase64)
                intent.putExtra("pickup", currentPickup)
                intent.putExtra("drop", currentDrop)
                intent.putExtra("fare", currentFare)
                intent.putExtra("vehicle_type", currentVehicleType)
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to accept ride", Toast.LENGTH_SHORT).show()
            }
    }

    private fun declineRide() {
        if (currentRequestId.isBlank()) {
            Toast.makeText(this, "No ride request selected", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("ride_requests")
            .document(currentRequestId)
            .update(
                mapOf(
                    "status" to "declined",
                    "declinedBy" to "driver",
                    "declinedAt" to System.currentTimeMillis(),
                    "hiddenFromCustomer" to false,
                    "hiddenFromDriver" to false
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Ride declined", Toast.LENGTH_SHORT).show()
                clearCurrentRide()
                hideRequest()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to decline ride", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearCurrentRide() {
        currentRequestId = ""
        currentCustomerId = ""
        currentCustomerName = ""
        currentCustomerProfileImageUri = ""
        currentCustomerProfileImageBase64 = ""
        currentPickup = ""
        currentDrop = ""
        currentFare = ""
        currentVehicleType = ""
        customerCancelHandled = false
    }

    private fun goBackToDriverHomeAfterCustomerCancel() {
        requestListListener?.remove()
        requestListListener = null

        currentRequestListener?.remove()
        currentRequestListener = null

        val intent = Intent(this, DriverHomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("show_online_page", true)
        intent.putExtra("customer_cancelled_ride", true)
        intent.putExtra("driver_status", "online")
        startActivity(intent)
        finish()
    }

    private fun goOfflineAndClose() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            finish()
            return
        }

        db.collection("users")
            .document(currentUser.uid)
            .update("isAvailable", false)
            .addOnCompleteListener {
                finish()
            }
    }

    override fun onDestroy() {
        super.onDestroy()

        requestListListener?.remove()
        requestListListener = null

        currentRequestListener?.remove()
        currentRequestListener = null
    }
}