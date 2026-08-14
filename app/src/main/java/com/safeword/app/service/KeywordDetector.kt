package com.safeword.app.service

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class KeywordDetector(
    private val context: Context,
    private val onKeywordDetected: (String) -> Unit,
    private val onStatusChanged: (Status) -> Unit
) {

    enum class Status {
        UNINITIALIZED,
        INITIALIZING,
        READY,
        LISTENING,
        PAUSED,
        ERROR
    }

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var isListening = false

    fun initModel() {
        onStatusChanged(Status.INITIALIZING)
        
        // StorageService.unpack copies the model folder from assets to internal files directory
        // Use full SAM interface implementation for Callback to prevent ambiguous lambda mapping issues
        StorageService.unpack(context, "vosk-model-small-en", "model",
            object : StorageService.Callback<Model> {
                override fun onComplete(loadedModel: Model) {
                    model = loadedModel
                    Log.d("KeywordDetector", "Vosk model successfully loaded")
                    onStatusChanged(Status.READY)
                }
            },
            object : StorageService.Callback<IOException> {
                override fun onComplete(e: IOException) {
                    Log.e("KeywordDetector", "Failed to unpack or load Vosk model from assets", e)
                    // If no asset model is present, check if we already have it in app files
                    onStatusChanged(Status.UNINITIALIZED)
                }
            }
        )
    }

    fun startListening(triggerWords: List<String>) {
        val currentModel = model
        if (currentModel == null) {
            Log.w("KeywordDetector", "Vosk model not loaded. Offline voice triggers disabled.")
            onStatusChanged(Status.UNINITIALIZED)
            return
        }

        try {
            // Build Vosk grammar string like: ["help", "help me", "emergency", "[unk]"]
            val grammar = triggerWords.toMutableList().apply {
                add("[unk]")
            }
            val grammarJson = grammar.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }

            val recognizer = Recognizer(currentModel, 16000.0f, grammarJson)
            speechService = SpeechService(recognizer, 16000.0f)
            
            speechService?.startListening(object : RecognitionListener {
                override fun onResult(hypothesis: String) {
                    processHypothesis(hypothesis, triggerWords)
                }

                override fun onFinalResult(hypothesis: String) {
                    processHypothesis(hypothesis, triggerWords)
                }

                override fun onPartialResult(hypothesis: String) {
                    // Do nothing for partial results
                }

                override fun onError(exception: Exception) {
                    Log.e("KeywordDetector", "Vosk recognizer error", exception)
                    onStatusChanged(Status.ERROR)
                }

                override fun onTimeout() {
                    // Do nothing
                }
            })
            isListening = true
            onStatusChanged(Status.LISTENING)
            Log.d("KeywordDetector", "Keyword listener started successfully")
        } catch (e: Exception) {
            Log.e("KeywordDetector", "Failed to start listening", e)
            onStatusChanged(Status.ERROR)
        }
    }

    private fun processHypothesis(hypothesis: String, triggerWords: List<String>) {
        try {
            val json = JSONObject(hypothesis)
            val text = json.optString("text", "").lowercase().trim()
            if (text.isNotEmpty()) {
                Log.d("KeywordDetector", "Hypothesis detected: $text")
                // Check if any trigger word is contained in the text
                for (trigger in triggerWords) {
                    if (text.contains(trigger.lowercase().trim())) {
                        Log.d("KeywordDetector", "Trigger word MATCH: $trigger")
                        onKeywordDetected(trigger)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("KeywordDetector", "Failed to parse hypothesis json", e)
        }
    }

    fun stopListening() {
        speechService?.let {
            it.stop()
            speechService = null
        }
        isListening = false
        onStatusChanged(if (model != null) Status.READY else Status.UNINITIALIZED)
    }

    fun destroy() {
        stopListening()
        model?.onDestroy()
        model = null
    }
}
