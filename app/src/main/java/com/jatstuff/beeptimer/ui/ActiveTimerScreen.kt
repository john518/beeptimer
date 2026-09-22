package com.jatstuff.beeptimer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ActiveTimerScreen(
    durationMinutes: Int,
    intervalSeconds: Int,
    currentCheckpoint: Int, // e.g., 1 through 6
    totalCheckpoints: Int = 6,
    onEndClicked: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top section: Status / Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text(
                    text = "Timer Running",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$durationMinutes Min Session ($intervalSeconds s intervals)",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Middle section: Prominent End Button (Centered now)
            // Using a distinct red container/color scheme to ensure it stands out immediately
            val stopButtonColor = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )

            Button(
                onClick = onEndClicked,
                colors = stopButtonColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp), // Slightly larger button for easier access
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = "END",
                    fontSize = 32.sp, // Larger text for the main action
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom section: Current Progress / Interval Checkpoint indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp) // Give some breathing room at the bottom
            ) {
                Text(
                    text = "Checkpoint",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$currentCheckpoint / $totalCheckpoints",
                    fontSize = 48.sp, // Smaller than previous, but still readable
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActiveTimerScreenPreview() {
    MaterialTheme {
        ActiveTimerScreen(
            durationMinutes = 1,
            intervalSeconds = 10,
            currentCheckpoint = 3,
            onEndClicked = {}
        )
    }
}