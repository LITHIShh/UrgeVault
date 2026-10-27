package com.betshield.app.data

import android.content.Context
import android.content.SharedPreferences
import com.betshield.app.model.DailyCheckIn
import com.betshield.app.model.GoalType
import com.betshield.app.model.HabitProfile
import com.betshield.app.model.HabitType
import com.betshield.app.model.ProgressMilestone
import com.betshield.app.model.SavingsGoal
import com.betshield.app.model.UrgeSession
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

class UrgeVaultRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "urgevault_core_data"
        private const val KEY_PROFILES = "key_habit_profiles"
        private const val KEY_CHECK_INS = "key_daily_check_ins"
        private const val KEY_URGE_SESSIONS = "key_urge_sessions"
        private const val KEY_SAVINGS_GOALS = "key_savings_goals"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_ALCOHOL_DISCLAIMER_ACCEPTED = "key_alcohol_disclaimer_accepted"
    }

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var isAlcoholDisclaimerAccepted: Boolean
        get() = prefs.getBoolean(KEY_ALCOHOL_DISCLAIMER_ACCEPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_ALCOHOL_DISCLAIMER_ACCEPTED, value).apply()

    fun getHabitProfiles(): List<HabitProfile> {
        val json = prefs.getString(KEY_PROFILES, null) ?: return getDefaultProfiles()
        val type = object : TypeToken<List<HabitProfile>>() {}.type
        return try {
            gson.fromJson(json, type) ?: getDefaultProfiles()
        } catch (e: Exception) {
            getDefaultProfiles()
        }
    }

    fun saveHabitProfiles(profiles: List<HabitProfile>) {
        val json = gson.toJson(profiles)
        prefs.edit().putString(KEY_PROFILES, json).apply()
    }

    fun getProfile(type: HabitType): HabitProfile? {
        return getHabitProfiles().firstOrNull { it.type == type && it.isActive }
    }

    fun saveProfile(profile: HabitProfile) {
        val current = getHabitProfiles().toMutableList()
        val index = current.indexOfFirst { it.type == profile.type }
        if (index != -1) {
            current[index] = profile
        } else {
            current.add(profile)
        }
        saveHabitProfiles(current)
    }

    fun getDailyCheckIns(): List<DailyCheckIn> {
        val json = prefs.getString(KEY_CHECK_INS, null) ?: return emptyList()
        val type = object : TypeToken<List<DailyCheckIn>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun recordCheckIn(checkIn: DailyCheckIn) {
        val current = getDailyCheckIns().toMutableList()
        // Replace if already checked in today for this habit
        current.removeAll { it.habitType == checkIn.habitType && it.dateEpochDay == checkIn.dateEpochDay }
        current.add(0, checkIn)
        prefs.edit().putString(KEY_CHECK_INS, gson.toJson(current)).apply()
    }

    fun getUrgeSessions(): List<UrgeSession> {
        val json = prefs.getString(KEY_URGE_SESSIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<UrgeSession>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun recordUrgeSession(session: UrgeSession) {
        val current = getUrgeSessions().toMutableList()
        current.add(0, session)
        prefs.edit().putString(KEY_URGE_SESSIONS, gson.toJson(current)).apply()
    }

    fun getSavingsGoals(): List<SavingsGoal> {
        val json = prefs.getString(KEY_SAVINGS_GOALS, null) ?: return getDefaultSavingsGoals()
        val type = object : TypeToken<List<SavingsGoal>>() {}.type
        return try {
            gson.fromJson(json, type) ?: getDefaultSavingsGoals()
        } catch (e: Exception) {
            getDefaultSavingsGoals()
        }
    }

    fun saveSavingsGoals(goals: List<SavingsGoal>) {
        prefs.edit().putString(KEY_SAVINGS_GOALS, gson.toJson(goals)).apply()
    }

    private fun getDefaultProfiles(): List<HabitProfile> {
        val todayEpochDay = LocalDate.now().toEpochDay()
        return listOf(
            HabitProfile(
                id = "habit_betting",
                type = HabitType.BETTING,
                goal = GoalType.QUIT,
                startDateEpochDay = todayEpochDay,
                baselineSpendPerDay = 350.0,
                baselineMinutesPerDay = 45,
                baselineQuantityPerDay = 1.0,
                currencyCode = "INR",
                isActive = true,
                motivations = listOf(
                    "Protect my hard-earned savings",
                    "Regain peace of mind and family trust",
                    "Stop the toxic chase cycle"
                )
            )
        )
    }

    private fun getDefaultSavingsGoals(): List<SavingsGoal> {
        return listOf(
            SavingsGoal("goal_emergency", "Emergency Fund", 25000.0, "🛡️"),
            SavingsGoal("goal_camera", "New Tech / Camera", 15000.0, "📸"),
            SavingsGoal("goal_trip", "Weekend Trip", 10000.0, "✈️")
        )
    }
}
