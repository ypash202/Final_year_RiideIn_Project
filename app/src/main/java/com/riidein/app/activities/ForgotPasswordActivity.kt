package com.riidein.app.activities

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.riidein.app.R

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEdit: EditText
    private lateinit var sendResetButton: Button
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        emailEdit = findViewById(R.id.emailEdit)
        sendResetButton = findViewById(R.id.sendResetButton)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }

        sendResetButton.setOnClickListener {
            val email = emailEdit.text.toString().trim()

            if (email.isEmpty()) {
                emailEdit.error = "Please enter your email"
                emailEdit.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEdit.error = "Enter a valid email"
                emailEdit.requestFocus()
                return@setOnClickListener
            }

            sendResetButton.isEnabled = false

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    sendResetButton.isEnabled = true

                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "If this email is registered, a reset link has been sent. Check inbox, spam, and junk folder.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Reset failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}