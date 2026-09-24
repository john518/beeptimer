package com.jatstuff.beeptimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jatstuff.beeptimer.MainActivity
import com.jatstuff.beeptimer.R
import com.jatstuff.beeptimer.audio.AudioNotifier
import com.jatstuff.beeptimer.audio.DefaultAudioNotifier
import com.jatstuff.beeptimer.audio.TonePlayer
import com.jatstuff.beeptimer.audio.TtsPlayer
import com.jatstuff.beeptimer.timer.TimerEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

class TimerService : Service() {

    private val tonePlayer = TonePlayer()
    private lateinit var ttsPlayer: TtsPlayer
    private lateinit var audioNotifier: AudioNotifier
    private lateinit var timerEngine: TimerEngine

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    companion object {
        const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "BeepTimerChannel"

        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_INTERVAL_SECONDS = "extra_interval_seconds"
        const val EXTRA_VOICE_ENABLED = "extra_voice_enabled"
    }

    val currentCheckpoint: StateFlow<Int>
        get() = timerEngine.currentCheckpoint

    val isTimerRunning: StateFlow<Boolean>
        get() = timerEngine.isTimerRunning

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        ttsPlayer = TtsPlayer(this)
        audioNotifier = DefaultAudioNotifier(tonePlayer, ttsPlayer)
        timerEngine = TimerEngine(audioNotifier, serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 1) ?: 1
        val intervalSeconds = intent?.getIntExtra(EXTRA_INTERVAL_SECONDS, 10) ?: 10
        val voiceEnabled = intent?.getBooleanExtra(EXTRA_VOICE_ENABLED, true) ?: true

        if (!timerEngine.isTimerRunning.value) {
            startForegroundServiceWithNotification()

            val messagesArrayResId = getMessageArrayResId(durationMinutes)
            val messagesList = resources.getStringArray(messagesArrayResId).toList()

            timerEngine.startTimer(
                durationMinutes = durationMinutes,
                intervalSeconds = intervalSeconds,
                voiceEnabled = voiceEnabled,
                messages = messagesList,
                onCompleted = {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            )
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

    private fun getMessageArrayResId(durationMinutes: Int): Int {
        return when (durationMinutes) {
            1 -> R.array.messages_1min
            2 -> R.array.messages_2min
            6 -> R.array.messages_6min
            else -> R.array.messages_1min
        }
    }

    fun stopTimerSession() {
        timerEngine.stopTimer()
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
        timerEngine.stopTimer()
        tonePlayer.release()
        if (::ttsPlayer.isInitialized) {
            ttsPlayer.release()
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
