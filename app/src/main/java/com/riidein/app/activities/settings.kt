package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.riidein.app.R

class settings : AppCompatActivity() {

    private var userRole: String = "customer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        userRole = intent.getStringExtra("user_role")?.trim()?.lowercase() ?: "customer"

        val backButton = findViewById<ImageButton?>(R.id.backButton)
        val closeButton = findViewById<ImageButton?>(R.id.closeButton)

        backButton?.setOnClickListener {
            openSideMenu("settings")
        }

        closeButton?.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun openSideMenu(selectedMenu: String) {
        startActivity(Intent(this, SideMenuActivity::class.java).apply {
            putExtra("user_role", userRole)
            putExtra("selected_menu", selectedMenu)
        })
        finish()
    }
}