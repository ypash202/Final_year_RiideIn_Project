package com.riidein.app.activities

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.riidein.app.R

class chat_detail : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_detail)

        val driverName = intent.getStringExtra("driver_name") ?: "Binod"

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val driverNameText = findViewById<TextView>(R.id.driverNameText)
        val profileImgTop = findViewById<ImageView>(R.id.profileImgTop)
        val driverMessageAvatar1 = findViewById<ImageView>(R.id.driverMessageAvatar1)
        val driverMessageAvatar2 = findViewById<ImageView>(R.id.driverMessageAvatar2)

        driverNameText.text = driverName

        if (driverName.equals("Sulav", ignoreCase = true)) {
            profileImgTop.setImageResource(R.drawable.profile2)
            driverMessageAvatar1.setImageResource(R.drawable.profile2)
            driverMessageAvatar2.setImageResource(R.drawable.profile2)
        } else {
            profileImgTop.setImageResource(R.drawable.profile1)
            driverMessageAvatar1.setImageResource(R.drawable.profile1)
            driverMessageAvatar2.setImageResource(R.drawable.profile1)
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}