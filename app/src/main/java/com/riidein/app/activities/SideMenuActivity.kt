package com.riidein.app.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R

class SideMenuActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var backButton: ImageButton
    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView
    private lateinit var modeButton: Button

    private lateinit var menuHome: LinearLayout
    private lateinit var menuWallet: LinearLayout
    private lateinit var menuMessages: LinearLayout
    private lateinit var menuHistory: LinearLayout
    private lateinit var menuNotifications: LinearLayout
    private lateinit var menuInvite: LinearLayout
    private lateinit var menuSettings: LinearLayout
    private lateinit var menuLogout: LinearLayout

    private var userRole: String = "customer"
    private var selectedMenu: String = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_side_menu)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        userRole = intent.getStringExtra("user_role")?.trim()?.lowercase() ?: "customer"
        selectedMenu = intent.getStringExtra("selected_menu")?.trim()?.lowercase() ?: "home"

        backButton = findViewById(R.id.backButton)
        profileImage = findViewById(R.id.profileImage)
        userName = findViewById(R.id.userName)
        modeButton = findViewById(R.id.driverModeButton)

        menuHome = findViewById(R.id.menuHome)
        menuWallet = findViewById(R.id.menuWallet)
        menuMessages = findViewById(R.id.menuMessages)
        menuHistory = findViewById(R.id.menuHistory)
        menuNotifications = findViewById(R.id.menuNotifications)
        menuInvite = findViewById(R.id.menuInvite)
        menuSettings = findViewById(R.id.menuSettings)
        menuLogout = findViewById(R.id.menuLogout)

        backButton.setOnClickListener {
            finish()
        }

        menuHome.setOnClickListener {
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

        menuWallet.setOnClickListener {
            startActivity(Intent(this, DriverWalletActivity::class.java).apply {
                putExtra("user_role", userRole)
                putExtra("selected_tab", "wallet")
            })
            finish()
        }

        menuMessages.setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java).apply {
                putExtra("user_role", userRole)
                putExtra("selected_tab", "messages")
            })
            finish()
        }

        menuHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java).apply {
                putExtra("user_role", userRole)
                putExtra("selected_menu", "history")
            })
            finish()
        }

        menuNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java).apply {
                putExtra("user_role", userRole)
                putExtra("selected_menu", "notifications")
            })
            finish()
        }

        menuInvite.setOnClickListener {
            highlightSelectedMenu("invite")

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_friends_text))
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.invite_friends)))
        }

        menuSettings.setOnClickListener {
            startActivity(Intent(this, settings::class.java).apply {
                putExtra("user_role", userRole)
                putExtra("selected_menu", "settings")
            })
            finish()
        }

        menuLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        setupModeButton()
        loadUserData()
        highlightSelectedMenu(selectedMenu)
    }

    private fun setupModeButton() {
        modeButton.text = if (userRole == "driver") "Driver Mode" else "Customer Mode"
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return

        db.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "User"
                    val driverPhotoUrl = document.getString("driverPhotoUrl") ?: ""

                    userName.text = name

                    if (userRole == "driver" && driverPhotoUrl.isNotEmpty()) {
                        try {
                            profileImage.setImageURI(Uri.parse(driverPhotoUrl))
                        } catch (e: Exception) {
                            profileImage.setImageResource(R.drawable.profile2)
                        }
                    } else {
                        profileImage.setImageResource(R.drawable.profile2)
                    }
                } else {
                    userName.text = "User"
                    profileImage.setImageResource(R.drawable.profile2)
                }
            }
            .addOnFailureListener {
                userName.text = "User"
                profileImage.setImageResource(R.drawable.profile2)
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun highlightSelectedMenu(selected: String) {
        val selectedBg = ContextCompat.getDrawable(this, R.drawable.side_menu_selected_bg)

        val selectedTextColor = ContextCompat.getColor(this, android.R.color.white)
        val normalTextColor = ContextCompat.getColor(this, android.R.color.white)

        val selectedIconColor = ContextCompat.getColor(this, android.R.color.white)
        val normalIconColor = ContextCompat.getColor(this, android.R.color.white)

        val menuPairs = listOf(
            "home" to menuHome,
            "wallet" to menuWallet,
            "messages" to menuMessages,
            "history" to menuHistory,
            "notifications" to menuNotifications,
            "invite" to menuInvite,
            "settings" to menuSettings,
            "logout" to menuLogout
        )

        for ((key, view) in menuPairs) {
            view.background = if (key == selected) selectedBg else null

            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)

                if (child is TextView) {
                    child.setTextColor(if (key == selected) selectedTextColor else normalTextColor)
                }

                if (child is ImageView) {
                    child.setColorFilter(if (key == selected) selectedIconColor else normalIconColor)
                }
            }
        }
    }
}