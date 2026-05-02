package com.riidein.app.activities

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R
import com.riidein.app.utils.ProfileImageHelper

class MyAccountActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var userRole: String = "customer"

    private lateinit var backButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var profileImage: ImageView
    private lateinit var changePhotoButton: TextView
    private lateinit var profileNameText: TextView
    private lateinit var profileRoleText: TextView

    private lateinit var levelValueText: TextView
    private lateinit var nameValueText: TextView
    private lateinit var phoneValueText: TextView
    private lateinit var emailValueText: TextView
    private lateinit var birthdayValueText: TextView

    private lateinit var itemName: RelativeLayout
    private lateinit var itemPhoneNumber: RelativeLayout
    private lateinit var itemEmail: RelativeLayout
    private lateinit var itemBirthday: RelativeLayout

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                saveSelectedImage(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_account)

        userRole = intent.getStringExtra("user_role")?.trim()?.lowercase() ?: "customer"

        bindViews()
        setupClicks()
        loadUserProfile()
    }

    private fun bindViews() {
        backButton = findViewById(R.id.backButton)
        closeButton = findViewById(R.id.closeButton)
        profileImage = findViewById(R.id.profileImage)
        changePhotoButton = findViewById(R.id.changePhotoButton)
        profileNameText = findViewById(R.id.profileNameText)
        profileRoleText = findViewById(R.id.profileRoleText)

        levelValueText = findViewById(R.id.levelValueText)
        nameValueText = findViewById(R.id.nameValueText)
        phoneValueText = findViewById(R.id.phoneValueText)
        emailValueText = findViewById(R.id.emailValueText)
        birthdayValueText = findViewById(R.id.birthdayValueText)

        itemName = findViewById(R.id.itemName)
        itemPhoneNumber = findViewById(R.id.itemPhoneNumber)
        itemEmail = findViewById(R.id.itemEmail)
        itemBirthday = findViewById(R.id.itemBirthday)
    }

    private fun setupClicks() {
        backButton.setOnClickListener {
            finish()
        }

        closeButton.setOnClickListener {
            openCorrectHome()
        }

        profileImage.setOnClickListener {
            openImagePicker()
        }

        changePhotoButton.setOnClickListener {
            openImagePicker()
        }

        itemName.setOnClickListener {
            showEditDialog(
                title = "Edit Name",
                currentValue = nameValueText.text.toString(),
                fieldName = "name",
                hint = "Enter your full name"
            )
        }

        itemEmail.setOnClickListener {
            showEditDialog(
                title = "Edit Email",
                currentValue = emailValueText.text.toString(),
                fieldName = "email",
                hint = "Enter your email",
                inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            )
        }

        itemBirthday.setOnClickListener {
            showEditDialog(
                title = "Edit Birthday",
                currentValue = birthdayValueText.text.toString(),
                fieldName = "birthday",
                hint = "Example: 2002-04-12"
            )
        }

        itemPhoneNumber.setOnClickListener {
            Toast.makeText(
                this,
                "Phone number cannot be changed for security",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch(arrayOf("image/*"))
    }

    private fun saveSelectedImage(uri: Uri) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            openLogin()
            return
        }

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Some devices may not need persistable permission.
        }

        val uriString = uri.toString()
        profileImage.setImageURI(uri)

        val base64Image = ProfileImageHelper.uriToBase64(
            context = this,
            uriString = uriString
        )

        if (base64Image.isBlank()) {
            Toast.makeText(
                this,
                "Failed to process profile picture",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val updates = mapOf(
            "profileImageUri" to uriString,
            "profileImageBase64" to base64Image,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(currentUser.uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save profile picture", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            openLogin()
            return
        }

        val roleText = if (userRole == "driver") "Driver" else "Customer"
        profileRoleText.text = roleText
        levelValueText.text = roleText

        db.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                val name = firstAvailableText(
                    document.getString("name"),
                    document.getString("fullName"),
                    currentUser.displayName,
                    "User"
                )

                val phone = firstAvailableText(
                    document.getString("phone"),
                    document.getString("phoneNumber"),
                    currentUser.phoneNumber,
                    "Not added"
                )

                val email = firstAvailableText(
                    document.getString("email"),
                    currentUser.email,
                    "Not added"
                )

                val birthday = firstAvailableText(
                    document.getString("birthday"),
                    document.getString("dob"),
                    document.getString("dateOfBirth"),
                    "Not added"
                )

                val profileImageBase64 = document.getString("profileImageBase64") ?: ""

                val profileImageUri = firstAvailableText(
                    document.getString("profileImageUri"),
                    document.getString("profilePhotoUri"),
                    document.getString("profilePhotoUrl"),
                    document.getString("driverPhotoUrl"),
                    ""
                )

                profileNameText.text = name
                nameValueText.text = name
                phoneValueText.text = phone
                emailValueText.text = email
                birthdayValueText.text = birthday

                ProfileImageHelper.loadProfileImage(
                    imageView = profileImage,
                    base64Image = profileImageBase64,
                    uriString = profileImageUri,
                    fallbackRes = R.drawable.profile2
                )
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun firstAvailableText(vararg values: String?): String {
        for (value in values) {
            if (!value.isNullOrBlank()) {
                return value
            }
        }

        return ""
    }

    private fun showEditDialog(
        title: String,
        currentValue: String,
        fieldName: String,
        hint: String,
        inputType: Int = InputType.TYPE_CLASS_TEXT
    ) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            openLogin()
            return
        }

        val editText = EditText(this)
        editText.setText(if (currentValue == "Not added") "" else currentValue)
        editText.hint = hint
        editText.inputType = inputType
        editText.setSingleLine(true)
        editText.setSelection(editText.text.length)

        val container = FrameLayout(this)
        val padding = 48
        container.setPadding(padding, 16, padding, 0)
        container.addView(editText)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val newValue = editText.text.toString().trim()

                if (newValue.isEmpty()) {
                    Toast.makeText(this, "Field cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                updateFirestoreField(currentUser.uid, fieldName, newValue)
            }
            .show()
    }

    private fun updateFirestoreField(uid: String, fieldName: String, newValue: String) {
        db.collection("users")
            .document(uid)
            .update(fieldName, newValue)
            .addOnSuccessListener {
                updateLocalText(fieldName, newValue)
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateLocalText(fieldName: String, newValue: String) {
        when (fieldName) {
            "name" -> {
                profileNameText.text = newValue
                nameValueText.text = newValue
            }

            "email" -> {
                emailValueText.text = newValue
            }

            "birthday" -> {
                birthdayValueText.text = newValue
            }
        }
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