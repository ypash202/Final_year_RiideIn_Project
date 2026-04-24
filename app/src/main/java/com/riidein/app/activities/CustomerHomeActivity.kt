package com.riidein.app.activities

import android.content.Intent
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riidein.app.R

class CustomerHomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val mapWebView = findViewById<WebView>(R.id.mapWebView)
        val menuButton = findViewById<ImageButton>(R.id.menuButton)
        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        val searchBox = findViewById<TextView>(R.id.searchBox)
        val promoButton = findViewById<Button>(R.id.promoButton)
        val locationButton = findViewById<ImageButton>(R.id.locationButton)

        setupMapWebView(mapWebView)

        onBackPressedDispatcher.addCallback(this) {
            if (mapWebView.canGoBack()) {
                mapWebView.goBack()
            } else {
                finish()
            }
        }

        menuButton.setOnClickListener {
            startActivity(Intent(this@CustomerHomeActivity, SideMenuActivity::class.java).apply {
                putExtra("user_role", "customer")
                putExtra("selected_menu", "home")
            })
        }

        closeButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        searchBox.setOnClickListener {
            startActivity(Intent(this@CustomerHomeActivity, EnterRouteActivity::class.java))
        }

        locationButton.setOnClickListener {
            Toast.makeText(this, "Location feature coming soon", Toast.LENGTH_SHORT).show()
        }

        promoButton.setOnClickListener {
            Toast.makeText(this, "Promo feature coming soon", Toast.LENGTH_SHORT).show()
        }

        loadCustomerName()
    }

    private fun setupMapWebView(webView: WebView) {
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.loadsImagesAutomatically = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true

        webView.loadUrl("file:///android_asset/map.html")
    }

    private fun loadCustomerName() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val customerName = document.getString("name") ?: ""
                    val customerNameText = findViewById<TextView>(R.id.customerNameText)
                    customerNameText.text = customerName
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load customer name", Toast.LENGTH_SHORT).show()
            }
    }
}