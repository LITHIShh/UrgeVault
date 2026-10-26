package com.betshield.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.betshield.app.BetShieldApplication
import com.betshield.app.ui.BlockedActivity
import com.betshield.app.ui.PasswordUnlockActivity

class BetBlockerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "BetBlockerAccessibility"
        @Volatile
        var isServiceActive: Boolean = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceActive = true
        Log.i(TAG, "BetBlocker Accessibility Service connected and active.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val app = BetShieldApplication.instance
        if (!app.preferencesManager.isShieldEnabled) return

        val eventPackage = event.packageName?.toString() ?: return
        val currentPackage = packageName // BetShield's package name

        // Don't intercept ourselves
        if (eventPackage == currentPackage) return

        val blocklist = app.blocklistDatabase
        val passwordManager = app.passwordManager

        // 1. Check for Anti-Tampering (Attempting to Force Stop, Uninstall, or Disable in Settings)
        if (eventPackage == "com.android.settings") {
            val textContent = extractEventText(event)
            val isTamperingAttempt = textContent.contains("betshield", ignoreCase = true) ||
                    textContent.contains("uninstall", ignoreCase = true) ||
                    textContent.contains("force stop", ignoreCase = true)

            if (isTamperingAttempt && passwordManager.isPasswordSet() && !passwordManager.isTemporarilyUnlocked()) {
                Log.w(TAG, "Tamper attempt detected in Settings. Forcing unlock authorization.")
                performGlobalAction(GLOBAL_ACTION_HOME)

                val unlockIntent = Intent(this, PasswordUnlockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(PasswordUnlockActivity.EXTRA_REASON, "Tamper Protection: Authorization Required")
                }
                startActivity(unlockIntent)
                return
            }
        }

        // 2. Check if the foreground application is a gambling/betting platform
        val windowTitle = event.className?.toString()
        val isBlockedApp = blocklist.isAppBlocked(eventPackage, windowTitle)

        if (isBlockedApp) {
            Log.w(TAG, "BLOCKED gambling application detected: $eventPackage")
            app.preferencesManager.incrementInterceptionCount()

            // Kick user immediately to home screen
            performGlobalAction(GLOBAL_ACTION_HOME)

            // Display high-priority motivational block overlay
            val blockIntent = Intent(this, BlockedActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(BlockedActivity.EXTRA_TARGET, eventPackage)
                putExtra(BlockedActivity.EXTRA_IS_APP, true)
            }
            startActivity(blockIntent)
        }
    }

    private fun extractEventText(event: AccessibilityEvent): String {
        val sb = StringBuilder()
        for (text in event.text) {
            sb.append(text).append(" ")
        }
        val contentDesc = event.contentDescription
        if (contentDesc != null) {
            sb.append(contentDesc).append(" ")
        }
        return sb.toString()
    }

    override fun onInterrupt() {
        Log.w(TAG, "BetBlocker Accessibility Service interrupted.")
    }

    override fun onDestroy() {
        isServiceActive = false
        super.onDestroy()
    }
}
