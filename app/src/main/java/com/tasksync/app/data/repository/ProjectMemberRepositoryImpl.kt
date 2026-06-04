package com.tasksync.app.data.repository

import com.tasksync.app.data.local.dao.ProjectMemberDao
import com.tasksync.app.data.local.dao.UserDao
import com.tasksync.app.data.local.entity.ProjectMemberEntity
import com.tasksync.app.data.mapper.toDomain
import com.tasksync.app.data.remote.FirestoreService // Tambahan
import com.tasksync.app.domain.model.ProjectMember
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ProjectMemberRepository
import com.tasksync.app.util.NetworkMonitor // Tambahan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProjectMemberRepositoryImpl @Inject constructor(
    private val memberDao: ProjectMemberDao,
    private val userDao: UserDao,
    private val firestoreService: FirestoreService, // Tambahan Isu #3
    private val networkMonitor: NetworkMonitor     // Tambahan untuk instant sync
) : ProjectMemberRepository {

    override fun getMembersByProject(projectId: String): Flow<List<ProjectMember>> =
        memberDao.getMembersByProject(projectId).map { list -> list.map { it.toDomain() } }

    override suspend fun getRole(projectId: String, userId: String): UserRole {
        val roleStr = memberDao.getRole(projectId, userId) ?: return UserRole.MEMBER
        return UserRole.fromValue(roleStr)
    }

    override suspend fun isMember(projectId: String, userId: String): Boolean {
        return memberDao.getRole(projectId, userId) != null
    }

    // Tambahan Isu #2: Diperlukan ProjectRepositoryImpl saat sync pendings
    override suspend fun getMemberIdsForProject(projectId: String): List<String> {
        return memberDao.getMemberIdsForProject(projectId)
    }

    override suspend fun addMember(projectId: String, userId: String, role: UserRole) {
        val userEntity = userDao.getUserById(userId)

        // 1. Tetap simpan lokal terlebih dahulu
        memberDao.addMember(
            ProjectMemberEntity(
                projectId = projectId,
                userId = userId,
                role = role.value,
                userName = userEntity?.name ?: "",
                userEmail = userEntity?.email ?: "",
                joinedAt = System.currentTimeMillis(),
                isSynced = false
            )
        )

        // 2. Jika online, langsung daftarkan ke Firestore
        if (networkMonitor.isOnline()) {
            try {
                firestoreService.addProjectMemberRemote(projectId, userId)
                memberDao.markAsSynced(projectId, userId)
            } catch (e: Exception) {
                android.util.Log.e("MemberRepo", "Direct member sync failed: ${e.message}")
            }
        }
    }

    // Tambahan Isu #3: Dipanggil oleh SyncWorker secara berkala
    override suspend fun syncAllPending() {
        memberDao.getUnsyncedMembers().forEach { memberEntity ->
            try {
                firestoreService.addProjectMemberRemote(memberEntity.projectId, memberEntity.userId)
                memberDao.markAsSynced(memberEntity.projectId, memberEntity.userId)
            } catch (e: Exception) {
                android.util.Log.e("MemberRepo", "Background sync failed for user ${memberEntity.userId}: ${e.message}")
            }
        }
    }

    override suspend fun refreshMemberName(
        projectId: String,
        userId: String,
        name: String,
        email: String
    ) {
        memberDao.updateMemberName(projectId, userId, name, email)
    }

    override suspend fun updateRole(projectId: String, userId: String, role: UserRole) {
        memberDao.updateRole(projectId, userId, role.value)
    }

    override suspend fun removeMember(projectId: String, userId: String) {
        memberDao.removeMember(projectId, userId)
    }
}