package com.tasksync.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tasksync.app.data.local.entity.ProjectMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectMemberDao {

    @Query("SELECT role FROM project_members WHERE projectId = :projectId AND userId = :userId")
    suspend fun getRole(projectId: String, userId: String): String?

    @Query("SELECT * FROM project_members WHERE projectId = :projectId")
    fun getMembersByProject(projectId: String): Flow<List<ProjectMemberEntity>>

    @Query("SELECT * FROM project_members WHERE projectId = :projectId AND role = 'owner'")
    suspend fun getOwner(projectId: String): ProjectMemberEntity?

    @Query("SELECT * FROM project_members WHERE userId = :userId")
    fun getProjectsByUser(userId: String): Flow<List<ProjectMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMember(member: ProjectMemberEntity)

    @Query("UPDATE project_members SET role = :newRole, isSynced = 0 WHERE projectId = :projectId AND userId = :userId")
    suspend fun updateRole(projectId: String, userId: String, newRole: String)

    @Query("DELETE FROM project_members WHERE projectId = :projectId AND userId = :userId")
    suspend fun removeMember(projectId: String, userId: String)

    @Query("DELETE FROM project_members WHERE projectId = :projectId")
    suspend fun removeAllMembers(projectId: String)
}