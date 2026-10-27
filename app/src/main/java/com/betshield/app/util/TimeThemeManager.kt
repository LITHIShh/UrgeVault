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
    fun getPersonalizedGreeting(soberDays: Long = 0L): PersonalizedGreeting {
        val slot = getCurrentTimeSlot()
        val streakText = if (soberDays > 0L) "Day $soberDays Clean" else "Fortress Active"

        return when (slot) {
            TimeSlot.MORNING -> PersonalizedGreeting(
                title = "Good Morning, Champion",
                subtitle = if (soberDays > 0) "Day $soberDays Clean • Protect your peace today." else "Start fresh. Your focus and freedom are protected.",
                iconEmoji = "🌅",
                badgeText = streakText
            )
            TimeSlot.AFTERNOON -> PersonalizedGreeting(
                title = "Good Afternoon, Stay Strong",
                subtitle = if (soberDays > 0) "Day $soberDays Clean • Midday momentum is strong." else "Midday resilience check. Defense shields are on guard.",
                iconEmoji = "☀️",
                badgeText = streakText
            )
            TimeSlot.EVENING -> PersonalizedGreeting(
                title = "Good Evening, Unwind Safely",
                subtitle = if (soberDays > 0) "Day $soberDays Clean • Evening peak secured. Relax in peace." else "Evening urge peak neutralized. You are in control.",
                iconEmoji = "🌆",
                badgeText = streakText
            )
            TimeSlot.MIDNIGHT -> PersonalizedGreeting(
                title = "Late Night Vigil, Stand Firm",
                subtitle = if (soberDays > 0) "Day $soberDays Clean • Guardian actively locking all traps." else "High-risk night window. Guardian actively locking traps.",
                iconEmoji = "🌙",
                badgeText = "Max Shield 🔒"
            )
        }
    }

    fun getPersonalizedGreeting(soberDays: Int): PersonalizedGreeting =
        getPersonalizedGreeting(soberDays.toLong())

    /**
     * Returns true if current time is night / midnight (for OLED black shift).
     */
    fun isNightShiftActive(): Boolean {
        val slot = getCurrentTimeSlot()
        return slot == TimeSlot.EVENING || slot == TimeSlot.MIDNIGHT
    }
}
