package com.jatstuff.beeptimer.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class TtsPlayer(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = true
            }
        }
    }

    /**
     * Speaks the specified text message asynchronously.
     */
    fun speak(text: String, utteranceId: String = "interval_cue") {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    /**
     * Speaks the specified text message and suspends until speech completes or fails.
     */
    suspend fun speakAndWait(text: String, utteranceId: String = "interval_cue") {
        if (!isInitialized) return

        suspendCancellableCoroutine { continuation ->
            val listener = object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}

                override fun onDone(id: String?) {
                    if (id == utteranceId && continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    if (id == utteranceId && continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }

                override fun onError(id: String?, errorCode: Int) {
                    if (id == utteranceId && continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            }

            tts?.setOnUtteranceProgressListener(listener)
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

            if (result == TextToSpeech.ERROR) {
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }

            continuation.invokeOnCancellation {
                tts?.stop()
            }
        }
    }

    /**
     * Releases TTS resources when no longer needed.
     */
    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
