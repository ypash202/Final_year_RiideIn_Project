package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.riidein.app.R

class CancelRideActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cancel_ride)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        val cancelReasonGroup = findViewById<RadioGroup>(R.id.cancelReasonGroup)
        val continueRideButton = findViewById<Button>(R.id.continueRideButton)

        val driverNameText = findViewById<TextView>(R.id.driverName)
        val vehicleNameText = findViewById<TextView>(R.id.vehicleName)

        val driverName = intent.getStringExtra("driver_name") ?: "Binod"
        val vehicleName = intent.getStringExtra("vehicle_name") ?: "MOTOR-BIKE Honda"

        driverNameText.text = driverName
        vehicleNameText.text = vehicleName

        backButton.setOnClickListener {
            finish()
        }

        closeButton.setOnClickListener {
            finish()
        }

        continueRideButton.setOnClickListener {
            val selectedReasonId = cancelReasonGroup.checkedRadioButtonId

            if (selectedReasonId == -1) {
                Toast.makeText(
                    this,
                    "Please select a reason first",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Your ride has been successfully cancelled",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(this, CustomerHomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}
