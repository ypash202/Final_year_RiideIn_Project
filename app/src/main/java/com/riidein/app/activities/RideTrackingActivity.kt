package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.riidein.app.R

class RideTrackingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_tracking)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        val cancelRideButton = findViewById<Button>(R.id.cancelRideButton)

        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        closeButton.setOnClickListener {
            finish()
        }

        cancelRideButton.setOnClickListener {
            val intent = Intent(this, CancelRideActivity::class.java)
            startActivity(intent)
        }
    }
}