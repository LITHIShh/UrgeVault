package com.betshield.app.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.betshield.app.BetShieldApplication
import com.betshield.app.R
import com.betshield.app.databinding.ActivityUrgeSosBinding
import com.betshield.app.model.HabitType
import com.betshield.app.model.UrgeSession
import java.util.UUID

class UrgeSosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUrgeSosBinding
    private val repo by lazy { BetShieldApplication.instance.urgeVaultRepository }

    private var selectedHabit = HabitType.BETTING
    private var urgeIntensity = 7

    private var breathingTimer: CountDownTimer? = null
    private var isBreathingRunning = false
    private var breathingAnimation: ObjectAnimator? = null

    private var delayTimer: CountDownTimer? = null
    private var isDelayRunning = false
    private var delayMillisRemaining = 15 * 60 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUrgeSosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHabitSelection()
        setupSlider()
        setupBreathingExercise()
        setupDelayTimer()
        setupMotivations()
        setupHelplines()
        setupLogVictory()

        binding.btnCloseSos.setOnClickListener {
            finish()
        }
    }

    private fun setupHabitSelection() {
        val activeProfiles = repo.getHabitProfiles().filter { it.isActive }
        if (activeProfiles.isNotEmpty()) {
            selectedHabit = activeProfiles.first().type
        }

        when (selectedHabit) {
            HabitType.BETTING -> binding.chipBetting.isChecked = true
            HabitType.ALCOHOL -> binding.chipAlcohol.isChecked = true
            HabitType.SMOKING -> binding.chipSmoking.isChecked = true
        }

        binding.chipGroupSosHabit.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            when (checkedIds.first()) {
                R.id.chipBetting -> selectedHabit = HabitType.BETTING
                R.id.chipAlcohol -> selectedHabit = HabitType.ALCOHOL
                R.id.chipSmoking -> selectedHabit = HabitType.SMOKING
            }
            updateHabitHeader()
            setupMotivations()
        }

        updateHabitHeader()
    }

    private fun updateHabitHeader() {
        binding.tvSosHabitTitle.text = when (selectedHabit) {
            HabitType.BETTING -> "Pause before placing a bet."
            HabitType.ALCOHOL -> "Pause before taking a drink."
            HabitType.SMOKING -> "Pause before reaching for a smoke."
        }
    }

    private fun setupSlider() {
        binding.sliderUrgeIntensity.addOnChangeListener { _, value, _ ->
            urgeIntensity = value.toInt()
            val label = when (urgeIntensity) {
                in 1..3 -> "$urgeIntensity/10 • Mild thought"
                in 4..6 -> "$urgeIntensity/10 • Moderate craving"
                in 7..8 -> "$urgeIntensity/10 • Intense urge"
                else -> "$urgeIntensity/10 • Overwhelming spike"
            }
            binding.tvUrgeIntensityValue.text = label
        }
    }

    private fun setupBreathingExercise() {
        binding.btnToggleBreathing.setOnClickListener {
            if (isBreathingRunning) {
                stopBreathing()
            } else {
                startBreathing()
            }
        }
    }

    private fun startBreathing() {
        isBreathingRunning = true
        binding.btnToggleBreathing.text = "Pause Breathing"
        binding.btnToggleBreathing.setIconResource(android.R.drawable.ic_media_pause)

        // 4-7-8 Breathing Cycle Loop (Total 19 seconds per full breath)
        runBreathingCycle()

        breathingTimer?.cancel()
        breathingTimer = object : CountDownTimer(60_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.tvGroundingCountdown.text = String.format("00:%02d", seconds)
            }

            override fun onFinish() {
                binding.tvGroundingCountdown.text = "00:00"
                binding.tvBreathingPhase.text = "Completed ✨"
                stopBreathing()
                Toast.makeText(this@UrgeSosActivity, "Grounding complete. Notice your heart rate slowing down.", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun runBreathingCycle() {
        if (!isBreathingRunning) return

        // 4s Inhale: Expand circle
        binding.tvBreathingPhase.text = "Inhale (4s)..."
        val scaleUpX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.8f, 1.25f)
        val scaleUpY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.8f, 1.25f)
        breathingAnimation = ObjectAnimator.ofPropertyValuesHolder(binding.viewBreathingCircle, scaleUpX, scaleUpY).apply {
            duration = 4000
            start()
        }

        binding.root.postDelayed({
            if (!isBreathingRunning) return@postDelayed
            // 7s Hold
            binding.tvBreathingPhase.text = "Hold (7s)..."

            binding.root.postDelayed({
                if (!isBreathingRunning) return@postDelayed
                // 8s Exhale: Shrink circle
                binding.tvBreathingPhase.text = "Exhale (8s)..."
                val scaleDownX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.25f, 0.8f)
                val scaleDownY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.25f, 0.8f)
                breathingAnimation = ObjectAnimator.ofPropertyValuesHolder(binding.viewBreathingCircle, scaleDownX, scaleDownY).apply {
                    duration = 8000
                    start()
                }

                binding.root.postDelayed({
                    if (isBreathingRunning) {
                        runBreathingCycle() // Loop next breath
                    }
                }, 8000)
            }, 7000)
        }, 4000)
    }

    private fun stopBreathing() {
        isBreathingRunning = false
        breathingTimer?.cancel()
        breathingAnimation?.cancel()
        binding.btnToggleBreathing.text = "Start Grounding (60s)"
        binding.btnToggleBreathing.setIconResource(android.R.drawable.ic_media_play)
        binding.viewBreathingCircle.scaleX = 1.0f
        binding.viewBreathingCircle.scaleY = 1.0f
        binding.tvBreathingPhase.text = "Breathe with circle"
    }

    private fun setupDelayTimer() {
        binding.btnStartDelayTimer.setOnClickListener {
            if (isDelayRunning) {
                delayTimer?.cancel()
                isDelayRunning = false
                binding.btnStartDelayTimer.text = "Resume"
            } else {
                startDelayTimer()
            }
        }
    }

    private fun startDelayTimer() {
        isDelayRunning = true
        binding.btnStartDelayTimer.text = "Pause"

        delayTimer?.cancel()
        delayTimer = object : CountDownTimer(delayMillisRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                delayMillisRemaining = millisUntilFinished
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.tvDelayTimer.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                isDelayRunning = false
                binding.tvDelayTimer.text = "00:00"
                binding.btnStartDelayTimer.text = "Finished"
                Toast.makeText(this@UrgeSosActivity, "The 15-minute window has passed. The craving is receding.", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun setupMotivations() {
        val profile = repo.getProfile(selectedHabit)
        if (profile != null && profile.motivations.isNotEmpty()) {
            binding.tvPersonalMotivations.text = profile.motivations.joinToString("\n") { "• $it" }
        } else {
            binding.tvPersonalMotivations.text = when (selectedHabit) {
                HabitType.BETTING -> "• Protecting my hard-earned money and long-term security\n• Rebuilding trust with family and loved ones\n• Breaking free from chasing losses and anxiety"
                HabitType.ALCOHOL -> "• Protecting my liver, mental clarity, and deep sleep\n• Being fully present for my loved ones\n• Waking up feeling proud, calm, and in control"
                HabitType.SMOKING -> "• Preserving my lung capacity, stamina, and heart health\n• Smelling clean and saving thousands every month\n• Freedom from nicotine mood swings"
            }
        }
    }

    private fun setupHelplines() {
        binding.btnDialContact.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                // Allows user to pick an ally or speed dial
                data = Uri.parse("tel:")
            }
            startActivity(intent)
        }

        binding.btnDialHelpline.setOnClickListener {
            val helplineNumber = when (selectedHabit) {
                HabitType.BETTING -> "18005224700" // National Council on Problem Gambling
                HabitType.ALCOHOL -> "14416" // Tele-MANAS Mental Health & Addiction Line
                HabitType.SMOKING -> "1800112356" // National Quitline
            }
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$helplineNumber"))
            startActivity(intent)
        }
    }

    private fun setupLogVictory() {
        binding.btnSurfedUrge.setOnClickListener {
            val session = UrgeSession(
                id = UUID.randomUUID().toString(),
                habitType = selectedHabit,
                intensityBefore = urgeIntensity,
                intensityAfter = (urgeIntensity - 4).coerceAtLeast(1),
                timestamp = System.currentTimeMillis(),
                copingMethodUsed = "60s Grounding breath & delay pause",
                wasUrgeResisted = true
            )
            repo.recordUrgeSession(session)

            Toast.makeText(
                this,
                "Victory recorded! You successfully rode out the craving.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        breathingTimer?.cancel()
        delayTimer?.cancel()
        breathingAnimation?.cancel()
    }
}
