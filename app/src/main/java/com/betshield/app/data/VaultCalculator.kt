package com.betshield.app.data

import com.betshield.app.model.DailyCheckIn
import com.betshield.app.model.HabitProfile
import com.betshield.app.model.HabitType
import com.betshield.app.model.ProgressMilestone
import com.betshield.app.model.SavingsGoal
import com.betshield.app.model.UrgeSession
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

data class HabitSavingsBreakdown(
    val habitType: HabitType,
    val eligibleDays: Long,
    val moneyProtected: Double,
    val minutesReclaimed: Long,
    val quantityAvoided: Double, // cigarettes or drinks avoided
    val unitLabel: String
)

data class VaultSummary(
    val totalMoneyProtected: Double,
    val totalHoursReclaimed: Double,
    val totalMinutesReclaimed: Long,
    val longestActiveDays: Long,
    val totalCheckIns: Int,
    val urgesResisted: Int,
    val breakdowns: List<HabitSavingsBreakdown>
)

data class TriggerInsight(
    val topTrigger: String,
    val topCopingStrategy: String,
    val averageUrgeIntensity: Double,
    val totalUrgesLogged: Int
)

object VaultCalculator {

    fun calculateHabitBreakdown(
        profile: HabitProfile,
        checkIns: List<DailyCheckIn> = emptyList(),
        currentEpochDay: Long = LocalDate.now().toEpochDay()
    ): HabitSavingsBreakdown {
        val days = max(0L, currentEpochDay - profile.startDateEpochDay + 1)
        val grossMoney = profile.baselineSpendPerDay * days

        // Subtract any actual recorded spend during check-ins
        val actualSpend = checkIns
            .filter { it.habitType == profile.type }
            .sumOf { it.actualSpend ?: 0.0 }
        val netMoney = max(0.0, grossMoney - actualSpend)

        val minutes = profile.baselineMinutesPerDay.toLong() * days
        val quantity = profile.baselineQuantityPerDay * days

        val unitLabel = when (profile.type) {
            HabitType.BETTING -> "bets avoided"
            HabitType.ALCOHOL -> "standard drinks avoided"
            HabitType.SMOKING -> "cigarettes not smoked"
            HabitType.DIGITAL_ADDICTION -> "app opens avoided"
            HabitType.ADULT_CONTENT -> "triggers resisted"
            HabitType.SPECULATIVE_TRADING -> "trades avoided"
        }

        return HabitSavingsBreakdown(
            habitType = profile.type,
            eligibleDays = days,
            moneyProtected = netMoney,
            minutesReclaimed = minutes,
            quantityAvoided = quantity,
            unitLabel = unitLabel
        )
    }

    fun calculateVaultSummary(
        profiles: List<HabitProfile>,
        checkIns: List<DailyCheckIn> = emptyList(),
        urgeSessions: List<UrgeSession> = emptyList(),
        currentEpochDay: Long = LocalDate.now().toEpochDay()
    ): VaultSummary {
        val activeProfiles = profiles.filter { it.isActive }
        val breakdowns = activeProfiles.map { calculateHabitBreakdown(it, checkIns, currentEpochDay) }

        val totalMoney = breakdowns.sumOf { it.moneyProtected }
        val totalMinutes = breakdowns.sumOf { it.minutesReclaimed }
        val totalHours = totalMinutes / 60.0
        val longestDays = breakdowns.maxOfOrNull { it.eligibleDays } ?: 0L
        val urgesResisted = urgeSessions.count { it.wasUrgeResisted }

        return VaultSummary(
            totalMoneyProtected = totalMoney,
            totalHoursReclaimed = totalHours,
            totalMinutesReclaimed = totalMinutes,
            longestActiveDays = longestDays,
            totalCheckIns = checkIns.size,
            urgesResisted = urgesResisted,
            breakdowns = breakdowns
        )
    }

    fun calculateGoalProgress(goal: SavingsGoal, totalMoneyProtected: Double): Int {
        if (goal.targetAmount <= 0.0) return 100
        val percentage = (totalMoneyProtected / goal.targetAmount) * 100
        return percentage.coerceIn(0.0, 100.0).toInt()
    }

    fun calculateTriggerInsights(
        checkIns: List<DailyCheckIn>,
        urgeSessions: List<UrgeSession>
    ): TriggerInsight {
        val allTriggers = mutableListOf<String>()
        val allCoping = mutableListOf<String>()
        val allIntensities = mutableListOf<Int>()

        checkIns.forEach { ci ->
            ci.trigger?.takeIf { it.isNotBlank() }?.let { allTriggers.add(it) }
            ci.copingStrategy?.takeIf { it.isNotBlank() }?.let { allCoping.add(it) }
            ci.urgeIntensity?.let { allIntensities.add(it) }
        }

        urgeSessions.forEach { us ->
            if (us.copingMethodUsed.isNotBlank()) {
                allCoping.add(us.copingMethodUsed)
            }
            allIntensities.add(us.intensityBefore)
        }

        val topTrigger = allTriggers.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "Evening stress / downtime"
        val topCoping = allCoping.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "Grounding breathing & delay timer"
        val avgIntensity = if (allIntensities.isNotEmpty()) allIntensities.average() else 6.2

        return TriggerInsight(
            topTrigger = topTrigger,
            topCopingStrategy = topCoping,
            averageUrgeIntensity = (avgIntensity * 10).toInt() / 10.0,
            totalUrgesLogged = allIntensities.size
        )
    }

    fun getGardenStage(days: Long, checkInCount: Int): GardenStage {
        val score = days + (checkInCount * 2)
        return when {
            score >= 90 -> GardenStage.BLOOMING_SANCTUARY
            score >= 30 -> GardenStage.HEALTHY_GROVE
            score >= 14 -> GardenStage.GROWING_SAPLING
            score >= 3 -> GardenStage.EMERGING_SPROUT
            else -> GardenStage.NURTURED_SEED
        }
    }

    enum class GardenStage(
        val title: String,
        val subtitle: String,
        val emoji: String,
        val stageLevel: Int
    ) {
        NURTURED_SEED("Planted Seed", "Every recovery begins with a single conscious decision.", "🌱", 1),
        EMERGING_SPROUT("Emerging Sprout", "Roots are forming. You are taking your power back.", "🌿", 2),
        GROWING_SAPLING("Growing Sapling", "Resilience is taking hold against daily triggers.", "🪴", 3),
        HEALTHY_GROVE("Resilient Grove", "Habit neural pathways are healing and strengthening.", "🌳", 4),
        BLOOMING_SANCTUARY("Blooming Sanctuary", "Living proof of sustained freedom and peace of mind.", "🌸", 5)
    }

    fun getMilestones(summary: VaultSummary): List<ProgressMilestone> {
        val days = summary.longestActiveDays
        val list = mutableListOf<ProgressMilestone>()

        val milestonesConfig = listOf(
            Triple(1L, "First Step", "Began the journey toward freedom and reclamation."),
            Triple(3L, "3-Day Breakthrough", "Passed the critical initial 72 hours of cravings."),
            Triple(7L, "1 Week Clean", "First full seven days protected from impulses."),
            Triple(14L, "2 Weeks Grounded", "Noticeable mental clarity and financial preservation."),
            Triple(30L, "1 Month Milestone", "Habit circuits rewiring. ₹${summary.totalMoneyProtected.toInt()} protected."),
            Triple(60L, "60 Days of Resilience", "Overcoming triggers with structured coping routines."),
            Triple(90L, "Season of Transformation", "Solid identity shift away from dependency.")
        )

        val now = System.currentTimeMillis()
        for ((mDays, title, desc) in milestonesConfig) {
            if (days >= mDays) {
                list.add(
                    ProgressMilestone(
                        id = "milestone_$mDays",
                        habitType = HabitType.BETTING,
                        milestoneType = "DAY_COUNT",
                        title = title,
                        description = desc,
                        achievedAtEpochMillis = now
                    )
                )
            }
        }
        return list
    }
}
