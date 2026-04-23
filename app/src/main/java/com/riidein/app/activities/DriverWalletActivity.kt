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

    private lateinit var completeRideButton: Button
    private lateinit var ridesContainer: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var totalRidesText: TextView
    private lateinit var totalSpentText: TextView

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

        val userRole = intent.getStringExtra(EXTRA_USER_ROLE) ?: USER_ROLE_DRIVER

        if (userRole == USER_ROLE_CUSTOMER) {
            showCustomerWallet()
        } else {
            showDriverWallet()
        }

        setupClicks(userRole)
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
        navHome.visibility = View.GONE
        navWallet.visibility = View.GONE
        navMessages.visibility = View.GONE

        completeRideButton.text = getString(R.string.payment_details)
        loadAllCompletedRides()
    }

    private fun showDriverWallet() {
        bikeImage.visibility = View.VISIBLE
        driverNameTop.visibility = View.VISIBLE
        vehicleInfoTop.visibility = View.VISIBLE
        titleText.visibility = View.GONE

        bottomNav.visibility = View.VISIBLE
        navHome.visibility = View.VISIBLE
        navWallet.visibility = View.VISIBLE
        navMessages.visibility = View.VISIBLE

        completeRideButton.text = getString(R.string.complete_ride)
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

    private fun setupClicks(userRole: String) {
        backButton.setOnClickListener {
            finish()
        }

        if (userRole != USER_ROLE_CUSTOMER) {
            navHome.setOnClickListener {
                startActivity(Intent(this, CustomerHomeActivity::class.java))
                finish()
            }

            navWallet.setOnClickListener {
                // already on wallet page
            }

            navMessages.setOnClickListener {
                val intent = Intent(this, MessagesActivity::class.java)
                intent.putExtra(EXTRA_DRIVER_NAME, "")
                startActivity(intent)
            }
        }
    }

    companion object {
        private const val EXTRA_USER_ROLE = "user_role"
        private const val EXTRA_DRIVER_NAME = "driver_name"

        private const val USER_ROLE_CUSTOMER = "customer"
        private const val USER_ROLE_DRIVER = "driver"

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