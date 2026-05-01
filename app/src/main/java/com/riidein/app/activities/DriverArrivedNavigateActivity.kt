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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.riidein.app.R

class DriverArrivedNavigateActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private var requestId = ""
    private var customerId = ""
    private var customerName = ""
    private var pickupLocation = ""
    private var dropLocation = ""
    private var fare = ""
    private var vehicleType = ""
    private var driverArrived = false

    private var customerCancelListener: ListenerRegistration? = null
    private var chatToastListener: ListenerRegistration? = null
    private var hasHandledCustomerCancellation = false

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
        listenForCustomerCancellation()
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
            if (!driverArrived) {
                markDriverArrived()
            } else {
                completeRide()
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
        intent.putExtra("return_to_ride", true)
        startActivity(intent)
    }

    private fun listenForIncomingRideMessages() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (requestId.isBlank()) return

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

    private fun listenForCustomerCancellation() {
        if (requestId.isBlank()) {
            return
        }

        customerCancelListener?.remove()

        customerCancelListener = db.collection("ride_requests")
            .document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    return@addSnapshotListener
                }

                val status = snapshot.getString("status") ?: ""

                if (status == "cancelled_by_customer" && !hasHandledCustomerCancellation) {
                    hasHandledCustomerCancellation = true

                    customerCancelListener?.remove()
                    customerCancelListener = null

                    db.collection("ride_requests")
                        .document(requestId)
                        .update("driverNotified", true)

                    Toast.makeText(
                        this,
                        "Your ride has been cancelled by the customer",
                        Toast.LENGTH_LONG
                    ).show()

                    goBackToWaitingForRequestPage()
                }
            }
    }

    private fun openNavigationToCustomer() {
        if (pickupLocation.isBlank()) {
            Toast.makeText(
                this,
                "Pickup location not found",
                Toast.LENGTH_SHORT
            ).show()
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
            Toast.makeText(
                this,
                "Ride request not found",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        db.collection("ride_requests")
            .document(requestId)
            .update("status", "arrived")
            .addOnSuccessListener {
                driverArrived = true
                arrivedButton.text = getString(R.string.complete_ride)

                Toast.makeText(
                    this,
                    "Customer has been notified. Complete the ride after drop-off.",
                    Toast.LENGTH_SHORT
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
            Toast.makeText(
                this,
                "Ride request not found",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val updates = mapOf(
            "status" to "completed",
            "completedAt" to System.currentTimeMillis(),
            "hiddenFromCustomer" to false,
            "hiddenFromDriver" to false
        )

        db.collection("ride_requests")
            .document(requestId)
            .update(updates)
            .addOnSuccessListener {
                customerCancelListener?.remove()
                customerCancelListener = null

                Toast.makeText(
                    this,
                    "Ride completed successfully",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(this, DriverHomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
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
            goBackToDriverHome()
            return
        }

        val updates = mapOf(
            "status" to "cancelled_by_driver",
            "cancelledBy" to "driver",
            "cancelledAt" to System.currentTimeMillis(),
            "customerNotified" to false,
            "hiddenFromCustomer" to false,
            "hiddenFromDriver" to false
        )

        db.collection("ride_requests")
            .document(requestId)
            .update(updates)
            .addOnSuccessListener {
                customerCancelListener?.remove()
                customerCancelListener = null

                Toast.makeText(
                    this,
                    "Ride cancelled",
                    Toast.LENGTH_SHORT
                ).show()

                goBackToDriverHome()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to cancel ride",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun goBackToWaitingForRequestPage() {
        val intent = Intent(this, DriverRideRequestActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun goBackToDriverHome() {
        val intent = Intent(this, DriverHomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        customerCancelListener?.remove()
        customerCancelListener = null

        chatToastListener?.remove()
        chatToastListener = null
    }
}