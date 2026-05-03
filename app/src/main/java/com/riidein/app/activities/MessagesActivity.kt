package com.riidein.app.activities

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.riidein.app.R
import com.riidein.app.utils.ProfileImageHelper
import java.text.SimpleDateFormat
import java.util.Locale

class MessagesActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var backButton: ImageButton
    private lateinit var callButton: ImageButton
    private lateinit var deleteButton: Button
    private lateinit var titleText: TextView
    private lateinit var emptyText: TextView
    private lateinit var contentScrollView: ScrollView
    private lateinit var contentContainer: LinearLayout
    private lateinit var messageInputBar: LinearLayout
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton

    private var userRole: String = "customer"
    private var requestId: String = ""
    private var contactRole: String = ""
    private var contactUserId: String = ""
    private var contactName: String = ""
    private var contactPhotoUrl: String = ""
    private var contactPhotoBase64: String = ""
    private var returnToRide: Boolean = false

    private var selectedChatId: String = ""
    private var selectedChatView: View? = null
    private var messageListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        readIntentData()
        initViews()

        backButton.setOnClickListener {
            finish()
        }

        if (returnToRide && requestId.isNotBlank()) {
            setupRideChatMode()
        } else {
            setupInboxMode()
        }
    }

    private fun readIntentData() {
        userRole = intent.getStringExtra("user_role")
            ?.trim()
            ?.lowercase(Locale.getDefault())
            ?: "customer"

        requestId = intent.getStringExtra("request_id") ?: ""

        contactRole = intent.getStringExtra("contact_role")
            ?.trim()
            ?.lowercase(Locale.getDefault())
            ?: ""

        contactUserId = intent.getStringExtra("contact_user_id") ?: ""
        contactName = intent.getStringExtra("contact_name") ?: ""
        contactPhotoUrl = intent.getStringExtra("contact_photo_url") ?: ""
        contactPhotoBase64 = intent.getStringExtra("contact_photo_base64") ?: ""
        returnToRide = intent.getBooleanExtra("return_to_ride", false)
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        callButton = findViewById(R.id.callButton)
        deleteButton = findViewById(R.id.deleteButton)
        titleText = findViewById(R.id.titleText)
        emptyText = findViewById(R.id.emptyText)
        contentScrollView = findViewById(R.id.contentScrollView)
        contentContainer = findViewById(R.id.contentContainer)
        messageInputBar = findViewById(R.id.messageInputBar)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
    }

    private fun setupRideChatMode() {
        titleText.text = if (contactName.isNotBlank()) {
            contactName
        } else if (userRole == "driver") {
            "Customer"
        } else {
            "Driver"
        }

        deleteButton.visibility = View.GONE
        callButton.visibility = View.VISIBLE
        messageInputBar.visibility = View.VISIBLE
        emptyText.visibility = View.GONE
        contentContainer.removeAllViews()

        callButton.setOnClickListener {
            openDialer()
        }

        sendButton.setOnClickListener {
            sendRideMessage()
        }

        resolveContactFromRideRequest()
    }

    private fun resolveContactFromRideRequest() {
        if (requestId.isBlank()) {
            Toast.makeText(this, "Ride chat not found", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("ride_requests")
            .document(requestId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    if (userRole == "customer") {
                        val resolvedDriverId =
                            document.getString("driverId")
                                ?: document.getString("driver_id")
                                ?: contactUserId

                        val resolvedDriverName =
                            document.getString("driverName")
                                ?: document.getString("driver_name")
                                ?: contactName

                        if (resolvedDriverId.isNotBlank()) {
                            contactUserId = resolvedDriverId
                        }

                        if (resolvedDriverName.isNotBlank()) {
                            contactName = resolvedDriverName
                            titleText.text = resolvedDriverName
                        }

                        contactRole = "driver"
                    } else {
                        val resolvedCustomerId =
                            document.getString("customerId")
                                ?: document.getString("customer_id")
                                ?: document.getString("userId")
                                ?: document.getString("user_id")
                                ?: contactUserId

                        val resolvedCustomerName =
                            document.getString("customerName")
                                ?: document.getString("customer_name")
                                ?: document.getString("userName")
                                ?: document.getString("user_name")
                                ?: contactName

                        if (resolvedCustomerId.isNotBlank()) {
                            contactUserId = resolvedCustomerId
                        }

                        if (resolvedCustomerName.isNotBlank()) {
                            contactName = resolvedCustomerName
                            titleText.text = resolvedCustomerName
                        }

                        contactRole = "customer"
                    }
                }

                createOrUpdateRideChatSummary()
                listenToRideMessages()
            }
            .addOnFailureListener {
                createOrUpdateRideChatSummary()
                listenToRideMessages()
            }
    }

    private fun createOrUpdateRideChatSummary() {
        val currentUser = auth.currentUser ?: return

        val chatData = hashMapOf<String, Any>(
            "requestId" to requestId,
            "lastMessage" to "Chat started",
            "lastMessageTime" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (userRole == "customer") {
            chatData["customerId"] = currentUser.uid
            chatData["driverId"] = contactUserId
            chatData["driverName"] = if (contactName.isNotBlank()) contactName else "Driver"
            chatData["driverPhotoUrl"] = contactPhotoUrl
            chatData["driverPhotoBase64"] = contactPhotoBase64
        } else {
            chatData["driverId"] = currentUser.uid
            chatData["customerId"] = contactUserId
            chatData["customerName"] = if (contactName.isNotBlank()) contactName else "Customer"
            chatData["customerPhotoUrl"] = contactPhotoUrl
            chatData["customerPhotoBase64"] = contactPhotoBase64
        }

        db.collection("ride_chats")
            .document(requestId)
            .set(chatData, SetOptions.merge())
    }

    private fun listenToRideMessages() {
        messageListener?.remove()

        messageListener = db.collection("ride_chats")
            .document(requestId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load chat", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                contentContainer.removeAllViews()

                if (snapshot == null || snapshot.isEmpty) {
                    emptyText.visibility = View.VISIBLE
                    emptyText.text = "No messages yet.\n\nStart the conversation."
                    return@addSnapshotListener
                }

                emptyText.visibility = View.GONE

                val currentUserId = auth.currentUser?.uid ?: ""

                for (document in snapshot.documents) {
                    val messageText = document.getString("messageText") ?: ""
                    if (messageText.isBlank()) continue

                    val senderId = document.getString("senderId") ?: ""
                    val receiverId = document.getString("receiverId") ?: ""
                    val seenByReceiver = document.getBoolean("seenByReceiver") ?: false
                    val timestamp = document.getTimestamp("timestamp")

                    val isMine = senderId == currentUserId

                    addMessageBubble(
                        message = messageText,
                        isMine = isMine,
                        timestamp = timestamp,
                        seenByReceiver = seenByReceiver
                    )

                    if (receiverId == currentUserId && !seenByReceiver) {
                        document.reference.update("seenByReceiver", true)
                    }
                }

                contentScrollView.post {
                    contentScrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
    }

    private fun sendRideMessage() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            return
        }

        if (requestId.isBlank()) {
            Toast.makeText(this, "Ride chat not found", Toast.LENGTH_SHORT).show()
            return
        }

        if (contactUserId.isBlank()) {
            Toast.makeText(
                this,
                "Receiver not found. Please reopen the ride chat.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val messageText = messageEditText.text.toString().trim()

        if (messageText.isBlank()) {
            Toast.makeText(this, "Type a message first", Toast.LENGTH_SHORT).show()
            return
        }

        val receiverRole = if (userRole == "driver") {
            "customer"
        } else {
            "driver"
        }

        val messageData = hashMapOf<String, Any>(
            "senderId" to currentUser.uid,
            "senderRole" to userRole,
            "receiverId" to contactUserId,
            "receiverRole" to receiverRole,
            "messageText" to messageText,
            "timestamp" to FieldValue.serverTimestamp(),
            "seenByReceiver" to false
        )

        db.collection("ride_chats")
            .document(requestId)
            .collection("messages")
            .add(messageData)
            .addOnSuccessListener {
                updateChatLastMessage(messageText)
                messageEditText.text.clear()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateChatLastMessage(messageText: String) {
        db.collection("ride_chats")
            .document(requestId)
            .set(
                mapOf(
                    "lastMessage" to messageText,
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
    }

    private fun addMessageBubble(
        message: String,
        isMine: Boolean,
        timestamp: Timestamp?,
        seenByReceiver: Boolean
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isMine) Gravity.END else Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(12)
            }
        }

        if (!isMine) {
            val avatar = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(34)).apply {
                    marginEnd = dpToPx(8)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            ProfileImageHelper.loadProfileImage(
                imageView = avatar,
                base64Image = contactPhotoBase64,
                uriString = contactPhotoUrl,
                fallbackRes = if (contactRole == "driver") R.drawable.profile2 else R.drawable.profile1
            )

            row.addView(avatar)
        }

        val bubbleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (isMine) Gravity.END else Gravity.START
        }

        val bubble = TextView(this).apply {
            background = if (isMine) {
                AppCompatResources.getDrawable(this@MessagesActivity, R.drawable.chat_bubble_teal)
            } else {
                AppCompatResources.getDrawable(this@MessagesActivity, R.drawable.chat_bubble_dark)
            }

            setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
            text = message
            setTextColor(resources.getColor(android.R.color.white, theme))
            textSize = 14f
            maxWidth = dpToPx(230)
        }

        val seenStatus = if (isMine) {
            if (seenByReceiver) {
                "✓✓ Seen"
            } else {
                "✓ Sent"
            }
        } else {
            ""
        }

        val timeText = TextView(this).apply {
            text = if (isMine) {
                "$seenStatus • ${formatTime(timestamp)}"
            } else {
                formatTime(timestamp)
            }

            setTextColor(resources.getColor(R.color.teal_200, theme))
            textSize = 10f
            alpha = 0.8f
        }

        bubbleColumn.addView(bubble)
        bubbleColumn.addView(timeText)
        row.addView(bubbleColumn)

        contentContainer.addView(row)
    }

    private fun setupInboxMode() {
        titleText.text = if (userRole == "driver") {
            "Driver Messages"
        } else {
            "Customer Messages"
        }

        callButton.visibility = View.GONE
        messageInputBar.visibility = View.GONE
        deleteButton.visibility = View.GONE
        selectedChatId = ""
        selectedChatView = null

        deleteButton.setOnClickListener {
            deleteSelectedChat()
        }

        loadInboxChats()
    }

    private fun loadInboxChats() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            showInboxMessage("Please login again to view your messages.")
            return
        }

        showInboxMessage("Loading messages...")

        val queryField = if (userRole == "driver") {
            "driverId"
        } else {
            "customerId"
        }

        db.collection("ride_chats")
            .whereEqualTo(queryField, currentUser.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                contentContainer.removeAllViews()

                if (snapshot.isEmpty) {
                    showInboxMessage(
                        if (userRole == "driver") {
                            "No customer messages yet.\n\nYour customer ride chats will appear here."
                        } else {
                            "No driver messages yet.\n\nYour driver ride chats will appear here."
                        }
                    )
                    return@addOnSuccessListener
                }

                emptyText.visibility = View.GONE

                val sortedChats = snapshot.documents.sortedByDescending {
                    it.getTimestamp("lastMessageTime")?.toDate()?.time ?: 0L
                }

                for (document in sortedChats) {
                    addInboxCard(document)
                }
            }
            .addOnFailureListener {
                showInboxMessage("Could not load messages.")
            }
    }

    private fun addInboxCard(document: DocumentSnapshot) {
        val chatRequestId = document.getString("requestId") ?: document.id

        val personName = if (userRole == "driver") {
            document.getString("customerName") ?: "Customer"
        } else {
            document.getString("driverName") ?: "Driver"
        }

        val personPhotoUrl = if (userRole == "driver") {
            document.getString("customerPhotoUrl") ?: ""
        } else {
            document.getString("driverPhotoUrl") ?: ""
        }

        val personPhotoBase64 = if (userRole == "driver") {
            document.getString("customerPhotoBase64") ?: ""
        } else {
            document.getString("driverPhotoBase64") ?: ""
        }

        val lastMessage = document.getString("lastMessage") ?: "No messages yet"
        val lastMessageTime = document.getTimestamp("lastMessageTime")
        val oppositeRole = if (userRole == "driver") "customer" else "driver"

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
            background = AppCompatResources.getDrawable(
                this@MessagesActivity,
                R.drawable.chat_bubble_dark
            )
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(12)
            }
        }

        val avatar = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(52), dpToPx(52)).apply {
                marginEnd = dpToPx(12)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        ProfileImageHelper.loadProfileImage(
            imageView = avatar,
            base64Image = personPhotoBase64,
            uriString = personPhotoUrl,
            fallbackRes = if (oppositeRole == "driver") R.drawable.profile2 else R.drawable.profile1
        )

        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val nameText = TextView(this).apply {
            text = personName
            setTextColor(resources.getColor(android.R.color.white, theme))
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        }

        val messageText = TextView(this).apply {
            text = lastMessage
            setTextColor(resources.getColor(android.R.color.darker_gray, theme))
            textSize = 13f
            maxLines = 1
        }

        val timeText = TextView(this).apply {
            text = formatTime(lastMessageTime)
            setTextColor(resources.getColor(R.color.teal_200, theme))
            textSize = 11f
        }

        textColumn.addView(nameText)
        textColumn.addView(messageText)
        textColumn.addView(timeText)

        card.addView(avatar)
        card.addView(textColumn)

        card.setOnClickListener {
            val intent = Intent(this, MessagesActivity::class.java).apply {
                putExtra("user_role", userRole)
                putExtra("request_id", chatRequestId)
                putExtra("contact_role", oppositeRole)
                putExtra(
                    "contact_user_id",
                    if (userRole == "driver") {
                        document.getString("customerId") ?: ""
                    } else {
                        document.getString("driverId") ?: ""
                    }
                )
                putExtra("contact_name", personName)
                putExtra("contact_photo_url", personPhotoUrl)
                putExtra("contact_photo_base64", personPhotoBase64)
                putExtra("return_to_ride", true)
            }
            startActivity(intent)
        }

        card.setOnLongClickListener {
            selectInboxCard(chatRequestId, card)
            true
        }

        contentContainer.addView(card)
    }

    private fun selectInboxCard(chatRequestId: String, card: LinearLayout) {
        selectedChatView?.background = AppCompatResources.getDrawable(
            this,
            R.drawable.chat_bubble_dark
        )

        selectedChatId = chatRequestId
        selectedChatView = card

        card.background = AppCompatResources.getDrawable(
            this,
            R.drawable.side_menu_selected_bg
        )

        deleteButton.visibility = View.VISIBLE
    }

    private fun deleteSelectedChat() {
        if (selectedChatId.isBlank()) {
            Toast.makeText(this, "Long press a chat first", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("ride_chats")
            .document(selectedChatId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Chat deleted", Toast.LENGTH_SHORT).show()
                selectedChatId = ""
                selectedChatView = null
                deleteButton.visibility = View.GONE
                loadInboxChats()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete chat", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showInboxMessage(message: String) {
        contentContainer.removeAllViews()
        emptyText.visibility = View.VISIBLE
        emptyText.text = message
    }

    private fun openDialer() {
        if (contactUserId.isBlank()) {
            openDialerWithNumber(getFallbackPhoneNumber())
            return
        }

        db.collection("users")
            .document(contactUserId)
            .get()
            .addOnSuccessListener { document ->
                val phoneNumber = document.getString("phone")
                    ?: document.getString("phoneNumber")
                    ?: document.getString("mobile")
                    ?: document.getString("mobileNumber")
                    ?: getFallbackPhoneNumber()

                openDialerWithNumber(phoneNumber)
            }
            .addOnFailureListener {
                openDialerWithNumber(getFallbackPhoneNumber())
            }
    }

    private fun openDialerWithNumber(phoneNumber: String) {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        startActivity(dialIntent)
    }

    private fun getFallbackPhoneNumber(): String {
        return if (contactRole == "driver") {
            if (contactName.equals("Sulav", ignoreCase = true)) "9800000002" else "9800000001"
        } else {
            "9800000001"
        }
    }

    private fun formatTime(timestamp: Timestamp?): String {
        if (timestamp == null) return "Just now"

        return SimpleDateFormat(
            "dd MMM, h:mm a",
            Locale.getDefault()
        ).format(timestamp.toDate())
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        messageListener?.remove()
        messageListener = null
    }
}