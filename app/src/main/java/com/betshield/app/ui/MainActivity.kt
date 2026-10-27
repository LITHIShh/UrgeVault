package com.betshield.app.ui

import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.betshield.app.BetShieldApplication
import com.betshield.app.R
import com.betshield.app.data.VaultCalculator
import com.betshield.app.databinding.ActivityMainBinding
import com.betshield.app.databinding.DialogAddGoalBinding
import com.betshield.app.databinding.DialogDailyCheckinBinding
import com.betshield.app.model.DailyCheckIn
import com.betshield.app.model.HabitType
import com.betshield.app.model.SavingsGoal
import com.betshield.app.monetization.AdManager
import com.betshield.app.monetization.AdSettingsDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.betshield.app.data.PreferencesManager
import com.betshield.app.data.QuoteManager
import com.betshield.app.data.RecoveryQuote
import com.betshield.app.monetization.AdBlockerDetector
import com.betshield.app.service.BetBlockerAccessibilityService
import com.betshield.app.service.BetShieldVpnService
import com.betshield.app.util.CurrencyFormatter
import com.betshield.app.util.HapticEngine
import com.betshield.app.util.LocaleManager
import com.betshield.app.util.ThemeHelper
import com.betshield.app.util.TimeThemeManager
import com.betshield.app.ui.AllyConfigDialog
import com.betshield.app.widget.UrgeVaultWidgetProvider
import com.google.android.material.card.MaterialCardView
import java.time.LocalDate
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val app = BetShieldApplication.instance
    private val repo by lazy { app.urgeVaultRepository }
    private var tabSwitchCount = 0
    private var currentQuote: RecoveryQuote = QuoteManager.getTodayQuote()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
            updateShieldUi()
            Toast.makeText(this, "UrgeVault Full Defense is active.", Toast.LENGTH_SHORT).show()
            if (!BetBlockerAccessibilityService.isAccessibilityServiceEnabled(this)) {
                promptAccessibilityActivation()
            }
        } else {
            Toast.makeText(this, "VPN permission skipped. Using Browser Guardian instead.", Toast.LENGTH_SHORT).show()
            app.preferencesManager.isVpnProtectionEnabled = false
            updateShieldUi()
            if (!BetBlockerAccessibilityService.isAccessibilityServiceEnabled(this)) {
                promptAccessibilityActivation()
            }
        }
    }

    private val shieldSwitchListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
            app.preferencesManager.isShieldEnabled = true
            enableProtection()
            updateShieldUi()
        } else {
            if (app.passwordManager.isPasswordSet()) {
                PasswordDialog(
                    context = this,
                    onSuccess = {
                        app.preferencesManager.isShieldEnabled = false
                        disableProtection()
                        updateShieldUi()
                        Toast.makeText(this, "Protection paused.", Toast.LENGTH_SHORT).show()
                    },
                    onCancel = {
                        updateShieldUi()
                    }
                ).show()
            } else {
                app.preferencesManager.isShieldEnabled = false
                disableProtection()
                updateShieldUi()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Route to onboarding if not yet completed
        if (!repo.isOnboardingCompleted) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        // Apply selected theme mode (Auto/Light/Dark)
        ThemeHelper.applyTheme(app.preferencesManager.appThemeMode)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize AdMob and preload ads
        AdManager.initialize(this)
        AdManager.loadInterstitial(this)
        AdManager.loadRewardedAd(this)
        AdManager.loadAppOpenAd(this)

        setupBottomNavigation()
        setupListeners()
        updateThemeToggleUi()
        updateShieldUi()
        refreshAllData()

        // Probe for active ad blocker sinkholes
        lifecycleScope.launch {
            if (AdBlockerDetector.checkAdBlockerActive()) {
                binding.cardAdBlockerAlert.visibility = View.VISIBLE
            }
        }

        // Check if region & currency setup should be prompted
        if (!app.preferencesManager.isRegionSetupCompleted) {
            LocaleManager.showLanguageCurrencyDialog(this, isFirstStart = true) {
                updateShieldUi()
                refreshAllData()
            }
        }

        // Check if tutorial should be shown to new users
        if (!app.preferencesManager.isTutorialCompleted) {
            TutorialDialog(this) {
                app.preferencesManager.isTutorialCompleted = true
            }.show()
        }

        // Load AdMob banner into bottom container and detect ad blocker
        AdManager.loadBanner(binding.adBannerContainer, this) {
            binding.cardAdBlockerAlert.visibility = View.VISIBLE
        }

        // Prompt initial required permissions on launch
        checkInitialPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (app.preferencesManager.appThemeMode == PreferencesManager.THEME_AUTO) {
            ThemeHelper.applyTheme(PreferencesManager.THEME_AUTO)
        }
        updateThemeToggleUi()
        updateShieldUi()
        refreshAllData()
        AdManager.showAppOpenAd(this)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            HapticEngine.performClick(binding.bottomNavigationView)
            tabSwitchCount++
            if (tabSwitchCount >= 10 && AdManager.canShowFullScreenAd()) {
                tabSwitchCount = 0
                AdManager.showInterstitial(this) {}
            }
            binding.containerHomeTab.visibility = if (item.itemId == R.id.nav_home) View.VISIBLE else View.GONE
            binding.containerVaultTab.visibility = if (item.itemId == R.id.nav_vault) View.VISIBLE else View.GONE
            binding.containerProgressTab.visibility = if (item.itemId == R.id.nav_progress) View.VISIBLE else View.GONE
            binding.containerSettingsTab.visibility = if (item.itemId == R.id.nav_settings) View.VISIBLE else View.GONE
            true
        }
    }

    private fun setupListeners() {
        // Theme Toggle Button (☀️ / 🌙)
        binding.btnThemeToggle.setOnClickListener {
            HapticEngine.performClick(it)
            val currentMode = app.preferencesManager.appThemeMode
            val newMode = if (ThemeHelper.isDarkActive(currentMode)) {
                PreferencesManager.THEME_LIGHT
            } else {
                PreferencesManager.THEME_DARK
            }
            app.preferencesManager.appThemeMode = newMode
            ThemeHelper.applyTheme(newMode)
            updateThemeToggleUi()
            val label = if (newMode == PreferencesManager.THEME_LIGHT) "Light Mode active ☀️" else "Dark Mode active 🌙"
            Toast.makeText(this, label, Toast.LENGTH_SHORT).show()
        }

        // Top Bar Quick Tutorial Button
        binding.btnQuickTutorial.setOnClickListener {
            HapticEngine.performClick(it)
            TutorialDialog(this).show()
        }

        // AdBlocker Support Rewarded Ad Action
        binding.btnSupportRewardedAd.setOnClickListener {
            HapticEngine.performClick(it)
            AdManager.showRewardedAd(
                this,
                onRewardEarned = {
                    HapticEngine.performSuccess(context = this)
                    binding.cardAdBlockerAlert.visibility = View.GONE
                    Toast.makeText(this, "Thank you for supporting UrgeVault! 🌟", Toast.LENGTH_LONG).show()
                },
                onFailedOrDismissed = {
                    Toast.makeText(this, "Rewarded ad closed.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        binding.cardAdBlockerAlert.setOnClickListener {
            HapticEngine.performClick(it)
            showAdBlockerHelpDialog()
        }

        // Hero Urge SOS Action
        binding.btnTriggerUrgeSos.setOnClickListener {
            HapticEngine.performWarning(it)
            startActivity(Intent(this, UrgeSosActivity::class.java))
        }

        // Mindful Focus & Nature Sounds Action
        binding.btnLaunchFocusHub.setOnClickListener {
            HapticEngine.performClick(it)
            startActivity(Intent(this, FocusTimerActivity::class.java))
        }

        // Daily Motivational Quotes
        setupQuoteCard()

        // Daily Check-In Action
        binding.btnDailyCheckIn.setOnClickListener {
            HapticEngine.performClick(it)
            showDailyCheckInDialog()
        }
        binding.btnQuickCheckIn.setOnClickListener {
            HapticEngine.performClick(it)
            showDailyCheckInDialog()
        }

        // Home Direct Custom Blocker Action
        binding.btnHomeCustomBlocker.setOnClickListener {
            HapticEngine.performClick(it)
            startActivity(Intent(this, CustomBlockerActivity::class.java))
        }

        // Vault Add Goal
        binding.btnAddSavingsGoal.setOnClickListener {
            HapticEngine.performClick(it)
            showAddGoalDialog()
        }

        // Settings: Shield Toggle
        binding.switchProtectionShield.setOnCheckedChangeListener(shieldSwitchListener)

        // Settings navigation items
        binding.btnCustomBlocker.setOnClickListener {
            HapticEngine.performClick(it)
            startActivity(Intent(this, CustomBlockerActivity::class.java))
        }

        binding.btnPermissionsGuide.setOnClickListener {
            HapticEngine.performClick(it)
            startActivity(Intent(this, SetupPermissionsActivity::class.java))
        }

        binding.btnSettingsThemeCard.setOnClickListener {
            HapticEngine.performClick(it)
            val options = arrayOf(
                "Auto (Daylight / Dark)",
                "Light Mode",
                "Dark Mode"
            )
            val currentIdx = when (app.preferencesManager.appThemeMode) {
                PreferencesManager.THEME_LIGHT -> 1
                PreferencesManager.THEME_DARK -> 2
                else -> 0
            }
            AlertDialog.Builder(this, R.style.Theme_BetShield_Dialog)
                .setTitle("Select Appearance")
                .setSingleChoiceItems(options, currentIdx) { dialog, which ->
                    val chosen = when (which) {
                        1 -> PreferencesManager.THEME_LIGHT
                        2 -> PreferencesManager.THEME_DARK
                        else -> PreferencesManager.THEME_AUTO
                    }
                    app.preferencesManager.appThemeMode = chosen
                    ThemeHelper.applyTheme(chosen)
                    updateThemeToggleUi()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnSettingsLanguageCard.setOnClickListener {
            HapticEngine.performClick(it)
            LocaleManager.showLanguageCurrencyDialog(this) {
                updateShieldUi()
                refreshAllData()
            }
        }

        binding.btnSettingsProtectionModeCard.setOnClickListener {
            HapticEngine.performClick(it)
            showProtectionModeDialog()
        }

        binding.btnSettingsAllyCard.setOnClickListener {
            HapticEngine.performClick(it)
            AllyConfigDialog.show(this) { _, _ ->
                updateShieldUi()
            }
        }

        binding.btnSettingsPassword.setOnClickListener {
            HapticEngine.performClick(it)
            if (app.passwordManager.isPasswordSet()) {
                PasswordDialog(
                    context = this,
                    onSuccess = { promptPasswordSetup() }
                ).show()
            } else {
                promptPasswordSetup()
            }
        }

        binding.btnMedicalAdvisory.setOnClickListener {
            HapticEngine.performClick(it)
            showMedicalAdvisoryDialog()
        }

        binding.btnAdSettingsCard.setOnClickListener {
            HapticEngine.performClick(it)
            AdSettingsDialog(this) {
                AdManager.loadBanner(binding.adBannerContainer, this) {
                    binding.cardAdBlockerAlert.visibility = View.VISIBLE
                }
            }.show()
        }

        binding.cardQuickShieldStatus.setOnClickListener {
            HapticEngine.performClick(it)
            if (app.preferencesManager.isShieldEnabled && !BetBlockerAccessibilityService.isAccessibilityServiceEnabled(this)) {
                promptAccessibilityActivation()
            } else {
                binding.bottomNavigationView.selectedItemId = R.id.nav_settings
            }
        }
    }

    private fun refreshAllData() {
        val profiles = repo.getHabitProfiles().filter { it.isActive }
        val checkIns = repo.getDailyCheckIns()
        val urgeSessions = repo.getUrgeSessions()
        val summary = VaultCalculator.calculateVaultSummary(profiles, checkIns, urgeSessions)

        // 0. Time-Based Liquid Glass Dynamic Greeting
        val greeting = TimeThemeManager.getPersonalizedGreeting(summary.longestActiveDays)
        binding.tvHeaderGreetingEmoji.text = greeting.iconEmoji
        binding.tvHeaderGreetingTitle.text = greeting.title
        binding.tvHeaderGreetingSubtitle.text = greeting.subtitle

        // 1. Home Dashboard Metrics & Enhanced Streak Experience
        val days = summary.longestActiveDays
        binding.tvHomeDaysClean.text = days.toString()
        binding.tvHomeMoneyProtected.text = CurrencyFormatter.format(summary.totalMoneyProtected)
        val hours = String.format("%.1f", summary.totalHoursReclaimed)
        binding.tvHomeTimeReclaimed.text = "${hours}h"
        binding.tvHomeUrgesResisted.text = "${summary.urgesResisted} Urges"

        // Streak Tier
        val tier = when {
            days >= 90 -> "🌸 Sanctuary of Freedom"
            days >= 31 -> "💎 Diamond Clarity"
            days >= 15 -> "⚡ Unbreakable Momentum"
            days >= 8 -> "🛡️ Shield of Resilience"
            days >= 4 -> "🔥 Ember of Discipline"
            else -> "🌱 Seedling of Resolve"
        }
        binding.tvHomeStreakTier.text = tier

        // Milestone Countdown & Progress Bar
        val (targetDays, milestoneLabel) = when {
            days < 3 -> Pair(3L, "3-Day Breakthrough")
            days < 7 -> Pair(7L, "1 Week Clean")
            days < 14 -> Pair(14L, "2 Weeks Grounded")
            days < 30 -> Pair(30L, "1 Month Milestone")
            days < 60 -> Pair(60L, "60 Days of Resilience")
            days < 90 -> Pair(90L, "Season of Transformation")
            else -> Pair(180L, "Half-Year Master")
        }
        val daysLeft = (targetDays - days).coerceAtLeast(1)
        val pct = ((days.toDouble() / targetDays) * 100).toInt().coerceIn(1, 100)
        binding.tvHomeNextMilestoneBadge.text = "Next: $milestoneLabel ($daysLeft days left)"
        binding.tvHomeMilestonePct.text = "$pct%"
        binding.progressStreakMilestone.progress = pct

        // Render 7-Day Consistency Horizon Dots
        renderWeeklyConsistencyDots(days, checkIns)

        // Render Active Habit cards in Home
        renderActiveHabitsList(summary.breakdowns)

        // 2. Vault Tab
        renderVaultBreakdowns(summary.breakdowns)
        renderSavingsGoals(summary.totalMoneyProtected)

        // 3. Progress Sanctuary
        renderProgressSanctuary(summary, checkIns, urgeSessions)

        // 4. Settings & Shield State
        updateShieldUi()

        // 5. Update Android Home Screen & Lock Screen Widget
        UrgeVaultWidgetProvider.updateAllWidgets(this)
    }

    private fun setupQuoteCard() {
        renderQuote(currentQuote)
        binding.btnShuffleQuote.setOnClickListener {
            HapticEngine.performClick(it)
            currentQuote = QuoteManager.getRandomQuote(currentQuote)
            renderQuote(currentQuote)
        }
        binding.btnShareQuote.setOnClickListener {
            HapticEngine.performClick(it)
            QuoteManager.shareQuote(this, currentQuote)
        }
    }

    private fun renderQuote(quote: RecoveryQuote) {
        binding.tvHomeQuoteText.text = "“${quote.quote}”"
        binding.tvHomeQuoteAuthor.text = "— ${quote.author}"
        binding.tvQuoteCategory.text = quote.category
    }

    private fun renderWeeklyConsistencyDots(days: Long, checkIns: List<com.betshield.app.model.DailyCheckIn>) {
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()
        val checkInMap = checkIns.associateBy { it.dateEpochDay }
        val streakStartEpoch = todayEpoch - days + 1

        val dotViews = listOf(
            Pair(binding.dotDay1, binding.tvCheckDay1 to binding.tvDayLabel1),
            Pair(binding.dotDay2, binding.tvCheckDay2 to binding.tvDayLabel2),
            Pair(binding.dotDay3, binding.tvCheckDay3 to binding.tvDayLabel3),
            Pair(binding.dotDay4, binding.tvCheckDay4 to binding.tvDayLabel4),
            Pair(binding.dotDay5, binding.tvCheckDay5 to binding.tvDayLabel5),
            Pair(binding.dotDay6, binding.tvCheckDay6 to binding.tvDayLabel6),
            Pair(binding.dotDay7, binding.tvCheckDay7 to binding.tvDayLabel7)
        )

        for (i in 0..6) {
            val targetDate = today.minusDays((6 - i).toLong())
            val targetEpoch = targetDate.toEpochDay()
            val (dotFrame, texts) = dotViews[i]
            val (checkText, labelText) = texts

            val isToday = (i == 6)
            val hasCleanCheckIn = checkInMap[targetEpoch]?.let { !it.useRecorded } == true
            val isCoveredByStreak = targetEpoch in streakStartEpoch..todayEpoch
            val isClean = hasCleanCheckIn || isCoveredByStreak

            val dayInitial = when (targetDate.dayOfWeek) {
                java.time.DayOfWeek.MONDAY -> "M"
                java.time.DayOfWeek.TUESDAY -> "T"
                java.time.DayOfWeek.WEDNESDAY -> "W"
                java.time.DayOfWeek.THURSDAY -> "T"
                java.time.DayOfWeek.FRIDAY -> "F"
                java.time.DayOfWeek.SATURDAY -> "S"
                java.time.DayOfWeek.SUNDAY -> "S"
            }

            if (isToday) {
                labelText.text = "Today"
                labelText.setTextColor(ContextCompat.getColor(this, R.color.primary))
                dotFrame.setBackgroundResource(R.drawable.bg_streak_dot_today)
                checkText.text = if (isClean) "✓" else "★"
                checkText.setTextColor(ContextCompat.getColor(this, R.color.primary))
            } else {
                labelText.text = dayInitial
                labelText.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
                if (isClean) {
                    dotFrame.setBackgroundResource(R.drawable.bg_streak_dot_active)
                    checkText.text = "✓"
                    checkText.setTextColor(0xFFFFFFFF.toInt())
                } else {
                    dotFrame.setBackgroundResource(R.drawable.bg_streak_dot_inactive)
                    checkText.text = ""
                }
            }
        }
    }

    private fun renderActiveHabitsList(breakdowns: List<com.betshield.app.data.HabitSavingsBreakdown>) {
        binding.layoutActiveHabitsList.removeAllViews()
        val inflater = LayoutInflater.from(this)

        for (item in breakdowns) {
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 28 }
                radius = 42f
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                strokeWidth = 2
                setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke))
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(42, 38, 42, 38)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val emoji = TextView(this).apply {
                text = item.habitType.emoji
                textSize = 28f
                setPadding(0, 0, 36, 0)
            }

            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val title = TextView(this).apply {
                text = item.habitType.displayName
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val sub = TextView(this).apply {
                text = "${item.eligibleDays} days · ${CurrencyFormatter.format(item.moneyProtected)} saved · ${item.quantityAvoided.toInt()} ${item.unitLabel}"
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 12f
            }

            textLayout.addView(title)
            textLayout.addView(sub)

            val badge = TextView(this).apply {
                text = "Clean"
                setTextColor(ContextCompat.getColor(context, R.color.primary))
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            row.addView(emoji)
            row.addView(textLayout)
            row.addView(badge)
            card.addView(row)

            binding.layoutActiveHabitsList.addView(card)
        }
    }

    private fun renderVaultBreakdowns(breakdowns: List<com.betshield.app.data.HabitSavingsBreakdown>) {
        binding.layoutVaultBreakdowns.removeAllViews()
        for (item in breakdowns) {
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 28 }
                radius = 42f
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                strokeWidth = 2
                setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke))
            }

            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(44, 40, 44, 40)
            }

            val title = TextView(this).apply {
                text = "${item.habitType.emoji}  ${item.habitType.displayName}"
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val moneyText = TextView(this).apply {
                text = "${CurrencyFormatter.format(item.moneyProtected)} Protected"
                setTextColor(ContextCompat.getColor(context, R.color.primary))
                textSize = 22f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 16, 0, 8)
            }

            val detailsText = TextView(this).apply {
                val hours = item.minutesReclaimed / 60.0
                text = "${String.format("%.1f", hours)} hours saved  •  ${item.quantityAvoided.toInt()} ${item.unitLabel}"
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 13f
            }

            content.addView(title)
            content.addView(moneyText)
            content.addView(detailsText)
            card.addView(content)
            binding.layoutVaultBreakdowns.addView(card)
        }
    }

    private fun renderSavingsGoals(totalMoneyProtected: Double) {
        binding.layoutSavingsGoalsList.removeAllViews()
        val goals = repo.getSavingsGoals()

        for (goal in goals) {
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 24 }
                radius = 38f
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                strokeWidth = 2
                setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke))
            }

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(42, 36, 42, 36)
            }

            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val title = TextView(this).apply {
                text = "${goal.emoji} ${goal.title}"
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val progressPct = VaultCalculator.calculateGoalProgress(goal, totalMoneyProtected)
            val pctText = TextView(this).apply {
                text = "$progressPct%"
                setTextColor(ContextCompat.getColor(context, R.color.primary))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            topRow.addView(title)
            topRow.addView(pctText)

            val pBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    18
                ).apply { topMargin = 20 }
                progress = progressPct
                progressDrawable = ContextCompat.getDrawable(context, android.R.drawable.progress_horizontal)
                progressTintList = ContextCompat.getColorStateList(context, R.color.primary)
            }

            val targetText = TextView(this).apply {
                text = "Target: ${CurrencyFormatter.format(goal.targetAmount)} (Funded from protected spend)"
                setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                textSize = 11f
                setPadding(0, 12, 0, 0)
            }

            layout.addView(topRow)
            layout.addView(pBar)
            layout.addView(targetText)
            card.addView(layout)

            binding.layoutSavingsGoalsList.addView(card)
        }
    }

    private fun renderProgressSanctuary(
        summary: com.betshield.app.data.VaultSummary,
        checkIns: List<DailyCheckIn>,
        urgeSessions: List<com.betshield.app.model.UrgeSession>
    ) {
        val stage = VaultCalculator.getGardenStage(summary.longestActiveDays, checkIns.size)
        binding.tvGardenEmoji.text = stage.emoji
        binding.tvGardenStageTitle.text = stage.title
        binding.tvGardenStageSubtitle.text = stage.subtitle

        val insights = VaultCalculator.calculateTriggerInsights(checkIns, urgeSessions)
        binding.tvInsightTopTrigger.text = insights.topTrigger
        binding.tvInsightTopCoping.text = insights.topCopingStrategy

        // Render Milestones
        binding.layoutMilestonesList.removeAllViews()
        val milestones = VaultCalculator.getMilestones(summary)

        for (m in milestones) {
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 20 }
                radius = 36f
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                strokeWidth = 2
                setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke))
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(36, 30, 36, 30)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val badgeIcon = TextView(this).apply {
                text = "🏆"
                textSize = 24f
                setPadding(0, 0, 30, 0)
            }

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val title = TextView(this).apply {
                text = m.title
                setTextColor(ContextCompat.getColor(context, R.color.milestone_gold))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val desc = TextView(this).apply {
                text = m.description
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 12f
            }

            info.addView(title)
            info.addView(desc)
            row.addView(badgeIcon)
            row.addView(info)
            card.addView(row)
            binding.layoutMilestonesList.addView(card)
        }
    }

    private fun showDailyCheckInDialog() {
        val dialogBinding = DialogDailyCheckinBinding.inflate(layoutInflater)
        dialogBinding.tilLapseSpend.prefixText = "${CurrencyFormatter.currentSymbol} "
        val dialog = AlertDialog.Builder(this, R.style.Theme_BetShield_Dialog)
            .setView(dialogBinding.root)
            .setPositiveButton("Save Check-In") { _, _ ->
                val isClean = dialogBinding.rbCleanYes.isChecked
                val intensity = dialogBinding.sliderCheckInUrge.value.toInt()
                val trigger = dialogBinding.etCheckInTrigger.text.toString().trim()
                val lapseSpend = if (!isClean) dialogBinding.etLapseSpend.text.toString().toDoubleOrNull() ?: 0.0 else 0.0

                val activeHabits = repo.getHabitProfiles().filter { it.isActive }
                val targetHabit = activeHabits.firstOrNull()?.type ?: HabitType.BETTING

                val checkIn = DailyCheckIn(
                    id = UUID.randomUUID().toString(),
                    habitType = targetHabit,
                    dateEpochDay = LocalDate.now().toEpochDay(),
                    urgeIntensity = intensity,
                    trigger = trigger.ifEmpty { "Daily reflection" },
                    copingStrategy = if (isClean) "Mindful awareness & delay" else "Compassionate recovery reset",
                    useRecorded = !isClean,
                    actualSpend = lapseSpend,
                    notes = if (isClean) "Maintained commitment" else "Resetting today without shame"
                )
                repo.recordCheckIn(checkIn)
                refreshAllData()
                HapticEngine.performSuccess(context = this)

                // Show Interstitial on milestone check-in
                AdManager.showInterstitial(this) {}

                val msg = if (isClean) "Great work! Daily streak protected." else "Lapse logged without shame. Progress is never lost."
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialogBinding.rgCleanStatus.setOnCheckedChangeListener { _, checkedId ->
            dialogBinding.layoutLapseSpend.visibility = if (checkedId == R.id.rbCleanLapse) View.VISIBLE else View.GONE
        }

        dialog.show()
    }

    private fun showAddGoalDialog() {
        val dialogBinding = DialogAddGoalBinding.inflate(layoutInflater)
        dialogBinding.tilGoalAmount.prefixText = "${CurrencyFormatter.currentSymbol} "
        AlertDialog.Builder(this, R.style.Theme_BetShield_Dialog)
            .setView(dialogBinding.root)
            .setPositiveButton("Add Goal") { _, _ ->
                val name = dialogBinding.etGoalTitle.text.toString().trim()
                val amt = dialogBinding.etGoalAmount.text.toString().toDoubleOrNull() ?: 5000.0
                if (name.isNotEmpty()) {
                    val current = repo.getSavingsGoals().toMutableList()
                    current.add(SavingsGoal(UUID.randomUUID().toString(), name, amt, "🎯"))
                    repo.saveSavingsGoals(current)
                    refreshAllData()
                    HapticEngine.performSuccess(context = this)
                    Toast.makeText(this, "New goal created!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMedicalAdvisoryDialog() {
        AlertDialog.Builder(this, R.style.Theme_BetShield_Dialog)
            .setTitle(R.string.alcohol_medical_warning_title)
            .setMessage(R.string.alcohol_medical_warning)
            .setPositiveButton("I Understand", null)
            .setNeutralButton("Emergency Call 14416") { _, _ ->
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:14416")))
            }
            .show()
    }

    private fun showAdBlockerHelpDialog() {
        AlertDialog.Builder(this, R.style.Theme_BetShield_Dialog)
            .setTitle("🛡️ Ad Blocker Detected")
            .setMessage("UrgeVault is 100% free with no paywalls or subscriptions. Non-intrusive ads keep our website protection filters free for everyone.\n\nTo ensure all features work smoothly:\n1. Whitelist UrgeVault in your Ad Blocker or Private DNS settings.\n2. Or tap 'Support with 1 Ad' to support our servers.")
            .setPositiveButton("Support with 1 Ad") { _, _ ->
                binding.btnSupportRewardedAd.performClick()
            }
            .setNegativeButton("I Understand", null)
            .show()
    }

    private fun updateThemeToggleUi() {
        val isDark = ThemeHelper.isDarkActive(app.preferencesManager.appThemeMode)
        binding.tvThemeToggleIcon.text = if (isDark) "☀️" else "🌙"
        updateThemeSettingsLabel()
    }

    private fun updateThemeSettingsLabel() {
        val label = when (app.preferencesManager.appThemeMode) {
            PreferencesManager.THEME_LIGHT -> "Light Mode"
            PreferencesManager.THEME_DARK -> "Dark Mode"
            else -> "Auto (Time-based shift: Daylight / Dark)"
        }
        binding.tvSettingsCurrentTheme.text = label
    }

    private fun updateShieldUi() {
        val isEnabled = app.preferencesManager.isShieldEnabled
        binding.switchProtectionShield.setOnCheckedChangeListener(null)
        binding.switchProtectionShield.isChecked = isEnabled
        binding.switchProtectionShield.setOnCheckedChangeListener(shieldSwitchListener)

        val isAccessibilityOn = BetBlockerAccessibilityService.isAccessibilityServiceEnabled(this)
        val isVpnOn = BetShieldVpnService.isRunning

        if (isEnabled) {
            when {
                isAccessibilityOn && isVpnOn -> {
                    binding.tvQuickShieldText.text = "Active (Full Defense)"
                    binding.viewShieldDot.backgroundTintList = ContextCompat.getColorStateList(this, R.color.shield_active)
                }
                isAccessibilityOn -> {
                    binding.tvQuickShieldText.text = "Active (Browser Shield)"
                    binding.viewShieldDot.backgroundTintList = ContextCompat.getColorStateList(this, R.color.shield_active)
                }
                isVpnOn -> {
                    binding.tvQuickShieldText.text = "Active (DNS Filter)"
                    binding.viewShieldDot.backgroundTintList = ContextCompat.getColorStateList(this, R.color.shield_active)
                }
                else -> {
                    binding.tvQuickShieldText.text = "Setup Required"
                    binding.viewShieldDot.backgroundTintList = ContextCompat.getColorStateList(this, R.color.warning)
                }
            }
        } else {
            binding.tvQuickShieldText.text = "Paused"
            binding.viewShieldDot.backgroundTintList = ContextCompat.getColorStateList(this, R.color.shield_inactive)
        }

        updateSettingsSubtitles()
    }

    private fun updateSettingsSubtitles() {
        val currentOption = LocaleManager.getCurrentOption()
        binding.tvSettingsCurrentLanguage.text = "${currentOption.displayName} • ${CurrencyFormatter.currentSymbol} ${CurrencyFormatter.currentCode}"

        binding.tvSettingsCurrentProtectionMode.text = if (app.preferencesManager.isVpnProtectionEnabled) {
            "Full Defense (Browser + VPN)"
        } else {
            "Browser Guardian (No VPN)"
        }

        val allyName = app.preferencesManager.allyName
        val allyPhone = app.preferencesManager.allyPhone
        binding.tvSettingsCurrentAlly.text = when {
            allyName.isNotBlank() && allyPhone.isNotBlank() -> "$allyName ($allyPhone)"
            allyName.isNotBlank() -> allyName
            allyPhone.isNotBlank() -> allyPhone
            else -> "Tap to set trusted contact"
        }
    }

    private fun showProtectionModeDialog() {
        val options = arrayOf(
            "Browser Guardian (No VPN - Recommended)\nZero battery drain. Blocks betting & harmful sites in Chrome, Brave, Firefox, and Samsung Internet.",
            "Full Defense (Browser Guardian + VPN Sinkhole)\nLocal DNS sinkhole drops gambling connections system-wide plus browser inspection."
        )
        val currentIdx = if (app.preferencesManager.isVpnProtectionEnabled) 1 else 0

        AlertDialog.Builder(this, R.style.Theme_BetShield_Dialog)
            .setTitle("Shield Protection Method")
            .setSingleChoiceItems(options, currentIdx) { dialog, which ->
                if (which == 1) {
                    app.preferencesManager.isVpnProtectionEnabled = true
                    enableProtection()
                    Toast.makeText(this, "Full Defense (VPN + Browser) activated.", Toast.LENGTH_SHORT).show()
                } else {
                    app.preferencesManager.isVpnProtectionEnabled = false
                    disableVpnOnly()
                    Toast.makeText(this, "Browser Guardian active. VPN disabled.", Toast.LENGTH_SHORT).show()
                    if (!BetBlockerAccessibilityService.isAccessibilityServiceEnabled(this)) {
                        promptAccessibilityActivation()
                    }
                }
                updateShieldUi()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun disableVpnOnly() {
        try {
            val intent = Intent(this, BetShieldVpnService::class.java).apply {
                action = BetShieldVpnService.ACTION_STOP
            }
            startService(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error stopping VPN service", e)
        }
    }

    private fun enableProtection() {
        if (app.preferencesManager.isVpnProtectionEnabled) {
            try {
                val vpnIntent = VpnService.prepare(this)
                if (vpnIntent != null) {
                    vpnPrepareLauncher.launch(vpnIntent)
                } else {
                    startVpnService()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error preparing VPN", e)
                Toast.makeText(this, "VPN setup error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        if (!BetBlockerAccessibilityService.isAccessibilityServiceEnabled(this)) {
            promptAccessibilityActivation()
        }
    }

    private fun promptAccessibilityActivation() {
        AlertDialog.Builder(this, R.style.Theme_BetShield_Dialog)
            .setTitle("Turn ON Browser Guardian")
            .setMessage("To block betting websites and custom URLs inside Chrome, Firefox, and other browsers, UrgeVault requires Accessibility access.\n\n1. Tap 'Enable Now'\n2. Look under 'Installed apps' (or 'Downloaded apps')\n3. Tap 'BetShield' (or 'UrgeVault') and switch it ON.")
            .setPositiveButton("Enable Now") { _, _ ->
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun checkInitialPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val isAccessibilityGranted = BetBlockerAccessibilityService.isAccessibilityServiceEnabled(this)
        if (!isAccessibilityGranted) {
            AlertDialog.Builder(this, R.style.Theme_BetShield_Dialog)
                .setTitle("🛡️ Activate Browser Guardian")
                .setMessage("To block betting websites and harmful URLs inside Chrome, Brave, Samsung Internet, and Firefox, UrgeVault requires Accessibility access.\n\n(VPN is 100% optional and not required).")
                .setPositiveButton("Enable Now") { _, _ ->
                    startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton("Later", null)
                .show()
        }
    }

    private fun disableProtection() {
        try {
            val intent = Intent(this, BetShieldVpnService::class.java).apply {
                action = BetShieldVpnService.ACTION_STOP
            }
            startService(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error stopping VPN service", e)
        }
    }

    private fun startVpnService() {
        try {
            val intent = Intent(this, BetShieldVpnService::class.java).apply {
                action = BetShieldVpnService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start VPN foreground service", e)
            Toast.makeText(this, "Unable to activate shield service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun promptPasswordSetup() {
        PasswordDialog(this, onSuccess = {
            Toast.makeText(this, "Master Password verified / configured.", Toast.LENGTH_SHORT).show()
        }).show()
    }
}
