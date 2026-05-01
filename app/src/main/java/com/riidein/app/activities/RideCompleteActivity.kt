package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R

class RideCompleteActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private var requestId: String = ""
    private var userRole: String = "driver"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ride_complete)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestId = intent.getStringExtra("request_id") ?: ""
        userRole = intent.getStringExtra("user_role") ?: "driver"

        markRideAsCompleted()

        val mainView = findViewById<View>(R.id.main)
        mainView.setOnClickListener {
            openCorrectHome()
        }
    }

    private fun markRideAsCompleted() {
        if (requestId.isBlank()) {
            Toast.makeText(
                this,
                "Ride request not found",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val updates = hashMapOf<String, Any>(
            "status" to "completed",
            "completedAt" to System.currentTimeMillis(),
            "hiddenFromCustomer" to false,
            "hiddenFromDriver" to false
        )

        db.collection("ride_requests")
            .document(requestId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Ride completed successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to save completed ride",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun openCorrectHome() {
        val targetActivity = if (userRole == "driver") {
            DriverHomeActivity::class.java
        } else {
            CustomerHomeActivity::class.java
        }

        val intent = Intent(this, targetActivity)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}