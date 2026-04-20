package com.riidein.app.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.riidein.app.R

class MessagesActivity : AppCompatActivity() {

    private lateinit var deleteButton: ImageButton

    private lateinit var selectedDriverRow: LinearLayout
    private lateinit var staticBinodRow: LinearLayout
    private lateinit var staticSulavRow: LinearLayout

    private var selectedRow: LinearLayout? = null
    private var selectedDriverNameForChat: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        deleteButton = findViewById(R.id.deleteButton)

        selectedDriverRow = findViewById(R.id.selectedDriverRow)
        staticBinodRow = findViewById(R.id.staticBinodRow)
        staticSulavRow = findViewById(R.id.staticSulavRow)

        val selectedDriverImage = findViewById<ImageView>(R.id.selectedDriverImage)
        val selectedDriverNameText = findViewById<TextView>(R.id.selectedDriverNameText)
        val selectedDriverMessageText = findViewById<TextView>(R.id.selectedDriverMessageText)

        val driverName = intent.getStringExtra("driver_name") ?: "Binod"

        selectedDriverNameText.text = driverName
        selectedDriverMessageText.text = "I am on the way."

        if (driverName.equals("Sulav", ignoreCase = true)) {
            selectedDriverImage.setImageResource(R.drawable.profile2)
        } else {
            selectedDriverImage.setImageResource(R.drawable.profile1)
        }

        backButton.setOnClickListener {
            val intent = Intent(this, CustomerHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        deleteButton.setOnClickListener {
            deleteSelectedMessage()
        }

        setupMessageRow(selectedDriverRow, driverName)
        setupMessageRow(staticBinodRow, "Binod")
        setupMessageRow(staticSulavRow, "Sulav")
    }

    private fun setupMessageRow(row: LinearLayout, driverName: String) {
        row.setOnClickListener {
            if (selectedRow == row) {
                clearSelection()
                openChatDetail(driverName)
            } else {
                openChatDetail(driverName)
            }
        }

        row.setOnLongClickListener {
            toggleSelection(row, driverName)
            true
        }
    }

    private fun toggleSelection(row: LinearLayout, driverName: String) {
        if (selectedRow == row) {
            clearSelection()
        } else {
            clearSelection()
            selectedRow = row
            selectedDriverNameForChat = driverName
            row.setBackgroundColor(Color.parseColor("#33FFFFFF"))
            Toast.makeText(this, "$driverName selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearSelection() {
        selectedRow?.setBackgroundColor(Color.TRANSPARENT)
        selectedRow = null
        selectedDriverNameForChat = null
    }

    private fun deleteSelectedMessage() {
        val rowToDelete = selectedRow

        if (rowToDelete == null) {
            Toast.makeText(this, "Long press a message to select it", Toast.LENGTH_SHORT).show()
            return
        }

        rowToDelete.visibility = LinearLayout.GONE
        Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show()
        clearSelection()
    }

    private fun openChatDetail(driverName: String) {
        val intent = Intent(this, chat_detail::class.java)
        intent.putExtra("driver_name", driverName)
        startActivity(intent)
    }
}