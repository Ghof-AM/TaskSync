package com.tasksync.app.domain.usecase.team

import com.tasksync.app.domain.model.ActivityLog
import com.tasksync.app.domain.model.LogEvent
import com.tasksync.app.domain.model.User
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ActivityLogRepository
import com.tasksync.app.domain.repository.ProjectMemberRepository
import javax.inject.Inject

class TransferOwnershipUseCase @Inject constructor(
    private val memberRepository: ProjectMemberRepository,
    private val logRepository: ActivityLogRepository
) {
    suspend operator fun invoke(
        projectId: String,
        currentOwner: User,
        newOwner: User
    ) {
        memberRepository.updateRole(projectId, currentOwner.id, UserRole.MEMBER)
        memberRepository.updateRole(projectId, newOwner.id, UserRole.OWNER)
        logRepository.addLog(
            ActivityLog(
                projectId = projectId,
                actorId = currentOwner.id,
                actorName = currentOwner.name,
                eventType = LogEvent.OWNER_TRANSFERRED,
                message = "${currentOwner.name} mentransfer ownership ke ${newOwner.name}",
                targetId = newOwner.id
            )
        )
    }
}