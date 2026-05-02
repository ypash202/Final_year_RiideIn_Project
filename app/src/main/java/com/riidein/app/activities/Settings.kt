package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.riidein.app.R

class Settings : AppCompatActivity() {

    private var userRole: String = "customer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        userRole = intent.getStringExtra("user_role")?.trim()?.lowercase() ?: "customer"

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        val itemMyAccount = findViewById<RelativeLayout>(R.id.itemMyAccount)
        val itemSecurity = findViewById<RelativeLayout>(R.id.itemSecurity)

        backButton.setOnClickListener {
            openSideMenu()
        }

        closeButton.setOnClickListener {
            openCorrectHome()
        }

        itemMyAccount.setOnClickListener {
            val intent = Intent(this, MyAccountActivity::class.java)
            intent.putExtra("user_role", userRole)
            startActivity(intent)
        }

        itemSecurity.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            intent.putExtra("user_role", userRole)
            startActivity(intent)
        }
    }

    private fun openSideMenu() {
        val intent = Intent(this, SideMenuActivity::class.java)
        intent.putExtra("user_role", userRole)
        intent.putExtra("selected_menu", "settings")
        startActivity(intent)
        finish()
    }

    private fun openCorrectHome() {
        val intent = if (userRole == "driver") {
            Intent(this, DriverHomeActivity::class.java).apply {
                putExtra("user_role", "driver")
                putExtra("selected_menu", "home")
            }
        } else {
            Intent(this, CustomerHomeActivity::class.java).apply {
                putExtra("user_role", "customer")
                putExtra("selected_menu", "home")
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}