package com.tasksync.app.domain.usecase.team

import com.tasksync.app.domain.model.ActivityLog
import com.tasksync.app.domain.model.LogEvent
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ActivityLogRepository
import com.tasksync.app.domain.repository.ProjectMemberRepository
import javax.inject.Inject

class DemoteMemberUseCase @Inject constructor(
    private val memberRepository: ProjectMemberRepository,
    private val activityLogRepository: ActivityLogRepository
) {
    suspend operator fun invoke(
        projectId: String,
        userId: String,        // Ini adalah target anggota yang di-demote
        actorId: String = "",  // ID Pengguna yang melakukan aksi demote
        actorName: String = "" // Nama Pengguna yang melakukan aksi demote
    ) {
        // 1. Kembalikan role ke member biasa
        memberRepository.updateRole(projectId, userId, UserRole.MEMBER)

        // 2. Buat objek log sesuai struktur data ActivityLog terbaru
        val log = ActivityLog(
            projectId = projectId,
            actorId = actorId,
            actorName = actorName,
            eventType = LogEvent.MEMBER_DEMOTED,
            message = "Jabatan anggota diturunkan menjadi Member.",
            targetId = userId,
            isSynced = false
        )

        activityLogRepository.addLog(log)
    }
}