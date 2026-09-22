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
import com.jatstuff.beeptimer.audio.TonePlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.milliseconds

class TimerService : Service() {

    private val tonePlayer = TonePlayer()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "BeepTimerChannel"

        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_INTERVAL_SECONDS = "extra_interval_seconds"
    }

    // Expose state for the UI to observe
    private val _currentCheckpoint = MutableStateFlow(0)
    val currentCheckpoint: StateFlow<Int> = _currentCheckpoint.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    // Singleton-like accessor pattern or Binder approach can be used to hook UI to Service.
    // For simplicity, we can use a local binder or static instance reference for local binding.
    inner class LocalBinder : android.os.Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 1) ?: 1
        val intervalSeconds = intent?.getIntExtra(EXTRA_INTERVAL_SECONDS, 10) ?: 10

        if (!_isTimerRunning.value) {
            startForegroundServiceWithNotification()
            startTimer(durationMinutes * 60, intervalSeconds)
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
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Replace with your custom app icon later
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startTimer(totalSeconds: Int, intervalSeconds: Int) {
        _isTimerRunning.value = true
        _currentCheckpoint.value = 0

        timerJob = serviceScope.launch {
            var elapsedSeconds = 0

            while (elapsedSeconds < totalSeconds && _isTimerRunning.value) {
                delay(1000L.milliseconds) // Wait exactly 1 second
                elapsedSeconds++

                if (elapsedSeconds % intervalSeconds == 0) {
                    val checkpointIndex = elapsedSeconds / intervalSeconds
                    _currentCheckpoint.value = checkpointIndex

                    // TODO: Trigger Audio / Tone Burst + TTS here!
                    // Trigger the tone bursts asynchronously inside the service scope
                    launch {
                        tonePlayer.playToneBursts(checkpointIndex)
                    }
                }
            }

            // Timer completed naturally
            stopTimerSession()
        }
    }

    private fun triggerAudioCue(checkpointIndex: Int) {
        // We will wire up the Audio/TTS manager here next.
    }

    fun stopTimerSession() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
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

    override fun onDestroy() {
        tonePlayer.release()
        serviceScope.cancel()
        super.onDestroy()
    }
}