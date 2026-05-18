package com.tasksync.app.domain.usecase.team

import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ProjectMemberRepository
import javax.inject.Inject

class PromoteMemberUseCase @Inject constructor(
    private val memberRepository: ProjectMemberRepository
) {
    suspend operator fun invoke(projectId: String, userId: String) {
        memberRepository.updateRole(projectId, userId, UserRole.SECOND_OWNER)
    }
}