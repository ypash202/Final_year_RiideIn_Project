package com.riidein.app.activities

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R

class VehicleSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_selection)

        val fromLocation = intent.getStringExtra("from_location") ?: ""
        val toLocation = intent.getStringExtra("to_location") ?: ""

        val fromLocationText = findViewById<TextView>(R.id.fromLocationText)
        val toLocationText = findViewById<TextView>(R.id.toLocationText)
        val customerNameText = findViewById<TextView>(R.id.customerNameText)

        fromLocationText.text = fromLocation
        toLocationText.text = toLocation

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val customerName = document.getString("name") ?: ""
                        customerNameText.text = customerName
                    }
                }
        }
    }
}