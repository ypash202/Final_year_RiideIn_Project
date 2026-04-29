package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.riidein.app.R

class Settings : AppCompatActivity() {

    private var userRole: String = "customer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        userRole = intent.getStringExtra("user_role")?.trim()?.lowercase() ?: "customer"

        val backButton = findViewById<ImageButton?>(R.id.backButton)

        backButton?.setOnClickListener {
            openSideMenu()
        }
    }

    private fun openSideMenu() {
        val intent = Intent(this, SideMenuActivity::class.java)
        intent.putExtra("user_role", userRole)
        intent.putExtra("selected_menu", "settings")
        startActivity(intent)
        finish()
    }
}