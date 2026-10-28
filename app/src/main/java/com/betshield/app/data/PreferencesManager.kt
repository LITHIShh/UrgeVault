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

        // Category Blocker Keys
        private const val KEY_BLOCK_GAMBLING = "key_block_gambling"
        private const val KEY_BLOCK_ADULT = "key_block_adult"
        private const val KEY_BLOCK_SOCIAL = "key_block_social"
        private const val KEY_BLOCK_GAMING = "key_block_gaming"
        private const val KEY_BLOCK_CRYPTO = "key_block_crypto"

        const val THEME_AUTO = "auto"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

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

    // Category Toggles
    var isGamblingBlockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_GAMBLING, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_GAMBLING, value).apply()

    var isAdultBlockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_ADULT, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_ADULT, value).apply()

    var isSocialMediaBlockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_SOCIAL, false)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_SOCIAL, value).apply()

    var isGamingBlockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_GAMING, false)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_GAMING, value).apply()

    var isCryptoBlockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_CRYPTO, false)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_CRYPTO, value).apply()

    // Custom Domains (Websites)
    var customDomains: Set<String>
        get() = prefs.getStringSet(KEY_CUSTOM_DOMAINS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_CUSTOM_DOMAINS, value).apply()

    fun addCustomDomain(domain: String) {
        val current = customDomains.toMutableSet()
        val cleanDomain = domain.trim().lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .split("/")[0]
        if (cleanDomain.isNotEmpty()) {
            current.add(cleanDomain)
            customDomains = current
        }
    }

    fun removeCustomDomain(domain: String) {
        val current = customDomains.toMutableSet()
        current.remove(domain.trim().lowercase())
        customDomains = current
    }

    // Custom Apps
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

    var isTutorialCompleted: Boolean
        get() = prefs.getBoolean("key_tutorial_completed", false)
        set(value) = prefs.edit().putBoolean("key_tutorial_completed", value).apply()

    var appThemeMode: String
        get() = prefs.getString("key_app_theme_mode", THEME_AUTO) ?: THEME_AUTO
        set(value) = prefs.edit().putString("key_app_theme_mode", value).apply()

    // Global Internationalization & Dynamic Currency
    var isRegionSetupCompleted: Boolean
        get() = prefs.getBoolean("key_region_setup_completed", false)
        set(value) = prefs.edit().putBoolean("key_region_setup_completed", value).apply()

    var selectedLanguageTag: String
        get() = prefs.getString("key_selected_language_tag", "en-US") ?: "en-US"
        set(value) = prefs.edit().putString("key_selected_language_tag", value).apply()

    var selectedCurrencySymbol: String
        get() = prefs.getString("key_selected_currency_symbol", "$") ?: "$"
        set(value) = prefs.edit().putString("key_selected_currency_symbol", value).apply()

    var selectedCurrencyCode: String
        get() = prefs.getString("key_selected_currency_code", "USD") ?: "USD"
        set(value) = prefs.edit().putString("key_selected_currency_code", value).apply()

    // Emergency Support Ally
    var allyName: String
        get() = prefs.getString("key_ally_name", "") ?: ""
        set(value) = prefs.edit().putString("key_ally_name", value).apply()

    var allyPhone: String
        get() = prefs.getString("key_ally_phone", "") ?: ""
        set(value) = prefs.edit().putString("key_ally_phone", value).apply()

    // Optional VPN Protection (false = Browser Guardian Only, true = Browser + VPN Sinkhole)
    var isVpnProtectionEnabled: Boolean
        get() = prefs.getBoolean("key_vpn_protection_enabled", false)
        set(value) = prefs.edit().putBoolean("key_vpn_protection_enabled", value).apply()

    // UrgeVault PRO Tier & Hardened Friction Defense
    var isProUser: Boolean
        get() = prefs.getBoolean("key_is_pro_user", false)
        set(value) = prefs.edit().putBoolean("key_is_pro_user", value).apply()

    var proTier: String
        get() = prefs.getString("key_pro_tier", "free") ?: "free"
        set(value) = prefs.edit().putString("key_pro_tier", value).apply()

    // 24-Hour Mandatory Cool-Down Delay (Relapse-Proof Armor)
    var is24HourCoolDownEnabled: Boolean
        get() = prefs.getBoolean("key_24h_cooldown_enabled", false)
        set(value) = prefs.edit().putBoolean("key_24h_cooldown_enabled", value).apply()

    var coolDownExpiryTimestamp: Long
        get() = prefs.getLong("key_cooldown_expiry_timestamp", 0L)
        set(value) = prefs.edit().putLong("key_cooldown_expiry_timestamp", value).apply()

    // Accountability Ally Secret PIN Lock
    var isAllyPinEnabled: Boolean
        get() = prefs.getBoolean("key_ally_pin_enabled", false)
        set(value) = prefs.edit().putBoolean("key_ally_pin_enabled", value).apply()

    // Device Admin Anti-Uninstall Hardened Protection
    var isDeviceAdminProtectionEnabled: Boolean
        get() = prefs.getBoolean("key_device_admin_protection_enabled", false)
        set(value) = prefs.edit().putBoolean("key_device_admin_protection_enabled", value).apply()
}
