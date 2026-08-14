package com.safeword.app.alert

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import com.safeword.app.data.Contact
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsSender(private val context: Context) {

    private val smsManager: SmsManager by lazy {
        val manager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        manager ?: throw IllegalStateException("SMS Manager is not available on this device")
    }

    fun sendEmergencySms(
        contacts: List<Contact>,
        userName: String,
        latitude: Double,
        longitude: Double,
        triggerWord: String? = null
    ): Boolean {
        if (contacts.isEmpty()) {
            Log.w("SmsSender", "No contacts configured, cannot send SMS.")
            return false
        }

        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val formattedTime = timeFormat.format(Date())

        val mapsLink = "https://maps.google.com/?q=$latitude,$longitude"
        val triggerInfo = if (triggerWord != null) " (Trigger: \"$triggerWord\")" else ""
        
        val message = """
            🚨 SafeWord Alert 🚨
            $userName may need help$triggerInfo.
            Location: $mapsLink
            Time: $formattedTime
            This is an automated message.
        """.trimIndent()

        var allSent = true
        for (contact in contacts) {
            try {
                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(contact.phoneNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null)
                }
                Log.d("SmsSender", "Alert successfully sent to ${contact.name} (${contact.phoneNumber})")
            } catch (e: Exception) {
                Log.e("SmsSender", "Failed to send SMS to ${contact.name}", e)
                allSent = false
            }
        }
        return allSent
    }

    fun sendTrackingUpdate(
        contacts: List<Contact>,
        userName: String,
        latitude: Double,
        longitude: Double
    ) {
        val mapsLink = "https://maps.google.com/?q=$latitude,$longitude"
        val message = """
            📍 SafeWord Location Update 📍
            $userName's location: $mapsLink
        """.trimIndent()

        for (contact in contacts) {
            try {
                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(contact.phoneNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null)
                }
            } catch (e: Exception) {
                Log.e("SmsSender", "Failed to send tracking update SMS to ${contact.name}", e)
            }
        }
    }
}
