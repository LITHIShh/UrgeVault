package com.betshield.app.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {

    private const val TAG = "BetShieldAds"

    // Official Google AdMob Test Ad Unit IDs
    const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_TEST_ID = "ca-app-pub-3940256099942544/5224354917"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    /**
     * Initialize MobileAds SDK. Call once in Application.onCreate().
     */
    fun initialize(context: Context) {
        MobileAds.initialize(context) { initializationStatus ->
            Log.i(TAG, "AdMob MobileAds initialized: ${initializationStatus.adapterStatusMap}")
        }
    }

    /**
     * Attaches and loads an AdMob Adaptive Banner into the provided container.
     */
    fun loadBanner(container: ViewGroup, context: Context) {
        try {
            val adView = AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BANNER_TEST_ID
            }
            container.removeAllViews()
            container.addView(adView)

            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading banner ad", e)
        }
    }

    /**
     * Preloads an Interstitial Ad to be shown between navigation or actions.
     */
    fun loadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_TEST_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.i(TAG, "Interstitial Ad loaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    Log.w(TAG, "Interstitial Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Displays the preloaded Interstitial Ad if available.
     */
    fun showInterstitial(activity: Activity, onClosed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial(activity) // Preload next
                    onClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    onClosed()
                }
            }
            ad.show(activity)
        } else {
            // Not ready yet, proceed immediately
            loadInterstitial(activity)
            onClosed()
        }
    }

    /**
     * Preloads a Rewarded Video Ad.
     */
    fun loadRewardedAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_TEST_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.i(TAG, "Rewarded Ad loaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    Log.w(TAG, "Rewarded Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Displays a Rewarded Video Ad (e.g. for temporary emergency bypass).
     */
    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit, onFailedOrDismissed: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            var earned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAd(activity)
                    if (earned) onRewardEarned() else onFailedOrDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    onFailedOrDismissed()
                }
            }
            ad.show(activity) { rewardItem ->
                earned = true
                Log.i(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            }
        } else {
            loadRewardedAd(activity)
            onFailedOrDismissed()
        }
    }
}
