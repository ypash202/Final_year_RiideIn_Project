package com.riidein.app.activities

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var backButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var searchIcon: ImageView
    private lateinit var searchEditText: EditText
    private lateinit var historyContainer: LinearLayout
    private lateinit var emptyStateText: TextView

    private var userRole: String = "customer"

    private val allHistoryItems = mutableListOf<HistoryItem>()
    private val visibleHistoryItems = mutableListOf<HistoryItem>()

    private var selectedHistoryItem: HistoryItem? = null
    private var selectedCardView: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        userRole = intent.getStringExtra("user_role")
            ?.trim()
            ?.lowercase(Locale.getDefault())
            ?: "customer"

        initViews()
        setupClicks()
        setupSearch()
        loadHistory()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        deleteButton = findViewById(R.id.deleteButton)
        searchIcon = findViewById(R.id.searchIcon)
        searchEditText = findViewById(R.id.searchEditText)
        historyContainer = findViewById(R.id.historyContainer)
        emptyStateText = findViewById(R.id.emptyStateText)
    }

    private fun setupClicks() {
        backButton.setOnClickListener {
            openSideMenu()
        }

        deleteButton.setOnClickListener {
            deleteSelectedHistory()
        }

        searchIcon.setOnClickListener {
            filterHistory(searchEditText.text.toString())
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                filterHistory(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        searchEditText.setOnEditorActionListener { _, actionId, event ->
            val pressedEnter =
                event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN

            if (actionId == EditorInfo.IME_ACTION_SEARCH || pressedEnter) {
                filterHistory(searchEditText.text.toString())
                true
            } else {
                false
            }
        }
    }

    private fun loadHistory() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            showEmptyState("Please login again")
            return
        }

        selectedHistoryItem = null
        selectedCardView = null
        allHistoryItems.clear()
        visibleHistoryItems.clear()
        historyContainer.removeAllViews()
        emptyStateText.visibility = View.GONE

        val uid = currentUser.uid
        val idField = if (userRole == "driver") "driverId" else "customerId"
        val hiddenField = if (userRole == "driver") "hiddenFromDriver" else "hiddenFromCustomer"

        db.collection("ride_requests")
            .whereEqualTo(idField, uid)
            .get()
            .addOnSuccessListener { result ->
                allHistoryItems.clear()

                for (document in result.documents) {
                    val hidden = document.getBoolean(hiddenField) ?: false
                    if (hidden) continue

                    val status = document.getString("status")
                        ?.trim()
                        ?.lowercase(Locale.getDefault())
                        ?: ""

                    if (!shouldShowStatusForRole(status)) continue

                    val pickup = document.getString("pickup") ?: "Pickup location"
                    val drop = document.getString("drop") ?: "Drop location"
                    val fare = normalizeFare(document.getString("fare") ?: "Rs 0")
                    val createdAt = document.getLong("createdAt") ?: 0L

                    allHistoryItems.add(
                        HistoryItem(
                            documentId = document.id,
                            pickupLocation = pickup,
                            dropLocation = drop,
                            fare = fare,
                            status = getStatusLabel(status),
                            statusColor = getStatusColor(status),
                            timestamp = createdAt
                        )
                    )
                }

                renderHistory(allHistoryItems)
            }
            .addOnFailureListener {
                showEmptyState("Failed to load history")
            }
    }

    private fun shouldShowStatusForRole(status: String): Boolean {
        return when (userRole) {
            "driver" -> {
                status == "completed" || status == "cancelled_by_driver"
            }

            else -> {
                status == "completed" || status == "cancelled_by_customer"
            }
        }
    }

    private fun getStatusLabel(status: String): String {
        return when (status) {
            "completed" -> "Completed"
            "cancelled_by_driver" -> "Driver cancelled"
            "cancelled_by_customer" -> "Customer cancelled"
            else -> "Cancelled"
        }
    }

    private fun getStatusColor(status: String): String {
        return when (status) {
            "completed" -> "#00E58B"
            else -> "#A0A0A0"
        }
    }

    private fun filterHistory(query: String) {
        val cleanQuery = query.trim().lowercase(Locale.getDefault())

        selectedHistoryItem = null
        selectedCardView = null

        if (cleanQuery.isEmpty()) {
            renderHistory(allHistoryItems)
            return
        }

        val filteredItems = allHistoryItems.filter { item ->
            item.pickupLocation.lowercase(Locale.getDefault()).contains(cleanQuery) ||
                    item.dropLocation.lowercase(Locale.getDefault()).contains(cleanQuery) ||
                    item.fare.lowercase(Locale.getDefault()).contains(cleanQuery) ||
                    item.status.lowercase(Locale.getDefault()).contains(cleanQuery)
        }

        renderHistory(filteredItems)
    }

    private fun renderHistory(items: List<HistoryItem>) {
        val sortedItems = items.sortedByDescending { it.timestamp }

        visibleHistoryItems.clear()
        visibleHistoryItems.addAll(sortedItems)

        historyContainer.removeAllViews()

        if (sortedItems.isEmpty()) {
            showEmptyState("No history found")
            return
        }

        emptyStateText.visibility = View.GONE

        for (item in sortedItems) {
            historyContainer.addView(createHistoryCard(item))
        }
    }

    private fun createHistoryCard(item: HistoryItem): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.history_card_bg)
            setPadding(dpToInt(14f), dpToInt(14f), dpToInt(14f), dpToInt(14f))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToInt(12f)
            }

            isClickable = true
            isFocusable = true

            setOnClickListener {
                selectedCardView?.setBackgroundResource(R.drawable.history_card_bg)

                selectedHistoryItem = item
                selectedCardView = this

                setBackgroundResource(R.drawable.history_selected_card_bg)

                Toast.makeText(
                    this@HistoryActivity,
                    "History selected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        card.addView(createLocationRow(R.drawable.ic_pickup_pin, item.pickupLocation))
        card.addView(createLocationRow(R.drawable.ic_drop_pin, item.dropLocation))

        val divider = View(this).apply {
            setBackgroundColor(0x22FFFFFF)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToInt(1f)
            ).apply {
                topMargin = dpToInt(14f)
                bottomMargin = dpToInt(12f)
            }
        }

        card.addView(divider)

        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val fareText = TextView(this).apply {
            text = item.fare
            setTextColor(ContextCompat.getColor(this@HistoryActivity, android.R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val statusText = TextView(this).apply {
            text = item.status
            setTextColor(item.statusColor.toColorInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
        }

        bottomRow.addView(fareText)
        bottomRow.addView(statusText)

        card.addView(bottomRow)

        return card
    }

    private fun createLocationRow(iconRes: Int, location: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToInt(14f)
            }
        }

        val icon = ImageView(this).apply {
            setImageResource(iconRes)

            layoutParams = LinearLayout.LayoutParams(
                dpToInt(22f),
                dpToInt(22f)
            )
        }

        val locationText = TextView(this).apply {
            text = location
            setTextColor(ContextCompat.getColor(this@HistoryActivity, android.R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dpToInt(10f)
            }
        }

        row.addView(icon)
        row.addView(locationText)

        return row
    }

    private fun deleteSelectedHistory() {
        val itemToDelete = selectedHistoryItem

        if (itemToDelete == null) {
            Toast.makeText(
                this,
                "Please select one history item first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val hiddenField = if (userRole == "driver") {
            "hiddenFromDriver"
        } else {
            "hiddenFromCustomer"
        }

        db.collection("ride_requests")
            .document(itemToDelete.documentId)
            .update(hiddenField, true)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Selected history deleted",
                    Toast.LENGTH_SHORT
                ).show()

                selectedHistoryItem = null
                selectedCardView = null
                searchEditText.setText("")
                loadHistory()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to delete selected history",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun normalizeFare(value: String): String {
        val cleanValue = value
            .replace("NPR", "", ignoreCase = true)
            .replace("Rs", "", ignoreCase = true)
            .replace("$", "")
            .trim()

        return if (cleanValue.isBlank()) {
            "Rs 0"
        } else {
            "Rs $cleanValue"
        }
    }

    private fun showEmptyState(message: String) {
        historyContainer.removeAllViews()
        emptyStateText.text = message
        emptyStateText.visibility = View.VISIBLE
    }

    private fun openSideMenu() {
        startActivity(Intent(this, SideMenuActivity::class.java).apply {
            putExtra("user_role", userRole)
            putExtra("selected_menu", "history")
        })
        finish()
    }

    private fun dpToInt(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        ).toInt()
    }
}

private data class HistoryItem(
    val documentId: String,
    val pickupLocation: String,
    val dropLocation: String,
    val fare: String,
    val status: String,
    val statusColor: String,
    val timestamp: Long
)