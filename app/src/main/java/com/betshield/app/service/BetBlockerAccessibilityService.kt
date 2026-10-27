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

        // 2. Real-time Browser URL Inspection
        if (BROWSER_PACKAGES.contains(eventPackage)) {
            val rootNode = rootInActiveWindow ?: event.source
            if (rootNode != null) {
                val detectedBlockedUrl = findBlockedUrlInBrowser(rootNode)
                if (detectedBlockedUrl != null) {
                    Log.w(TAG, "BLOCKED website in browser: $detectedBlockedUrl")
                    app.preferencesManager.incrementInterceptionCount()

                    // Kick browser to home screen immediately
                    performGlobalAction(GLOBAL_ACTION_HOME)

                    // Launch high-priority blocked screen
                    val blockIntent = Intent(this, BlockedActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(BlockedActivity.EXTRA_TARGET, detectedBlockedUrl)
                        putExtra(BlockedActivity.EXTRA_IS_APP, false)
                    }
                    startActivity(blockIntent)
                    return
                }
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

    private fun findBlockedUrlInBrowser(rootNode: AccessibilityNodeInfo): String? {
        val app = BetShieldApplication.instance
        val blocklist = app.blocklistDatabase

        // Phase 1: Check known address bar view IDs directly
        for (viewId in ADDRESS_BAR_VIEW_IDS) {
            try {
                val matches = rootNode.findAccessibilityNodeInfosByViewId(viewId)
                if (!matches.isNullOrEmpty()) {
                    for (node in matches) {
                        val text = node.text?.toString()
                        if (!text.isNullOrBlank()) {
                            val clean = cleanUrl(text)
                            if (clean != null && blocklist.isDomainBlocked(clean)) {
                                return clean
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Phase 2: Recursive scan for any address node or blocked domain in the node tree
        return scanNodeHierarchyForBlockedUrl(rootNode)
    }

    private fun scanNodeHierarchyForBlockedUrl(node: AccessibilityNodeInfo): String? {
        val app = BetShieldApplication.instance
        val blocklist = app.blocklistDatabase

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        val isAddressNode = viewId.contains("url") || viewId.contains("address") ||
                viewId.contains("location") || viewId.contains("omnibar") || viewId.contains("search")

        if (text.isNotBlank()) {
            val clean = cleanUrl(text)
            if (clean != null && blocklist.isDomainBlocked(clean)) {
                return clean
            }
        }

        if (desc.isNotBlank()) {
            val clean = cleanUrl(desc)
            if (clean != null && blocklist.isDomainBlocked(clean)) {
                return clean
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = scanNodeHierarchyForBlockedUrl(child)
            if (found != null) return found
        }
        return null
    }

    private fun cleanUrl(raw: String): String? {
        var str = raw.trim().lowercase()
        if (str.startsWith("https://")) str = str.substring(8)
        if (str.startsWith("http://")) str = str.substring(7)
        if (str.startsWith("www.")) str = str.substring(4)

        val slashIndex = str.indexOf('/')
        if (slashIndex != -1) {
            str = str.substring(0, slashIndex)
        }
        val colonIndex = str.indexOf(':')
        if (colonIndex != -1) {
            str = str.substring(0, colonIndex)
        }

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
