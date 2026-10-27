package com.betshield.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.betshield.app.databinding.ActivityBlockedBinding

class BlockedActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TARGET = "extra_target"
        const val EXTRA_IS_APP = "extra_is_app"
    }

    private lateinit var binding: ActivityBlockedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val target = intent.getStringExtra(EXTRA_TARGET) ?: "Gambling Platform"
        val isApp = intent.getBooleanExtra(EXTRA_IS_APP, false)

        // Haptic warning feedback on block encounter
        com.betshield.app.util.HapticEngine.performWarning(context = this)

        binding.tvBlockedTarget.text = if (isApp) "App: $target" else "Domain: $target"

        // Helpline Phone Call Intent
        binding.tvHelpline.setOnClickListener {
            val phoneIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:18005224700")
            }
            startActivity(phoneIntent)
        }

        // Return Home
        binding.btnReturnHome.setOnClickListener {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finish()
        }
    }

    override fun onBackPressed() {
        // Prevent back button from returning to blocked gambling app
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}
