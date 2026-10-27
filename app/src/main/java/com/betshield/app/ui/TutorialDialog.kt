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
            badge = "Option 1: Home & Shield",
            emoji = "🏠",
            title = "Home & Shield Control",
            desc = "Your main dashboard. View your clean streaks, hours reclaimed, and tap 'Shield ON/OFF' to instantly enable or pause local DNS filtering for websites and apps."
        ),
        TutorialStep(
            badge = "Option 2: The Vault",
            emoji = "💰",
            title = "The Vault & Savings Goals",
            desc = "Track how much money you save by resisting urges. Set tangible savings goals (like trips, gadgets, or emergency savings) and track your milestone progress."
        ),
        TutorialStep(
            badge = "Option 3: Progress & Check-Ins",
            emoji = "📈",
            title = "Daily Check-Ins & Milestones",
            desc = "Log your daily mindset in 30 seconds. Reflect on triggers without judgment, earn celebratory milestone badges, and visualize your growing streaks."
        ),
        TutorialStep(
            badge = "Option 4: Urge SOS & Blocker",
            emoji = "🧘",
            title = "Urge SOS & Custom Lists",
            desc = "When a craving strikes, tap the red Urge SOS button for 60 seconds of 4-7-8 breathing vibrations. Use Custom Blocker in Settings to add your own blocked sites or apps."
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
