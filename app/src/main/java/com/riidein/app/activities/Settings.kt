package com.riidein.app.activities

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.riidein.app.R
import java.io.File

class Settings : AppCompatActivity() {

    private var userRole: String = "customer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        userRole = intent.getStringExtra("user_role")?.trim()?.lowercase() ?: "customer"

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        val itemMyAccount = findViewById<RelativeLayout>(R.id.itemMyAccount)
        val itemSecurity = findViewById<RelativeLayout>(R.id.itemSecurity)

        backButton.setOnClickListener {
            openSideMenu()
        }

        closeButton.setOnClickListener {
            openCorrectHome()
        }

        itemMyAccount.setOnClickListener {
            val intent = Intent(this, MyAccountActivity::class.java)
            intent.putExtra("user_role", userRole)
            startActivity(intent)
        }

        itemSecurity.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            intent.putExtra("user_role", userRole)
            startActivity(intent)
        }

        setRowClick("itemLanguage") {
            showLanguageDialog()
        }

        setRowClick("itemClearCache") {
            showClearCacheDialog()
        }

        setRowClick(
            "itemTermsConditions",
            "itemTermsAndConditions",
            "itemTerms"
        ) {
            showTermsDialog()
        }

        setRowClick(
            "itemContactUs",
            "itemContact",
            "itemContactUsStar"
        ) {
            showContactDialog()
        }
    }

    private fun setRowClick(vararg possibleIds: String, action: () -> Unit) {
        for (idName in possibleIds) {
            val id = resources.getIdentifier(idName, "id", packageName)

            if (id != 0) {
                val row = findViewById<View>(id)

                if (row != null) {
                    row.setOnClickListener {
                        action()
                    }
                    return
                }
            }
        }
    }

    private fun showLanguageDialog() {
        AlertDialog.Builder(this)
            .setTitle("Language")
            .setMessage(
                "English is currently selected.\n\n" +
                        "Nepali language support can be added in a future version."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showClearCacheDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Cache")
            .setMessage(
                "This will clear temporary app and map cache data.\n\n" +
                        "Your account, profile, messages, ride history, and Firestore data will not be deleted."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear") { _, _ ->
                clearAppCache()
            }
            .show()
    }

    private fun clearAppCache() {
        try {
            WebStorage.getInstance().deleteAllData()

            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()

            deleteDirectory(cacheDir)

            Toast.makeText(
                this,
                "Cache cleared successfully",
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Failed to clear cache",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun deleteDirectory(directory: File?): Boolean {
        if (directory == null || !directory.exists()) {
            return true
        }

        val files = directory.listFiles()

        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
        }

        return directory.delete()
    }

    private fun showTermsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Terms & Conditions")
            .setMessage(
                "By using Riide In, customers and drivers agree to use the app responsibly.\n\n" +
                        "Ride details, profile information, cancellation records, messages, and safety alerts may be stored to support app functionality.\n\n" +
                        "Users should provide accurate information and should not misuse the ride request, cancellation, chat, or SOS features.\n\n" +
                        "This prototype is developed for academic demonstration purposes."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showContactDialog() {
        AlertDialog.Builder(this)
            .setTitle("Contact us")
            .setMessage("Visit our website for support:\n\nwww.riidein.com")
            .setNegativeButton("Close", null)
            .setPositiveButton("Open Website") { _, _ ->
                openWebsite()
            }
            .show()
    }

    private fun openWebsite() {
        val websiteIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.riidein.com")
        )

        try {
            startActivity(websiteIntent)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Unable to open website",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openSideMenu() {
        val intent = Intent(this, SideMenuActivity::class.java)
        intent.putExtra("user_role", userRole)
        intent.putExtra("selected_menu", "settings")
        startActivity(intent)
        finish()
    }

    private fun openCorrectHome() {
        val intent = if (userRole == "driver") {
            Intent(this, DriverHomeActivity::class.java).apply {
                putExtra("user_role", "driver")
                putExtra("selected_menu", "home")
            }
        } else {
            Intent(this, CustomerHomeActivity::class.java).apply {
                putExtra("user_role", "customer")
                putExtra("selected_menu", "home")
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}