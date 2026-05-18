package com.tasksync.app.domain.usecase.team

import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ProjectMemberRepository
import javax.inject.Inject

class InviteMemberUseCase @Inject constructor(
    private val memberRepository: ProjectMemberRepository
) {
    suspend operator fun invoke(projectId: String, userId: String) {
        require(projectId.isNotBlank()) { "Project ID tidak boleh kosong" }
        require(userId.isNotBlank()) { "User ID tidak boleh kosong" }
        memberRepository.addMember(projectId, userId, UserRole.MEMBER)
    }
}