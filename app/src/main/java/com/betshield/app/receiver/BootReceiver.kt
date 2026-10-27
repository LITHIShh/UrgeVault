package com.betshield.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.betshield.app.data.PreferencesManager
import com.betshield.app.service.BetShieldVpnService

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BetShieldBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(TAG, "Device reboot/power-on detected: $action")

        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )

        if (validActions.contains(action)) {
            val prefs = PreferencesManager(context)
            if (prefs.isShieldEnabled) {
                // If VPN permission was previously granted, prepare() returns null
                if (VpnService.prepare(context) == null) {
                    val vpnIntent = Intent(context, BetShieldVpnService::class.java).apply {
                        this.action = BetShieldVpnService.ACTION_START
                    }
                    try {
                        ContextCompat.startForegroundService(context, vpnIntent)
                        Log.i(TAG, "BetShield VPN service auto-started on phone reboot.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to auto-start VPN on boot", e)
                    }
                }
            }
        }
    }
}
