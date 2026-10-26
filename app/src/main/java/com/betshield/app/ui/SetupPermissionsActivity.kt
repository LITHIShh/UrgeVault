package com.betshield.app.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.betshield.app.databinding.ActivitySetupPermissionsBinding
import com.betshield.app.receiver.BetShieldDeviceAdminReceiver
import com.betshield.app.service.BetBlockerAccessibilityService
import com.betshield.app.service.BetShieldVpnService

class SetupPermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupPermissionsBinding

    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
            updateUiState()
        } else {
            Toast.makeText(this, "VPN Permission was denied.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
    }

    private fun setupListeners() {
        // 1. VPN Permission
        binding.btnGrantVpn.setOnClickListener {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                vpnPrepareLauncher.launch(vpnIntent)
            } else {
                startVpnService()
                updateUiState()
            }
        }

        // 2. Accessibility Permission
        binding.btnGrantAccessibility.setOnClickListener {
            Toast.makeText(this, "Find 'BetShield' in the list and switch it ON", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        // 3. Device Administrator
        binding.btnGrantAdmin.setOnClickListener {
            val component = ComponentName(this, BetShieldDeviceAdminReceiver::class.java)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Enabling Device Admin prevents uninstallation of BetShield without entering the master password."
                )
            }
            startActivity(intent)
        }

        // 4. Battery Optimization
        binding.btnIgnoreBattery.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
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

    private fun updateUiState() {
        // Check VPN
        if (BetShieldVpnService.isRunning) {
            binding.btnGrantVpn.text = "✓ Web Filter Enabled"
            binding.btnGrantVpn.isEnabled = false
        } else {
            binding.btnGrantVpn.text = "Enable Web Filter"
            binding.btnGrantVpn.isEnabled = true
        }

        // Check Accessibility
        if (BetBlockerAccessibilityService.isServiceActive) {
            binding.btnGrantAccessibility.text = "✓ App Guardian Enabled"
            binding.btnGrantAccessibility.isEnabled = false
        } else {
            binding.btnGrantAccessibility.text = "Enable App Guardian"
            binding.btnGrantAccessibility.isEnabled = true
        }

        // Check Device Admin
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, BetShieldDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) {
            binding.btnGrantAdmin.text = "✓ Device Admin Active"
            binding.btnGrantAdmin.isEnabled = false
        } else {
            binding.btnGrantAdmin.text = "Activate Device Admin"
            binding.btnGrantAdmin.isEnabled = true
        }

        // Check Battery Saver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isIgnoringBatteryOptimizations(packageName)) {
                binding.btnIgnoreBattery.text = "✓ Battery Exemption Active"
                binding.btnIgnoreBattery.isEnabled = false
            }
        }
    }
}
