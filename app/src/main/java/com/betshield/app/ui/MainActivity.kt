package com.betshield.app.ui

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.betshield.app.BetShieldApplication
import com.betshield.app.R
import com.betshield.app.databinding.ActivityMainBinding
import com.betshield.app.monetization.AdManager
import com.betshield.app.service.BetBlockerAccessibilityService
import com.betshield.app.service.BetShieldVpnService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val app = BetShieldApplication.instance

    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
            updateUi()
        } else {
            Toast.makeText(this, "VPN Permission is needed to filter websites.", Toast.LENGTH_SHORT).show()
            binding.switchProtection.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check first launch master password setup
        if (!app.passwordManager.isPasswordSet()) {
            promptPasswordSetup()
        }

        setupListeners()

        // Monetization: Load AdMob Banner & Preload Ads
        AdManager.loadBanner(binding.adBannerContainer, this)
        AdManager.loadInterstitial(this)
        AdManager.loadRewardedAd(this)
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun setupListeners() {
        // Master Protection Toggle
        binding.switchProtection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Turning ON Protection
                app.preferencesManager.isShieldEnabled = true
                enableProtection()
                updateUi()
            } else {
                // User is attempting to Turn OFF Protection -> Must pass Password Lock!
                if (app.passwordManager.isPasswordSet()) {
                    PasswordDialog(
                        context = this,
                        onSuccess = {
                            app.preferencesManager.isShieldEnabled = false
                            disableProtection()
                            updateUi()
                            Toast.makeText(this, "Protection paused.", Toast.LENGTH_SHORT).show()
                        },
                        onCancel = {
                            // User cancelled or failed -> revert switch back to ON
                            binding.switchProtection.isChecked = true
                        }
                    ).show()
                } else {
                    app.preferencesManager.isShieldEnabled = false
                    disableProtection()
                    updateUi()
                }
            }
        }

        // Top Lock Button (Manage Password)
        binding.btnPasswordSettings.setOnClickListener {
            if (app.passwordManager.isPasswordSet()) {
                PasswordDialog(
                    context = this,
                    onSuccess = {
                        Toast.makeText(this, "Master Password verified.", Toast.LENGTH_SHORT).show()
                        promptPasswordSetup()
                    }
                ).show()
            } else {
                promptPasswordSetup()
            }
        }

        // Custom App & Website Blocker
        binding.btnCustomBlocker.setOnClickListener {
            startActivity(Intent(this, CustomBlockerActivity::class.java))
        }

        // Rewarded Ad Monetization: Emergency 10-Minute Access
        binding.btnEmergencyBypass.setOnClickListener {
            AdManager.showRewardedAd(
                activity = this,
                onRewardEarned = {
                    app.passwordManager.unlockSession(10)
                    Toast.makeText(this, "Ad watched! 10-minute temporary unlock granted.", Toast.LENGTH_LONG).show()
                },
                onFailedOrDismissed = {
                    Toast.makeText(this, "Watch the full sponsor ad to receive temporary unlock.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Permissions Guide
        binding.btnPermissionsSetup.setOnClickListener {
            startActivity(Intent(this, SetupPermissionsActivity::class.java))
        }

        // Blocklist Explorer
        binding.btnManageBlocklist.setOnClickListener {
            startActivity(Intent(this, BlocklistActivity::class.java))
        }

        binding.cardVpnStatus.setOnClickListener {
            startActivity(Intent(this, SetupPermissionsActivity::class.java))
        }

        binding.cardAccessibilityStatus.setOnClickListener {
            startActivity(Intent(this, SetupPermissionsActivity::class.java))
        }
    }

    private fun enableProtection() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnPrepareLauncher.launch(vpnIntent)
        } else {
            startVpnService()
        }
    }

    private fun disableProtection() {
        val intent = Intent(this, BetShieldVpnService::class.java).apply {
            action = BetShieldVpnService.ACTION_STOP
        }
        startService(intent)
    }

    private fun startVpnService() {
        val intent = Intent(this, BetShieldVpnService::class.java).apply {
            action = BetShieldVpnService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun promptPasswordSetup() {
        PasswordDialog(
            context = this,
            onSuccess = {
                Toast.makeText(this, "Master password set successfully.", Toast.LENGTH_SHORT).show()
                updateUi()
            }
        ).show()
    }

    private fun updateUi() {
        val isShieldActive = app.preferencesManager.isShieldEnabled

        // Switch
        binding.switchProtection.setOnCheckedChangeListener(null)
        binding.switchProtection.isChecked = isShieldActive
        binding.switchProtection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                app.preferencesManager.isShieldEnabled = true
                enableProtection()
                updateUi()
            } else {
                if (app.passwordManager.isPasswordSet()) {
                    PasswordDialog(
                        context = this,
                        onSuccess = {
                            app.preferencesManager.isShieldEnabled = false
                            disableProtection()
                            updateUi()
                            Toast.makeText(this, "Protection paused.", Toast.LENGTH_SHORT).show()
                        },
                        onCancel = {
                            binding.switchProtection.isChecked = true
                        }
                    ).show()
                } else {
                    app.preferencesManager.isShieldEnabled = false
                    disableProtection()
                    updateUi()
                }
            }
        }

        // Shield Hero Card
        if (isShieldActive) {
            binding.ivShieldIcon.setImageResource(R.drawable.ic_shield_check)
            binding.tvShieldState.text = getString(R.string.status_protected)
            binding.tvShieldState.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            binding.tvShieldStateDesc.text = getString(R.string.status_protected_desc)
        } else {
            binding.ivShieldIcon.setImageResource(R.drawable.ic_shield_off)
            binding.tvShieldState.text = getString(R.string.status_unprotected)
            binding.tvShieldState.setTextColor(ContextCompat.getColor(this, R.color.danger))
            binding.tvShieldStateDesc.text = getString(R.string.status_unprotected_desc)
        }

        // Stats
        binding.tvStatDomains.text = "${app.blocklistDatabase.defaultDomains.size + app.preferencesManager.customDomains.size}"
        binding.tvStatApps.text = "${app.blocklistDatabase.defaultAppPackages.size + app.preferencesManager.customApps.size}"
        binding.tvStatInterceptions.text = "${app.preferencesManager.interceptionCount}"

        // Engine Badges
        val vpnActive = BetShieldVpnService.isRunning
        binding.tvVpnBadge.text = if (vpnActive) "ACTIVE" else "INACTIVE"
        binding.tvVpnBadge.setTextColor(
            ContextCompat.getColor(this, if (vpnActive) R.color.shield_active else R.color.shield_inactive)
        )

        val accessibilityActive = BetBlockerAccessibilityService.isServiceActive
        binding.tvAccessibilityBadge.text = if (accessibilityActive) "ACTIVE" else "SETUP NEEDED"
        binding.tvAccessibilityBadge.setTextColor(
            ContextCompat.getColor(this, if (accessibilityActive) R.color.shield_active else R.color.warning)
        )
    }
}
