package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R

class CancelRideActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private var requestId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cancel_ride)

        requestId = intent.getStringExtra("request_id") ?: ""

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        val cancelReasonGroup = findViewById<RadioGroup>(R.id.cancelReasonGroup)
        val continueRideButton = findViewById<Button>(R.id.continueRideButton)

        val driverNameText = findViewById<TextView>(R.id.driverName)
        val vehicleNameText = findViewById<TextView>(R.id.vehicleName)

        val driverName = intent.getStringExtra("driver_name") ?: "Driver"
        val vehicleName = intent.getStringExtra("vehicle_name") ?: "Vehicle"

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
                return@setOnClickListener
            }

            val selectedReason = findViewById<RadioButton>(selectedReasonId)
                ?.text
                ?.toString()
                ?: "No reason selected"

            cancelRideByCustomer(selectedReason)
        }
    }

    private fun cancelRideByCustomer(reason: String) {
        if (requestId.isBlank()) {
            Toast.makeText(
                this,
                "Ride request not found",
                Toast.LENGTH_SHORT
            ).show()
            goToCustomerHome()
            return
        }

        val updates = mapOf(
            "status" to "cancelled_by_customer",
            "cancelledBy" to "customer",
            "cancelReason" to reason,
            "cancelledAt" to System.currentTimeMillis(),
            "customerNotified" to true,
            "driverNotified" to false,
            "hiddenFromCustomer" to false,
            "hiddenFromDriver" to false
        )

        db.collection("ride_requests")
            .document(requestId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Your ride has been successfully cancelled",
                    Toast.LENGTH_LONG
                ).show()

                goToCustomerHome()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to cancel ride",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun goToCustomerHome() {
        val intent = Intent(this, CustomerHomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("user_role", "customer")
        startActivity(intent)
        finish()
    }
}