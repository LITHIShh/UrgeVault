package com.betshield.app.monetization

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.widget.Toast
import com.betshield.app.databinding.DialogAdSettingsBinding

class AdSettingsDialog(
    private val context: Context,
    private val onSaved: () -> Unit
) {

    private val dialog = Dialog(context)
    private val binding = DialogAdSettingsBinding.inflate(LayoutInflater.from(context))

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Preload current settings
        binding.switchRealAds.isChecked = AdConfig.isRealAdsEnabled(context)

        val currentBanner = AdConfig.getBannerAdUnitId(context)
        if (currentBanner != AdConfig.TEST_BANNER_ID) {
            binding.etBannerId.setText(currentBanner)
        }

        val currentInterstitial = AdConfig.getInterstitialAdUnitId(context)
        if (currentInterstitial != AdConfig.TEST_INTERSTITIAL_ID) {
            binding.etInterstitialId.setText(currentInterstitial)
        }

        val currentRewarded = AdConfig.getRewardedAdUnitId(context)
        if (currentRewarded != AdConfig.TEST_REWARDED_ID) {
            binding.etRewardedId.setText(currentRewarded)
        }

        binding.btnSaveAds.setOnClickListener {
            val isReal = binding.switchRealAds.isChecked
            AdConfig.setRealAdsEnabled(context, isReal)

            val bannerInput = binding.etBannerId.text?.toString()?.trim().orEmpty()
            if (bannerInput.isNotEmpty()) {
                AdConfig.setBannerAdUnitId(context, bannerInput)
            }

            val interstitialInput = binding.etInterstitialId.text?.toString()?.trim().orEmpty()
            if (interstitialInput.isNotEmpty()) {
                AdConfig.setInterstitialAdUnitId(context, interstitialInput)
            }

            val rewardedInput = binding.etRewardedId.text?.toString()?.trim().orEmpty()
            if (rewardedInput.isNotEmpty()) {
                AdConfig.setRewardedAdUnitId(context, rewardedInput)
            }

            Toast.makeText(
                context,
                if (isReal) "Real Live Ads Mode Activated!" else "Test Ads Mode Enabled",
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
            onSaved()
        }

        binding.btnCancelAds.setOnClickListener {
            dialog.dismiss()
        }
    }

    fun show() {
        dialog.show()
    }
}
