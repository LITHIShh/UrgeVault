package com.betshield.app.util

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Tactile Haptic Vibration Engine for UrgeVault.
 * Provides subtle iOS-style liquid feedback across taps, transitions,
 * breathing exercises, and urgent defense alerts.
 */
object HapticEngine {

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Subtle, crisp tap feedback for buttons, switches, and tab switches.
     */
    fun performClick(view: View? = null, context: Context? = view?.context) {
        view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        val ctx = context ?: return
        val v = getVibrator(ctx) ?: return
        if (!v.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(20, 100))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(20)
            }
        } catch (_: Exception) {}
    }

    /**
     * Celebratory tactile pattern for check-ins, goal completion, and SOS recovery.
     */
    fun performSuccess(view: View? = null, context: Context? = view?.context) {
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        val ctx = context ?: return
        val v = getVibrator(ctx) ?: return
        if (!v.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Two upbeat pulses: 30ms light, 50ms pause, 60ms strong
                val timings = longArrayOf(0, 30, 60, 70)
                val amplitudes = intArrayOf(0, 120, 0, 220)
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 40, 50, 70), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Sharp double-pulse warning for blocked access, tamper attempts, or security unlock.
     */
    fun performWarning(view: View? = null, context: Context? = view?.context) {
        view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
        val ctx = context ?: return
        val v = getVibrator(ctx) ?: return
        if (!v.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 80, 80, 120)
                val amplitudes = intArrayOf(0, 240, 0, 255)
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 80, 60, 100), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Calming somatic pulse during 4-7-8 Urge SOS breathing cycle.
     */
    fun performBreathingPulse(context: Context, isInhale: Boolean) {
        val v = getVibrator(context) ?: return
        if (!v.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = if (isInhale) 45L else 30L
                val strength = if (isInhale) 90 else 60
                v.vibrate(VibrationEffect.createOneShot(duration, strength))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(40)
            }
        } catch (_: Exception) {}
    }
}
