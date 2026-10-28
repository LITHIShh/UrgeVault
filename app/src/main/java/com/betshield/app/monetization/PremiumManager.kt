package com.betshield.app.monetization

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.betshield.app.BetShieldApplication
import com.betshield.app.util.HapticEngine
import java.util.UUID

class PremiumManager(private val context: Context) {

    companion object {
        const val TIER_FREE = "free"
        const val TIER_MONTHLY = "monthly"
        const val TIER_ANNUAL = "annual"
        const val TIER_LIFETIME = "lifetime"

        val PROMO_CODES = mapOf(
            "FREEDOM2026" to TIER_LIFETIME,
            "SOBERLIFETIME" to TIER_LIFETIME,
            "ALLYPRO" to TIER_LIFETIME,
            "CLEANSTART" to TIER_ANNUAL
        )
    }

    private val prefs get() = BetShieldApplication.instance.preferencesManager

    val isPro: Boolean
        get() = prefs.isProUser

    val currentTier: String
        get() = prefs.proTier

    fun getTierDisplayName(): String {
        return when (currentTier) {
            TIER_ANNUAL -> "UrgeVault PRO • Annual Sanctuary"
            TIER_MONTHLY -> "UrgeVault PRO • Monthly Shield"
            TIER_LIFETIME -> "UrgeVault PRO • Lifetime Sanctuary"
            else -> "UrgeVault Free"
        }
    }

    fun activatePro(tier: String, transactionId: String = UUID.randomUUID().toString()) {
        prefs.isProUser = true
        prefs.proTier = tier
    }

    fun deactivatePro() {
        prefs.isProUser = false
        prefs.proTier = TIER_FREE
        prefs.is24HourCoolDownEnabled = false
        prefs.coolDownExpiryTimestamp = 0L
    }

    /**
     * Redeems a valid lifetime or promotional code.
     */
    fun redeemPromoCode(code: String): Pair<Boolean, String> {
        val cleanCode = code.trim().uppercase()
        val targetTier = PROMO_CODES[cleanCode]
        return if (targetTier != null) {
            activatePro(targetTier, "PROMO_${cleanCode}")
            Pair(true, "🎉 Promo code verified! You now have permanent access to UrgeVault PRO.")
        } else {
            Pair(false, "Invalid promo code. Please enter a valid VIP or partner pass.")
        }
    }

    /**
     * Restores previous purchases if any license is present.
     */
    fun restorePurchases(): Boolean {
        return if (prefs.isProUser) {
            true
        } else {
            false
        }
    }
}
