package com.riidein.app.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.riidein.app.R

class chat_detail : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_detail)

        val driverName = intent.getStringExtra("driver_name") ?: "Binod"

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val callButton = findViewById<ImageButton>(R.id.callButton)
        val driverNameText = findViewById<TextView>(R.id.driverNameText)
        val profileImgTop = findViewById<ImageView>(R.id.profileImgTop)
        val driverMessageAvatar1 = findViewById<ImageView>(R.id.driverMessageAvatar1)
        val driverMessageAvatar2 = findViewById<ImageView>(R.id.driverMessageAvatar2)

        val chatContainer = findViewById<LinearLayout>(R.id.chatContainer)
        val chatScrollView = findViewById<ScrollView>(R.id.chatScrollView)
        val messageEditText = findViewById<EditText>(R.id.messageEditText)
        val sendButton = findViewById<ImageButton>(R.id.sendButton)

        driverNameText.text = driverName

        if (driverName.equals("Sulav", ignoreCase = true)) {
            profileImgTop.setImageResource(R.drawable.profile2)
            driverMessageAvatar1.setImageResource(R.drawable.profile2)
            driverMessageAvatar2.setImageResource(R.drawable.profile2)
        } else {
            profileImgTop.setImageResource(R.drawable.profile1)
            driverMessageAvatar1.setImageResource(R.drawable.profile1)
            driverMessageAvatar2.setImageResource(R.drawable.profile1)
        }

        backButton.setOnClickListener {
            finish()
        }

        callButton.setOnClickListener {
            val driverPhone = getDriverPhoneNumber(driverName)
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$driverPhone")
            }
            startActivity(dialIntent)
        }

        sendButton.setOnClickListener {
            val message = messageEditText.text.toString().trim()

            if (message.isEmpty()) {
                Toast.makeText(this, "Type a message first", Toast.LENGTH_SHORT).show()
            } else {
                addUserMessage(chatContainer, message)
                messageEditText.text.clear()

                chatScrollView.post {
                    chatScrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }
    }

    private fun getDriverPhoneNumber(driverName: String): String {
        return when (driverName.lowercase()) {
            "sulav" -> "9800000002"
            else -> "9800000001"
        }
    }

    private fun addUserMessage(chatContainer: LinearLayout, message: String) {
        val messageWrapper = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(14)
            }
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }

        val messageBubble = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = getDrawable(R.drawable.chat_bubble_teal)
            setPadding(dpToPx(18), dpToPx(12), dpToPx(18), dpToPx(12))
            text = message
            setTextColor(resources.getColor(android.R.color.white, theme))
            textSize = 14f
        }

        val seenText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
            }
            text = "✓✓ 3:00PM"
            setTextColor(resources.getColor(R.color.teal_200, theme))
            textSize = 11f
        }

        messageWrapper.addView(messageBubble)
        messageWrapper.addView(seenText)
        chatContainer.addView(messageWrapper)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}