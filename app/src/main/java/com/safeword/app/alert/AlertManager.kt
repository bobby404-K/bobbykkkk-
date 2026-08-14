package com.safeword.app.alert

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.safeword.app.data.AppDatabase
import com.safeword.app.data.ContactsRepository
import com.safeword.app.data.IncidentLog
import com.safeword.app.data.IncidentLogRepository
import com.safeword.app.location.LocationHelper
import com.safeword.app.service.TrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlertManager(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val contactsRepository = ContactsRepository(database.contactDao())
    private val incidentLogRepository = IncidentLogRepository(database.incidentLogDao())
    private val locationHelper = LocationHelper(context)
    private val smsSender = SmsSender(context)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun triggerEmergencyAlert(triggerWord: String? = null) {
        coroutineScope.launch {
            Log.d("AlertManager", "Emergency alert triggered! Word: $triggerWord")
            
            // 1. Fetch user configuration
            val prefs = context.getSharedPreferences("safeword_prefs", Context.MODE_PRIVATE)
            val userName = prefs.getString("user_name", "Someone") ?: "Someone"
            val trackingEnabled = prefs.getBoolean("tracking_enabled", true)

            // 2. Fetch contacts
            val contacts = contactsRepository.getAllContactsDirect()
            if (contacts.isEmpty()) {
                Log.w("AlertManager", "No contacts defined. Can't send alert.")
                val timestamp = System.currentTimeMillis()
                incidentLogRepository.insert(
                    IncidentLog(
                        timestamp = timestamp,
                        triggerWord = triggerWord ?: "Manual Panic",
                        latitude = 0.0,
                        longitude = 0.0,
                        status = "NO CONTACTS"
                    )
                )
                return@launch
            }

            // 3. Fetch location
            val location = locationHelper.getCurrentLocation()
            val latitude = location?.latitude ?: 0.0
            val longitude = location?.longitude ?: 0.0
            val timestamp = System.currentTimeMillis()

            // 4. Send SMS alert
            val smsSuccess = smsSender.sendEmergencySms(
                contacts = contacts,
                userName = userName,
                latitude = latitude,
                longitude = longitude,
                triggerWord = triggerWord
            )

            // 5. Log incident locally
            val status = if (smsSuccess) "SENT" else "FAILED"
            incidentLogRepository.insert(
                IncidentLog(
                    timestamp = timestamp,
                    triggerWord = triggerWord ?: "Manual Panic",
                    latitude = latitude,
                    longitude = longitude,
                    status = status
                )
            )

            // 6. Optionally start tracking service for continuous updates
            if (trackingEnabled && location != null) {
                val intent = Intent(context, TrackingService::class.java).apply {
                    action = TrackingService.ACTION_START_TRACKING
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }
}
