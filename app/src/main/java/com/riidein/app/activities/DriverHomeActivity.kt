package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R

class DriverHomeActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var statusText: TextView
    private lateinit var goOnlineButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_home)

        val menuButton = findViewById<ImageButton>(R.id.menuButton)
        val closeButton = findViewById<ImageButton>(R.id.closeButton)

        goOnlineButton = findViewById(R.id.goOnlineButton)
        statusText = findViewById(R.id.statusText)

        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navWallet = findViewById<LinearLayout>(R.id.navWallet)
        val navMessages = findViewById<LinearLayout>(R.id.navMessages)

        checkIfReturnedFromCancelledRide()

        menuButton.setOnClickListener {
            startActivity(
                Intent(this, SideMenuActivity::class.java).apply {
                    putExtra("user_role", "driver")
                    putExtra("selected_menu", "home")
                }
            )
        }

        closeButton.setOnClickListener {
            auth.signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        goOnlineButton.setOnClickListener {
            makeDriverOnlineAndOpenRequests()
        }

        navHome.setOnClickListener {
            Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show()
        }

        navWallet.setOnClickListener {
            startActivity(
                Intent(this, DriverWalletActivity::class.java).apply {
                    putExtra("user_role", "driver")
                    putExtra("selected_tab", "wallet")
                }
            )
        }

        navMessages.setOnClickListener {
            startActivity(
                Intent(this, MessagesActivity::class.java).apply {
                    putExtra("user_role", "driver")
                    putExtra("selected_tab", "messages")
                }
            )
        }
    }

    private fun checkIfReturnedFromCancelledRide() {
        val showOnlinePage = intent.getBooleanExtra("show_online_page", false)
        val customerCancelledRide = intent.getBooleanExtra("customer_cancelled_ride", false)

        if (showOnlinePage) {
            statusText.text = "You are online"

            if (customerCancelledRide) {
                Toast.makeText(
                    this,
                    "Customer has cancelled the ride",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "You are online",
                    Toast.LENGTH_SHORT
                ).show()
            }

            setDriverAvailableAgain()
        }
    }

    private fun makeDriverOnlineAndOpenRequests() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
            .document(currentUser.uid)
            .update("isAvailable", true)
            .addOnSuccessListener {
                statusText.text = "You are online"

                Toast.makeText(
                    this,
                    "Driver is now online",
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(Intent(this, DriverRideRequestActivity::class.java))
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to go online",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun setDriverAvailableAgain() {
        val currentUser = auth.currentUser ?: return

        db.collection("users")
            .document(currentUser.uid)
            .update("isAvailable", true)
    }
}