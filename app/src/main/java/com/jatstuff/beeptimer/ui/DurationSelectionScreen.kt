package com.jatstuff.beeptimer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jatstuff.beeptimer.ui.theme.BeepTimerTheme

@Composable
fun DurationSelectionScreen(
    onDurationSelected: (durationMinutes: Int, intervalSeconds: Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BeepTimer",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Select duration and cadence",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // 1 Minute Option
            DurationButton(
                title = "1 Minute",
                subtitle = "Beeps every 10 seconds",
                onClick = { onDurationSelected(1, 10) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2 Minute Option
            DurationButton(
                title = "2 Minutes",
                subtitle = "Beeps every 20 seconds",
                onClick = { onDurationSelected(2, 20) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 6 Minute Option
            DurationButton(
                title = "6 Minutes",
                subtitle = "Beeps every 60 seconds",
                onClick = { onDurationSelected(6, 60) }
            )
        }
    }
}

@Composable
fun DurationButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DurationSelectionPreview() {
    BeepTimerTheme {
        DurationSelectionScreen(
            onDurationSelected = { minutes, seconds ->
                // Dummy action for preview purposes; does nothing when tapped in preview
            }
        )
    }

}