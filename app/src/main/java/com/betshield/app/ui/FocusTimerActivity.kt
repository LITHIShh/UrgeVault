package com.betshield.app.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.betshield.app.R
import com.betshield.app.audio.NatureSound
import com.betshield.app.audio.NatureSoundEngine
import com.betshield.app.databinding.ActivityFocusTimerBinding
import com.betshield.app.util.HapticEngine

class FocusTimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFocusTimerBinding

    private var selectedDurationMinutes = 25
    private var millisRemaining = 25 * 60 * 1000L
    private var isRunning = false
    private var timer: CountDownTimer? = null
    private var pulseAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFocusTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDurationChips()
        setupSoundChips()
        setupControls()
        setupPulseAnimation()
        updateDisplay(millisRemaining)

        binding.btnCloseFocus.setOnClickListener {
            finish()
        }
    }

    private fun setupDurationChips() {
        binding.chipGroupFocusDuration.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            if (isRunning) stopTimer()

            selectedDurationMinutes = when (checkedIds.first()) {
                R.id.chipDuration5 -> 5
                R.id.chipDuration15 -> 15
                R.id.chipDuration25 -> 25
                R.id.chipDuration45 -> 45
                R.id.chipDuration60 -> 60
                else -> 25
            }
            millisRemaining = selectedDurationMinutes * 60 * 1000L
            updateDisplay(millisRemaining)
            binding.tvFocusPhaseNotice.text = "Tap Start to Focus ($selectedDurationMinutes min)"
        }
    }

    private fun setupSoundChips() {
        binding.chipGroupSoundscapes.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val sound = when (checkedIds.first()) {
                R.id.chipSoundRain -> NatureSound.RAIN
                R.id.chipSoundOcean -> NatureSound.OCEAN
                R.id.chipSoundForest -> NatureSound.FOREST
                R.id.chipSoundCampfire -> NatureSound.CAMPFIRE
                R.id.chipSoundZen -> NatureSound.ZEN_DRONE
                else -> NatureSound.RAIN
            }
            binding.tvFocusSoundEmoji.text = sound.emoji
            binding.tvSelectedSoundDescription.text = sound.subtitle

            if (isRunning) {
                NatureSoundEngine.play(sound)
            }
        }

        binding.sliderSoundVolume.addOnChangeListener { _, value, _ ->
            NatureSoundEngine.setVolume(value / 100.0f)
        }
    }

    private fun setupControls() {
        binding.btnToggleFocusTimer.setOnClickListener {
            HapticEngine.performClick(it)
            if (isRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        binding.btnResetFocusTimer.setOnClickListener {
            HapticEngine.performClick(it)
            resetTimer()
        }
    }

    private fun setupPulseAnimation() {
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.14f, 1.0f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.14f, 1.0f)
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(binding.viewFocusPulseCircle, scaleX, scaleY).apply {
            duration = 4000
            repeatCount = ObjectAnimator.INFINITE
        }
    }

    private fun startTimer() {
        isRunning = true
        binding.btnToggleFocusTimer.text = "⏸ Pause"
        binding.tvFocusStateBadge.text = "Focusing"
        binding.tvFocusPhaseNotice.text = "Stay centered in the present moment"

        val selectedSound = when (binding.chipGroupSoundscapes.checkedChipId) {
            R.id.chipSoundOcean -> NatureSound.OCEAN
            R.id.chipSoundForest -> NatureSound.FOREST
            R.id.chipSoundCampfire -> NatureSound.CAMPFIRE
            R.id.chipSoundZen -> NatureSound.ZEN_DRONE
            else -> NatureSound.RAIN
        }
        NatureSoundEngine.play(selectedSound)
        pulseAnimator?.start()

        timer?.cancel()
        timer = object : CountDownTimer(millisRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                millisRemaining = millisUntilFinished
                updateDisplay(millisRemaining)
            }

            override fun onFinish() {
                isRunning = false
                millisRemaining = 0
                updateDisplay(0)
                NatureSoundEngine.stop()
                pulseAnimator?.cancel()
                binding.viewFocusPulseCircle.scaleX = 1.0f
                binding.viewFocusPulseCircle.scaleY = 1.0f
                binding.btnToggleFocusTimer.text = "▶ Start Focus"
                binding.tvFocusStateBadge.text = "Complete"
                binding.tvFocusPhaseNotice.text = "Focus completed! Urge successfully diffused."
                HapticEngine.performSuccess(this@FocusTimerActivity)
                Toast.makeText(this@FocusTimerActivity, "Mindful session completed! Craving diffused.", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun pauseTimer() {
        isRunning = false
        timer?.cancel()
        NatureSoundEngine.stop()
        pulseAnimator?.cancel()
        binding.btnToggleFocusTimer.text = "▶ Resume"
        binding.tvFocusStateBadge.text = "Paused"
        binding.tvFocusPhaseNotice.text = "Session paused"
    }

    private fun resetTimer() {
        stopTimer()
        millisRemaining = selectedDurationMinutes * 60 * 1000L
        updateDisplay(millisRemaining)
        binding.tvFocusPhaseNotice.text = "Tap Start to Focus ($selectedDurationMinutes min)"
    }

    private fun stopTimer() {
        isRunning = false
        timer?.cancel()
        NatureSoundEngine.stop()
        pulseAnimator?.cancel()
        binding.viewFocusPulseCircle.scaleX = 1.0f
        binding.viewFocusPulseCircle.scaleY = 1.0f
        binding.btnToggleFocusTimer.text = "▶ Start Focus"
        binding.tvFocusStateBadge.text = "Ready"
    }

    private fun updateDisplay(millis: Long) {
        val totalSecs = millis / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        binding.tvFocusTimerDisplay.text = String.format("%02d:%02d", mins, secs)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        NatureSoundEngine.stop()
        pulseAnimator?.cancel()
    }
}
