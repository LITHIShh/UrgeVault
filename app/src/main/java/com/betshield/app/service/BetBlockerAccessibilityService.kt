package com.betshield.app.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
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
            "com.opera.gx", "com.duckduckgo.mobile.android", "com.android.browser", "com.UCMobile.intl",
            "com.vivaldi.browser", "com.kiwibrowser.browser", "mark.via.gp", "com.yandex.browser"
        )

        private val ADDRESS_BAR_VIEW_IDS = listOf(
            "com.android.chrome:id/url_bar",
            "com.android.chrome:id/search_box_text",
            "org.mozilla.firefox:id/url_bar_title",
            "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            "org.mozilla.firefox:id/toolbar_url",
            "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            "com.sec.android.app.sbrowser:id/url_bar",
            "com.microsoft.emmx:id/url_bar",
            "com.microsoft.emmx:id/search_box",
            "com.brave.browser:id/url_bar",
            "com.brave.browser:id/search_box_text",
            "com.opera.browser:id/url_field",
            "com.opera.gx:id/url_field",
            "com.duckduckgo.mobile.android:id/omnibar_text_input",
            "com.UCMobile.intl:id/address_bar"
        )

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            if (isServiceActive) return true
            val expectedComponentName = ComponentName(context, BetBlockerAccessibilityService::class.java).flattenToString()
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            return enabledServices.contains(expectedComponentName)
        }
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
        if (eventPackage == packageName) return // Don't intercept ourselves

        val blocklist = app.blocklistDatabase
        val passwordManager = app.passwordManager

        // 1. Anti-Tampering: Protect Settings from disabling BetShield without password
        if (eventPackage == "com.android.settings") {
            val textContent = extractEventText(event)
            val isTamperingAttempt = textContent.contains("urgevault", ignoreCase = true) ||
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

        // 2. Real-time Browser Address Bar & Navigation Inspection
        if (BROWSER_PACKAGES.contains(eventPackage)) {
            val detectedTarget = findBlockedUrlInBrowser(event)
            if (detectedTarget != null) {
                triggerBlock(detectedTarget)
                return
            }
        }

        // 3. Foreground App Package Detection
        val windowTitle = event.className?.toString()
        val isBlockedApp = blocklist.isAppBlocked(eventPackage, windowTitle)

        if (isBlockedApp) {
            Log.w(TAG, "BLOCKED application detected: $eventPackage")
            app.preferencesManager.incrementInterceptionCount()

            performGlobalAction(GLOBAL_ACTION_HOME)

            val blockIntent = Intent(this, BlockedActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(BlockedActivity.EXTRA_TARGET, eventPackage)
                putExtra(BlockedActivity.EXTRA_IS_APP, true)
            }
            startActivity(blockIntent)
        }
    }

    private fun triggerBlock(target: String) {
        val app = BetShieldApplication.instance
        Log.w(TAG, "BLOCKED gambling target in browser: $target")
        app.preferencesManager.incrementInterceptionCount()

        // Kick browser to home screen immediately
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Launch high-priority blocked screen with Urge SOS helper
        val blockIntent = Intent(this, BlockedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(BlockedActivity.EXTRA_TARGET, target)
            putExtra(BlockedActivity.EXTRA_IS_APP, false)
        }
        startActivity(blockIntent)
    }

    /**
     * Inspects only browser address bar nodes and typed inputs.
     * Guaranteed zero false positives on standard webpage body content.
     */
    private fun findBlockedUrlInBrowser(event: AccessibilityEvent): String? {
        val app = BetShieldApplication.instance
        val blocklist = app.blocklistDatabase

        // Check if event source directly belongs to an address/search bar
        val source = event.source
        if (source != null) {
            val viewId = source.viewIdResourceName?.lowercase() ?: ""
            if (isAddressBarNode(source, viewId)) {
                val text = source.text?.toString()
                if (!text.isNullOrBlank() && blocklist.isBrowserUrlBlocked(text)) {
                    return text
                }
            }
        }

        // Check window hierarchy address bar nodes
        val rootNode = rootInActiveWindow ?: source ?: return null

        // Phase 1: Direct lookup by known address bar IDs
        for (viewId in ADDRESS_BAR_VIEW_IDS) {
            try {
                val matches = rootNode.findAccessibilityNodeInfosByViewId(viewId)
                if (!matches.isNullOrEmpty()) {
                    for (node in matches) {
                        val text = node.text?.toString()
                        if (!text.isNullOrBlank() && blocklist.isBrowserUrlBlocked(text)) {
                            return text
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Phase 2: Scan top toolbar elements (depth <= 5), strictly excluding WebViews
        return scanToolbarForAddressBar(rootNode, 0)
    }

    private fun scanToolbarForAddressBar(node: AccessibilityNodeInfo, depth: Int): String? {
        if (depth > 5) return null // Address bars reside in the top levels of browser UI
        val app = BetShieldApplication.instance
        val blocklist = app.blocklistDatabase

        val className = node.className?.toString() ?: ""
        // Crucial: NEVER inspect inside web content / webview DOM to prevent false positives
        if (className.contains("WebView", ignoreCase = true)) {
            return null
        }

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        if (isAddressBarNode(node, viewId)) {
            val text = node.text?.toString()
            if (!text.isNullOrBlank() && blocklist.isBrowserUrlBlocked(text)) {
                return text
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = scanToolbarForAddressBar(child, depth + 1)
            if (found != null) return found
        }
        return null
    }

    private fun isAddressBarNode(node: AccessibilityNodeInfo, viewId: String): Boolean {
        if (ADDRESS_BAR_VIEW_IDS.contains(viewId)) return true
        if (viewId.contains("url_bar") || viewId.contains("urlbar") ||
            viewId.contains("location_bar") || viewId.contains("address_bar") ||
            viewId.contains("omnibar") || viewId.contains("search_box_text")) {
            return true
        }
        if (node.isEditable && (viewId.contains("url") || viewId.contains("address") || viewId.contains("location"))) {
            return true
        }
        return false
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
