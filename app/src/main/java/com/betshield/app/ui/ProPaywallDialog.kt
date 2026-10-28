package com.betshield.app.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.betshield.app.BetShieldApplication
import com.betshield.app.R
import com.betshield.app.databinding.DialogProPaywallBinding
import com.betshield.app.monetization.PremiumManager
import com.betshield.app.util.HapticEngine

class ProPaywallDialog(
    private val context: Context,
    private val onSuccess: (() -> Unit)? = null
) {

    private val dialog: Dialog = Dialog(context)
    private val binding: DialogProPaywallBinding =
        DialogProPaywallBinding.inflate(LayoutInflater.from(context))
    private val premiumManager = BetShieldApplication.instance.premiumManager

    private var selectedTier: String = PremiumManager.TIER_ANNUAL

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        setupPlanSelection()
        setupButtons()
    }

    private fun setupPlanSelection() {
        val strokeSelected = ContextCompat.getColor(context, R.color.primary)
        val strokeUnselected = ContextCompat.getColor(context, R.color.card_stroke)

        fun updateCardStyles() {
            binding.cardPlanAnnual.strokeColor = if (selectedTier == PremiumManager.TIER_ANNUAL) strokeSelected else strokeUnselected
            binding.cardPlanAnnual.strokeWidth = if (selectedTier == PremiumManager.TIER_ANNUAL) 4 else 2

            binding.cardPlanLifetime.strokeColor = if (selectedTier == PremiumManager.TIER_LIFETIME) strokeSelected else strokeUnselected
            binding.cardPlanLifetime.strokeWidth = if (selectedTier == PremiumManager.TIER_LIFETIME) 4 else 2

            binding.cardPlanMonthly.strokeColor = if (selectedTier == PremiumManager.TIER_MONTHLY) strokeSelected else strokeUnselected
            binding.cardPlanMonthly.strokeWidth = if (selectedTier == PremiumManager.TIER_MONTHLY) 4 else 2

            binding.btnUpgradeAction.text = when (selectedTier) {
                PremiumManager.TIER_ANNUAL -> "Start 7-Day Free Trial & Protect"
                PremiumManager.TIER_LIFETIME -> "Unlock Lifetime Sanctuary • $59.99"
                PremiumManager.TIER_MONTHLY -> "Activate Monthly Shield • $4.99/mo"
                else -> "Upgrade to UrgeVault PRO"
            }
        }

        binding.cardPlanAnnual.setOnClickListener {
            HapticEngine.performClick(it)
            selectedTier = PremiumManager.TIER_ANNUAL
            updateCardStyles()
        }

        binding.cardPlanLifetime.setOnClickListener {
            HapticEngine.performClick(it)
            selectedTier = PremiumManager.TIER_LIFETIME
            updateCardStyles()
        }

        binding.cardPlanMonthly.setOnClickListener {
            HapticEngine.performClick(it)
            selectedTier = PremiumManager.TIER_MONTHLY
            updateCardStyles()
        }

        updateCardStyles()
    }

    private fun setupButtons() {
        binding.btnCloseProPaywall.setOnClickListener {
            HapticEngine.performClick(it)
            dialog.dismiss()
        }

        binding.btnUpgradeAction.setOnClickListener {
            HapticEngine.performSuccess(context = context)
            premiumManager.activatePro(selectedTier)
            Toast.makeText(
                context,
                "👑 Welcome to UrgeVault PRO! All ads permanently disabled.",
                Toast.LENGTH_LONG
            ).show()
            dialog.dismiss()
            onSuccess?.invoke()
        }

        binding.btnRedeemPromoCode.setOnClickListener {
            showPromoCodeDialog()
        }

        binding.btnRestorePurchases.setOnClickListener {
            HapticEngine.performClick(it)
            if (premiumManager.restorePurchases()) {
                Toast.makeText(context, "Active Pro Subscription restored!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onSuccess?.invoke()
            } else {
                Toast.makeText(context, "No existing purchase found on this Google account.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPromoCodeDialog() {
        val input = EditText(context).apply {
            hint = "e.g. FREEDOM2026"
            setPadding(48, 36, 48, 36)
            isSingleLine = true
        }

        AlertDialog.Builder(context)
            .setTitle("Redeem Promo Code")
            .setMessage("Enter your VIP invitation or partner access code (e.g. FREEDOM2026):")
            .setView(input)
            .setPositiveButton("Redeem") { _, _ ->
                val code = input.text.toString().trim()
                val (success, message) = premiumManager.redeemPromoCode(code)
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                if (success) {
                    HapticEngine.performSuccess(context = context)
                    dialog.dismiss()
                    onSuccess?.invoke()
                } else {
                    HapticEngine.performWarning(context = context)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun show() {
        dialog.show()
    }
}
