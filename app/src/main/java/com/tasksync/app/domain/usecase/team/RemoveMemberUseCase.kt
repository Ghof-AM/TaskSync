package com.tasksync.app.domain.usecase.team

import com.tasksync.app.domain.repository.ProjectMemberRepository
import javax.inject.Inject

class RemoveMemberUseCase @Inject constructor(
    private val memberRepository: ProjectMemberRepository
) {
    suspend operator fun invoke(projectId: String, userId: String) {
        memberRepository.removeMember(projectId, userId)
    }
}