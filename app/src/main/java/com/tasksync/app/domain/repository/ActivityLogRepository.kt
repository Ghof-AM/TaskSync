package com.tasksync.app.domain.repository

import com.tasksync.app.domain.model.ActivityLog
import kotlinx.coroutines.flow.Flow

interface ActivityLogRepository {
    fun getLogsByProject(projectId: String): Flow<List<ActivityLog>>
    suspend fun addLog(log: ActivityLog)
    suspend fun syncAllPending()
}