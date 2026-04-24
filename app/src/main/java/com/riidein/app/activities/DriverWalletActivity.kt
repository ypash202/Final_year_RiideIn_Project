package com.riidein.app.activities

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R
import java.util.Locale

class DriverWalletActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var backButton: ImageButton
    private lateinit var bikeImage: ImageView
    private lateinit var driverNameTop: TextView
    private lateinit var vehicleInfoTop: TextView
    private lateinit var titleText: TextView
    private lateinit var bottomNav: LinearLayout

    private lateinit var navHome: LinearLayout
    private lateinit var navWallet: LinearLayout
    private lateinit var navMessages: LinearLayout

    private lateinit var navHomeIcon: ImageView
    private lateinit var navWalletIcon: ImageView
    private lateinit var navMessagesIcon: ImageView

    private lateinit var navHomeText: TextView
    private lateinit var navWalletText: TextView
    private lateinit var navMessagesText: TextView

    private lateinit var completeRideButton: Button
    private lateinit var ridesContainer: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var totalRidesText: TextView
    private lateinit var totalSpentText: TextView

    private var userRole: String = USER_ROLE_DRIVER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_driver_wallet)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()

        userRole = intent.getStringExtra(EXTRA_USER_ROLE)?.trim()?.lowercase() ?: USER_ROLE_DRIVER

        if (userRole == USER_ROLE_CUSTOMER) {
            showCustomerWallet()
        } else {
            showDriverWallet()
        }

        highlightSelectedTab(TAB_WALLET)
        setupClicks()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        bikeImage = findViewById(R.id.bikeImage)
        driverNameTop = findViewById(R.id.driverNameTop)
        vehicleInfoTop = findViewById(R.id.vehicleInfoTop)
        titleText = findViewById(R.id.titleText)
        bottomNav = findViewById(R.id.bottomNav)

        navHome = findViewById(R.id.navHome)
        navWallet = findViewById(R.id.navWallet)
        navMessages = findViewById(R.id.navMessages)

        navHomeIcon = navHome.getChildAt(0) as ImageView
        navHomeText = navHome.getChildAt(1) as TextView

        navWalletIcon = navWallet.getChildAt(0) as ImageView
        navWalletText = navWallet.getChildAt(1) as TextView

        navMessagesIcon = navMessages.getChildAt(0) as ImageView
        navMessagesText = navMessages.getChildAt(1) as TextView

        completeRideButton = findViewById(R.id.completeRideButton)
        ridesContainer = findViewById(R.id.ridesContainer)
        emptyStateText = findViewById(R.id.emptyStateText)
        totalRidesText = findViewById(R.id.totalRidesText)
        totalSpentText = findViewById(R.id.totalSpentText)
    }

    private fun showCustomerWallet() {
        bikeImage.visibility = View.GONE
        driverNameTop.visibility = View.GONE
        vehicleInfoTop.visibility = View.GONE
        titleText.visibility = View.VISIBLE

        bottomNav.visibility = View.GONE
        completeRideButton.text = getString(R.string.payment_details)
        loadAllCompletedRides()
    }

    private fun showDriverWallet() {
        bikeImage.visibility = View.VISIBLE
        driverNameTop.visibility = View.VISIBLE
        vehicleInfoTop.visibility = View.VISIBLE
        titleText.visibility = View.GONE

        bottomNav.visibility = View.VISIBLE
        completeRideButton.text = getString(R.string.complete_ride)
    }

    private fun setupClicks() {
        backButton.setOnClickListener {
            openSideMenu()
        }

        if (userRole != USER_ROLE_CUSTOMER) {
            navHome.setOnClickListener {
                highlightSelectedTab(TAB_HOME)
                openHomeByRole()
            }

            navWallet.setOnClickListener {
                highlightSelectedTab(TAB_WALLET)
                Toast.makeText(this, "Already on Wallet", Toast.LENGTH_SHORT).show()
            }

            navMessages.setOnClickListener {
                highlightSelectedTab(TAB_MESSAGES)
                startActivity(Intent(this, MessagesActivity::class.java).apply {
                    putExtra(EXTRA_USER_ROLE, userRole)
                    putExtra("selected_tab", "messages")
                })
                finish()
            }
        }
    }

    private fun openSideMenu() {
        startActivity(Intent(this, SideMenuActivity::class.java).apply {
            putExtra("user_role", userRole)
            putExtra("selected_menu", "wallet")
        })
        finish()
    }

    private fun highlightSelectedTab(selectedTab: String) {
        val selectedColor = ContextCompat.getColor(this, android.R.color.white)
        val unselectedColor = ContextCompat.getColor(this, R.color.wallet_text_muted)

        navHomeIcon.setColorFilter(if (selectedTab == TAB_HOME) selectedColor else unselectedColor)
        navWalletIcon.setColorFilter(if (selectedTab == TAB_WALLET) selectedColor else unselectedColor)
        navMessagesIcon.setColorFilter(if (selectedTab == TAB_MESSAGES) selectedColor else unselectedColor)

        navHomeText.setTextColor(if (selectedTab == TAB_HOME) selectedColor else unselectedColor)
        navWalletText.setTextColor(if (selectedTab == TAB_WALLET) selectedColor else unselectedColor)
        navMessagesText.setTextColor(if (selectedTab == TAB_MESSAGES) selectedColor else unselectedColor)

        navHomeText.setTypeface(null, if (selectedTab == TAB_HOME) Typeface.BOLD else Typeface.NORMAL)
        navWalletText.setTypeface(null, if (selectedTab == TAB_WALLET) Typeface.BOLD else Typeface.NORMAL)
        navMessagesText.setTypeface(null, if (selectedTab == TAB_MESSAGES) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun openHomeByRole() {
        val intent = if (userRole == USER_ROLE_DRIVER) {
            Intent(this, DriverHomeActivity::class.java)
        } else {
            Intent(this, CustomerHomeActivity::class.java)
        }
        intent.putExtra(EXTRA_USER_ROLE, userRole)
        intent.putExtra("selected_tab", "home")
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun loadAllCompletedRides() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showEmptyState()
            return
        }

        db.collection(COLLECTION_COMPLETED_RIDES)
            .whereEqualTo(FIELD_CUSTOMER_ID, currentUser.uid)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    showEmptyState()
                    return@addOnSuccessListener
                }

                val sortedDocuments = result.documents.sortedByDescending { document ->
                    document.getLong(FIELD_TIMESTAMP) ?: 0L
                }

                ridesContainer.removeAllViews()

                var totalSpent = 0.0

                for (document in sortedDocuments) {
                    val ride = CompletedRide(
                        driverName = document.getString(FIELD_DRIVER_NAME)
                            ?: getString(R.string.wallet_default_driver_name),
                        rideTime = document.getString(FIELD_RIDE_TIME)
                            ?: getString(R.string.wallet_default_ride_time),
                        rideDistance = document.getString(FIELD_RIDE_DISTANCE)
                            ?: getString(R.string.wallet_default_ride_distance),
                        rideDate = document.getString(FIELD_RIDE_DATE)
                            ?: getString(R.string.wallet_default_ride_date),
                        rideFare = document.getString(FIELD_RIDE_FARE)
                            ?: getString(R.string.wallet_default_ride_fare),
                        pickupLocation = document.getString(FIELD_PICKUP_LOCATION)
                            ?: getString(R.string.wallet_default_pickup),
                        dropLocation = document.getString(FIELD_DROP_LOCATION)
                            ?: getString(R.string.wallet_default_drop)
                    )

                    totalSpent += extractFareAmount(ride.rideFare)
                    ridesContainer.addView(createRideCard(ride))
                }

                totalRidesText.text =
                    getString(R.string.wallet_total_rides_format, sortedDocuments.size)
                totalSpentText.text =
                    getString(R.string.wallet_total_spent_format, formatMoney(totalSpent))
                emptyStateText.visibility = View.GONE
            }
            .addOnFailureListener {
                showEmptyState()
            }
    }

    private fun showEmptyState() {
        ridesContainer.removeAllViews()
        totalRidesText.text = getString(R.string.wallet_total_rides_format, 0)
        totalSpentText.text = getString(R.string.wallet_total_spent_format, formatMoney(0.0))
        emptyStateText.visibility = View.VISIBLE
    }

    private fun createRideCard(ride: CompletedRide): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(14f)
                setColor(ContextCompat.getColor(this@DriverWalletActivity, android.R.color.black))
            }
            setPadding(dpToInt(14f), dpToInt(14f), dpToInt(14f), dpToInt(14f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToInt(12f)
            }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val nameColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val driverNameView = createWhiteTextView(ride.driverName, 16f, true)
        val timeView = createMutedTextView(ride.rideTime)
        val distanceView = createMutedTextView(ride.rideDistance)
        val dateView = createMutedTextView(ride.rideDate)
        val fareView = createWhiteTextView(ride.rideFare, 14f, false)

        nameColumn.addView(driverNameView)
        nameColumn.addView(timeView)
        nameColumn.addView(distanceView)
        nameColumn.addView(dateView)

        topRow.addView(nameColumn)
        topRow.addView(fareView)

        val pickupView = createWhiteTextView(
            getString(R.string.wallet_pickup_format, ride.pickupLocation),
            14f,
            false
        ).apply {
            setPadding(0, dpToInt(18f), 0, 0)
        }

        val dropView = createWhiteTextView(
            getString(R.string.wallet_drop_format, ride.dropLocation),
            14f,
            false
        ).apply {
            setPadding(0, dpToInt(12f), 0, 0)
        }

        card.addView(topRow)
        card.addView(pickupView)
        card.addView(dropView)

        return card
    }

    private fun createWhiteTextView(text: String, textSizeSp: Float, isBold: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(this@DriverWalletActivity, android.R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            if (isBold) {
                setTypeface(typeface, Typeface.BOLD)
            }
        }
    }

    private fun createMutedTextView(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(this@DriverWalletActivity, R.color.wallet_text_muted))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        }
    }

    private fun extractFareAmount(fareTextValue: String): Double {
        val numberOnly = fareTextValue.replace(Regex("[^0-9.]"), "")
        return numberOnly.toDoubleOrNull() ?: 0.0
    }

    private fun formatMoney(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", amount)
        }
    }

    private fun dpToInt(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        ).toInt()
    }

    private fun dpToPx(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        )
    }

    companion object {
        private const val EXTRA_USER_ROLE = "user_role"

        private const val USER_ROLE_CUSTOMER = "customer"
        private const val USER_ROLE_DRIVER = "driver"

        private const val TAB_HOME = "home"
        private const val TAB_WALLET = "wallet"
        private const val TAB_MESSAGES = "messages"

        private const val COLLECTION_COMPLETED_RIDES = "completed_rides"

        private const val FIELD_CUSTOMER_ID = "customerId"
        private const val FIELD_DRIVER_NAME = "driverName"
        private const val FIELD_RIDE_TIME = "rideTime"
        private const val FIELD_RIDE_DISTANCE = "rideDistance"
        private const val FIELD_RIDE_DATE = "rideDate"
        private const val FIELD_RIDE_FARE = "rideFare"
        private const val FIELD_PICKUP_LOCATION = "pickupLocation"
        private const val FIELD_DROP_LOCATION = "dropLocation"
        private const val FIELD_TIMESTAMP = "timestamp"
    }
}

private data class CompletedRide(
    val driverName: String,
    val rideTime: String,
    val rideDistance: String,
    val rideDate: String,
    val rideFare: String,
    val pickupLocation: String,
    val dropLocation: String
)