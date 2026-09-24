package com.jatstuff.beeptimer.audio

class DefaultAudioNotifier(
    private val tonePlayer: TonePlayer,
    private val ttsPlayer: TtsPlayer
) : AudioNotifier {
    override suspend fun playCheckpointCue(
        checkpointIndex: Int,
        message: String?,
        voiceEnabled: Boolean
    ) {
        tonePlayer.playToneBursts(checkpointIndex)
        if (voiceEnabled && !message.isNullOrBlank()) {
            ttsPlayer.speakAndWait(message)
        }
    }
}
