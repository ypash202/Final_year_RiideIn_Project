package com.riidein.app.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.riidein.app.R

class MessagesActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var backButton: ImageButton
    private lateinit var callButton: ImageButton
    private lateinit var sendButton: ImageButton
    private lateinit var messagesTitle: TextView
    private lateinit var activeNowText: TextView
    private lateinit var chatProfileImage: ImageView
    private lateinit var chatScrollView: ScrollView
    private lateinit var messagesContainer: LinearLayout
    private lateinit var messageEditText: EditText

    private var currentUserId: String = ""
    private var currentUserRole: String = "customer"

    private var requestId: String = ""
    private var contactUserId: String = ""
    private var contactRole: String = "driver"
    private var contactName: String = "User"
    private var contactPhone: String = ""
    private var contactPhotoUrl: String = ""
    private var returnToRide: Boolean = false

    private var messagesListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentUserId = firebaseUser.uid

        readIntentData()
        initViews()
        setupClicks()
        loadContactDetails()
        listenForMessages()
    }

    private fun readIntentData() {
        currentUserRole = intent.getStringExtra("user_role") ?: "customer"
        requestId = intent.getStringExtra("request_id") ?: ""
        contactRole = intent.getStringExtra("contact_role") ?: "driver"
        contactUserId = intent.getStringExtra("contact_user_id") ?: ""
        contactName = intent.getStringExtra("contact_name") ?: "User"
        contactPhone = intent.getStringExtra("contact_phone") ?: ""
        contactPhotoUrl = intent.getStringExtra("contact_photo_url") ?: ""
        returnToRide = intent.getBooleanExtra("return_to_ride", false)
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        callButton = findViewById(R.id.callButton)
        sendButton = findViewById(R.id.sendButton)
        messagesTitle = findViewById(R.id.messagesTitle)
        activeNowText = findViewById(R.id.activeNowText)
        chatProfileImage = findViewById(R.id.chatProfileImage)
        chatScrollView = findViewById(R.id.chatScrollView)
        messagesContainer = findViewById(R.id.messagesContainer)
        messageEditText = findViewById(R.id.messageEditText)

        messagesTitle.text = contactName
        activeNowText.text = getString(R.string.active_now)

        loadProfileImageIntoView(
            imageView = chatProfileImage,
            imageUrl = contactPhotoUrl,
            fallbackRes = R.drawable.profile2
        )
    }

    private fun setupClicks() {
        backButton.setOnClickListener {
            if (returnToRide) {
                finish()
            } else {
                openSideMenu()
            }
        }

        callButton.setOnClickListener {
            callContact()
        }

        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun loadContactDetails() {
        if (contactUserId.isBlank()) return

        db.collection("users")
            .document(contactUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    contactName = document.getString("name") ?: contactName
                    contactPhone = document.getString("phone") ?: contactPhone
                    contactPhotoUrl = document.getString("driverPhotoUrl")
                        ?: document.getString("profileImageUrl")
                                ?: document.getString("profilePhotoUrl")
                                ?: contactPhotoUrl

                    messagesTitle.text = contactName

                    loadProfileImageIntoView(
                        imageView = chatProfileImage,
                        imageUrl = contactPhotoUrl,
                        fallbackRes = R.drawable.profile2
                    )
                }
            }
    }

    private fun listenForMessages() {
        if (requestId.isBlank()) {
            showSystemMessage(getString(R.string.chat_unavailable))
            return
        }

        messagesListener?.remove()

        messagesListener = db.collection("ride_chats")
            .document(requestId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    showSystemMessage("Failed to load messages.")
                    return@addSnapshotListener
                }

                messagesContainer.removeAllViews()

                if (snapshot == null || snapshot.isEmpty) {
                    showSystemMessage(getString(R.string.start_conversation))
                    return@addSnapshotListener
                }

                for (document in snapshot.documents) {
                    val senderId = document.getString("senderId") ?: ""
                    val message = document.getString("message") ?: ""
                    val seenByReceiver = document.getBoolean("seenByReceiver") ?: false
                    val isMine = senderId == currentUserId

                    val bubbleView = createMessageBubble(
                        message = message,
                        isMine = isMine,
                        seenByReceiver = seenByReceiver
                    )

                    messagesContainer.addView(bubbleView)

                    if (!isMine && !seenByReceiver) {
                        document.reference.update("seenByReceiver", true)
                    }
                }

                scrollToBottom()
            }
    }

    private fun sendMessage() {
        val text = messageEditText.text.toString().trim()

        if (text.isBlank()) {
            Toast.makeText(this, "Please type a message", Toast.LENGTH_SHORT).show()
            return
        }

        if (requestId.isBlank() || contactUserId.isBlank()) {
            Toast.makeText(this, "Chat details missing", Toast.LENGTH_SHORT).show()
            return
        }

        val messageData = hashMapOf<String, Any>(
            "senderId" to currentUserId,
            "receiverId" to contactUserId,
            "senderRole" to currentUserRole,
            "receiverRole" to contactRole,
            "message" to text,
            "createdAt" to System.currentTimeMillis(),
            "seenByReceiver" to false
        )

        db.collection("ride_chats")
            .document(requestId)
            .collection("messages")
            .add(messageData)
            .addOnSuccessListener {
                messageEditText.setText("")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createMessageBubble(
        message: String,
        isMine: Boolean,
        seenByReceiver: Boolean
    ): LinearLayout {
        val outerWrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10f)
            }
        }

        if (isMine) {
            val sentWrapper = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val bubble = TextView(this).apply {
                text = message
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
                background = roundedBg("#155E63", 18f)
                minWidth = 0
                minHeight = 0
                includeFontPadding = false
                maxWidth = (resources.displayMetrics.widthPixels * 0.72f).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val seenText = TextView(this).apply {
                text = if (seenByReceiver) "Seen" else "Sent"
                setTextColor(Color.parseColor("#8A95A5"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                includeFontPadding = false
                setPadding(0, dp(4f), dp(4f), 0)
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.END
                }
            }

            sentWrapper.addView(bubble)
            sentWrapper.addView(seenText)
            outerWrapper.addView(sentWrapper)

        } else {
            val rowWrapper = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START or Gravity.BOTTOM
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val avatar = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(34f), dp(34f)).apply {
                    marginEnd = dp(8f)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = ContextCompatHelper.getDrawable(this@MessagesActivity, R.drawable.profile_circle_border)
                setPadding(dp(1f), dp(1f), dp(1f), dp(1f))
            }

            loadProfileImageIntoView(
                imageView = avatar,
                imageUrl = contactPhotoUrl,
                fallbackRes = R.drawable.profile2
            )

            val bubble = TextView(this).apply {
                text = message
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
                background = roundedBg("#11151C", 18f)
                minWidth = 0
                minHeight = 0
                includeFontPadding = false
                maxWidth = (resources.displayMetrics.widthPixels * 0.62f).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            rowWrapper.addView(avatar)
            rowWrapper.addView(bubble)
            outerWrapper.addView(rowWrapper)
        }

        return outerWrapper
    }

    private fun showSystemMessage(message: String) {
        messagesContainer.removeAllViews()

        val textView = TextView(this).apply {
            text = message
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#8A95A5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(16f), dp(28f), dp(16f), dp(28f))
        }

        messagesContainer.addView(textView)
    }

    private fun callContact() {
        if (contactPhone.isBlank()) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:$contactPhone".toUri()
        }

        startActivity(intent)
    }

    private fun openSideMenu() {
        startActivity(
            Intent(this, SideMenuActivity::class.java).apply {
                putExtra("user_role", currentUserRole)
                putExtra("selected_menu", "messages")
            }
        )
        finish()
    }

    private fun loadProfileImageIntoView(
        imageView: ImageView,
        imageUrl: String,
        fallbackRes: Int
    ) {
        if (imageUrl.isBlank()) {
            imageView.setImageResource(fallbackRes)
            return
        }

        try {
            imageView.setImageURI(Uri.parse(imageUrl))
        } catch (_: Exception) {
            imageView.setImageResource(fallbackRes)
        }
    }

    private fun scrollToBottom() {
        chatScrollView.post {
            chatScrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun roundedBg(color: String, radiusDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(color))
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
        messagesListener = null
    }
}

private object ContextCompatHelper {
    fun getDrawable(context: android.content.Context, drawableRes: Int) =
        androidx.core.content.ContextCompat.getDrawable(context, drawableRes)
}