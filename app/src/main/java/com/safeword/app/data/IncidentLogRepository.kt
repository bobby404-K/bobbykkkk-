package com.safeword.app.data

import kotlinx.coroutines.flow.Flow

class IncidentLogRepository(private val incidentLogDao: IncidentLogDao) {
    val allLogs: Flow<List<IncidentLog>> = incidentLogDao.getAllLogs()

    suspend fun insert(log: IncidentLog) {
        incidentLogDao.insertLog(log)
    }

    suspend fun clearAll() {
        incidentLogDao.clearLogs()
    }
}
