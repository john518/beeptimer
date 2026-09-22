package com.jatstuff.beeptimer.audio

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class TonePlayer {
    // Use the STREAM_MUSIC stream so it respects device media volume settings
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    /**
     * Plays a specified number of tone bursts.
     * e.g., count = 3 will play 3 beeps with a short pause between them.
     */
    suspend fun playToneBursts(count: Int) {
        for (i in 1..count) {
            // TONE_DTMF_1 is a clean, standard beep tone; play for 150 milliseconds
            toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 150)

            // Wait for the beep duration plus a brief gap before the next beep
            delay(250.milliseconds)
        }
    }

    /**
     * Clean up system resources when the service or app terminates.
     */
    fun release() {
        toneGenerator.release()
    }
}