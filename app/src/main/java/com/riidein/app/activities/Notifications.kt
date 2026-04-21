package com.riidein.app.activities

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.riidein.app.R

class NotificationsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val deleteButton = findViewById<ImageButton>(R.id.deleteButton)

        backButton.setOnClickListener {
            finish()
        }

        deleteButton.setOnClickListener {
            finish()
        }
    }
}