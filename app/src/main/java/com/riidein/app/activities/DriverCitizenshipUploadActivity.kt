package com.riidein.app.activities

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.riidein.app.R

class DriverCitizenshipUploadActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var nextButton: Button

    private lateinit var frontPreviewImage: ImageView
    private lateinit var backPreviewImage: ImageView

    private lateinit var frontChooseButton: Button
    private lateinit var frontCameraButton: Button
    private lateinit var backChooseButton: Button
    private lateinit var backCameraButton: Button

    private var frontImageUri: Uri? = null
    private var backImageUri: Uri? = null
    private var selectingFrontImage = true

    private val frontGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                frontImageUri = uri
                frontPreviewImage.setImageURI(uri)
                frontPreviewImage.imageTintList = null
                Toast.makeText(this, "Front image selected", Toast.LENGTH_SHORT).show()
            }
        }

    private val backGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                backImageUri = uri
                backPreviewImage.setImageURI(uri)
                backPreviewImage.imageTintList = null
                Toast.makeText(this, "Back image selected", Toast.LENGTH_SHORT).show()
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val bitmap = result.data?.extras?.get("data") as? Bitmap
                if (bitmap != null) {
                    if (selectingFrontImage) {
                        frontImageUri = getImageUri(bitmap)
                        frontPreviewImage.setImageBitmap(bitmap)
                        frontPreviewImage.imageTintList = null
                        Toast.makeText(this, "Front photo captured", Toast.LENGTH_SHORT).show()
                    } else {
                        backImageUri = getImageUri(bitmap)
                        backPreviewImage.setImageBitmap(bitmap)
                        backPreviewImage.imageTintList = null
                        Toast.makeText(this, "Back photo captured", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_citizenship_upload)

        backButton = findViewById(R.id.backButton)
        closeButton = findViewById(R.id.closeButton)
        nextButton = findViewById(R.id.nextButton)

        frontPreviewImage = findViewById(R.id.frontPreviewImage)
        backPreviewImage = findViewById(R.id.backPreviewImage)

        frontChooseButton = findViewById(R.id.frontChooseButton)
        frontCameraButton = findViewById(R.id.frontCameraButton)
        backChooseButton = findViewById(R.id.backChooseButton)
        backCameraButton = findViewById(R.id.backCameraButton)

        backButton.setOnClickListener {
            finish()
        }

        closeButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        frontChooseButton.setOnClickListener {
            frontGalleryLauncher.launch("image/*")
        }

        backChooseButton.setOnClickListener {
            backGalleryLauncher.launch("image/*")
        }

        frontCameraButton.setOnClickListener {
            selectingFrontImage = true
            openCamera()
        }

        backCameraButton.setOnClickListener {
            selectingFrontImage = false
            openCamera()
        }

        nextButton.setOnClickListener {
            if (frontImageUri == null) {
                Toast.makeText(this, "Please upload front side of citizenship", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (backImageUri == null) {
                Toast.makeText(this, "Please upload back side of citizenship", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startActivity(Intent(this, DriverPhotoUploadActivity::class.java))
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
                "Citizenship_Image",
                null
            )
            if (path != null) Uri.parse(path) else null
        } catch (e: Exception) {
            null
        }
    }
}