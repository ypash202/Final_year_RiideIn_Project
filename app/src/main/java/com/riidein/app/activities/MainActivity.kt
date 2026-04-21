package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        } else {
            startActivity(Intent(this, CustomerHomeActivity::class.java))
            finish()
        }
    }
}