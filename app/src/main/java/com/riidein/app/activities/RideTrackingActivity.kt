package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.riidein.app.R

class RideTrackingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_tracking)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        val cancelRideButton = findViewById<Button>(R.id.cancelRideButton)
        val nextArrow = findViewById<ImageView>(R.id.nextArrow)
        val contactDriverRow = findViewById<LinearLayout>(R.id.contactDriverRow)

        val driverNameText = findViewById<TextView>(R.id.driverNameText)
        val driverVehicleText = findViewById<TextView>(R.id.driverVehicleText)
        val paymentAmountText = findViewById<TextView>(R.id.paymentAmountText)
        val pickupLocationText = findViewById<TextView>(R.id.pickupLocationText)
        val dropLocationText = findViewById<TextView>(R.id.dropLocationText)

        val driverName = intent.getStringExtra("driver_name") ?: "Binod"
        val vehicleName = intent.getStringExtra("vehicle_name") ?: "Moto"
        val selectedPrice = intent.getStringExtra("selected_price") ?: "Rs 200"
        val fromLocation = intent.getStringExtra("from_location") ?: "Boudha"
        val toLocation = intent.getStringExtra("to_location") ?: "Trade Tower, Thapathali"

        driverNameText.text = driverName
        driverVehicleText.text = when (vehicleName.lowercase()) {
            "moto" -> "MOTOR-BIKE Honda"
            "cab" -> "CAB"
            "delivery" -> "DELIVERY"
            else -> vehicleName
        }
        paymentAmountText.text = selectedPrice
        pickupLocationText.text = fromLocation
        dropLocationText.text = toLocation

        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        closeButton.setOnClickListener {
            finish()
        }

        cancelRideButton.setOnClickListener {
            val intent = Intent(this, CancelRideActivity::class.java)
            intent.putExtra("from_location", fromLocation)
            intent.putExtra("to_location", toLocation)
            startActivity(intent)
        }

        nextArrow.setOnClickListener {
            val intent = Intent(this, CancelRideActivity::class.java)
            intent.putExtra("from_location", fromLocation)
            intent.putExtra("to_location", toLocation)
            startActivity(intent)
        }

        contactDriverRow.setOnClickListener {
            val intent = Intent(this, MessagesActivity::class.java)
            intent.putExtra("driver_name", driverName)
            startActivity(intent)
        }
    }
}