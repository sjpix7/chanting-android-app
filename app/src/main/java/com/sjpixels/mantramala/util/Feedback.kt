package com.sjpixels.mantramala.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.sjpixels.mantramala.R

/** Haptics for each bead and a temple bell when a mala completes. */
class Feedback(context: Context) {
    private val appContext = context.applicationContext

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Vibrator::class.java)
    }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val bellId: Int = soundPool.load(appContext, R.raw.temple_bell, 1)

    fun bead() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        } else {
            VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        v.vibrate(effect)
    }

    fun malaComplete(haptics: Boolean, bell: Boolean) {
        if (haptics) {
            vibrator?.takeIf { it.hasVibrator() }
                ?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 90, 70, 90, 70, 260), -1))
        }
        if (bell) soundPool.play(bellId, 1f, 1f, 1, 0, 1f)
    }
}
