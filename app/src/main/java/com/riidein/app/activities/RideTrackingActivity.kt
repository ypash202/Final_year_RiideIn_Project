package com.riidein.app.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.riidein.app.R

class RideTrackingActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var cancelRideButton: Button
    private lateinit var nextArrow: ImageView
    private lateinit var contactDriverRow: LinearLayout
    private lateinit var sosButton: Button

    private lateinit var arrivalTitle: TextView
    private lateinit var driverNameText: TextView
    private lateinit var driverVehicleText: TextView
    private lateinit var paymentAmountText: TextView
    private lateinit var paymentMethodText: TextView
    private lateinit var pickupLocationText: TextView
    private lateinit var dropLocationText: TextView
    private lateinit var paymentLabel: TextView

    private lateinit var driverProfileImage: ImageView
    private lateinit var vehicleImage: ImageView

    private var driverName: String = "Binod"
    private var vehicleName: String = "Moto"
    private var selectedPrice: String = "Rs 200"
    private var fromLocation: String = "Boudha"
    private var toLocation: String = "Trade Tower, Thapathali"

    private enum class RideState {
        ARRIVING,
        ARRIVED,
        COMPLETED
    }

    private var currentRideState = RideState.ARRIVING

    private val callPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                callPoliceDirectly()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.call_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
                openPoliceDialer()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_tracking)

        initViews()
        readIntentData()
        bindRideData()
        updateRideStateUI()
        setupClicks()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        closeButton = findViewById(R.id.closeButton)
        cancelRideButton = findViewById(R.id.cancelRideButton)
        nextArrow = findViewById(R.id.nextArrow)
        contactDriverRow = findViewById(R.id.contactDriverRow)
        sosButton = findViewById(R.id.sosButton)

        arrivalTitle = findViewById(R.id.arrivalTitle)
        driverNameText = findViewById(R.id.driverNameText)
        driverVehicleText = findViewById(R.id.driverVehicleText)
        paymentAmountText = findViewById(R.id.paymentAmountText)
        paymentMethodText = findViewById(R.id.paymentMethodText)
        pickupLocationText = findViewById(R.id.pickupLocationText)
        dropLocationText = findViewById(R.id.dropLocationText)
        paymentLabel = findViewById(R.id.paymentLabel)

        driverProfileImage = findViewById(R.id.driverProfileImage)
        vehicleImage = findViewById(R.id.vehicleImage)
    }

    private fun readIntentData() {
        driverName = intent.getStringExtra("driver_name") ?: "Binod"
        vehicleName = intent.getStringExtra("vehicle_name") ?: "Moto"
        selectedPrice = intent.getStringExtra("selected_price") ?: "Rs 200"
        fromLocation = intent.getStringExtra("from_location") ?: "Boudha"
        toLocation = intent.getStringExtra("to_location") ?: "Trade Tower, Thapathali"
    }

    private fun bindRideData() {
        driverNameText.text = driverName

        driverVehicleText.text = when (vehicleName.lowercase()) {
            "moto" -> getString(R.string.driver_vehicle_honda)
            "cab" -> getString(R.string.vehicle_type_cab)
            "delivery" -> getString(R.string.vehicle_type_delivery)
            else -> vehicleName
        }

        paymentAmountText.text = selectedPrice
        paymentMethodText.text = getString(R.string.cash)
        pickupLocationText.text = fromLocation
        dropLocationText.text = toLocation

        if (driverName.equals("Sulav", ignoreCase = true)) {
            driverProfileImage.setImageResource(R.drawable.profile2)
        } else {
            driverProfileImage.setImageResource(R.drawable.profile1)
        }

        when (vehicleName.lowercase()) {
            "moto" -> vehicleImage.setImageResource(R.drawable.bike)
            "cab" -> vehicleImage.setImageResource(R.drawable.car)
            "delivery" -> vehicleImage.setImageResource(R.drawable.delivery)
            else -> vehicleImage.setImageResource(R.drawable.bike)
        }
    }

    private fun setupClicks() {
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        closeButton.setOnClickListener {
            finish()
        }

        contactDriverRow.setOnClickListener {
            val intent = Intent(this, MessagesActivity::class.java)
            intent.putExtra("driver_name", driverName)
            startActivity(intent)
        }

        cancelRideButton.setOnClickListener {
            when (currentRideState) {
                RideState.ARRIVING -> openCancelRidePage()

                RideState.ARRIVED -> {
                    currentRideState = RideState.COMPLETED
                    updateRideStateUI()
                    Toast.makeText(
                        this,
                        getString(R.string.ride_started_demo),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                RideState.COMPLETED -> {
                    val intent = Intent(this, CustomerHomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }

        nextArrow.setOnClickListener {
            when (currentRideState) {
                RideState.ARRIVING -> {
                    currentRideState = RideState.ARRIVED
                    updateRideStateUI()
                }

                RideState.ARRIVED -> {
                    currentRideState = RideState.COMPLETED
                    updateRideStateUI()
                }

                RideState.COMPLETED -> {
                    val intent = Intent(this, CustomerHomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }

        sosButton.setOnClickListener {
            showSosDialog()
        }
    }

    private fun updateRideStateUI() {
        when (currentRideState) {
            RideState.ARRIVING -> {
                arrivalTitle.text = getString(R.string.ride_state_arriving)
                cancelRideButton.text = getString(R.string.cancel_ride)
                contactDriverRow.visibility = View.VISIBLE
                nextArrow.visibility = View.VISIBLE
                sosButton.visibility = View.VISIBLE
                paymentLabel.visibility = View.VISIBLE
                paymentAmountText.visibility = View.VISIBLE
                paymentMethodText.visibility = View.VISIBLE
            }

            RideState.ARRIVED -> {
                arrivalTitle.text = getString(R.string.ride_state_arrived)
                cancelRideButton.text = getString(R.string.start_ride)
                contactDriverRow.visibility = View.VISIBLE
                nextArrow.visibility = View.VISIBLE
                sosButton.visibility = View.VISIBLE
                paymentLabel.visibility = View.VISIBLE
                paymentAmountText.visibility = View.VISIBLE
                paymentMethodText.visibility = View.VISIBLE
            }

            RideState.COMPLETED -> {
                arrivalTitle.text = getString(R.string.ride_state_completed)
                cancelRideButton.text = getString(R.string.back_to_home)
                contactDriverRow.visibility = View.GONE
                nextArrow.visibility = View.GONE
                sosButton.visibility = View.GONE
                paymentLabel.visibility = View.VISIBLE
                paymentAmountText.visibility = View.VISIBLE
                paymentMethodText.visibility = View.VISIBLE
            }
        }
    }

    private fun openCancelRidePage() {
        val intent = Intent(this, CancelRideActivity::class.java)
        intent.putExtra("driver_name", driverName)
        intent.putExtra("vehicle_name", driverVehicleText.text.toString())
        intent.putExtra("from_location", fromLocation)
        intent.putExtra("to_location", toLocation)
        startActivity(intent)
    }

    private fun showSosDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.sos_title))
            .setMessage(getString(R.string.sos_message))
            .setPositiveButton(getString(R.string.call_100)) { _, _ ->
                makeEmergencyCall()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun makeEmergencyCall() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED -> {
                callPoliceDirectly()
            }

            else -> {
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }
    }

    private fun callPoliceDirectly() {
        try {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = "tel:100".toUri()
            }
            startActivity(callIntent)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                getString(R.string.direct_call_failed),
                Toast.LENGTH_SHORT
            ).show()
            openPoliceDialer()
        }
    }

    private fun openPoliceDialer() {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:100".toUri()
        }
        startActivity(dialIntent)
    }
}