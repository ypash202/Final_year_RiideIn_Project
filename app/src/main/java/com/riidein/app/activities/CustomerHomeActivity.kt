package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.riidein.app.R

class CustomerHomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_home)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        val searchBox = findViewById<EditText>(R.id.searchBox)
        val promoButton = findViewById<Button>(R.id.promoButton)

        backButton.setOnClickListener {
            finish()
        }

        closeButton.setOnClickListener {
            finish()
        }

        searchBox.setOnClickListener {
            startActivity(Intent(this, EnterRouteActivity::class.java))
        }

        promoButton.setOnClickListener {
            Toast.makeText(this, "Promo feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}