package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.riidein.app.R

class MessagesActivity : AppCompatActivity() {

    private var userRole: String = "customer"

    private lateinit var backButton: ImageButton
    private lateinit var navHome: LinearLayout
    private lateinit var navWallet: LinearLayout
    private lateinit var navMessages: LinearLayout

    private lateinit var navHomeIcon: android.widget.ImageView
    private lateinit var navWalletIcon: android.widget.ImageView
    private lateinit var navMessagesIcon: android.widget.ImageView

    private lateinit var navHomeText: TextView
    private lateinit var navWalletText: TextView
    private lateinit var navMessagesText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        userRole = intent.getStringExtra("user_role")?.trim()?.lowercase() ?: "customer"

        backButton = findViewById(R.id.backButton)
        navHome = findViewById(R.id.navHome)
        navWallet = findViewById(R.id.navWallet)
        navMessages = findViewById(R.id.navMessages)

        navHomeIcon = navHome.getChildAt(0) as android.widget.ImageView
        navHomeText = navHome.getChildAt(1) as TextView

        navWalletIcon = navWallet.getChildAt(0) as android.widget.ImageView
        navWalletText = navWallet.getChildAt(1) as TextView

        navMessagesIcon = navMessages.getChildAt(0) as android.widget.ImageView
        navMessagesText = navMessages.getChildAt(1) as TextView

        highlightSelectedTab("messages")

        backButton.setOnClickListener {
            openSideMenu()
        }

        navHome.setOnClickListener {
            highlightSelectedTab("home")
            if (userRole == "driver") {
                startActivity(Intent(this, DriverHomeActivity::class.java).apply {
                    putExtra("user_role", "driver")
                    putExtra("selected_tab", "home")
                })
            } else {
                startActivity(Intent(this, CustomerHomeActivity::class.java).apply {
                    putExtra("user_role", "customer")
                    putExtra("selected_tab", "home")
                })
            }
            finish()
        }

        navWallet.setOnClickListener {
            highlightSelectedTab("wallet")
            startActivity(Intent(this, DriverWalletActivity::class.java).apply {
                putExtra("user_role", userRole)
                putExtra("selected_tab", "wallet")
            })
            finish()
        }

        navMessages.setOnClickListener {
            highlightSelectedTab("messages")
            Toast.makeText(this, "Already on Messages", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSideMenu() {
        startActivity(Intent(this, SideMenuActivity::class.java).apply {
            putExtra("user_role", userRole)
            putExtra("selected_menu", "messages")
        })
        finish()
    }

    private fun highlightSelectedTab(selectedTab: String) {
        val selectedColor = ContextCompat.getColor(this, android.R.color.white)
        val unselectedColor = ContextCompat.getColor(this, R.color.wallet_text_muted)

        navHomeIcon.setColorFilter(if (selectedTab == "home") selectedColor else unselectedColor)
        navWalletIcon.setColorFilter(if (selectedTab == "wallet") selectedColor else unselectedColor)
        navMessagesIcon.setColorFilter(if (selectedTab == "messages") selectedColor else unselectedColor)

        navHomeText.setTextColor(if (selectedTab == "home") selectedColor else unselectedColor)
        navWalletText.setTextColor(if (selectedTab == "wallet") selectedColor else unselectedColor)
        navMessagesText.setTextColor(if (selectedTab == "messages") selectedColor else unselectedColor)
    }
}