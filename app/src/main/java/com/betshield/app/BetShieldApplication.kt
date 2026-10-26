package com.betshield.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.betshield.app.data.BlocklistDatabase
import com.betshield.app.data.PasswordManager
import com.betshield.app.data.PreferencesManager

class BetShieldApplication : Application() {

    companion object {
        const val VPN_NOTIFICATION_CHANNEL_ID = "betshield_vpn_channel"
        lateinit var instance: BetShieldApplication
            private set
    }

    lateinit var preferencesManager: PreferencesManager
    lateinit var passwordManager: PasswordManager
    lateinit var blocklistDatabase: BlocklistDatabase

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferencesManager = PreferencesManager(this)
        passwordManager = PasswordManager(this)
        blocklistDatabase = BlocklistDatabase(this)

        // Initialize AdMob Monetization
        com.betshield.app.monetization.AdManager.initialize(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "BetShield Protection Service"
            val descriptionText = "Displays status of active web and app blocking."
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(VPN_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
