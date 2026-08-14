package com.safeword.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.safeword.app.alert.SmsSender
import com.safeword.app.data.AppDatabase
import com.safeword.app.data.ContactsRepository
import com.safeword.app.location.LocationHelper
import kotlinx.coroutines.*

class TrackingService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var trackingJob: Job? = null

    private lateinit var locationHelper: LocationHelper
    private lateinit var smsSender: SmsSender
    private lateinit var contactsRepository: ContactsRepository

    companion object {
        const val CHANNEL_ID = "SafeWordTrackingChannel"
        const val NOTIFICATION_ID = 1002
        const val ACTION_START_TRACKING = "START_TRACKING"
        const val ACTION_STOP_TRACKING = "STOP_TRACKING"
    }

    override fun onCreate() {
        super.onCreate()
        locationHelper = LocationHelper(this)
        smsSender = SmsSender(this)
        val database = AppDatabase.getDatabase(this)
        contactsRepository = ContactsRepository(database.contactDao())
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val serviceType = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    startForeground(NOTIFICATION_ID, createNotification(), serviceType)
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
                startTrackingLoop()
            }
            ACTION_STOP_TRACKING -> {
                stopTrackingLoop()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTrackingLoop() {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            val prefs = getSharedPreferences("safeword_prefs", Context.MODE_PRIVATE)
            val durationMinutes = prefs.getInt("tracking_duration_minutes", 10)
            val intervalSeconds = prefs.getInt("tracking_interval_seconds", 30)
            val userName = prefs.getString("user_name", "Someone") ?: "Someone"

            val endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000)
            val intervalMs = intervalSeconds * 1000L

            Log.d("TrackingService", "Starting tracking loop: $durationMinutes min duration, $intervalSeconds sec interval")

            while (System.currentTimeMillis() < endTime) {
                val contacts = contactsRepository.getAllContactsDirect()
                if (contacts.isEmpty()) {
                    Log.w("TrackingService", "No emergency contacts, stopping tracking.")
                    break
                }

                val location = locationHelper.getCurrentLocation()
                if (location != null) {
                    smsSender.sendTrackingUpdate(
                        contacts = contacts,
                        userName = userName,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    Log.d("TrackingService", "Sent tracking update: ${location.latitude}, ${location.longitude}")
                } else {
                    Log.w("TrackingService", "Could not fetch fresh location for tracking ping.")
                }

                delay(intervalMs)
            }
            Log.d("TrackingService", "Tracking duration elapsed, stopping service.")
            stopSelf()
        }
    }

    private fun stopTrackingLoop() {
        trackingJob?.cancel()
        trackingJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, TrackingService::class.java).apply {
            action = ACTION_STOP_TRACKING
        }
        val stopPendingIntent = android.app.PendingIntent.getService(
            this,
            0,
            stopIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeWord Tracking Active")
            .setContentText("Emergency tracking is sending periodic location pings.")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Sharing",
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "SafeWord Emergency Tracking",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
