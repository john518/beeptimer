package com.jatstuff.beeptimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jatstuff.beeptimer.MainActivity
import com.jatstuff.beeptimer.R
import com.jatstuff.beeptimer.audio.TonePlayer
import com.jatstuff.beeptimer.audio.TtsPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.milliseconds

class TimerService : Service() {

    private val tonePlayer = TonePlayer()
    private lateinit var ttsPlayer: TtsPlayer
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "BeepTimerChannel"

        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_INTERVAL_SECONDS = "extra_interval_seconds"
        const val EXTRA_VOICE_ENABLED = "extra_voice_enabled"
    }

    // Expose state for the UI to observe
    private val _currentCheckpoint = MutableStateFlow(0)
    val currentCheckpoint: StateFlow<Int> = _currentCheckpoint.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    inner class LocalBinder : android.os.Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 1) ?: 1
        val intervalSeconds = intent?.getIntExtra(EXTRA_INTERVAL_SECONDS, 10) ?: 10
        val voiceEnabled = intent?.getBooleanExtra(EXTRA_VOICE_ENABLED, true) ?: true

        if (!_isTimerRunning.value) {
            startForegroundServiceWithNotification()
            startTimer(durationMinutes, intervalSeconds, voiceEnabled)
        }

        return START_NOT_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BeepTimer Active")
            .setContentText("Stretching session in progress...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startTimer(durationMinutes: Int, intervalSeconds: Int, voiceEnabled: Boolean) {
        val totalSeconds = durationMinutes * 60
        val messagesArrayResId = getMessageArrayResId(durationMinutes)
        val messagesArray = resources.getStringArray(messagesArrayResId)

        _isTimerRunning.value = true
        _currentCheckpoint.value = 0

        timerJob = serviceScope.launch {
            var elapsedSeconds = 0
            var lastAudioJob: Job? = null

            while (elapsedSeconds < totalSeconds && _isTimerRunning.value) {
                delay(1000L.milliseconds) // Wait exactly 1 second
                elapsedSeconds++

                if (elapsedSeconds % intervalSeconds == 0) {
                    val checkpointIndex = elapsedSeconds / intervalSeconds
                    _currentCheckpoint.value = checkpointIndex

                    val message = messagesArray.getOrNull(checkpointIndex - 1)
                        ?: "Checkpoint $checkpointIndex"

                    // Trigger tone burst + optional text-to-speech
                    lastAudioJob = launch {
                        tonePlayer.playToneBursts(checkpointIndex)
                        if (voiceEnabled) {
                            ttsPlayer.speakAndWait(message)
                        }
                    }
                }
            }

            // Wait for the final audio cue (tone + TTS) to finish playing out
            lastAudioJob?.join()

            // Brief pause so audio ends naturally before resetting session
            delay(500L.milliseconds)

            // Timer completed naturally
            stopTimerSession()
        }
    }

    private fun getMessageArrayResId(durationMinutes: Int): Int {
        return when (durationMinutes) {
            1 -> R.array.messages_1min
            2 -> R.array.messages_2min
            6 -> R.array.messages_6min
            else -> R.array.messages_1min
        }
    }

    fun stopTimerSession() {
        timerJob?.cancel()
        _isTimerRunning.value = false

        // Remove the persistent notification and stop foreground mode
        stopForeground(STOP_FOREGROUND_REMOVE)

        // Explicitly tell the service to shut down entirely
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "BeepTimer Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onCreate() {
        super.onCreate()
        _isTimerRunning.value = false
        _currentCheckpoint.value = 0
        ttsPlayer = TtsPlayer(this)
    }

    override fun onDestroy() {
        tonePlayer.release()
        if (::ttsPlayer.isInitialized) {
            ttsPlayer.release()
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
