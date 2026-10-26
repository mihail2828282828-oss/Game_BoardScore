package com.example.boardgamescore

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal fun vibrateNow(context: Context) {
    runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(
                Context.VIBRATOR_MANAGER_SERVICE
            ) as? VibratorManager

            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(
                Context.VIBRATOR_SERVICE
            ) as? Vibrator
        }

        if (vibrator == null || !vibrator.hasVibrator()) {
            return@runCatching
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    450,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(450)
        }
    }
}

internal suspend fun fanfare(enabled: Boolean) {
    if (!enabled) return

    val generator = try {
        ToneGenerator(
            AudioManager.STREAM_MUSIC,
            70
        )
    } catch (_: RuntimeException) {
        return
    }

    try {
        generator.startTone(
            ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
            220
        )
        delay(260)

        generator.startTone(
            ToneGenerator.TONE_CDMA_ABBR_ALERT,
            220
        )
        delay(260)

        generator.startTone(
            ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE,
            220
        )
        delay(260)
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: RuntimeException) {
        // Ошибка воспроизведения не должна мешать работе приложения.
    } finally {
        generator.release()
    }
}