package com.tasksync.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tasksync.app.data.local.entity.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {

    @Query("SELECT * FROM activity_log WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getLogsByProject(projectId: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_log WHERE isSynced = 0")
    suspend fun getUnsyncedLogs(): List<ActivityLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)  // ← REPLACE bukan IGNORE
    suspend fun insert(log: ActivityLogEntity)

    @Query("UPDATE activity_log SET isSynced = 1 WHERE id = :logId")
    suspend fun markAsSynced(logId: String)
}
