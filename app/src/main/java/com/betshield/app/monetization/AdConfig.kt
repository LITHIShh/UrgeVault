package com.betshield.app.monetization

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration for Real Google AdMob Monetization.
 *
 * To earn real money from your app:
 * 1. Go to https://admob.google.com and create an AdMob account.
 * 2. Add your app "BetShield" and create:
 *    - Banner Ad Unit
 *    - Interstitial Ad Unit
 *    - Rewarded Video Ad Unit
 *    - App Open Ad Unit
 * 3. Paste your real AdMob IDs below or enter them inside the app settings!
 */
object AdConfig {

    private const val PREFS_NAME = "betshield_admob_config"
    private const val KEY_USE_REAL_ADS = "key_use_real_ads"
    private const val KEY_REAL_APP_ID = "key_real_app_id"
    private const val KEY_REAL_BANNER_ID = "key_real_banner_id"
    private const val KEY_REAL_INTERSTITIAL_ID = "key_real_interstitial_id"
    private const val KEY_REAL_REWARDED_ID = "key_real_rewarded_id"
    private const val KEY_REAL_APPOPEN_ID = "key_real_appopen_id"

    // Official Google Test Ad Unit IDs (used when Real Ads are not yet configured)
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_APPOPEN_ID = "ca-app-pub-3940256099942544/9257395921"

    /**
     * Set to TRUE to serve REAL live paying ads!
     * When true, AdMob will serve live commercial ads that generate revenue into your account.
     */
    var USE_REAL_PRODUCTION_ADS: Boolean = true

    fun isRealAdsEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_USE_REAL_ADS, USE_REAL_PRODUCTION_ADS)
    }

    fun setRealAdsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_USE_REAL_ADS, enabled).apply()
    }

    fun getBannerAdUnitId(context: Context): String {
        val prefs = getPrefs(context)
        val customId = prefs.getString(KEY_REAL_BANNER_ID, "")?.trim()
        return if (!customId.isNullOrEmpty() && isRealAdsEnabled(context)) {
            customId
        } else {
            TEST_BANNER_ID
        }
    }

    fun setBannerAdUnitId(context: Context, adUnitId: String) {
        getPrefs(context).edit().putString(KEY_REAL_BANNER_ID, adUnitId.trim()).apply()
    }

    fun getInterstitialAdUnitId(context: Context): String {
        val prefs = getPrefs(context)
        val customId = prefs.getString(KEY_REAL_INTERSTITIAL_ID, "")?.trim()
        return if (!customId.isNullOrEmpty() && isRealAdsEnabled(context)) {
            customId
        } else {
            TEST_INTERSTITIAL_ID
        }
    }

    fun setInterstitialAdUnitId(context: Context, adUnitId: String) {
        getPrefs(context).edit().putString(KEY_REAL_INTERSTITIAL_ID, adUnitId.trim()).apply()
    }

    fun getRewardedAdUnitId(context: Context): String {
        val prefs = getPrefs(context)
        val customId = prefs.getString(KEY_REAL_REWARDED_ID, "")?.trim()
        return if (!customId.isNullOrEmpty() && isRealAdsEnabled(context)) {
            customId
        } else {
            TEST_REWARDED_ID
        }
    }

    fun setRewardedAdUnitId(context: Context, adUnitId: String) {
        getPrefs(context).edit().putString(KEY_REAL_REWARDED_ID, adUnitId.trim()).apply()
    }

    fun getAppOpenAdUnitId(context: Context): String {
        val prefs = getPrefs(context)
        val customId = prefs.getString(KEY_REAL_APPOPEN_ID, "")?.trim()
        return if (!customId.isNullOrEmpty() && isRealAdsEnabled(context)) {
            customId
        } else {
            TEST_APPOPEN_ID
        }
    }

    fun setAppOpenAdUnitId(context: Context, adUnitId: String) {
        getPrefs(context).edit().putString(KEY_REAL_APPOPEN_ID, adUnitId.trim()).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
