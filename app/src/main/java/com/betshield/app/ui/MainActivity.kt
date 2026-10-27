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
import com.betshield.app.service.BetBlockerAccessibilityService
import com.betshield.app.service.BetShieldVpnService
import com.google.android.material.card.MaterialCardView
import java.time.LocalDate
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val app = BetShieldApplication.instance
    private val repo by lazy { app.urgeVaultRepository }

    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
            updateShieldUi()
            Toast.makeText(this, "UrgeVault Shield is active.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "VPN permission needed for web filter.", Toast.LENGTH_SHORT).show()
            app.preferencesManager.isShieldEnabled = false
            updateShieldUi()
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

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupListeners()

        // Load AdMob banner into bottom container
        AdManager.loadBanner(binding.adBannerContainer, this)
    }

    override fun onResume() {
        super.onResume()
        refreshAllData()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            binding.containerHomeTab.visibility = if (item.itemId == R.id.nav_home) View.VISIBLE else View.GONE
            binding.containerVaultTab.visibility = if (item.itemId == R.id.nav_vault) View.VISIBLE else View.GONE
            binding.containerProgressTab.visibility = if (item.itemId == R.id.nav_progress) View.VISIBLE else View.GONE
            binding.containerSettingsTab.visibility = if (item.itemId == R.id.nav_settings) View.VISIBLE else View.GONE
            true
        }
    }

    private fun setupListeners() {
        // Hero Urge SOS Action
        binding.btnTriggerUrgeSos.setOnClickListener {
            startActivity(Intent(this, UrgeSosActivity::class.java))
        }

        // Daily Check-In Action
        binding.btnDailyCheckIn.setOnClickListener { showDailyCheckInDialog() }
        binding.btnQuickCheckIn.setOnClickListener { showDailyCheckInDialog() }

        // Home Direct Custom Blocker Action
        binding.btnHomeCustomBlocker.setOnClickListener {
            startActivity(Intent(this, CustomBlockerActivity::class.java))
        }

        // Vault Add Goal
        binding.btnAddSavingsGoal.setOnClickListener { showAddGoalDialog() }

        // Settings: Shield Toggle
        binding.switchProtectionShield.setOnCheckedChangeListener(shieldSwitchListener)

        // Settings navigation items
        binding.btnCustomBlocker.setOnClickListener {
            startActivity(Intent(this, CustomBlockerActivity::class.java))
        }

        binding.btnPermissionsGuide.setOnClickListener {
            startActivity(Intent(this, SetupPermissionsActivity::class.java))
        }

        binding.btnSettingsPassword.setOnClickListener {
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
            showMedicalAdvisoryDialog()
        }

        binding.btnAdSettingsCard.setOnClickListener {
            AdSettingsDialog(this) {
                AdManager.loadBanner(binding.adBannerContainer, this)
            }.show()
        }

        binding.cardQuickShieldStatus.setOnClickListener {
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

        // 1. Home Dashboard Metrics
        binding.tvHomeDaysClean.text = summary.longestActiveDays.toString()
        binding.tvHomeMoneyProtected.text = "₹${summary.totalMoneyProtected.toInt()}"
        val hours = String.format("%.1f", summary.totalHoursReclaimed)
        binding.tvHomeTimeReclaimed.text = "$hours Hours Reclaimed"
        binding.tvHomeUrgesResisted.text = "${summary.urgesResisted} Urges Surfed"

        // Render Active Habit cards in Home
        renderActiveHabitsList(summary.breakdowns)

        // 2. Vault Tab
        renderVaultBreakdowns(summary.breakdowns)
        renderSavingsGoals(summary.totalMoneyProtected)

        // 3. Progress Sanctuary
        renderProgressSanctuary(summary, checkIns, urgeSessions)

        // 4. Settings & Shield State
        updateShieldUi()
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
                text = "${item.eligibleDays} days · ₹${item.moneyProtected.toInt()} saved · ${item.quantityAvoided.toInt()} ${item.unitLabel}"
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
                text = "₹${item.moneyProtected.toInt()} Protected"
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
                text = "Target: ₹${goal.targetAmount.toInt()} (Funded from protected spend)"
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

    private fun updateShieldUi() {
        val isEnabled = app.preferencesManager.isShieldEnabled
        binding.switchProtectionShield.setOnCheckedChangeListener(null)
        binding.switchProtectionShield.isChecked = isEnabled
        binding.switchProtectionShield.setOnCheckedChangeListener(shieldSwitchListener)

        val isAccessibilityOn = BetBlockerAccessibilityService.isAccessibilityServiceEnabled(this)

        if (isEnabled) {
            if (isAccessibilityOn) {
                binding.tvQuickShieldText.text = "Shield Active (Full Defense)"
                binding.viewShieldDot.backgroundTintList = ContextCompat.getColorStateList(this, R.color.shield_active)
            } else {
                binding.tvQuickShieldText.text = "Shield Active (Tap for Browser Shield)"
                binding.viewShieldDot.backgroundTintList = ContextCompat.getColorStateList(this, R.color.warning)
            }
        } else {
            binding.tvQuickShieldText.text = "Shield Paused"
            binding.viewShieldDot.backgroundTintList = ContextCompat.getColorStateList(this, R.color.shield_inactive)
        }
    }

    private fun enableProtection() {
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
