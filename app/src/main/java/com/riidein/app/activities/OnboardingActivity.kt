package com.riidein.app.activities
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.riidein.app.OnboardingAdapter
import com.riidein.app.OnboardingItem
import com.riidein.app.R

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val dotsLayout = findViewById<LinearLayout>(R.id.dotsLayout)

        val items = listOf(
            OnboardingItem(
                R.drawable.intro1,
                "Get your Ride",
                "Please login to book your Ride taxi or courier service."
            )
        )

        val adapter = OnboardingAdapter(items)
        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                for (i in 0 until dotsLayout.childCount) {
                    val dot = dotsLayout.getChildAt(i)

                    if (i == position) {
                        dot.alpha = 1f
                    } else {
                        dot.alpha = 0.3f
                    }
                }

                // find the button inside the current page
                val pageView = viewPager.getChildAt(0)
                val button = pageView.findViewById<Button>(R.id.getStartedButton)

                button?.setOnClickListener {

                    val intent = Intent(this@OnboardingActivity, LoginActivity::class.java)
                    startActivity(intent)

                }
            }
        })
    }
}