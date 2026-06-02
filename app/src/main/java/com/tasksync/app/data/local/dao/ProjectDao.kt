package com.tasksync.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tasksync.app.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("""
        SELECT p.* FROM projects p
        INNER JOIN project_members pm ON p.id = pm.projectId
        WHERE pm.userId = :userId AND p.isDeleted = 0
        ORDER BY p.updatedAt DESC
    """)
    fun getProjectsByUser(userId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedProjects(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("UPDATE projects SET isSynced = 1 WHERE id = :projectId")
    suspend fun markAsSynced(projectId: String)

    @Query("UPDATE projects SET isDeleted = 1, isSynced = 0 WHERE id = :projectId")
    suspend fun softDelete(projectId: String)
}