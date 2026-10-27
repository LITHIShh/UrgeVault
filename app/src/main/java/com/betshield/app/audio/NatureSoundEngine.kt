package com.betshield.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean

enum class NatureSound(val displayName: String, val emoji: String, val subtitle: String) {
    RAIN("Gentle Rain", "🌧️", "Soothing raindrops with subtle thunder rumble"),
    OCEAN("Ocean Swells", "🌊", "Rhythmic rolling waves for deep calm"),
    FOREST("Forest Breeze", "🌲", "Whispering wind with distant bird melodies"),
    CAMPFIRE("Night Campfire", "🔥", "Warm crackling wood under evening stars"),
    ZEN_DRONE("Zen Singing Bowl", "🧘", "432 Hz resonant grounding frequency")
}

object NatureSoundEngine {

    private const val TAG = "NatureSoundEngine"
    private const val SAMPLE_RATE = 44100
    private const val BUFFER_SIZE = 4096

    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null
    private val isPlaying = AtomicBoolean(false)

    @Volatile
    var currentSound: NatureSound = NatureSound.RAIN
        private set

    @Volatile
    private var volume = 0.85f

    fun play(sound: NatureSound) {
        currentSound = sound
        if (isPlaying.get()) {
            return // Audio loop will pick up new currentSound automatically
        }

        isPlaying.set(true)
        audioThread = Thread({
            runAudioLoop()
        }, "NatureSoundSynthThread").apply {
            priority = Thread.NORM_PRIORITY + 1
            start()
        }
    }

    fun stop() {
        isPlaying.set(false)
        try {
            audioThread?.interrupt()
            audioThread = null
        } catch (e: Exception) {
            Log.e(TAG, "Error interrupting audio thread", e)
        }

        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio track", e)
        }
    }

    fun isAudioPlaying(): Boolean = isPlaying.get()

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0.0f, 1.0f)
        try {
            audioTrack?.setVolume(volume)
        } catch (e: Exception) {}
    }

    private fun runAudioLoop() {
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = Math.max(minBuf, BUFFER_SIZE * 2)

        val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize,
                AudioTrack.MODE_STREAM
            )
        }

        audioTrack = track
        track.play()

        val buffer = ShortArray(BUFFER_SIZE)
        val random = Random()

        // Synth state variables
        var pinkB0 = 0.0
        var pinkB1 = 0.0
        var pinkB2 = 0.0
        var sampleIndex = 0L

        // Chirp state for forest
        var chirpSampleCountdown = 0
        var chirpPhase = 0.0
        var chirpDuration = 0

        while (isPlaying.get() && !Thread.currentThread().isInterrupted) {
            val sound = currentSound
            val twoPi = 2.0 * Math.PI

            for (i in 0 until BUFFER_SIZE) {
                sampleIndex++
                val white = (random.nextDouble() * 2.0 - 1.0)

                // 3-pole pink noise filter (Paul Kellet's filter)
                pinkB0 = 0.99765 * pinkB0 + white * 0.0990460
                pinkB1 = 0.96300 * pinkB1 + white * 0.2965164
                pinkB2 = 0.57000 * pinkB2 + white * 1.0526913
                val pink = (pinkB0 + pinkB1 + pinkB2 + white * 0.1848) * 0.15

                val sampleOut: Double = when (sound) {
                    NatureSound.RAIN -> {
                        // Rain: pink noise background + stochastic raindrop splatters
                        var s = pink * 0.5
                        if (random.nextInt(180) == 0) {
                            s += (random.nextDouble() * 2.0 - 1.0) * 0.4
                        }
                        // Gentle low rumble modulation
                        val rumble = Math.sin(sampleIndex * (twoPi * 0.12 / SAMPLE_RATE)) * 0.08
                        s + rumble
                    }
                    NatureSound.OCEAN -> {
                        // Ocean: 10-second rhythmic swell cycle
                        val waveCycle = (sampleIndex % (SAMPLE_RATE * 10)) / (SAMPLE_RATE * 10.0)
                        val swell = Math.sin(waveCycle * Math.PI) // 0 to 1 smooth swell
                        val surf = pink * (0.15 + (swell * swell) * 0.75)
                        surf
                    }
                    NatureSound.FOREST -> {
                        // Forest: Soft breeze with intermittent bird chirps
                        val windMod = 0.25 + 0.15 * Math.sin(sampleIndex * (twoPi * 0.2 / SAMPLE_RATE))
                        var s = pink * windMod

                        if (chirpSampleCountdown <= 0) {
                            if (random.nextInt(25000) == 0) {
                                chirpSampleCountdown = SAMPLE_RATE / 6 // ~160ms chirp
                                chirpDuration = chirpSampleCountdown
                                chirpPhase = 0.0
                            }
                        } else {
                            chirpSampleCountdown--
                            val progress = 1.0 - (chirpSampleCountdown.toDouble() / chirpDuration)
                            val freq = 2400.0 + Math.sin(progress * Math.PI) * 1200.0
                            chirpPhase += twoPi * freq / SAMPLE_RATE
                            val chirpEnvelope = Math.sin(progress * Math.PI)
                            s += Math.sin(chirpPhase) * chirpEnvelope * 0.22
                        }
                        s
                    }
                    NatureSound.CAMPFIRE -> {
                        // Campfire: Warm low thermal hiss + random sharp crackle pops
                        var s = pink * 0.35
                        if (random.nextInt(750) == 0) {
                            // Crackle burst
                            s += (random.nextDouble() * 2.0 - 1.0) * 0.8
                        }
                        s
                    }
                    NatureSound.ZEN_DRONE -> {
                        // Meditative 432 Hz fundamental + 216 Hz warm sub-octave + gentle swell
                        val swell = 0.7 + 0.3 * Math.sin(sampleIndex * (twoPi * 0.08 / SAMPLE_RATE))
                        val tone1 = Math.sin(sampleIndex * (twoPi * 432.0 / SAMPLE_RATE)) * 0.45
                        val tone2 = Math.sin(sampleIndex * (twoPi * 216.0 / SAMPLE_RATE)) * 0.35
                        val overtone = Math.sin(sampleIndex * (twoPi * 864.0 / SAMPLE_RATE)) * 0.1
                        (tone1 + tone2 + overtone) * swell * 0.7
                    }
                }

                // Soft clip and scale to 16-bit PCM
                val clipped = Math.max(-1.0, Math.min(1.0, sampleOut * volume))
                buffer[i] = (clipped * 32767.0).toInt().toShort()
            }

            try {
                track.write(buffer, 0, BUFFER_SIZE)
            } catch (e: Exception) {
                break
            }
        }
    }
}
