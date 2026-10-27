package com.betshield.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.betshield.app.BetShieldApplication
import java.util.Locale

class QuitBetNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "QuitBetNotification"
        @Volatile
        var isNotificationServiceRunning: Boolean = false
            private set

        private val NOTIFICATION_GAMBLING_KEYWORDS = listOf(
            "free bet", "deposit bonus", "casino bonus", "place your bet",
            "odds boost", "cash out", "spins bonus", "jackpot alert",
            "poker tournament", "sportsbook promo", "reload bonus"
        )
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isNotificationServiceRunning = true
        Log.i(TAG, "QuitBet Notification Listener Service connected and active.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val app = BetShieldApplication.instance
        if (!app.preferencesManager.isShieldEnabled) return

        val packageName = sbn.packageName ?: return
        // Don't cancel our own notifications
        if (packageName == applicationContext.packageName) return

        val blocklist = app.blocklistDatabase

        // 1. Check if notification is from a gambling or custom blocked app
        val isBlockedApp = blocklist.isAppBlocked(packageName)

        // 2. Check if notification text contains gambling promotional triggers
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE).orEmpty().lowercase(Locale.ROOT)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty().lowercase(Locale.ROOT)
        val hasGamblingKeywords = NOTIFICATION_GAMBLING_KEYWORDS.any { kw ->
            title.contains(kw) || text.contains(kw)
        }

        if (isBlockedApp || hasGamblingKeywords) {
            Log.w(TAG, "BLOCKED notification from: $packageName (Title: '$title'). Cancelling notification.")
            
            // Immediately dismiss/cancel the notification so the user never sees or hears it
            cancelNotification(sbn.key)

            // Increment interception statistics
            app.preferencesManager.incrementInterceptionCount()
        }
    }

    override fun onListenerDisconnected() {
        isNotificationServiceRunning = false
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        isNotificationServiceRunning = false
        super.onDestroy()
    }
}
