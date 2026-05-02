package com.riidein.app.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.riidein.app.R
import com.riidein.app.utils.ProfileImageHelper

class DriverArrivedNavigateActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private var requestId = ""
    private var customerId = ""
    private var customerName = ""
    private var customerProfileImageUri = ""
    private var customerProfileImageBase64 = ""

    private var pickupLocation = ""
    private var dropLocation = ""
    private var fare = ""
    private var vehicleType = ""

    private var driverArrived = false
    private var rideCompleted = false
    private var customerCancellationHandled = false
    private var sosHandled = false

    private var rideDocumentListener: ListenerRegistration? = null
    private var chatToastListener: ListenerRegistration? = null

    private lateinit var arrivedButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var navigateButton: Button
    private lateinit var contactCustomerRow: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_arrived_navigate)

        initViews()
        readIntentData()
        bindData()
        setupClicks()
        listenForRideDocumentUpdates()
        listenForIncomingRideMessages()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        closeButton = findViewById(R.id.closeButton)
        navigateButton = findViewById(R.id.navigateButton)
        arrivedButton = findViewById(R.id.arrivedButton)
        contactCustomerRow = findViewById(R.id.contactCustomerRow)
    }

    private fun readIntentData() {
        requestId = intent.getStringExtra("request_id") ?: ""
        customerId = intent.getStringExtra("customer_id") ?: ""
        customerName = intent.getStringExtra("customer_name") ?: "Customer"

        customerProfileImageUri =
            intent.getStringExtra("customer_profile_image_uri")
                ?: intent.getStringExtra("customerProfileImageUri")
                        ?: ""

        customerProfileImageBase64 =
            intent.getStringExtra("customer_profile_image_base64")
                ?: intent.getStringExtra("customerProfileImageBase64")
                        ?: ""

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

        val customerImage = findViewById<ImageView>(R.id.profileImage)

        if (customerProfileImageBase64.isNotBlank()) {
            ProfileImageHelper.loadBase64IntoImageView(
                imageView = customerImage,
                base64Image = customerProfileImageBase64,
                fallbackRes = R.drawable.profile1
            )
        } else {
            ProfileImageHelper.loadUriIntoImageView(
                imageView = customerImage,
                uriString = customerProfileImageUri,
                fallbackRes = R.drawable.profile1
            )
        }

        val vehicleInfoTop = findViewById<TextView>(R.id.vehicleInfoTop)
        val vehicleImage = findViewById<ImageView>(R.id.bikeImage)

        when (vehicleType.trim().lowercase()) {
            "cab", "car", "taxi" -> {
                vehicleInfoTop.text = getString(R.string.cab)
                vehicleImage.setImageResource(R.drawable.car)
            }

            "delivery" -> {
                vehicleInfoTop.text = getString(R.string.delivery)
                vehicleImage.setImageResource(R.drawable.delivery)
            }

            else -> {
                vehicleInfoTop.text = getString(R.string.motor_bike)
                vehicleImage.setImageResource(R.drawable.bike)
            }
        }
    }

    private fun setupClicks() {
        backButton.isEnabled = false
        backButton.alpha = 0.35f
        backButton.setOnClickListener {
            Toast.makeText(
                this,
                "Back is disabled during an active ride",
                Toast.LENGTH_SHORT
            ).show()
        }

        closeButton.setOnClickListener {
            cancelRideByDriver()
        }

        contactCustomerRow.setOnClickListener {
            openContactCustomerPage()
        }

        navigateButton.setOnClickListener {
            openNavigationToCustomer()
        }

        arrivedButton.setOnClickListener {
            when {
                rideCompleted -> openDriverHome()

                !driverArrived -> markDriverArrived()

                else -> completeRide()
            }
        }
    }

    private fun openContactCustomerPage() {
        val intent = Intent(this, MessagesActivity::class.java)
        intent.putExtra("user_role", "driver")
        intent.putExtra("request_id", requestId)
        intent.putExtra("contact_role", "customer")
        intent.putExtra("contact_user_id", customerId)
        intent.putExtra("contact_name", customerName)
        intent.putExtra("contact_photo_url", customerProfileImageUri)
        intent.putExtra("contact_photo_base64", customerProfileImageBase64)
        intent.putExtra("return_to_ride", true)
        startActivity(intent)
    }

    private fun listenForRideDocumentUpdates() {
        if (requestId.isBlank()) return

        rideDocumentListener?.remove()

        rideDocumentListener = db.collection("ride_requests")
            .document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    return@addSnapshotListener
                }

                val status = snapshot.getString("status") ?: ""

                when (status) {
                    "arrived" -> {
                        driverArrived = true
                    }

                    "in_progress" -> {
                        driverArrived = true

                        if (!rideCompleted) {
                            arrivedButton.isEnabled = true
                            arrivedButton.alpha = 1f
                            arrivedButton.text = getString(R.string.complete_ride)
                        }
                    }

                    "completed" -> {
                        rideCompleted = true
                    }

                    "cancelled_by_customer" -> {
                        if (!customerCancellationHandled) {
                            customerCancellationHandled = true

                            Toast.makeText(
                                this,
                                "Your ride has been cancelled by the customer",
                                Toast.LENGTH_LONG
                            ).show()

                            db.collection("ride_requests")
                                .document(requestId)
                                .update("driverNotified", true)

                            openDriverHomeAfterCustomerCancel()
                        }
                    }
                }

                val sosActive = snapshot.getBoolean("sosActive") ?: false
                val driverNotifiedSos = snapshot.getBoolean("driverNotifiedSos") ?: false

                if (sosActive && !driverNotifiedSos && !sosHandled) {
                    sosHandled = true

                    Toast.makeText(
                        this,
                        "Emergency alert: customer tapped SOS",
                        Toast.LENGTH_LONG
                    ).show()

                    db.collection("ride_requests")
                        .document(requestId)
                        .update("driverNotifiedSos", true)
                }
            }
    }

    private fun listenForIncomingRideMessages() {
        if (requestId.isBlank()) return

        val currentUserId =
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        chatToastListener?.remove()

        chatToastListener = db.collection("ride_chats")
            .document(requestId)
            .collection("messages")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("seenByReceiver", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    return@addSnapshotListener
                }

                val latestMessage = snapshot.documents.lastOrNull() ?: return@addSnapshotListener
                val senderRole = latestMessage.getString("senderRole") ?: ""

                if (senderRole == "customer") {
                    Toast.makeText(
                        this,
                        "Customer sent you a message",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun openNavigationToCustomer() {
        if (pickupLocation.isBlank()) {
            Toast.makeText(this, "Pickup location not found", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = "google.navigation:q=${Uri.encode(pickupLocation)}".toUri()
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(mapIntent)
        } catch (_: Exception) {
            val browserUri =
                "https://www.google.com/maps/search/?api=1&query=${Uri.encode(pickupLocation)}".toUri()
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }

    private fun markDriverArrived() {
        if (requestId.isBlank()) {
            Toast.makeText(this, "Ride request not found", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mapOf(
            "status" to "arrived",
            "arrivedAt" to System.currentTimeMillis(),
            "customerNotified" to false,
            "driverNotified" to true
        )

        db.collection("ride_requests")
            .document(requestId)
            .update(updates)
            .addOnSuccessListener {
                driverArrived = true

                arrivedButton.text = "Waiting for customer"
                arrivedButton.isEnabled = false
                arrivedButton.alpha = 0.6f

                Toast.makeText(
                    this,
                    "Customer has been notified. Wait for customer to start the ride.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to update arrival status",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun completeRide() {
        if (requestId.isBlank()) {
            Toast.makeText(this, "Ride request not found", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mapOf(
            "status" to "completed",
            "completedAt" to System.currentTimeMillis(),
            "completedBy" to "driver",
            "hiddenFromCustomer" to false,
            "hiddenFromDriver" to false,
            "customerNotified" to false,
            "driverNotified" to true
        )

        db.collection("ride_requests")
            .document(requestId)
            .update(updates)
            .addOnSuccessListener {
                rideCompleted = true

                arrivedButton.text = getString(R.string.back_to_home)
                arrivedButton.isEnabled = true
                arrivedButton.alpha = 1f

                Toast.makeText(
                    this,
                    "Ride completed successfully. Customer has been notified.",
                    Toast.LENGTH_LONG
                ).show()

                arrivedButton.postDelayed({
                    openDriverHome()
                }, 1200)
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to complete ride",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun cancelRideByDriver() {
        if (requestId.isBlank()) {
            Toast.makeText(this, "Ride request not found", Toast.LENGTH_SHORT).show()
            openDriverHome()
            return
        }

        val updates = mapOf(
            "status" to "cancelled_by_driver",
            "cancelledAt" to System.currentTimeMillis(),
            "cancelledBy" to "driver",
            "hiddenFromCustomer" to false,
            "hiddenFromDriver" to false,
            "customerNotified" to false,
            "driverNotified" to true
        )

        db.collection("ride_requests")
            .document(requestId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Ride cancelled",
                    Toast.LENGTH_SHORT
                ).show()

                openDriverHome()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to cancel ride",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun openDriverHomeAfterCustomerCancel() {
        val intent = Intent(this, DriverHomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("show_online_page", true)
        intent.putExtra("customer_cancelled_ride", true)
        intent.putExtra("driver_status", "online")
        startActivity(intent)
        finish()
    }

    private fun openDriverHome() {
        val intent = Intent(this, DriverHomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("show_online_page", true)
        intent.putExtra("driver_status", "online")
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        rideDocumentListener?.remove()
        rideDocumentListener = null

        chatToastListener?.remove()
        chatToastListener = null
    }
}