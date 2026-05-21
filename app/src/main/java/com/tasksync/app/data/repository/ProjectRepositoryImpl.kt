package com.tasksync.app.data.repository

import com.tasksync.app.data.local.dao.ProjectDao
import com.tasksync.app.data.mapper.toDomain
import com.tasksync.app.data.mapper.toEntity
import com.tasksync.app.data.remote.FirestoreService
import com.tasksync.app.domain.model.Project
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ProjectMemberRepository
import com.tasksync.app.domain.repository.ProjectRepository
import com.tasksync.app.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val firestoreService: FirestoreService,
    private val memberRepository: ProjectMemberRepository,
    private val networkMonitor: NetworkMonitor
) : ProjectRepository {

    override fun getProjectsByUser(userId: String): Flow<List<Project>> =
        projectDao.getProjectsByUser(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getProjectById(projectId: String): Project? =
        projectDao.getProjectById(projectId)?.toDomain()

    override suspend fun createProject(project: Project, creatorId: String) {
        // Simpan project lokal
        projectDao.insertProject(project.toEntity())
        // Tambah creator sebagai Owner
        memberRepository.addMember(project.id, creatorId, UserRole.OWNER)
        // Sync ke Firestore jika online
        if (networkMonitor.isOnline()) {
            try {
                firestoreService.uploadProject(project)
                projectDao.markAsSynced(project.id)
            } catch (e: Exception) { /* WorkManager retry */ }
        }
    }

    override suspend fun deleteProject(projectId: String) {
        projectDao.softDelete(projectId)
    }

    override suspend fun syncAllPending() {
        projectDao.getUnsyncedProjects().forEach { entity ->
            try {
                firestoreService.uploadProject(entity.toDomain())
                projectDao.markAsSynced(entity.id)
            } catch (e: Exception) { /* skip */ }
        }
    }
}