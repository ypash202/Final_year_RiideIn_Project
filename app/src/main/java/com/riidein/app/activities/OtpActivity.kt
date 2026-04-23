package com.riidein.app.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R

class OtpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var verificationId: String? = null

    private var name: String = ""
    private var email: String = ""
    private var phone: String = ""
    private var password: String = ""
    private var role: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        verificationId = intent.getStringExtra("verificationId")
        name = intent.getStringExtra("name") ?: ""
        email = intent.getStringExtra("email") ?: ""
        phone = intent.getStringExtra("phone") ?: ""
        password = intent.getStringExtra("password") ?: ""
        role = intent.getStringExtra("role") ?: ""

        val resendText = findViewById<TextView>(R.id.resendText)
        val verifyButton = findViewById<Button>(R.id.verifyButton)

        val fullText = "Not received code? Send again"
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf("Send again")
        val end = start + "Send again".length

        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#63B7C4")),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        resendText.text = spannable

        verifyButton.setOnClickListener {
            val otp = getOtpFromBoxes()

            if (otp.length != 6) {
                Toast.makeText(this, "Enter full OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (verificationId.isNullOrEmpty()) {
                Toast.makeText(this, "Verification ID missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val phoneCredential = PhoneAuthProvider.getCredential(verificationId!!, otp)

            auth.signInWithCredential(phoneCredential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        linkEmailPasswordAndSaveUser()
                    } else {
                        val message = if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            "Invalid OTP"
                        } else {
                            task.exception?.message ?: "OTP verification failed"
                        }
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun getOtpFromBoxes(): String {
        val otp1 = findViewById<EditText>(R.id.otp1).text.toString().trim()
        val otp2 = findViewById<EditText>(R.id.otp2).text.toString().trim()
        val otp3 = findViewById<EditText>(R.id.otp3).text.toString().trim()
        val otp4 = findViewById<EditText>(R.id.otp4).text.toString().trim()
        val otp5 = findViewById<EditText>(R.id.otp5).text.toString().trim()
        val otp6 = findViewById<EditText>(R.id.otp6).text.toString().trim()
        return otp1 + otp2 + otp3 + otp4 + otp5 + otp6
    }

    private fun linkEmailPasswordAndSaveUser() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "User not found after OTP verification", Toast.LENGTH_LONG).show()
            return
        }

        val emailCredential = EmailAuthProvider.getCredential(email, password)

        currentUser.linkWithCredential(emailCredential)
            .addOnCompleteListener { linkTask ->
                if (linkTask.isSuccessful) {
                    saveUserToFirestore()
                } else {
                    Toast.makeText(
                        this,
                        "Account linking failed: ${linkTask.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun saveUserToFirestore() {
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            Toast.makeText(this, "No authenticated user found", Toast.LENGTH_LONG).show()
            return
        }

        val cleanRole = role.trim().lowercase()

        val userMap = hashMapOf(
            "uid" to firebaseUser.uid,
            "name" to name,
            "email" to email,
            "phone" to phone,
            "role" to cleanRole,
            "profileCompleted" to false,
            "isAvailable" to false,
            "licenceNumber" to "",
            "vehicleType" to "",
            "vehicleNumber" to "",
            "citizenshipImageUrl" to "",
            "driverPhotoUrl" to ""
        )

        db.collection("users")
            .document(firebaseUser.uid)
            .set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_LONG).show()

                if (cleanRole == "driver") {
                    val intent = Intent(this, DriverLicenceNumberActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.putExtra("registered_email", email)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}