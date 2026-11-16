package dev.tunderlucky.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.tunderlucky.ui.OrbState
import java.util.Locale

/** Push-to-talk speech recognition + text-to-speech. No background / hidden recording. */
class VoiceManager(private val ctx: Context, private val onFinal: (String) -> Unit) {
    var state by mutableStateOf("Idle")      // Idle | Listening | Processing | Speaking
    var partial by mutableStateOf("")
    var lastError by mutableStateOf("")
    var lang: String = "en-IN"

    private var sr: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(ctx) { ttsReady = it == TextToSpeech.SUCCESS }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { state = "Speaking" }
            override fun onDone(id: String?) { state = "Idle"; OrbState.amp = 0f }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) { state = "Idle"; OrbState.amp = 0f }
        })
    }

    fun available(): Boolean = SpeechRecognizer.isRecognitionAvailable(ctx)

    fun listen() {
        stopSpeaking()
        lastError = ""; partial = ""
        if (!available()) { lastError = "No speech recognition service on this device"; state = "Idle"; return }
        sr?.destroy()
        sr = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rms: Float) { OrbState.amp = ((rms + 2f) / 12f).coerceIn(0f, 1f) }
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() { state = "Processing" }
                override fun onError(e: Int) {
                    OrbState.amp = 0f; state = "Idle"
                    lastError = when (e) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that. Tap the mic and try again."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission missing"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech service needs internet (or an offline language pack)"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy, try again"
                        else -> "Speech error $e"
                    }
                }
                override fun onResults(r: Bundle?) {
                    OrbState.amp = 0f
                    val t = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    partial = ""
                    if (t.isNullOrBlank()) { state = "Idle" } else { state = "Processing"; onFinal(t) }
                }
                override fun onPartialResults(p: Bundle?) {
                    partial = p?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                }
                override fun onEvent(t: Int, b: Bundle?) {}
            })
        }
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        state = "Listening"
        sr?.startListening(i)
    }

    fun stopListening() { sr?.stopListening(); state = "Processing" }

    fun speak(text: String) {
        if (!ttsReady) { state = "Idle"; return }
        val deva = text.any { it in '\u0900'..'\u097F' }
        tts?.language = if (deva) Locale("hi", "IN") else Locale("en", "IN")
        tts?.speak(text.take(600), TextToSpeech.QUEUE_FLUSH, null, "tl")
    }

    fun stopSpeaking() { tts?.stop(); if (state == "Speaking") state = "Idle" }
    fun stopAll() { sr?.cancel(); tts?.stop(); state = "Idle"; partial = ""; OrbState.amp = 0f }
    fun destroy() { sr?.destroy(); tts?.shutdown() }
}
