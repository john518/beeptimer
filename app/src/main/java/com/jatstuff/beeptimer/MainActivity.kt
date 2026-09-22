package com.jatstuff.beeptimer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.jatstuff.beeptimer.service.TimerService
import com.jatstuff.beeptimer.ui.ActiveTimerScreen
import com.jatstuff.beeptimer.ui.DurationSelectionScreen
// import com.jatstuff.beeptimer.ui.theme.BeepTimerTheme // Uncomment once your theme is set up

class MainActivity : ComponentActivity() {

    private var timerService by mutableStateOf<TimerService?>(null)
    private var isBound by mutableStateOf(false)

    // Keep track of chosen settings to pass to the active screen
    private var selectedMinutes by mutableIntStateOf(1)
    private var selectedIntervalSeconds by mutableIntStateOf(10)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            isBound = true

            // Launch a collector to track service state changes in real time
            // (In a full app lifecycle, collectAsState() or repeatOnLifecycle is ideal)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            isBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind to the service if it's already running in the background
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // BeepTimerTheme { // Wrap with your theme
            // Simple condition: Check if service says timer is running
            val isRunning by timerService?.isTimerRunning?.collectAsState(initial = false)
                ?: remember { mutableStateOf(false) }

            val checkpoint by timerService?.currentCheckpoint?.collectAsState(initial = 0)
                ?: remember { mutableIntStateOf(0) }

            if (isRunning) {
                ActiveTimerScreen(
                    durationMinutes = selectedMinutes,
                    intervalSeconds = selectedIntervalSeconds,
                    currentCheckpoint = checkpoint,
                    onEndClicked = {
                        // Stop the service from running
                        val stopIntent = Intent(this@MainActivity, TimerService::class.java)
                        stopService(stopIntent)
                    }
                )
            } else {
                DurationSelectionScreen(
                    onDurationSelected = { minutes, interval ->
                        selectedMinutes = minutes
                        selectedIntervalSeconds = interval

                        // Start the Foreground Service
                        val intent = Intent(this@MainActivity, TimerService::class.java).apply {
                            putExtra(TimerService.EXTRA_DURATION_MINUTES, minutes)
                            putExtra(TimerService.EXTRA_INTERVAL_SECONDS, interval)
                        }
                        startForegroundService(intent)
                    }
                )
            }
            // }
        }
    }
}