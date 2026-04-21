package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.riidein.app.R

class SideMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_side_menu)

        val backButton: ImageButton = findViewById(R.id.backButton)

        val menuHome: LinearLayout = findViewById(R.id.menuHome)
        val menuWallet: LinearLayout = findViewById(R.id.menuWallet)
        val menuHistory: LinearLayout = findViewById(R.id.menuHistory)
        val menuNotifications: LinearLayout = findViewById(R.id.menuNotifications)
        val menuInvite: LinearLayout = findViewById(R.id.menuInvite)
        val menuSettings: LinearLayout = findViewById(R.id.menuSettings)
        val menuLogout: LinearLayout = findViewById(R.id.menuLogout)

        backButton.setOnClickListener {
            finish()
        }

        menuHome.setOnClickListener {
            val intent = Intent(this, CustomerHomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        menuWallet.setOnClickListener {
            Toast.makeText(this, "Wallet page coming soon", Toast.LENGTH_SHORT).show()
        }

        menuHistory.setOnClickListener {
            Toast.makeText(this, "History page coming soon", Toast.LENGTH_SHORT).show()
        }

        menuNotifications.setOnClickListener {
            Toast.makeText(this, "Notifications page coming soon", Toast.LENGTH_SHORT).show()
        }

        menuInvite.setOnClickListener {
            Toast.makeText(this, "Invite page coming soon", Toast.LENGTH_SHORT).show()
        }

        menuSettings.setOnClickListener {
            Toast.makeText(this, "Settings page coming soon", Toast.LENGTH_SHORT).show()
        }

        menuLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}