package com.beehivehex.fill.core.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings

class FeedbackPlayer(private val context: Context) {
    private var tone: ToneGenerator? = null
    fun play(clear: Boolean, sound: Boolean, haptics: Boolean) {
        val manager = context.getSystemService(AudioManager::class.java)
        if (sound && manager != null && manager.ringerMode == AudioManager.RINGER_MODE_NORMAL && manager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0) {
            runCatching {
                val player = tone ?: ToneGenerator(AudioManager.STREAM_MUSIC, 25).also { tone = it }
                player.startTone(if (clear) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP, if (clear) 80 else 45)
            }
        }
        if (haptics && Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1) {
            runCatching { context.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE)) }
        }
    }
    fun stop() { tone?.release(); tone = null }
}
