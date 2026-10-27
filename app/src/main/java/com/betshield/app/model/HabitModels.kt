package com.betshield.app.model

enum class HabitType(val displayName: String, val emoji: String) {
    BETTING("Betting & Gambling", "🎲"),
    ALCOHOL("Alcohol", "🍷"),
    SMOKING("Smoking & Nicotine", "🚭"),
    DIGITAL_ADDICTION("Apps & Screen Addiction", "📱"),
    ADULT_CONTENT("Adult & Pornography", "🛑"),
    SPECULATIVE_TRADING("Crypto & Speculative Trading", "📈")
}

enum class GoalType(val displayName: String) {
    QUIT("Quit Completely"),
    REDUCE("Reduce Consumption")
}

data class HabitProfile(
    val id: String,
    val type: HabitType,
    val goal: GoalType,
    val startDateEpochDay: Long,
    val baselineSpendPerDay: Double, // in main currency units (e.g. ₹ or $)
    val baselineMinutesPerDay: Int,
    val baselineQuantityPerDay: Double, // cigarettes per day, drinks per day, or app opens avoided
    val currencyCode: String = "INR",
    val isActive: Boolean = true,
    val motivations: List<String> = emptyList()
)

data class DailyCheckIn(
    val id: String,
    val habitType: HabitType,
    val dateEpochDay: Long,
    val urgeIntensity: Int?, // 1 to 10
    val trigger: String?,
    val copingStrategy: String?,
    val useRecorded: Boolean,
    val actualSpend: Double? = 0.0,
    val notes: String? = null
)

data class ProgressMilestone(
    val id: String,
    val habitType: HabitType,
    val milestoneType: String,
    val title: String,
    val description: String,
    val achievedAtEpochMillis: Long
)

data class SavingsGoal(
    val id: String,
    val title: String,
    val targetAmount: Double,
    val emoji: String = "🎯"
)

data class UrgeSession(
    val id: String,
    val habitType: HabitType,
    val intensityBefore: Int, // 1 to 10
    val intensityAfter: Int?,
    val timestamp: Long,
    val copingMethodUsed: String,
    val wasUrgeResisted: Boolean = true
)
