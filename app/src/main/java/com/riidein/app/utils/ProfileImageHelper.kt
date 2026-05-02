package com.riidein.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import androidx.core.graphics.scale
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream

object ProfileImageHelper {

    fun uriToBase64(
        context: Context,
        uriString: String,
        maxSize: Int = 256,
        quality: Int = 70
    ): String {
        return try {
            val uri = uriString.toUri()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return ""
            val resizedBitmap = resizeBitmap(originalBitmap, maxSize)

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (_: Exception) {
            ""
        }
    }

    fun loadBase64IntoImageView(
        imageView: ImageView,
        base64Image: String?,
        fallbackRes: Int
    ) {
        if (base64Image.isNullOrBlank()) {
            imageView.setImageResource(fallbackRes)
            return
        }

        try {
            val cleanBase64 = base64Image
                .replace("data:image/jpeg;base64,", "")
                .replace("data:image/png;base64,", "")
                .trim()

            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageResource(fallbackRes)
            }
        } catch (_: Exception) {
            imageView.setImageResource(fallbackRes)
        }
    }

    fun loadUriIntoImageView(
        imageView: ImageView,
        uriString: String?,
        fallbackRes: Int
    ) {
        if (uriString.isNullOrBlank()) {
            imageView.setImageResource(fallbackRes)
            return
        }

        try {
            imageView.setImageURI(uriString.toUri())
        } catch (_: Exception) {
            imageView.setImageResource(fallbackRes)
        }
    }

    fun loadProfileImage(
        imageView: ImageView,
        base64Image: String?,
        uriString: String?,
        fallbackRes: Int
    ) {
        if (!base64Image.isNullOrBlank()) {
            loadBase64IntoImageView(imageView, base64Image, fallbackRes)
        } else {
            loadUriIntoImageView(imageView, uriString, fallbackRes)
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (ratio > 1f) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        return bitmap.scale(newWidth, newHeight)
    }
}