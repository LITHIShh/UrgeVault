package com.betshield.app.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import androidx.core.content.ContextCompat
import com.betshield.app.R
import com.betshield.app.databinding.DialogQuickTutorialBinding
import com.betshield.app.util.HapticEngine

class TutorialDialog(
    private val context: Context,
    private val onFinished: (() -> Unit)? = null
) {

    private val dialog = Dialog(context)
    private val binding = DialogQuickTutorialBinding.inflate(LayoutInflater.from(context))
    private var currentStep = 1

    private val steps = listOf(
        TutorialStep(
            badge = "Step 1 of 4: Sanctuary",
            emoji = "🛡️",
            title = "Welcome to UrgeVault",
            desc = "Your private, offline-first fortress against betting urges and digital traps. Everything is stored locally on your device with zero tracking."
        ),
        TutorialStep(
            badge = "Step 2 of 4: One-Tap Shield",
            emoji = "⚡",
            title = "Fast One-Tap Defense",
            desc = "Tap Shield ON to engage our local DNS filter. It instantly locks 500+ betting domains & custom apps with zero slowdown on your regular internet."
        ),
        TutorialStep(
            badge = "Step 3 of 4: Browser Guardian",
            emoji = "👁️",
            title = "Browser Guardian & 1xBet Block",
            desc = "Enable Browser Guardian in Accessibility. It monitors the address bar in Chrome, Brave, and Samsung Internet to stop 1xBet and circumvention mirrors."
        ),
        TutorialStep(
            badge = "Step 4 of 4: Urge SOS",
            emoji = "🧘",
            title = "Urge SOS Panic Button",
            desc = "When a craving or impulse strikes, tap the red Urge SOS button for 60 seconds of tactical 4-7-8 haptic breathing, self-talk, and emergency support."
        )
    )

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        renderStep(1)

        binding.btnTutorialNext.setOnClickListener {
            HapticEngine.performClick(it)
            if (currentStep < steps.size) {
                currentStep++
                renderStep(currentStep)
            } else {
                HapticEngine.performSuccess(it)
                dialog.dismiss()
                onFinished?.invoke()
            }
        }

        binding.btnDismissTutorial.setOnClickListener {
            HapticEngine.performClick(it)
            dialog.dismiss()
        }
    }

    private fun renderStep(step: Int) {
        val data = steps[step - 1]
        binding.tvTutorialStepBadge.text = data.badge
        binding.tvTutorialEmoji.text = data.emoji
        binding.tvTutorialTitle.text = data.title
        binding.tvTutorialDesc.text = data.desc

        binding.btnTutorialNext.text = if (step == steps.size) "Get Started 🚀" else "Next →"

        val activeColor = ContextCompat.getColor(context, R.color.primary)
        val inactiveColor = ContextCompat.getColor(context, R.color.card_stroke)

        binding.dot1.backgroundTintList = ContextCompat.getColorStateList(context, if (step == 1) activeColor else inactiveColor)
        binding.dot2.backgroundTintList = ContextCompat.getColorStateList(context, if (step == 2) activeColor else inactiveColor)
        binding.dot3.backgroundTintList = ContextCompat.getColorStateList(context, if (step == 3) activeColor else inactiveColor)
        binding.dot4.backgroundTintList = ContextCompat.getColorStateList(context, if (step == 4) activeColor else inactiveColor)
    }

    fun show() {
        dialog.show()
    }

    data class TutorialStep(
        val badge: String,
        val emoji: String,
        val title: String,
        val desc: String
    )
}
