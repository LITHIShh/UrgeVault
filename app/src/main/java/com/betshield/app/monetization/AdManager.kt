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
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {

    private const val TAG = "BetShieldAds"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var isShowingAppOpenAd = false

    /**
     * Initialize MobileAds SDK.
     */
    fun initialize(context: Context) {
        MobileAds.initialize(context) { initializationStatus ->
            Log.i(TAG, "AdMob MobileAds initialized successfully. Real Ads Enabled: ${AdConfig.isRealAdsEnabled(context)}")
        }
    }

    /**
     * Attaches and loads a real or test AdMob Adaptive Banner into the provided container.
     */
    fun loadBanner(container: ViewGroup, context: Context, onAdBlockerDetected: (() -> Unit)? = null) {
        try {
            val adUnitId = AdConfig.getBannerAdUnitId(context)
            val adView = AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
            }
            adView.adListener = object : com.google.android.gms.ads.AdListener() {
                override fun onAdLoaded() {
                    AdBlockerDetector.markAdLoadSucceeded()
                    Log.i(TAG, "Banner Ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdBlockerDetector.markAdLoadFailed()
                    Log.w(TAG, "Banner Ad failed to load: ${error.message} (code ${error.code})")
                    if (error.code == com.google.android.gms.ads.AdRequest.ERROR_CODE_NETWORK_ERROR) {
                        onAdBlockerDetected?.invoke()
                    }
                }
            }

            container.removeAllViews()
            container.addView(adView)

            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            Log.i(TAG, "Loading Banner Ad with Unit: $adUnitId")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading banner ad", e)
        }
    }

    /**
     * Preloads an Interstitial Ad.
     */
    fun loadInterstitial(context: Context) {
        val adUnitId = AdConfig.getInterstitialAdUnitId(context)
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.i(TAG, "Interstitial Ad loaded successfully: $adUnitId")
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
            loadInterstitial(activity)
            onClosed()
        }
    }

    /**
     * Preloads a Rewarded Video Ad.
     */
    fun loadRewardedAd(context: Context) {
        val adUnitId = AdConfig.getRewardedAdUnitId(context)
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.i(TAG, "Rewarded Video Ad loaded successfully: $adUnitId")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    Log.w(TAG, "Rewarded Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Displays a Rewarded Video Ad.
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
                Log.i(TAG, "User watched rewarded ad! Reward: ${rewardItem.amount} ${rewardItem.type}")
            }
        } else {
            loadRewardedAd(activity)
            onFailedOrDismissed()
        }
    }

    /**
     * Preloads an App Open Ad (high-revenue ad format on app open).
     */
    fun loadAppOpenAd(context: Context) {
        val adUnitId = AdConfig.getAppOpenAdUnitId(context)
        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            adUnitId,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    Log.i(TAG, "App Open Ad loaded: $adUnitId")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    Log.w(TAG, "App Open Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Displays App Open Ad when app enters foreground.
     */
    fun showAppOpenAd(activity: Activity) {
        val ad = appOpenAd
        if (ad != null && !isShowingAppOpenAd) {
            isShowingAppOpenAd = true
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAppOpenAd = false
                    loadAppOpenAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAppOpenAd = false
                    loadAppOpenAd(activity)
                }
            }
            ad.show(activity)
        } else {
            loadAppOpenAd(activity)
        }
    }
}
