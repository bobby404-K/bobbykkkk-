package com.safeword.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.safeword.app.MainActivity
import com.safeword.app.alert.AlertManager
import kotlinx.coroutines.*

class ListeningService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private lateinit var keywordDetector: KeywordDetector
    private lateinit var alertManager: AlertManager

    private var isListeningActive = false
    private var triggerWords = listOf("help", "help me")

    companion object {
        const val CHANNEL_ID = "SafeWordListeningChannel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "START_LISTENING"
        const val ACTION_STOP = "STOP_LISTENING"
        const val ACTION_PAUSE = "PAUSE_LISTENING"
        const val ACTION_RESUME = "RESUME_LISTENING"
        const val ACTION_PANIC = "MANUAL_PANIC"
        
        const val PREFS_NAME = "safeword_prefs"
        const val KEY_SERVICE_RUNNING = "service_running"
    }

    override fun onCreate() {
        super.onCreate()
        alertManager = AlertManager(this)
        
        keywordDetector = KeywordDetector(
            context = this,
            onKeywordDetected = { word ->
                Log.d("ListeningService", "Keyword detected: $word")
                alertManager.triggerEmergencyAlert(word)
            },
            onStatusChanged = { status ->
                Log.d("ListeningService", "Detector status: $status")
                updateNotification(status)
            }
        )
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.d("ListeningService", "onStartCommand action: $action")

        when (action) {
            ACTION_START -> {
                startServiceForeground()
                loadSettingsAndStart()
            }
            ACTION_STOP -> {
                stopListening()
                stopSelf()
            }
            ACTION_PAUSE -> {
                pauseListening()
            }
            ACTION_RESUME -> {
                resumeListening()
            }
            ACTION_PANIC -> {
                alertManager.triggerEmergencyAlert("Manual Trigger")
            }
        }

        return START_STICKY
    }

    private fun startServiceForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            startForeground(NOTIFICATION_ID, createNotification("Starting SafeWord...", true), serviceType)
        } else {
            startForeground(NOTIFICATION_ID, createNotification("Starting SafeWord...", true))
        }
        
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SERVICE_RUNNING, true)
            .apply()
    }

    private fun loadSettingsAndStart() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val wordSet = prefs.getStringSet("trigger_words", setOf("help", "help me")) ?: setOf("help", "help me")
        triggerWords = wordSet.toList()
        
        keywordDetector.initModel()
        serviceScope.launch {
            delay(1000)
            keywordDetector.startListening(triggerWords)
            isListeningActive = true
        }
    }

    private fun pauseListening() {
        keywordDetector.stopListening()
        isListeningActive = false
        updateNotification(KeywordDetector.Status.PAUSED)
    }

    private fun resumeListening() {
        keywordDetector.startListening(triggerWords)
        isListeningActive = true
    }

    private fun stopListening() {
        keywordDetector.destroy()
        isListeningActive = false
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SERVICE_RUNNING, false)
            .apply()
    }

    private fun updateNotification(status: KeywordDetector.Status) {
        val message = when (status) {
            KeywordDetector.Status.UNINITIALIZED -> "Offline model missing (Check settings)"
            KeywordDetector.Status.INITIALIZING -> "Initializing voice detector..."
            KeywordDetector.Status.READY -> "Voice detector ready"
            KeywordDetector.Status.LISTENING -> "SafeWord is listening..."
            KeywordDetector.Status.PAUSED -> "Voice detector paused"
            KeywordDetector.Status.ERROR -> "Voice detection error"
        }
        val showPause = status == KeywordDetector.Status.LISTENING
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification(message, showPause))
    }

    private fun createNotification(contentText: String, showPauseButton: Boolean): Notification {
        val mainPendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeWord Active Protection")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(mainPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)

        val panicIntent = Intent(this, ListeningService::class.java).apply { action = ACTION_PANIC }
        val panicPendingIntent = android.app.PendingIntent.getService(
            this, 1, panicIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_dialog_alert, "Panic Alert", panicPendingIntent)

        if (showPauseButton) {
            val pauseIntent = Intent(this, ListeningService::class.java).apply { action = ACTION_PAUSE }
            val pausePendingIntent = android.app.PendingIntent.getService(
                this, 2, pauseIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause Protection", pausePendingIntent)
        } else {
            val resumeIntent = Intent(this, ListeningService::class.java).apply { action = ACTION_RESUME }
            val resumePendingIntent = android.app.PendingIntent.getService(
                this, 3, resumeIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume Protection", resumePendingIntent)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SafeWord Background Protection",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
