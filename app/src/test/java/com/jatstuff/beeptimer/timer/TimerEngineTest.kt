package com.jatstuff.beeptimer.timer

import com.jatstuff.beeptimer.audio.AudioNotifier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class TimerEngineTest {

    private lateinit var fakeAudioNotifier: FakeAudioNotifier

    @Before
    fun setUp() {
        fakeAudioNotifier = FakeAudioNotifier()
    }

    private fun TestScope.advanceTimeAndRun(duration: Duration) {
        advanceTimeBy(duration)
        runCurrent()
    }

    @Test
    fun initialState_isNotRunningAndCheckpointIsZero() = runTest {
        val timerEngine = TimerEngine(fakeAudioNotifier, this)

        assertFalse(timerEngine.isTimerRunning.value)
        assertEquals(0, timerEngine.currentCheckpoint.value)
        assertTrue(fakeAudioNotifier.calls.isEmpty())
    }

    @Test
    fun startTimer_emitsCheckpointsAtCorrectIntervals() = runTest {
        val timerEngine = TimerEngine(fakeAudioNotifier, this)
        val messages = listOf("Ten", "Twenty", "Thirty", "Forty", "Fifty", "Sixty")

        timerEngine.startTimer(
            durationMinutes = 1,
            intervalSeconds = 10,
            voiceEnabled = true,
            messages = messages
        )

        assertTrue(timerEngine.isTimerRunning.value)

        // Advance 10 seconds -> First checkpoint
        advanceTimeAndRun(10.seconds)
        assertEquals(1, timerEngine.currentCheckpoint.value)
        assertEquals(1, fakeAudioNotifier.calls.size)
        assertEquals(1, fakeAudioNotifier.calls[0].checkpointIndex)
        assertEquals("Ten", fakeAudioNotifier.calls[0].message)
        assertTrue(fakeAudioNotifier.calls[0].voiceEnabled)

        // Advance another 20 seconds -> Third checkpoint (30 seconds total)
        advanceTimeAndRun(20.seconds)
        assertEquals(3, timerEngine.currentCheckpoint.value)
        assertEquals(3, fakeAudioNotifier.calls.size)
        assertEquals("Thirty", fakeAudioNotifier.calls[2].message)
    }

    @Test
    fun twoMinuteSession_triggers6CheckpointsAt20SecIntervals() = runTest {
        val timerEngine = TimerEngine(fakeAudioNotifier, this)

        timerEngine.startTimer(
            durationMinutes = 2,
            intervalSeconds = 20,
            voiceEnabled = true,
            messages = listOf("20s", "40s", "1m", "1m 20s", "1m 40s", "2m")
        )

        advanceTimeAndRun(20.seconds)
        assertEquals(1, timerEngine.currentCheckpoint.value)
        assertEquals("20s", fakeAudioNotifier.calls[0].message)

        advanceTimeAndRun(100.seconds) // 120 seconds total
        assertEquals(6, timerEngine.currentCheckpoint.value)
        assertEquals(6, fakeAudioNotifier.calls.size)
        assertEquals("2m", fakeAudioNotifier.calls[5].message)
    }

    @Test
    fun sixMinuteSession_triggers6CheckpointsAt60SecIntervals() = runTest {
        val timerEngine = TimerEngine(fakeAudioNotifier, this)

        timerEngine.startTimer(
            durationMinutes = 6,
            intervalSeconds = 60,
            voiceEnabled = true,
            messages = listOf("1m", "2m", "3m", "4m", "5m", "6m")
        )

        advanceTimeAndRun(1.minutes)
        assertEquals(1, timerEngine.currentCheckpoint.value)
        assertEquals("1m", fakeAudioNotifier.calls[0].message)

        advanceTimeAndRun(5.minutes) // 6 minutes total
        assertEquals(6, timerEngine.currentCheckpoint.value)
        assertEquals(6, fakeAudioNotifier.calls.size)
        assertEquals("6m", fakeAudioNotifier.calls[5].message)
    }

    @Test
    fun timerCompletion_resetsStateAndTriggersOnCompletedCallback() = runTest {
        val timerEngine = TimerEngine(fakeAudioNotifier, this)
        var completedCalled = false

        timerEngine.startTimer(
            durationMinutes = 1,
            intervalSeconds = 10,
            voiceEnabled = true,
            messages = listOf("10", "20", "30", "40", "50", "60"),
            onCompleted = { completedCalled = true }
        )

        // Advance through the entire 60 seconds plus the trailing 500ms delay
        advanceTimeAndRun(61.seconds)

        assertFalse(timerEngine.isTimerRunning.value)
        assertEquals(0, timerEngine.currentCheckpoint.value)
        assertEquals(6, fakeAudioNotifier.calls.size)
        assertTrue(completedCalled)
    }

    @Test
    fun stopTimer_cancelsSessionAndResetsState() = runTest {
        val timerEngine = TimerEngine(fakeAudioNotifier, this)

        timerEngine.startTimer(
            durationMinutes = 1,
            intervalSeconds = 10,
            voiceEnabled = true
        )

        // Advance 15 seconds (1 checkpoint triggered)
        advanceTimeAndRun(15.seconds)
        assertEquals(1, timerEngine.currentCheckpoint.value)

        // Stop manually
        timerEngine.stopTimer()

        assertFalse(timerEngine.isTimerRunning.value)
        assertEquals(0, timerEngine.currentCheckpoint.value)

        // Advance another 30 seconds -> no new checkpoints should fire
        advanceTimeAndRun(30.seconds)
        assertEquals(1, fakeAudioNotifier.calls.size)
    }

    @Test
    fun fallbackMessageUsed_whenMessagesListIsEmpty() = runTest {
        val timerEngine = TimerEngine(fakeAudioNotifier, this)

        timerEngine.startTimer(
            durationMinutes = 1,
            intervalSeconds = 10,
            voiceEnabled = false,
            messages = emptyList()
        )

        advanceTimeAndRun(10.seconds)

        assertEquals(1, fakeAudioNotifier.calls.size)
        assertEquals("Checkpoint 1", fakeAudioNotifier.calls[0].message)
        assertFalse(fakeAudioNotifier.calls[0].voiceEnabled)
    }

    private class FakeAudioNotifier : AudioNotifier {
        val calls = mutableListOf<AudioCueCall>()

        override suspend fun playCheckpointCue(
            checkpointIndex: Int,
            message: String?,
            voiceEnabled: Boolean
        ) {
            calls.add(AudioCueCall(checkpointIndex, message, voiceEnabled))
        }
    }

    private data class AudioCueCall(
        val checkpointIndex: Int,
        val message: String?,
        val voiceEnabled: Boolean
    )
}
