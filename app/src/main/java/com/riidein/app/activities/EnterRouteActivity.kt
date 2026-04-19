package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.riidein.app.R

class EnterRouteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_route)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        val fromEditText = findViewById<EditText>(R.id.fromEditText)
        val toEditText = findViewById<EditText>(R.id.toEditText)
        val nextButton = findViewById<Button>(R.id.nextButton)

        backButton.setOnClickListener {
            finish()
        }

        closeButton.setOnClickListener {
            finish()
        }

        nextButton.setOnClickListener {
            val from = fromEditText.text.toString().trim()
            val to = toEditText.text.toString().trim()

            if (from.isEmpty() || to.isEmpty()) {
                Toast.makeText(this, "Please enter both locations", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, VehicleSelectionActivity::class.java)
                intent.putExtra("from_location", from)
                intent.putExtra("to_location", to)
                startActivity(intent)
            }
        }
    }
}