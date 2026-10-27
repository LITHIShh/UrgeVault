package com.betshield.app.monetization

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

/**
 * Detects whether third-party Ad Blockers or private DNS filters
 * are active and blocking AdMob monetization domains.
 */
object AdBlockerDetector {

    private const val TAG = "AdBlockerDetector"
    private val AD_DOMAINS_TO_PROBE = listOf(
        "googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com"
    )

    @Volatile
    var isAdBlockerActive: Boolean = false
        private set

    /**
     * Checks if ad domains resolve to localhost or fail.
     */
    suspend fun checkAdBlockerActive(): Boolean = withContext(Dispatchers.IO) {
        try {
            for (domain in AD_DOMAINS_TO_PROBE) {
                val address = InetAddress.getByName(domain)
                val ip = address.hostAddress ?: ""
                if (ip.startsWith("127.") || ip == "0.0.0.0" || ip == "::1") {
                    Log.w(TAG, "AdBlocker sinkhole detected on $domain -> $ip")
                    isAdBlockerActive = true
                    return@withContext true
                }
            }
            isAdBlockerActive = false
            false
        } catch (e: Exception) {
            // Unresolvable or blocked by local host filter
            Log.w(TAG, "Ad domain failed to resolve, ad blocker suspected", e)
            isAdBlockerActive = true
            true
        }
    }

    fun markAdLoadFailed() {
        isAdBlockerActive = true
    }

    fun markAdLoadSucceeded() {
        isAdBlockerActive = false
    }
}
