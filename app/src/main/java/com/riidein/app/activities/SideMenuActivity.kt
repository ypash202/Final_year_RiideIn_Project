package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
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
        val driverModeButton: Button = findViewById(R.id.driverModeButton)

        backButton.setOnClickListener {
            finish()
        }

        menuHome.setOnClickListener {
            startActivity(Intent(this, CustomerHomeActivity::class.java))
            finish()
        }

        menuWallet.setOnClickListener {
            val intent = Intent(this, DriverWalletActivity::class.java)
            intent.putExtra("user_role", "customer")
            startActivity(intent)
        }

        menuHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        menuNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        menuInvite.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_friends_text))
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.invite_friends)))
        }

        menuSettings.setOnClickListener {
            startActivity(Intent(this, settings::class.java))
        }

        menuLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        driverModeButton.setOnClickListener {
            // keep for future driver mode navigation
        }
    }
}