package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.riidein.app.R
import java.util.concurrent.TimeUnit

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPasswordInput)

        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginLink = findViewById<TextView>(R.id.loginLink)
        val roleGroup = findViewById<RadioGroup>(R.id.roleGroup)

        registerButton.setOnClickListener {

            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            val selectedRoleId = roleGroup.checkedRadioButtonId

            if (selectedRoleId == -1) {
                Toast.makeText(this, "Select role", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val role = if (selectedRoleId == R.id.customerOption) "customer" else "driver"

            // VALIDATION
            if (name.isEmpty()) {
                nameInput.error = "Enter name"
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                emailInput.error = "Enter email"
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Enter a valid email address"
                return@setOnClickListener
            }

            if (!email.endsWith("@gmail.com", ignoreCase = true)) {
                emailInput.error = "Email must end with @gmail.com"
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                phoneInput.error = "Enter phone"
                return@setOnClickListener
            }
            if (!phone.all { it.isDigit() }) {
                phoneInput.error = "Phone number must contain only digits"
                return@setOnClickListener
            }

            if (phone.length != 10) {
                phoneInput.error = "Phone number must be 10 digits"
                return@setOnClickListener
            }

            if (!phone.startsWith("98") && !phone.startsWith("97")) {
                phoneInput.error = "Enter a valid Nepal phone number"
                return@setOnClickListener
            }

            val passwordRegex = Regex("^(?=.*[!@#\$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#\$%^&*(),.?\":{}|<>]{8,}$")


            if (password.isEmpty()) {
                passwordInput.error = "Enter password"
                return@setOnClickListener
            }

            if (!passwordRegex.matches(password)) {
                passwordInput.error = "Password must be at least 8 characters and include a special character"
                passwordInput.requestFocus()
                Toast.makeText(
                    this,
                    "Password must be at least 8 characters and include a special character",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                confirmPasswordInput.error = "Passwords do not match"
                return@setOnClickListener
            }

            val phoneNumber = "+977$phone"

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {}

                    override fun onVerificationFailed(e: FirebaseException) {
                        Toast.makeText(this@RegisterActivity, "OTP Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        Toast.makeText(this@RegisterActivity, "OTP Sent 📩", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@RegisterActivity, OtpActivity::class.java)
                        intent.putExtra("verificationId", verificationId)
                        intent.putExtra("name", name)
                        intent.putExtra("email", email)
                        intent.putExtra("phone", phone)
                        intent.putExtra("password", password)
                        intent.putExtra("role", role)

                        startActivity(intent)
                    }
                })
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }

        loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}