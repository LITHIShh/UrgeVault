package com.betshield.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.betshield.app.BetShieldApplication
import com.betshield.app.ui.BlockedActivity
import com.betshield.app.ui.PasswordUnlockActivity

class BetBlockerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "BetBlockerAccessibility"
        @Volatile
        var isServiceActive: Boolean = false
            private set

        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary",
            "org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.focus",
            "com.sec.android.app.sbrowser", "com.sec.android.app.sbrowser.beta",
            "com.microsoft.emmx", "com.brave.browser", "com.opera.browser", "com.opera.mini.native",
            "com.duckduckgo.mobile.android", "com.android.browser", "com.UCMobile.intl"
        )
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
            val isTamperingAttempt = textContent.contains("quitbet", ignoreCase = true) ||
                    textContent.contains("betshield", ignoreCase = true) ||
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

        // 2. Browser Website Monitoring (Deep URL Bar Inspection for Chrome, Edge, Firefox, Samsung Internet)
        if (BROWSER_PACKAGES.contains(eventPackage)) {
            val rootNode = rootInActiveWindow ?: event.source
            if (rootNode != null) {
                val detectedUrl = findUrlInNode(rootNode)
                if (detectedUrl != null && blocklist.isDomainBlocked(detectedUrl)) {
                    Log.w(TAG, "BLOCKED gambling website in browser: $detectedUrl")
                    app.preferencesManager.incrementInterceptionCount()

                    // Kick browser to home screen immediately
                    performGlobalAction(GLOBAL_ACTION_HOME)

                    // Launch high-priority blocked screen
                    val blockIntent = Intent(this, BlockedActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(BlockedActivity.EXTRA_TARGET, detectedUrl)
                        putExtra(BlockedActivity.EXTRA_IS_APP, false)
                    }
                    startActivity(blockIntent)
                    return
                }
            }
        }

        // 3. Check if foreground app is a gambling/betting platform or custom blocked app
        val windowTitle = event.className?.toString()
        val isBlockedApp = blocklist.isAppBlocked(eventPackage, windowTitle)

        if (isBlockedApp) {
            Log.w(TAG, "BLOCKED gambling/custom application detected: $eventPackage")
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

    private fun findUrlInNode(node: AccessibilityNodeInfo): String? {
        val text = node.text?.toString()
        if (!text.isNullOrBlank()) {
            val cleaned = cleanUrl(text)
            if (cleaned != null) return cleaned
        }

        val contentDesc = node.contentDescription?.toString()
        if (!contentDesc.isNullOrBlank()) {
            val cleaned = cleanUrl(contentDesc)
            if (cleaned != null) return cleaned
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findUrlInNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun cleanUrl(raw: String): String? {
        var str = raw.trim().lowercase()
        // Strip common prefixes
        if (str.startsWith("https://")) str = str.substring(8)
        if (str.startsWith("http://")) str = str.substring(7)
        if (str.startsWith("www.")) str = str.substring(4)

        // Strip path and query parameters
        val slashIndex = str.indexOf('/')
        if (slashIndex != -1) {
            str = str.substring(0, slashIndex)
        }
        val colonIndex = str.indexOf(':')
        if (colonIndex != -1) {
            str = str.substring(0, colonIndex)
        }

        // Check if looks like domain name
        if (str.contains(".") && !str.contains(" ") && str.length > 3) {
            return str
        }
        return null
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
