package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.riidein.app.R

class MessagesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        val backButton = findViewById<ImageButton>(R.id.backButton)

        val selectedDriverRow = findViewById<LinearLayout>(R.id.selectedDriverRow)
        val selectedDriverImage = findViewById<ImageView>(R.id.selectedDriverImage)
        val selectedDriverNameText = findViewById<TextView>(R.id.selectedDriverNameText)
        val selectedDriverMessageText = findViewById<TextView>(R.id.selectedDriverMessageText)

        val staticBinodRow = findViewById<LinearLayout>(R.id.staticBinodRow)
        val staticSulavRow = findViewById<LinearLayout>(R.id.staticSulavRow)

        val driverName = intent.getStringExtra("driver_name") ?: "Binod"

        selectedDriverNameText.text = driverName
        selectedDriverMessageText.text = "I am on the way."

        if (driverName.equals("Sulav", ignoreCase = true)) {
            selectedDriverImage.setImageResource(R.drawable.profile2)
        } else {
            selectedDriverImage.setImageResource(R.drawable.profile1)
        }

        backButton.setOnClickListener {
            val intent = Intent(this, CustomerHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        selectedDriverRow.setOnClickListener {
            openChatDetail(driverName)
        }

        staticBinodRow.setOnClickListener {
            openChatDetail("Binod")
        }

        staticSulavRow.setOnClickListener {
            openChatDetail("Sulav")
        }
    }

    private fun openChatDetail(driverName: String) {
        val intent = Intent(this, chat_detail::class.java)
        intent.putExtra("driver_name", driverName)
        startActivity(intent)
    }
}