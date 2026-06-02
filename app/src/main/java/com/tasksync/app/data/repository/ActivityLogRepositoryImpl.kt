package com.tasksync.app.data.repository

import com.tasksync.app.data.local.dao.ActivityLogDao
import com.tasksync.app.data.mapper.toDomain
import com.tasksync.app.data.mapper.toEntity
import com.tasksync.app.data.mapper.toFirestoreMap
import com.tasksync.app.data.remote.FirestoreService
import com.tasksync.app.domain.model.ActivityLog
import com.tasksync.app.domain.repository.ActivityLogRepository
import com.tasksync.app.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ActivityLogRepositoryImpl @Inject constructor(
    private val logDao: ActivityLogDao,
    private val firestoreService: FirestoreService,
    private val networkMonitor: NetworkMonitor
) : ActivityLogRepository {

    override fun getLogsByProject(projectId: String): Flow<List<ActivityLog>> =
        logDao.getLogsByProject(projectId).map { list -> list.map { it.toDomain() } }

    override suspend fun addLog(log: ActivityLog) {
        android.util.Log.d("ActivityLogRepo", "Adding log: ${log.message}")
        logDao.insert(log.toEntity())
        android.util.Log.d("ActivityLogRepo", "Log inserted successfully")
        if (networkMonitor.isOnline()) {
            try {
                firestoreService.uploadLog(log)
                logDao.markAsSynced(log.id)
                android.util.Log.d("ActivityLogRepo", "Log synced to Firestore")
            } catch (e: Exception) {
                android.util.Log.e("ActivityLogRepo", "Sync failed: ${e.message}")
            }
        }
    }

    override suspend fun syncAllPending() {
        logDao.getUnsyncedLogs().forEach { entity ->
            try {
                firestoreService.uploadLog(entity.toDomain())
                logDao.markAsSynced(entity.id)
            } catch (e: Exception) { /* skip */ }
        }
    }
}