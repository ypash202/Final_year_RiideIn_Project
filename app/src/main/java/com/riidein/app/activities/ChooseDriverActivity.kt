package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

    private var fromLocation: String = "Current Location"
    private var toLocation: String = "Destination"
    private var selectedVehicle: String = "Moto"
    private var selectedPrice: String = "Rs 0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_driver)

        readIntentData()
        initViews()
        bindMapTexts()
        bindDriverData()
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

    private fun bindDriverData() {
        driver1NameText.text = "Binod"
        driver2NameText.text = "Sulav"

        driver1VehicleText.text = buildVehicleText(selectedVehicle, "Honda")
        driver2VehicleText.text = buildVehicleText(selectedVehicle, "Bajaj")

        driver1PriceText.text = selectedPrice
        driver2PriceText.text = selectedPrice

        driver1TimeText.text = "1 min"
        driver2TimeText.text = "2 min"
    }

    private fun buildVehicleText(vehicleType: String, brand: String): String {
        return when (vehicleType.trim().lowercase()) {
            "moto" -> "MOTOR-BIKE $brand"
            "cab" -> "CAB $brand"
            "delivery" -> "DELIVERY $brand"
            else -> "$vehicleType $brand"
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
            openRideTracking(driver1NameText.text.toString())
        }

        acceptButton2.setOnClickListener {
            openRideTracking(driver2NameText.text.toString())
        }
    }

    private fun openRideTracking(driverName: String) {
        val intent = Intent(this, RideTrackingActivity::class.java)
        intent.putExtra("driver_name", driverName)
        intent.putExtra("vehicle_name", selectedVehicle)
        intent.putExtra("selected_price", selectedPrice)
        intent.putExtra("from_location", fromLocation)
        intent.putExtra("to_location", toLocation)
        startActivity(intent)
    }

    private fun shortenPlaceName(place: String, maxLength: Int): String {
        val cleanText = place.trim()
        return if (cleanText.length <= maxLength) cleanText else cleanText.take(maxLength - 3) + "..."
    }
}
