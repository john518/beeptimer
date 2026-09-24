package com.jatstuff.beeptimer.timer

import com.jatstuff.beeptimer.audio.AudioNotifier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.milliseconds

class TimerEngine(
    private val audioNotifier: AudioNotifier,
    private val scope: CoroutineScope
) {
    private val _currentCheckpoint = MutableStateFlow(0)
    val currentCheckpoint: StateFlow<Int> = _currentCheckpoint.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer(
        durationMinutes: Int,
        intervalSeconds: Int,
        voiceEnabled: Boolean,
        messages: List<String> = emptyList(),
        onCompleted: (() -> Unit)? = null
    ) {
        if (_isTimerRunning.value) return

        val totalSeconds = durationMinutes * 60
        _isTimerRunning.value = true
        _currentCheckpoint.value = 0

        timerJob = scope.launch {
            var elapsedSeconds = 0
            var lastAudioJob: Job? = null

            while (elapsedSeconds < totalSeconds && _isTimerRunning.value) {
                delay(1000L.milliseconds)
                elapsedSeconds++

                if (elapsedSeconds % intervalSeconds == 0) {
                    val checkpointIndex = elapsedSeconds / intervalSeconds
                    _currentCheckpoint.value = checkpointIndex

                    val message = messages.getOrNull(checkpointIndex - 1)
                        ?: "Checkpoint $checkpointIndex"

                    lastAudioJob = launch {
                        audioNotifier.playCheckpointCue(checkpointIndex, message, voiceEnabled)
                    }
                }
            }

            // Wait for the final audio cue (tone + TTS) to finish playing
            lastAudioJob?.join()

            // Brief pause before ending the session
            delay(500L.milliseconds)

            if (_isTimerRunning.value) {
                stopTimer()
                onCompleted?.invoke()
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _currentCheckpoint.value = 0
    }
}
