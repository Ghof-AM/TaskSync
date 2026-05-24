package com.tasksync.app.data.repository

import com.tasksync.app.data.local.dao.ProjectMemberDao
import com.tasksync.app.data.local.dao.UserDao
import com.tasksync.app.data.local.entity.ProjectMemberEntity
import com.tasksync.app.data.mapper.toDomain
import com.tasksync.app.domain.model.ProjectMember
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ProjectMemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProjectMemberRepositoryImpl @Inject constructor(
    private val memberDao: ProjectMemberDao,
    private val userDao: UserDao  // inject UserDao langsung, bukan UserRepository
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

    override suspend fun addMember(projectId: String, userId: String, role: UserRole) {
        // Ambil nama dari UserDao (cache lokal)
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