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
import com.riidein.app.R

class DriverHomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_home)

        val menuButton = findViewById<ImageButton>(R.id.menuButton)
        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        val goOnlineButton = findViewById<Button>(R.id.goOnlineButton)

        val motoOption = findViewById<LinearLayout>(R.id.motoOption)
        val cabOption = findViewById<LinearLayout>(R.id.cabOption)
        val deliveryOption = findViewById<LinearLayout>(R.id.deliveryOption)

        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navWallet = findViewById<LinearLayout>(R.id.navWallet)
        val navMessages = findViewById<LinearLayout>(R.id.navMessages)
        val statusText = findViewById<TextView>(R.id.statusText)

        menuButton.setOnClickListener {
            startActivity(Intent(this, SideMenuActivity::class.java).apply {
                putExtra("user_role", "driver")
                putExtra("selected_menu", "home")
            })
        }

        closeButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        goOnlineButton.setOnClickListener {
            statusText.text = "You are online"
            Toast.makeText(this, "Driver is now online", Toast.LENGTH_SHORT).show()
        }

        motoOption.setOnClickListener {
            Toast.makeText(this, "Moto selected", Toast.LENGTH_SHORT).show()
        }

        cabOption.setOnClickListener {
            Toast.makeText(this, "Cab selected", Toast.LENGTH_SHORT).show()
        }

        deliveryOption.setOnClickListener {
            Toast.makeText(this, "Delivery selected", Toast.LENGTH_SHORT).show()
        }

        navHome.setOnClickListener {
            Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show()
        }

        navWallet.setOnClickListener {
            startActivity(Intent(this, DriverWalletActivity::class.java).apply {
                putExtra("user_role", "driver")
                putExtra("selected_tab", "wallet")
            })
        }

        navMessages.setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java).apply {
                putExtra("user_role", "driver")
                putExtra("selected_tab", "messages")
            })
        }
    }
}