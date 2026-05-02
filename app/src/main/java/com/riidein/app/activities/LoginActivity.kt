package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailEdit = findViewById<EditText>(R.id.emailEdit)
        val passwordEdit = findViewById<EditText>(R.id.passwordEdit)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signupLink = findViewById<TextView>(R.id.signupLink)
        val forgotPasswordLink = findViewById<TextView>(R.id.forgotPasswordLink)

        val registeredEmail = intent.getStringExtra("registered_email")
        if (!registeredEmail.isNullOrEmpty()) {
            emailEdit.setText(registeredEmail)
        }

        loginButton.setOnClickListener {
            val email = emailEdit.text.toString().trim()
            val password = passwordEdit.text.toString().trim()

            if (email.isEmpty()) {
                emailEdit.error = "Enter your email"
                emailEdit.requestFocus()
                Toast.makeText(
                    this,
                    "Please enter your email address.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEdit.error = "Enter a valid email"
                emailEdit.requestFocus()
                Toast.makeText(
                    this,
                    "Please enter a valid email address.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordEdit.error = "Enter your password"
                passwordEdit.requestFocus()
                Toast.makeText(
                    this,
                    "Please enter your password.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val currentUser = auth.currentUser

                        if (currentUser != null) {
                            val uid = currentUser.uid

                            db.collection("users").document(uid).get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        val role = document.getString("role")
                                            ?.trim()
                                            ?.lowercase()
                                            ?: ""

                                        val profileCompleted =
                                            document.getBoolean("profileCompleted") ?: false

                                        when (role) {
                                            "customer" -> {
                                                Toast.makeText(
                                                    this,
                                                    "Login Successful",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                startActivity(
                                                    Intent(
                                                        this,
                                                        CustomerHomeActivity::class.java
                                                    )
                                                )
                                                finish()
                                            }

                                            "driver" -> {
                                                if (profileCompleted) {
                                                    Toast.makeText(
                                                        this,
                                                        "Login Successful",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    startActivity(
                                                        Intent(
                                                            this,
                                                            DriverHomeActivity::class.java
                                                        )
                                                    )
                                                } else {
                                                    startActivity(
                                                        Intent(
                                                            this,
                                                            DriverLicenceNumberActivity::class.java
                                                        )
                                                    )
                                                }
                                                finish()
                                            }

                                            else -> {
                                                Toast.makeText(
                                                    this,
                                                    "Unable to identify your account type. Please contact support.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Account details were not found. Please contact support.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        this,
                                        "Unable to load your account details. Please try again.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        } else {
                            Toast.makeText(
                                this,
                                "Unable to sign in. Please try again.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Email or password is incorrect. Please check and try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        forgotPasswordLink.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        signupLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}