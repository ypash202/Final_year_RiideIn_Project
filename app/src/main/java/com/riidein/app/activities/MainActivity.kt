package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.riidein.app.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            // User not logged in → go to onboarding
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        } else {
            // User logged in → stay in main screen
            setContentView(R.layout.activity_main)

            // Logout button
            val logoutButton = findViewById<Button>(R.id.logoutButton)

            logoutButton.setOnClickListener {
                FirebaseAuth.getInstance().signOut()

                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }
}