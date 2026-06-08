package com.tasksync.app.data.repository

import com.tasksync.app.data.local.dao.ProjectMemberDao
import com.tasksync.app.data.local.dao.UserDao
import com.tasksync.app.data.local.entity.ProjectMemberEntity
import com.tasksync.app.data.mapper.toDomain
import com.tasksync.app.data.remote.FirestoreService
import com.tasksync.app.domain.model.ProjectMember
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ProjectMemberRepository
import com.tasksync.app.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class ProjectMemberRepositoryImpl @Inject constructor(
    private val memberDao: ProjectMemberDao,
    private val userDao: UserDao,
    private val firestoreService: FirestoreService,
    private val networkMonitor: NetworkMonitor

) : ProjectMemberRepository {

    private val listenerJobs = mutableMapOf<String, Job>()

    override fun getMembersByProject(projectId: String): Flow<List<ProjectMember>> =
        memberDao.getMembersByProject(projectId).map { list -> list.map { it.toDomain() } }

    override suspend fun getRole(projectId: String, userId: String): UserRole {
        val roleStr = memberDao.getRole(projectId, userId) ?: return UserRole.MEMBER
        return UserRole.fromValue(roleStr)
    }

    override suspend fun isMember(projectId: String, userId: String): Boolean =
        memberDao.getRole(projectId, userId) != null

    override suspend fun getMemberIdsForProject(projectId: String): List<String> =
        memberDao.getMemberIdsForProject(projectId)

    /**
     * Mulai listener real-time untuk member sebuah project.
     * Dipanggil dari TeamViewModel saat halaman team dibuka.
     * Satu project hanya punya satu listener aktif — listener lama dibatalkan dulu.
     */
    override fun startListening(projectId: String) {
        listenerJobs[projectId]?.cancel()
        listenerJobs[projectId] = CoroutineScope(Dispatchers.IO).launch {
            firestoreService.listenToMembersByProject(projectId)
                .collect { memberMaps ->
                    memberMaps.forEach { map ->
                        try {
                            val userId = map["userId"] as? String ?: return@forEach
                            val role = map["role"] as? String ?: UserRole.MEMBER.value
                            val existing = memberDao.getRole(projectId, userId)
                            if (existing != null && existing != role) {
                                // Role berubah — update lokal
                                memberDao.updateRole(projectId, userId, role)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MemberRepo", "Listener error: ${e.message}")
                        }
                    }

                    // Deteksi member yang dihapus dari Firestore tapi masih ada di Room
                    val remoteUserIds = memberMaps
                        .mapNotNull { it["userId"] as? String }
                        .toSet()
                    val localMembers = memberDao.getMemberIdsForProject(projectId)
                    localMembers.forEach { localUserId ->
                        if (localUserId !in remoteUserIds) {
                            memberDao.removeMember(projectId, localUserId)
                        }
                    }
                }
        }
    }

    override suspend fun addMember(projectId: String, userId: String, role: UserRole) {
        val userEntity = userDao.getUserById(userId)
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
        if (networkMonitor.isOnline()) {
            try {
                firestoreService.addProjectMemberRemote(projectId, userId)
                memberDao.markAsSynced(projectId, userId)
            } catch (e: Exception) {
                android.util.Log.e("MemberRepo", "addMember sync failed: ${e.message}")
            }
        }
    }

    /**
     * Update role lokal DAN langsung sync ke Firestore.
     * Ini yang membuat user lain bisa detect perubahan via listener.
     */
    override suspend fun updateRole(projectId: String, userId: String, role: UserRole) {
        memberDao.updateRole(projectId, userId, role.value)
        if (networkMonitor.isOnline()) {
            try {
                firestoreService.updateMemberRoleRemote(projectId, userId, role.value)
            } catch (e: Exception) {
                android.util.Log.e("MemberRepo", "updateRole remote failed: ${e.message}")
            }
        }
    }

    /**
     * Hapus member lokal DAN langsung sync ke Firestore.
     */
    override suspend fun removeMember(projectId: String, userId: String) {
        memberDao.removeMember(projectId, userId)
        if (networkMonitor.isOnline()) {
            try {
                firestoreService.removeMemberRemote(projectId, userId)
            } catch (e: Exception) {
                android.util.Log.e("MemberRepo", "removeMember remote failed: ${e.message}")
            }
        }
    }

    override suspend fun refreshMemberName(
        projectId: String, userId: String, name: String, email: String
    ) {
        memberDao.updateMemberName(projectId, userId, name, email)
    }

    override suspend fun syncAllPending() {
        memberDao.getUnsyncedMembers().forEach { memberEntity ->
            try {
                firestoreService.addProjectMemberRemote(
                    memberEntity.projectId, memberEntity.userId
                )
                memberDao.markAsSynced(memberEntity.projectId, memberEntity.userId)
            } catch (e: Exception) {
                android.util.Log.e("MemberRepo", "syncAllPending failed: ${e.message}")
            }
        }
    }

    override fun stopListening(projectId: String) {
        listenerJobs[projectId]?.cancel()
        listenerJobs.remove(projectId)
    }
}