package com.jatstuff.beeptimer.audio

interface AudioNotifier {
    suspend fun playCheckpointCue(
        checkpointIndex: Int,
        message: String?,
        voiceEnabled: Boolean
    )
}
