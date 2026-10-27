package com.betshield.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.betshield.app.BetShieldApplication
import com.betshield.app.R
import com.betshield.app.databinding.ActivityOnboardingBinding
import com.betshield.app.model.GoalType
import com.betshield.app.model.HabitProfile
import com.betshield.app.model.HabitType
import com.betshield.app.util.CurrencyFormatter
import com.betshield.app.util.LocaleManager
import java.time.LocalDate

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val repo by lazy { BetShieldApplication.instance.urgeVaultRepository }
    private val prefs by lazy { BetShieldApplication.instance.preferencesManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateCurrencyPrefix()

        if (!prefs.isRegionSetupCompleted) {
            LocaleManager.showLanguageCurrencyDialog(this, isFirstStart = true) {
                updateCurrencyPrefix()
            }
        }

        setupStep1()
        setupStep2()
        setupStep3()
    }

    private fun updateCurrencyPrefix() {
        binding.tilDailySpend.prefixText = "${CurrencyFormatter.currentSymbol} "
        binding.tvDailySpendingLabel.text = "Typical daily spending (${CurrencyFormatter.currentSymbol})"
    }

    private fun setupStep1() {
        binding.btnStartPrivately.setOnClickListener {
            binding.viewFlipperOnboarding.displayedChild = 1
        }

        binding.tvPrivacySafetyLink.setOnClickListener {
            AlertDialog.Builder(this, R.style.Theme_BetShield_Dialog)
                .setTitle("Privacy & Health Advisory")
                .setMessage(
                    "UrgeVault is strictly offline-first. All baselines, urge logs, and check-ins are stored exclusively on your device.\n\n" +
                    "Health Notice: UrgeVault is a self-management support companion, not a medical device. For physical alcohol dependence, seek professional medical guidance before stopping abruptly."
                )
                .setPositiveButton("I Understand", null)
                .show()
        }
    }

    private fun setupStep2() {
        binding.cardHabitBetting.setOnClickListener {
            binding.cbHabitBetting.isChecked = !binding.cbHabitBetting.isChecked
        }
        binding.cardHabitAlcohol.setOnClickListener {
            binding.cbHabitAlcohol.isChecked = !binding.cbHabitAlcohol.isChecked
        }
        binding.cardHabitSmoking.setOnClickListener {
            binding.cbHabitSmoking.isChecked = !binding.cbHabitSmoking.isChecked
        }
        binding.cardHabitDigital.setOnClickListener {
            binding.cbHabitDigital.isChecked = !binding.cbHabitDigital.isChecked
        }
        binding.cardHabitAdult.setOnClickListener {
            binding.cbHabitAdult.isChecked = !binding.cbHabitAdult.isChecked
        }

        binding.cbHabitAlcohol.setOnCheckedChangeListener { _, isChecked ->
            binding.tvAlcoholNoticeInOnboarding.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.btnContinueToBaseline.setOnClickListener {
            val anyChecked = binding.cbHabitBetting.isChecked || binding.cbHabitAlcohol.isChecked ||
                    binding.cbHabitSmoking.isChecked || binding.cbHabitDigital.isChecked || binding.cbHabitAdult.isChecked
            if (!anyChecked) {
                Toast.makeText(this, "Please select at least one focus habit.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.viewFlipperOnboarding.displayedChild = 2
        }
    }

    private fun setupStep3() {
        binding.btnCompleteOnboarding.setOnClickListener {
            val spend = binding.etDailySpend.text.toString().toDoubleOrNull() ?: 300.0
            val minutes = binding.etDailyMinutes.text.toString().toIntOrNull() ?: 45
            val isQuit = binding.rbQuitCompletely.isChecked
            val goal = if (isQuit) GoalType.QUIT else GoalType.REDUCE

            val todayEpochDay = LocalDate.now().toEpochDay()
            val profiles = mutableListOf<HabitProfile>()

            if (binding.cbHabitBetting.isChecked) {
                profiles.add(
                    HabitProfile(
                        id = "habit_betting",
                        type = HabitType.BETTING,
                        goal = goal,
                        startDateEpochDay = todayEpochDay,
                        baselineSpendPerDay = spend,
                        baselineMinutesPerDay = minutes,
                        baselineQuantityPerDay = 1.0,
                        isActive = true,
                        motivations = listOf(
                            "Protect my hard-earned savings",
                            "Regain peace of mind and family trust",
                            "Stop chasing losses"
                        )
                    )
                )
            }

            if (binding.cbHabitAlcohol.isChecked) {
                profiles.add(
                    HabitProfile(
                        id = "habit_alcohol",
                        type = HabitType.ALCOHOL,
                        goal = goal,
                        startDateEpochDay = todayEpochDay,
                        baselineSpendPerDay = spend * 0.8,
                        baselineMinutesPerDay = minutes,
                        baselineQuantityPerDay = 3.0,
                        isActive = true,
                        motivations = listOf(
                            "Clear mental focus and better sleep",
                            "Protect long-term liver and physical health",
                            "Show up fully for my family"
                        )
                    )
                )
            }

            if (binding.cbHabitSmoking.isChecked) {
                profiles.add(
                    HabitProfile(
                        id = "habit_smoking",
                        type = HabitType.SMOKING,
                        goal = goal,
                        startDateEpochDay = todayEpochDay,
                        baselineSpendPerDay = 150.0,
                        baselineMinutesPerDay = 30,
                        baselineQuantityPerDay = 10.0,
                        isActive = true,
                        motivations = listOf(
                            "Preserve lung capacity and stamina",
                            "Break nicotine chemical dependency",
                            "Save money for meaningful life goals"
                        )
                    )
                )
            }

            if (binding.cbHabitDigital.isChecked) {
                profiles.add(
                    HabitProfile(
                        id = "habit_digital",
                        type = HabitType.DIGITAL_ADDICTION,
                        goal = goal,
                        startDateEpochDay = todayEpochDay,
                        baselineSpendPerDay = 0.0,
                        baselineMinutesPerDay = 90,
                        baselineQuantityPerDay = 15.0,
                        isActive = true,
                        motivations = listOf(
                            "Reclaim 2-3 hours of wasted screen time every day",
                            "Restore natural dopamine baseline and mental clarity",
                            "Be present for real-life relationships and hobbies"
                        )
                    )
                )
            }

            if (binding.cbHabitAdult.isChecked) {
                profiles.add(
                    HabitProfile(
                        id = "habit_adult",
                        type = HabitType.ADULT_CONTENT,
                        goal = goal,
                        startDateEpochDay = todayEpochDay,
                        baselineSpendPerDay = 50.0,
                        baselineMinutesPerDay = 40,
                        baselineQuantityPerDay = 2.0,
                        isActive = true,
                        motivations = listOf(
                            "Heal brain reward pathways and intimacy",
                            "Build genuine self-confidence and self-respect",
                            "Reclaim mental energy and focus"
                        )
                    )
                )
            }

            repo.saveHabitProfiles(profiles)
            repo.isOnboardingCompleted = true

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
