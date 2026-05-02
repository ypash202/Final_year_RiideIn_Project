package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.riidein.app.R
import com.riidein.app.utils.ProfileImageHelper
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

    private var requestId: String = ""
    private var driverId: String = ""
    private var driverName: String = "Binod"
    private var driverPhotoUrl: String = ""
    private var driverPhotoBase64: String = ""

    private var vehicleName: String = "Moto"
    private var vehicleNumber: String = "BA 01 PA 1234"
    private var selectedPrice: String = "Rs 200"
    private var fromLocation: String = "Boudha"
    private var toLocation: String = "Trade Tower, Thapathali"

    private var driverHasArrived = false
    private var completedRideSaved = false
    private var sosAlreadySent = false

    private enum class RideState {
        ARRIVING,
        ARRIVED,
        IN_PROGRESS,
        COMPLETED
    }

    private var currentRideState = RideState.ARRIVING
    private var chatToastListener: ListenerRegistration? = null
    private var rideStatusListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_tracking)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        readIntentData()
        bindRideData()
        updateRideStateUI()
        setupClicks()
        disableNextUntilDriverArrives()
        listenForRideStatus()
        loadDriverPhotoFromFirestore()
        listenForIncomingRideMessages()
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
        driverId = intent.getStringExtra("driver_id") ?: ""
        driverName = intent.getStringExtra("driver_name") ?: "Binod"

        driverPhotoUrl =
            intent.getStringExtra("driver_profile_image_uri")
                ?: intent.getStringExtra("driver_photo_url")
                        ?: ""

        driverPhotoBase64 =
            intent.getStringExtra("driver_profile_image_base64")
                ?: intent.getStringExtra("driver_photo_base64")
                        ?: ""

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

        driverVehicleText.text = when (vehicleName.lowercase(Locale.getDefault())) {
            "moto", "bike", "motor-bike", "motorbike" -> getString(R.string.driver_vehicle_honda)
            "cab", "car", "taxi" -> getString(R.string.vehicle_type_cab)
            "delivery" -> getString(R.string.vehicle_type_delivery)
            else -> vehicleName
        }

        paymentAmountText.text = selectedPrice
        paymentMethodText.text = getString(R.string.cash)
        pickupLocationText.text = fromLocation
        dropLocationText.text = toLocation

        loadDriverImage()

        when (vehicleName.lowercase(Locale.getDefault())) {
            "moto", "bike", "motor-bike", "motorbike" -> vehicleImage.setImageResource(R.drawable.bike)
            "cab", "car", "taxi" -> vehicleImage.setImageResource(R.drawable.car)
            "delivery" -> vehicleImage.setImageResource(R.drawable.delivery)
            else -> vehicleImage.setImageResource(R.drawable.bike)
        }
    }

    private fun loadDriverImage() {
        ProfileImageHelper.loadProfileImage(
            imageView = driverProfileImage,
            base64Image = driverPhotoBase64,
            uriString = driverPhotoUrl,
            fallbackRes = R.drawable.profile2
        )
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
            Toast.makeText(
                this,
                "Please cancel the ride before leaving this screen",
                Toast.LENGTH_SHORT
            ).show()
        }

        contactDriverRow.setOnClickListener {
            val intent = Intent(this, MessagesActivity::class.java)
            intent.putExtra("user_role", "customer")
            intent.putExtra("request_id", requestId)
            intent.putExtra("contact_role", "driver")
            intent.putExtra("contact_user_id", driverId)
            intent.putExtra("contact_name", driverName)
            intent.putExtra("contact_photo_url", driverPhotoUrl)
            intent.putExtra("contact_photo_base64", driverPhotoBase64)
            intent.putExtra("return_to_ride", true)
            startActivity(intent)
        }

        cancelRideButton.setOnClickListener {
            when (currentRideState) {
                RideState.ARRIVING -> openCancelRidePage()
                RideState.ARRIVED -> startRideForCustomer()

                RideState.IN_PROGRESS -> {
                    Toast.makeText(
                        this,
                        "You are on a ride. The driver will complete it after drop-off.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                RideState.COMPLETED -> goToCustomerHome()
            }
        }

        nextArrow.setOnClickListener {
            when (currentRideState) {
                RideState.ARRIVING -> {
                    Toast.makeText(
                        this,
                        "Please wait until your driver arrives.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                RideState.ARRIVED -> startRideForCustomer()

                RideState.IN_PROGRESS -> {
                    Toast.makeText(
                        this,
                        "You are on a ride. Please wait until the driver completes the ride.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                RideState.COMPLETED -> goToCustomerHome()
            }
        }

        sosButton.setOnClickListener {
            showSosDialog()
        }
    }

    private fun startRideForCustomer() {
        currentRideState = RideState.IN_PROGRESS
        updateRideStateUI()
        disableNextUntilDriverCompletes()

        if (requestId.isNotBlank()) {
            db.collection("ride_requests")
                .document(requestId)
                .update(
                    mapOf(
                        "status" to "in_progress",
                        "startedAt" to System.currentTimeMillis(),
                        "startedBy" to "customer"
                    )
                )
        }

        Toast.makeText(
            this,
            "Ride started. You are now on your trip.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun updateRideStateUI() {
        when (currentRideState) {
            RideState.ARRIVING -> {
                arrivalTitle.text = getString(R.string.ride_state_arriving)
                cancelRideButton.text = getString(R.string.cancel_ride)
                cancelRideButton.isEnabled = true
                contactDriverRow.visibility = LinearLayout.VISIBLE
                nextArrow.visibility = ImageView.VISIBLE
                sosButton.visibility = Button.VISIBLE
                paymentLabel.visibility = TextView.VISIBLE
                paymentAmountText.visibility = TextView.VISIBLE
                paymentMethodText.visibility = TextView.VISIBLE
            }

            RideState.ARRIVED -> {
                arrivalTitle.text = "Your driver has arrived"
                cancelRideButton.text = "Start Ride"
                cancelRideButton.isEnabled = true
                contactDriverRow.visibility = LinearLayout.VISIBLE
                nextArrow.visibility = ImageView.VISIBLE
                sosButton.visibility = Button.VISIBLE
                paymentLabel.visibility = TextView.VISIBLE
                paymentAmountText.visibility = TextView.VISIBLE
                paymentMethodText.visibility = TextView.VISIBLE
            }

            RideState.IN_PROGRESS -> {
                arrivalTitle.text = "You are on a ride"
                cancelRideButton.text = "Ride in progress"
                cancelRideButton.isEnabled = false
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
                cancelRideButton.isEnabled = true
                contactDriverRow.visibility = LinearLayout.GONE
                nextArrow.visibility = ImageView.GONE
                sosButton.visibility = Button.GONE
                paymentLabel.visibility = TextView.VISIBLE
                paymentAmountText.visibility = TextView.VISIBLE
                paymentMethodText.visibility = TextView.VISIBLE
            }
        }
    }

    private fun disableNextUntilDriverArrives() {
        nextArrow.isEnabled = false
        nextArrow.alpha = 0.4f
    }

    private fun enableNextAfterDriverArrives() {
        nextArrow.isEnabled = true
        nextArrow.alpha = 1f
    }

    private fun disableNextUntilDriverCompletes() {
        nextArrow.isEnabled = false
        nextArrow.alpha = 0.4f
    }

    private fun listenForRideStatus() {
        if (requestId.isEmpty()) {
            Toast.makeText(this, "Request ID missing", Toast.LENGTH_SHORT).show()
            return
        }

        rideStatusListener?.remove()

        rideStatusListener = db.collection("ride_requests")
            .document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    return@addSnapshotListener
                }

                val status = snapshot.getString("status") ?: ""

                when (status) {
                    "arrived" -> {
                        if (!driverHasArrived) {
                            driverHasArrived = true
                            currentRideState = RideState.ARRIVED
                            updateRideStateUI()
                            enableNextAfterDriverArrives()

                            Toast.makeText(
                                this,
                                "Your driver has arrived. Tap Start Ride.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    "in_progress" -> {
                        if (currentRideState != RideState.IN_PROGRESS) {
                            currentRideState = RideState.IN_PROGRESS
                            updateRideStateUI()
                            disableNextUntilDriverCompletes()
                        }
                    }

                    "completed" -> {
                        if (!completedRideSaved) {
                            completedRideSaved = true
                            currentRideState = RideState.COMPLETED
                            updateRideStateUI()

                            Toast.makeText(
                                this,
                                "Ride completed successfully. Returning home.",
                                Toast.LENGTH_LONG
                            ).show()

                            saveCompletedRideToFirestore()

                            nextArrow.postDelayed({
                                goToCustomerHome()
                            }, 1800)
                        }
                    }

                    "cancelled_by_driver" -> {
                        Toast.makeText(
                            applicationContext,
                            "Your ride has been cancelled by the driver.",
                            Toast.LENGTH_LONG
                        ).show()

                        nextArrow.postDelayed({
                            val intent = Intent(this, VehicleSelectionActivity::class.java)
                            intent.putExtra("from_location", fromLocation)
                            intent.putExtra("to_location", toLocation)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        }, 1800)
                    }
                }
            }
    }

    private fun listenForIncomingRideMessages() {
        val currentUserId = auth.currentUser?.uid ?: return

        if (requestId.isBlank()) {
            return
        }

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

                if (senderRole == "driver") {
                    Toast.makeText(
                        this,
                        "Driver sent you a message",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun loadDriverPhotoFromFirestore() {
        if (driverId.isBlank()) {
            loadDriverImage()
            return
        }

        db.collection("users")
            .document(driverId)
            .get()
            .addOnSuccessListener { document ->
                driverPhotoBase64 =
                    document.getString("profileImageBase64")
                        ?: document.getString("driverPhotoBase64")
                                ?: driverPhotoBase64

                driverPhotoUrl =
                    document.getString("profileImageUri")
                        ?: document.getString("driverPhotoUrl")
                                ?: document.getString("profileImageUrl")
                                ?: document.getString("profilePhotoUrl")
                                ?: driverPhotoUrl

                loadDriverImage()
            }
            .addOnFailureListener {
                loadDriverImage()
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
            "driverId" to driverId,
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
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.failed_to_save_ride), Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun goToCustomerHome() {
        val intent = Intent(this, CustomerHomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("user_role", "customer")
        startActivity(intent)
        finish()
    }

    private fun openCancelRidePage() {
        val intent = Intent(this, CancelRideActivity::class.java)
        intent.putExtra("request_id", requestId)
        intent.putExtra("driver_name", driverName)
        intent.putExtra("vehicle_name", driverVehicleText.text.toString())
        intent.putExtra("vehicle_number", vehicleNumber)
        intent.putExtra("from_location", fromLocation)
        intent.putExtra("to_location", toLocation)
        intent.putExtra("fare", selectedPrice)
        startActivity(intent)
    }

    private fun showSosDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.sos_title))
            .setMessage("Send an emergency alert to the driver and save this SOS record?")
            .setPositiveButton("Send SOS Alert") { _, _ ->
                triggerSosFlow()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun triggerSosFlow() {
        if (sosAlreadySent) {
            Toast.makeText(
                this,
                "SOS alert has already been sent for this ride.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (requestId.isBlank()) {
            Toast.makeText(
                this,
                "Ride request not found. SOS could not be linked to this ride.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        sosAlreadySent = true

        db.collection("ride_requests")
            .document(requestId)
            .update(
                mapOf(
                    "sosActive" to true,
                    "sosTriggeredBy" to "customer",
                    "sosTriggeredAt" to System.currentTimeMillis(),
                    "driverNotifiedSos" to false
                )
            )
            .addOnSuccessListener {
                saveSosAlertToFirestore()

                Toast.makeText(
                    this,
                    "SOS alert sent to driver and saved.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener {
                sosAlreadySent = false

                Toast.makeText(
                    this,
                    "Failed to send SOS alert.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun saveSosAlertToFirestore() {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid ?: "unknown_user"

        val sosData = hashMapOf(
            "requestId" to requestId,
            "userId" to userId,
            "driverId" to driverId,
            "driverName" to driverName,
            "vehicleName" to driverVehicleText.text.toString(),
            "vehicleNumber" to vehicleNumber,
            "fromLocation" to fromLocation,
            "toLocation" to toLocation,
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

    override fun onDestroy() {
        super.onDestroy()

        rideStatusListener?.remove()
        rideStatusListener = null

        chatToastListener?.remove()
        chatToastListener = null
    }
}