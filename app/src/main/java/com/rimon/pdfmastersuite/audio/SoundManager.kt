package com.rimon.pdfmastersuite.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.rimon.pdfmastersuite.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SoundManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: SoundManager? = null

        fun getInstance(context: Context): SoundManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SoundManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var soundPool: SoundPool? = null
    private var clickSoundId: Int = 0
    private var isSoundLoaded: Boolean = false
    private var lastPlayTimestamp: Long = 0L

    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && sampleId == clickSoundId) {
                isSoundLoaded = true
            }
        }

        try {
            clickSoundId = soundPool?.load(context, R.raw.click_sound, 1) ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playClick() {
        val now = System.currentTimeMillis()
        // Prevent duplicate clicks within 60ms
        if (now - lastPlayTimestamp < 60) return
        lastPlayTimestamp = now

        // Check user setting
        if (soundEnabled) {
            // Check Android ringer mode: respect silent and vibrate mode
            val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
            if (ringerMode == AudioManager.RINGER_MODE_NORMAL && isSoundLoaded && soundPool != null) {
                try {
                    soundPool?.play(clickSoundId, 0.7f, 0.7f, 1, 0, 1.0f)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Haptic feedback
        if (hapticsEnabled) {
            triggerHaptic()
        }
    }

    private fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(18)
            }
        } catch (e: Exception) {
            // Silently ignore vibration exceptions
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        isSoundLoaded = false
    }
}
