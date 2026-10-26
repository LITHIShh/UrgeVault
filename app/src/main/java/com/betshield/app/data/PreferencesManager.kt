package com.betshield.app.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "betshield_settings"
        private const val KEY_SHIELD_ENABLED = "key_shield_enabled"
        private const val KEY_INTERCEPTION_COUNT = "key_interception_count"
        private const val KEY_CUSTOM_DOMAINS = "key_custom_domains"
        private const val KEY_CUSTOM_APPS = "key_custom_apps"
        private const val KEY_STRICT_MODE = "key_strict_mode"
        private const val KEY_UPSTREAM_DNS = "key_upstream_dns"

        // Default: Cloudflare Family (1.1.1.3 blocks malware and adult/gambling)
        const val DEFAULT_DNS_PRIMARY = "1.1.1.3"
        const val DEFAULT_DNS_SECONDARY = "1.0.0.3"
    }

    var isShieldEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHIELD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SHIELD_ENABLED, value).apply()

    var interceptionCount: Int
        get() = prefs.getInt(KEY_INTERCEPTION_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_INTERCEPTION_COUNT, value).apply()

    fun incrementInterceptionCount(): Int {
        val current = interceptionCount + 1
        interceptionCount = current
        return current
    }

    var customDomains: Set<String>
        get() = prefs.getStringSet(KEY_CUSTOM_DOMAINS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_CUSTOM_DOMAINS, value).apply()

    fun addCustomDomain(domain: String) {
        val current = customDomains.toMutableSet()
        current.add(domain.trim().lowercase())
        customDomains = current
    }

    fun removeCustomDomain(domain: String) {
        val current = customDomains.toMutableSet()
        current.remove(domain.trim().lowercase())
        customDomains = current
    }

    var customApps: Set<String>
        get() = prefs.getStringSet(KEY_CUSTOM_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_CUSTOM_APPS, value).apply()

    fun addCustomApp(packageName: String) {
        val current = customApps.toMutableSet()
        current.add(packageName.trim().lowercase())
        customApps = current
    }

    fun removeCustomApp(packageName: String) {
        val current = customApps.toMutableSet()
        current.remove(packageName.trim().lowercase())
        customApps = current
    }

    var isStrictMode: Boolean
        get() = prefs.getBoolean(KEY_STRICT_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_STRICT_MODE, value).apply()

    var upstreamDns: String
        get() = prefs.getString(KEY_UPSTREAM_DNS, DEFAULT_DNS_PRIMARY) ?: DEFAULT_DNS_PRIMARY
        set(value) = prefs.edit().putString(KEY_UPSTREAM_DNS, value).apply()
}
