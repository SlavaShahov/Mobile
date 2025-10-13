package com.example.myapplication.game.sound

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.myapplication.R

class SoundManager(private val context: Context) {

    companion object {
        private const val TAG = "SoundManager"
        private const val MAX_CONCURRENT_SCREAMS = 5
    }

    private var tiltBonusSound: MediaPlayer? = null
    private val insectScreamPool = mutableListOf<MediaPlayer>()
    private var isSoundEnabled = true
    private var lastRollingScreamTime = 0L

    init {
        setupSounds()
    }

    private fun setupSounds() {
        try {
            tiltBonusSound = MediaPlayer.create(context, R.raw.tilt_bonus_activate)

            // Создаем пул звуков для криков при скатывании
            repeat(MAX_CONCURRENT_SCREAMS) {
                val scream = MediaPlayer.create(context, R.raw.insect_scream)
                scream?.let { insectScreamPool.add(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up sounds", e)
        }
    }

    fun playTiltBonusSound() {
        if (!isSoundEnabled) return
        try {
            tiltBonusSound?.seekTo(0)
            tiltBonusSound?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing tilt bonus sound", e)
        }
    }

    fun playRollingScream() {
        if (!isSoundEnabled) return

        val currentTime = System.currentTimeMillis()
        // Ограничиваем частоту криков чтобы не было сплошного шума
        if (currentTime - lastRollingScreamTime < 500) return

        try {
            // Ищем свободный MediaPlayer в пуле
            val availableScream = insectScreamPool.find { !it.isPlaying }
            availableScream?.let {
                it.seekTo(0)
                it.start()
                lastRollingScreamTime = currentTime
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing rolling scream", e)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        isSoundEnabled = enabled
    }

    fun release() {
        tiltBonusSound?.release()
        insectScreamPool.forEach { it.release() }
        insectScreamPool.clear()
    }
}