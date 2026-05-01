package com.riidein.app.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.riidein.app.R
import java.util.Locale

class ChooseDriverActivity : AppCompatActivity() {

    private lateinit var fromMapText: TextView
    private lateinit var toMapText: TextView
    private lateinit var driverListContainer: LinearLayout
    private lateinit var cancelRequestButton: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var fromLocation: String = "Current Location"
    private var toLocation: String = "Destination"
    private var selectedVehicle: String = "Moto"
    private var selectedPrice: String = "Rs 0"

    private val availableDrivers = mutableListOf<DriverOption>()
    private var requestStatusListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_driver)

        readIntentData()
        initViews()
        bindLocationText()
        setupClicks()
        loadAvailableDrivers()
    }

    private fun readIntentData() {
        fromLocation = intent.getStringExtra("from_location") ?: "Current Location"
        toLocation = intent.getStringExtra("to_location") ?: "Destination"

        selectedVehicle =
            intent.getStringExtra("selected_vehicle")
                ?: intent.getStringExtra("vehicle_type")
                        ?: "Moto"

        selectedPrice =
            intent.getStringExtra("selected_price")
                ?: intent.getStringExtra("fare")
                        ?: "Rs 0"
    }

    private fun initViews() {
        fromMapText = findViewById(R.id.fromMapText)
        toMapText = findViewById(R.id.toMapText)
        driverListContainer = findViewById(R.id.driverListContainer)
        cancelRequestButton = findViewById(R.id.cancelRequestButton)
    }

    private fun bindLocationText() {
        fromMapText.text = shortenPlaceName(fromLocation, 18)
        toMapText.text = shortenPlaceName(toLocation, 18)
    }

    private fun setupClicks() {
        cancelRequestButton.setOnClickListener {
            requestStatusListener?.remove()
            requestStatusListener = null
            finish()
        }
    }

    private fun loadAvailableDrivers() {
        driverListContainer.removeAllViews()
        availableDrivers.clear()

        showMessageCard("Loading available drivers...")

        db.collection("users")
            .whereEqualTo("role", "driver")
            .whereEqualTo("isAvailable", true)
            .get()
            .addOnSuccessListener { result ->
                driverListContainer.removeAllViews()
                availableDrivers.clear()

                val filteredDrivers = result.documents.mapNotNull { document ->
                    val rawVehicleType = document.getString("vehicleType") ?: ""
                    val driverVehicleType = normalizeVehicleType(rawVehicleType)

                    if (!isDriverAllowedForSelectedService(driverVehicleType)) {
                        return@mapNotNull null
                    }

                    DriverOption(
                        id = document.id,
                        name = document.getString("name") ?: "Driver",
                        vehicleType = driverVehicleType,
                        rawVehicleType = rawVehicleType,
                        phone = document.getString("phone") ?: ""
                    )
                }

                availableDrivers.addAll(filteredDrivers)

                if (availableDrivers.isEmpty()) {
                    val message = if (isDeliverySelected()) {
                        "No available bike or cab drivers found for delivery"
                    } else {
                        "No available ${displaySelectedVehicle()} drivers found"
                    }

                    showMessageCard(message)
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                showAllDriverCards()
            }
            .addOnFailureListener {
                driverListContainer.removeAllViews()
                showMessageCard("Failed to load drivers")
                Toast.makeText(this, "Failed to load drivers", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAllDriverCards() {
        driverListContainer.removeAllViews()

        availableDrivers.forEachIndexed { index, driver ->
            driverListContainer.addView(createDriverCard(driver, index))
        }
    }

    private fun createDriverCard(driver: DriverOption, index: Int): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundedDrawable(
                color = "#000000",
                radiusDp = 22f
            )
            setPadding(
                dpToInt(16f),
                dpToInt(16f),
                dpToInt(16f),
                dpToInt(16f)
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToInt(18f)
            }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val avatarText = TextView(this).apply {
            text = driver.name.firstOrNull()?.uppercase() ?: "D"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            background = createOvalDrawable("#4A4A4A")
            layoutParams = LinearLayout.LayoutParams(
                dpToInt(42f),
                dpToInt(42f)
            )
        }

        val nameColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dpToInt(12f)
            }
        }

        val nameText = TextView(this).apply {
            text = driver.name
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
        }

        val vehicleText = TextView(this).apply {
            text = buildVehicleTextForCard(driver.vehicleType)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
        }

        nameColumn.addView(nameText)
        nameColumn.addView(vehicleText)

        val priceColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }

        val priceText = TextView(this).apply {
            text = selectedPrice
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
        }

        val timeText = TextView(this).apply {
            text = "${index + 1} min"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
        }

        priceColumn.addView(priceText)
        priceColumn.addView(timeText)

        topRow.addView(avatarText)
        topRow.addView(nameColumn)
        topRow.addView(priceColumn)

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToInt(26f)
            }
        }

        val acceptButton = Button(this).apply {
            text = "Accept"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            background = createRoundedDrawable(
                color = "#00E676",
                radiusDp = 22f
            )
            layoutParams = LinearLayout.LayoutParams(
                0,
                dpToInt(48f),
                1f
            ).apply {
                marginEnd = dpToInt(9f)
            }

            setOnClickListener {
                sendRideRequest(driver)
            }
        }

        val declineButton = Button(this).apply {
            text = "Decline"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            background = createRoundedDrawable(
                color = "#FF2525",
                radiusDp = 22f
            )
            layoutParams = LinearLayout.LayoutParams(
                0,
                dpToInt(48f),
                1f
            ).apply {
                marginStart = dpToInt(9f)
            }

            setOnClickListener {
                driverListContainer.removeView(card)
                availableDrivers.remove(driver)

                if (availableDrivers.isEmpty()) {
                    showMessageCard("No more drivers available")
                }
            }
        }

        buttonRow.addView(acceptButton)
        buttonRow.addView(declineButton)

        card.addView(topRow)
        card.addView(buttonRow)

        return card
    }

    private fun sendRideRequest(driver: DriverOption) {
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

                val request = hashMapOf<String, Any>(
                    "customerId" to currentUser.uid,
                    "customerName" to customerName,
                    "driverId" to driver.id,
                    "driverName" to driver.name,

                    // serviceType = what customer selected: bike/cab/delivery
                    // vehicleType = real driver vehicle: bike/cab
                    "serviceType" to normalizeServiceType(selectedVehicle),
                    "vehicleType" to driver.vehicleType,

                    "pickup" to fromLocation,
                    "drop" to toLocation,
                    "fare" to selectedPrice,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis(),

                    "hiddenFromCustomer" to false,
                    "hiddenFromDriver" to false,
                    "customerNotified" to false,
                    "driverNotified" to false
                )

                db.collection("ride_requests")
                    .add(request)
                    .addOnSuccessListener { document ->
                        Toast.makeText(
                            this,
                            "Request sent to ${driver.name}",
                            Toast.LENGTH_SHORT
                        ).show()

                        listenForDriverResponse(
                            requestId = document.id,
                            selectedDriver = driver
                        )
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Failed to send request",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to load customer details",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun listenForDriverResponse(
        requestId: String,
        selectedDriver: DriverOption
    ) {
        requestStatusListener?.remove()

        requestStatusListener = db.collection("ride_requests")
            .document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    return@addSnapshotListener
                }

                val status = snapshot.getString("status") ?: ""

                when (status) {
                    "accepted" -> {
                        requestStatusListener?.remove()
                        requestStatusListener = null

                        val intent = Intent(this, RideTrackingActivity::class.java)
                        intent.putExtra("request_id", requestId)
                        intent.putExtra("driver_id", selectedDriver.id)
                        intent.putExtra("driver_name", selectedDriver.name)
                        intent.putExtra(
                            "vehicle_name",
                            buildVehicleTextForTracking(selectedDriver.vehicleType)
                        )
                        intent.putExtra("selected_price", selectedPrice)
                        intent.putExtra("from_location", fromLocation)
                        intent.putExtra("to_location", toLocation)
                        intent.putExtra("selected_service", selectedVehicle)
                        startActivity(intent)
                        finish()
                    }

                    "declined" -> {
                        Toast.makeText(
                            this,
                            "${selectedDriver.name} declined your request",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    "cancelled_by_driver" -> {
                        requestStatusListener?.remove()
                        requestStatusListener = null

                        Toast.makeText(
                            this,
                            "${selectedDriver.name} cancelled your request",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }

    private fun isDriverAllowedForSelectedService(driverVehicleType: String): Boolean {
        return if (isDeliverySelected()) {
            driverVehicleType == "bike" || driverVehicleType == "cab"
        } else {
            driverVehicleType == normalizeVehicleType(selectedVehicle)
        }
    }

    private fun isDeliverySelected(): Boolean {
        return normalizeServiceType(selectedVehicle) == "delivery"
    }

    private fun normalizeServiceType(serviceType: String): String {
        return when (serviceType.trim().lowercase(Locale.getDefault())) {
            "moto" -> "bike"
            "motorbike" -> "bike"
            "motor-bike" -> "bike"
            "bike" -> "bike"

            "cab" -> "cab"
            "car" -> "cab"
            "taxi" -> "cab"

            "delivery" -> "delivery"

            else -> serviceType.trim().lowercase(Locale.getDefault())
        }
    }

    private fun normalizeVehicleType(vehicleType: String): String {
        return when (vehicleType.trim().lowercase(Locale.getDefault())) {
            "moto" -> "bike"
            "motorbike" -> "bike"
            "motor-bike" -> "bike"
            "bike" -> "bike"

            "cab" -> "cab"
            "car" -> "cab"
            "taxi" -> "cab"

            else -> vehicleType.trim().lowercase(Locale.getDefault())
        }
    }

    private fun buildVehicleTextForCard(driverVehicleType: String): String {
        return if (isDeliverySelected()) {
            when (driverVehicleType) {
                "bike" -> "DELIVERY • MOTOR-BIKE"
                "cab" -> "DELIVERY • CAB"
                else -> "DELIVERY"
            }
        } else {
            when (driverVehicleType) {
                "bike" -> "MOTOR-BIKE"
                "cab" -> "CAB"
                else -> displaySelectedVehicle().uppercase(Locale.getDefault())
            }
        }
    }

    private fun buildVehicleTextForTracking(driverVehicleType: String): String {
        return when (driverVehicleType) {
            "bike" -> "MOTOR-BIKE"
            "cab" -> "CAB"
            else -> displaySelectedVehicle().uppercase(Locale.getDefault())
        }
    }

    private fun displaySelectedVehicle(): String {
        return when (normalizeServiceType(selectedVehicle)) {
            "bike" -> "bike"
            "cab" -> "cab"
            "delivery" -> "delivery"
            else -> selectedVehicle
        }
    }

    private fun showMessageCard(message: String) {
        driverListContainer.removeAllViews()

        val textView = TextView(this).apply {
            text = message
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(this@ChooseDriverActivity, android.R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(
                dpToInt(18f),
                dpToInt(22f),
                dpToInt(18f),
                dpToInt(22f)
            )
            background = createRoundedDrawable(
                color = "#000000",
                radiusDp = 18f
            )
        }

        driverListContainer.addView(
            textView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun shortenPlaceName(place: String, maxLength: Int): String {
        val cleanText = place.trim()

        return if (cleanText.length <= maxLength) {
            cleanText
        } else {
            cleanText.take(maxLength - 3) + "..."
        }
    }

    private fun createRoundedDrawable(
        color: String,
        radiusDp: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(color))
            cornerRadius = dpToInt(radiusDp).toFloat()
        }
    }

    private fun createOvalDrawable(color: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(color))
        }
    }

    private fun dpToInt(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        requestStatusListener?.remove()
        requestStatusListener = null
    }
}

private data class DriverOption(
    val id: String,
    val name: String,
    val vehicleType: String,
    val rawVehicleType: String,
    val phone: String
)