package com.opencookie.app.ui.screens.cookie

import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View

object CookieHaptics {

    /**
     * Tactile feedback on initial cookie tap/press.
     */
    fun performTapHaptic(view: View) {
        @Suppress("DEPRECATION")
        view.performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
        )
    }

    /**
     * Subtle micro-crunch tick when pressure builds up in the cookie (at ~320ms into bending).
     */
    fun performPreCrackHaptic(vibrator: Vibrator) {
        try {
            val effect = VibrationEffect.createOneShot(25, 170)
            vibrateWithTouchAttributes(vibrator, effect)
        } catch (_: Throwable) {
        }
    }

    /**
     * Rich, high-sensitivity crunch & crack vibration when the cookie breaks open (at ~720ms).
     */
    fun performMainCrackHaptic(vibrator: Vibrator) {
        // Try hardware-driven composition primitives on Android 11+ (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                    val composition = VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.75f, 30)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 40)
                        .compose()
                    vibrateWithTouchAttributes(vibrator, composition)
                    return
                }
            } catch (_: Throwable) {
            }
        }

        // Custom multi-burst high-amplitude waveform for guaranteed strong "crunch"
        try {
            // Waveform: 50ms @ max (255), 30ms pause, 35ms @ 210, 25ms pause, 65ms @ max (255)
            val timings = longArrayOf(0, 50, 30, 35, 25, 65)
            val amplitudes = intArrayOf(0, 255, 0, 210, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrateWithTouchAttributes(vibrator, effect)
            return
        } catch (_: Throwable) {
        }

        // Legacy fallback
        try {
            val pattern = longArrayOf(0, 50, 30, 35, 25, 65)
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        } catch (_: Throwable) {
        }
    }

    private fun vibrateWithTouchAttributes(vibrator: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val attributes = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_TOUCH)
                    .build()
                vibrator.vibrate(effect, attributes)
            } catch (_: Throwable) {
                vibrator.vibrate(effect)
            }
        } else {
            vibrator.vibrate(effect)
        }
    }
}
