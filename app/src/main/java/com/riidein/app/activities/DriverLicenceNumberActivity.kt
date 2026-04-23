package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R

class DriverLicenceNumberActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var backButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var nextButton: Button
    private lateinit var licenceNumberInput: EditText
    private lateinit var vehicleTypeInput: EditText
    private lateinit var vehicleNumberInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_licence_number)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        backButton = findViewById(R.id.backButton)
        closeButton = findViewById(R.id.closeButton)
        nextButton = findViewById(R.id.nextButton)
        licenceNumberInput = findViewById(R.id.licenceNumberInput)
        vehicleTypeInput = findViewById(R.id.vehicleTypeInput)
        vehicleNumberInput = findViewById(R.id.vehicleNumberInput)

        backButton.setOnClickListener {
            finish()
        }

        closeButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        nextButton.setOnClickListener {
            validateAndSaveDriverDetails()
        }
    }

    private fun validateAndSaveDriverDetails() {
        val licenceNumber = licenceNumberInput.text.toString().trim()
        val vehicleType = vehicleTypeInput.text.toString().trim()
        val vehicleNumber = vehicleNumberInput.text.toString().trim()

        if (licenceNumber.isEmpty()) {
            licenceNumberInput.error = "Enter licence number"
            licenceNumberInput.requestFocus()
            return
        }

        if (licenceNumber.length !in 11..12) {
            licenceNumberInput.error = "Licence number must be 11 or 12 characters"
            licenceNumberInput.requestFocus()
            return
        }

        if (!licenceNumber.matches(Regex("^[A-Za-z0-9]+$"))) {
            licenceNumberInput.error = "Licence number must contain only letters and numbers"
            licenceNumberInput.requestFocus()
            return
        }

        if (vehicleType.isEmpty()) {
            vehicleTypeInput.error = "Enter vehicle type"
            vehicleTypeInput.requestFocus()
            return
        }

        if (vehicleType.length < 2) {
            vehicleTypeInput.error = "Enter a valid vehicle type"
            vehicleTypeInput.requestFocus()
            return
        }

        if (vehicleNumber.isEmpty()) {
            vehicleNumberInput.error = "Enter vehicle number"
            vehicleNumberInput.requestFocus()
            return
        }

        if (vehicleNumber.length < 6) {
            vehicleNumberInput.error = "Enter a valid vehicle number"
            vehicleNumberInput.requestFocus()
            return
        }

        if (!vehicleNumber.matches(Regex("^[A-Za-z0-9\\- ]+$"))) {
            vehicleNumberInput.error = "Vehicle number can contain letters, numbers, spaces, and hyphen only"
            vehicleNumberInput.requestFocus()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
            return
        }

        nextButton.isEnabled = false

        val updates = hashMapOf<String, Any>(
            "licenceNumber" to licenceNumber,
            "vehicleType" to vehicleType,
            "vehicleNumber" to vehicleNumber
        )

        db.collection("users")
            .document(currentUser.uid)
            .update(updates)
            .addOnSuccessListener {
                nextButton.isEnabled = true
                startActivity(Intent(this, DriverCitizenshipUploadActivity::class.java))
            }
            .addOnFailureListener { e ->
                nextButton.isEnabled = true
                Toast.makeText(this, "Failed to save details: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}