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
            badge = "Step 1 of 4: Digital Peace",
            emoji = "🛡️",
            title = "Welcome to UrgeVault",
            desc = "Your private, offline-first sanctuary. Protect your mind, build healthy streaks, and reclaim control with zero data collection and complete privacy."
        ),
        TutorialStep(
            badge = "Step 2 of 4: One-Tap Shield",
            emoji = "⚡",
            title = "Fast Website & App Protection",
            desc = "Turn Shield ON to block distracting websites and impulsive apps at the DNS level. Enjoy lightning-fast browsing with zero slowdown on your normal internet."
        ),
        TutorialStep(
            badge = "Step 3 of 4: Smart Guardian",
            emoji = "🌐",
            title = "Smart Browser Guardian",
            desc = "Enable Browser Guardian to keep all major browsers clean. It detects bypass links and tricky redirect mirrors in real-time to keep your space safe."
        ),
        TutorialStep(
            badge = "Step 4 of 4: Urge SOS",
            emoji = "🧘",
            title = "Tactile Urge SOS",
            desc = "When an impulse strikes, tap the Urge SOS button for 60 seconds of gentle guided 4-7-8 breathing vibrations, grounding affirmations, and relief."
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

        val activeRes = R.color.primary
        val inactiveRes = R.color.card_stroke

        binding.dot1.backgroundTintList = ContextCompat.getColorStateList(context, if (step == 1) activeRes else inactiveRes)
        binding.dot2.backgroundTintList = ContextCompat.getColorStateList(context, if (step == 2) activeRes else inactiveRes)
        binding.dot3.backgroundTintList = ContextCompat.getColorStateList(context, if (step == 3) activeRes else inactiveRes)
        binding.dot4.backgroundTintList = ContextCompat.getColorStateList(context, if (step == 4) activeRes else inactiveRes)
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
