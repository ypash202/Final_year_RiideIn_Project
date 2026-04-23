package com.riidein.app.activities

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R

class DriverPhotoUploadActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var backButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var chooseLibraryCard: LinearLayout
    private lateinit var takePhotoCard: LinearLayout
    private lateinit var nextButton: Button
    private lateinit var profilePreviewImage: ImageView

    private var driverPhotoUri: Uri? = null

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                driverPhotoUri = uri
                profilePreviewImage.setImageURI(uri)
                profilePreviewImage.imageTintList = null
                Toast.makeText(this, "Photo selected successfully", Toast.LENGTH_SHORT).show()
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val bitmap = result.data?.extras?.get("data") as? Bitmap
                if (bitmap != null) {
                    driverPhotoUri = getImageUri(bitmap)
                    profilePreviewImage.setImageBitmap(bitmap)
                    profilePreviewImage.imageTintList = null
                    Toast.makeText(this, "Photo captured successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_photo_upload)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        backButton = findViewById(R.id.backButton)
        closeButton = findViewById(R.id.closeButton)
        chooseLibraryCard = findViewById(R.id.chooseLibraryCard)
        takePhotoCard = findViewById(R.id.takePhotoCard)
        nextButton = findViewById(R.id.nextButton)
        profilePreviewImage = findViewById(R.id.profilePreviewImage)

        backButton.setOnClickListener {
            finish()
        }

        closeButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        chooseLibraryCard.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        takePhotoCard.setOnClickListener {
            openCamera()
        }

        nextButton.setOnClickListener {
            completeDriverVerification()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun getImageUri(bitmap: Bitmap): Uri? {
        return try {
            val path = MediaStore.Images.Media.insertImage(
                contentResolver,
                bitmap,
                "Driver_Profile_Photo",
                null
            )
            if (path != null) Uri.parse(path) else null
        } catch (e: Exception) {
            null
        }
    }

    private fun completeDriverVerification() {
        if (driverPhotoUri == null) {
            Toast.makeText(this, "Please upload your photo first", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
            return
        }

        nextButton.isEnabled = false

        val updates = hashMapOf<String, Any>(
            "driverPhotoUrl" to driverPhotoUri.toString(),
            "profileCompleted" to true,
            "isVerified" to true,
            "verificationStatus" to "approved"
        )

        db.collection("users")
            .document(currentUser.uid)
            .update(updates)
            .addOnSuccessListener {
                nextButton.isEnabled = true
                startActivity(Intent(this, VerificationCompleteActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                nextButton.isEnabled = true
                Toast.makeText(
                    this,
                    "Failed to complete verification: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}