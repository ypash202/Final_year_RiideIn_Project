package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.riidein.app.R

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var backButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var oldPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var changePasswordButton: Button

    private var userRole: String = "customer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        auth = FirebaseAuth.getInstance()
        userRole = intent.getStringExtra("user_role")?.trim()?.lowercase() ?: "customer"

        bindViews()
        setupInputs()
        setupClicks()
    }

    private fun bindViews() {
        backButton = findViewById(R.id.backButton)
        closeButton = findViewById(R.id.closeButton)
        oldPasswordEditText = findViewById(R.id.oldPasswordEditText)
        newPasswordEditText = findViewById(R.id.newPasswordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        changePasswordButton = findViewById(R.id.changePasswordButton)
    }

    private fun setupInputs() {
        oldPasswordEditText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        newPasswordEditText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        confirmPasswordEditText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    private fun setupClicks() {
        backButton.setOnClickListener {
            openSettingsPage()
        }

        closeButton.setOnClickListener {
            openCorrectHome()
        }

        changePasswordButton.setOnClickListener {
            changePassword()
        }
    }

    private fun changePassword() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            openLogin()
            return
        }

        val email = currentUser.email

        if (email.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Password change is available only for email accounts",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val oldPassword = oldPasswordEditText.text.toString().trim()
        val newPassword = newPasswordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        if (oldPassword.isEmpty()) {
            oldPasswordEditText.error = "Enter your old password"
            oldPasswordEditText.requestFocus()
            return
        }

        if (newPassword.isEmpty()) {
            newPasswordEditText.error = "Enter your new password"
            newPasswordEditText.requestFocus()
            return
        }

        if (!isStrongPassword(newPassword)) {
            newPasswordEditText.error =
                "Password must be at least 8 characters and include 1 special character"
            newPasswordEditText.requestFocus()

            Toast.makeText(
                this,
                "New password must be at least 8 characters and include 1 special character",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (newPassword == oldPassword) {
            newPasswordEditText.error = "New password must be different from old password"
            newPasswordEditText.requestFocus()
            return
        }

        if (confirmPassword != newPassword) {
            confirmPasswordEditText.error = "Passwords do not match"
            confirmPasswordEditText.requestFocus()
            return
        }

        setLoading(true)

        val credential = EmailAuthProvider.getCredential(email, oldPassword)

        currentUser.reauthenticate(credential)
            .addOnSuccessListener {
                currentUser.updatePassword(newPassword)
                    .addOnSuccessListener {
                        setLoading(false)

                        Toast.makeText(
                            this,
                            "Password changed successfully. Please login again.",
                            Toast.LENGTH_LONG
                        ).show()

                        auth.signOut()
                        openLogin()
                    }
                    .addOnFailureListener {
                        setLoading(false)

                        Toast.makeText(
                            this,
                            "Failed to update password. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener {
                setLoading(false)

                oldPasswordEditText.error = "Your old password is mistaken"
                oldPasswordEditText.requestFocus()

                Toast.makeText(
                    this,
                    "Your old password is mistaken",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun isStrongPassword(password: String): Boolean {
        val hasMinimumLength = password.length >= 8
        val hasSpecialCharacter = password.any { character ->
            !character.isLetterOrDigit()
        }

        return hasMinimumLength && hasSpecialCharacter
    }

    private fun setLoading(isLoading: Boolean) {
        changePasswordButton.isEnabled = !isLoading
        changePasswordButton.text = if (isLoading) {
            "Changing..."
        } else {
            "Change Password"
        }
    }

    private fun openSettingsPage() {
        val intent = Intent(this, Settings::class.java)
        intent.putExtra("user_role", userRole)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun openCorrectHome() {
        val intent = if (userRole == "driver") {
            Intent(this, DriverHomeActivity::class.java).apply {
                putExtra("user_role", "driver")
            }
        } else {
            Intent(this, CustomerHomeActivity::class.java).apply {
                putExtra("user_role", "customer")
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}