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

        // Dynamic SOS Grounding Quote
        val randomQuote = com.betshield.app.data.QuoteManager.getRandomQuote()
        binding.tvQuote.text = "\"${randomQuote.quote}\"\n— ${randomQuote.author}"

        // Urgent Action: 60-Second Grounding Breath (Urge SOS)
        binding.btnUrgeSosGrounding.setOnClickListener {
            val sosIntent = Intent(this, UrgeSosActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(sosIntent)
            finish()
        }

        // Helpline or Accountability Ally Dialing
        val prefs = com.betshield.app.BetShieldApplication.instance.preferencesManager
        val hasAlly = prefs.allyPhone.isNotBlank()
        if (hasAlly) {
            binding.tvHelpline.text = "Call Ally: ${prefs.allyName} (${prefs.allyPhone})"
        }

        binding.tvHelpline.setOnClickListener {
            val phoneUri = if (hasAlly) {
                Uri.parse("tel:${prefs.allyPhone.trim()}")
            } else {
                Uri.parse("tel:18005224700")
            }
            val phoneIntent = Intent(Intent.ACTION_DIAL).apply {
                data = phoneUri
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
