package com.tasksync.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tasksync.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND projectId = :projectId ORDER BY updatedAt DESC")
    fun getAllTasks(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE assignedTo = :userId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getMyTasks(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId AND isDeleted = 0")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isDeleted = 1 AND isSynced = 0")
    suspend fun getDeletedUnsyncedTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isSynced = 1 WHERE id = :taskId")
    suspend fun markAsSynced(taskId: String)

    @Query("UPDATE tasks SET isDeleted = 1, isSynced = 0, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun softDelete(taskId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM tasks WHERE projectId = :projectId")
    suspend fun deleteAllByProject(projectId: String)
}