package com.betshield.app.util

import android.content.Context
import java.time.LocalTime

object TimeThemeManager {

    enum class TimeSlot {
        MORNING,   // 05:00 - 11:59
        AFTERNOON, // 12:00 - 16:59
        EVENING,   // 17:00 - 21:59
        MIDNIGHT   // 22:00 - 04:59
    }

    data class PersonalizedGreeting(
        val title: String,
        val subtitle: String,
        val iconEmoji: String,
        val badgeText: String
    )

    fun getCurrentTimeSlot(): TimeSlot {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..11 -> TimeSlot.MORNING
            in 12..16 -> TimeSlot.AFTERNOON
            in 17..21 -> TimeSlot.EVENING
            else -> TimeSlot.MIDNIGHT
        }
    }

    /**
     * Generates a warm, highly personalized greeting for the top corner of the app,
     * tailored to urge management and emotional resilience at that hour.
     */
    fun getPersonalizedGreeting(soberDays: Int = 0): PersonalizedGreeting {
        val slot = getCurrentTimeSlot()
        val streakText = if (soberDays > 0) "Day $soberDays Clean" else "Fortress Active"

        return when (slot) {
            TimeSlot.MORNING -> PersonalizedGreeting(
                title = "Good Morning, Champion",
                subtitle = "Start clear. Protect your peace and your wallet today.",
                iconEmoji = "🌅",
                badgeText = streakText
            )
            TimeSlot.AFTERNOON -> PersonalizedGreeting(
                title = "Good Afternoon, Stay Strong",
                subtitle = "Midday resilience check. Defense shields are on guard.",
                iconEmoji = "☀️",
                badgeText = streakText
            )
            TimeSlot.EVENING -> PersonalizedGreeting(
                title = "Good Evening, Unwind Safely",
                subtitle = "Evening urge peak neutralized. Relax, you are in control.",
                iconEmoji = "🛡️",
                badgeText = streakText
            )
            TimeSlot.MIDNIGHT -> PersonalizedGreeting(
                title = "Late Night Vigil, Stand Firm",
                subtitle = "High-risk vulnerability window. Guardian actively locking traps.",
                iconEmoji = "🌙",
                badgeText = "Max Shield 🔒"
            )
        }
    }

    /**
     * Returns true if current time is night / midnight (for OLED black shift).
     */
    fun isNightShiftActive(): Boolean {
        val slot = getCurrentTimeSlot()
        return slot == TimeSlot.EVENING || slot == TimeSlot.MIDNIGHT
    }
}
