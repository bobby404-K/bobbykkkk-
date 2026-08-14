package com.safeword.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incident_logs")
data class IncidentLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val triggerWord: String,
    val latitude: Double,
    val longitude: Double,
    val status: String = "SENT"
)
