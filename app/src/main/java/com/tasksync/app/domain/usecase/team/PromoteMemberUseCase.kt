package com.tasksync.app.domain.usecase.team

import com.tasksync.app.domain.model.ActivityLog
import com.tasksync.app.domain.model.LogEvent
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ActivityLogRepository
import com.tasksync.app.domain.repository.ProjectMemberRepository
import javax.inject.Inject

class PromoteMemberUseCase @Inject constructor(
    private val memberRepository: ProjectMemberRepository,
    private val activityLogRepository: ActivityLogRepository
) {
    suspend operator fun invoke(
        projectId: String,
        userId: String,        // Ini adalah target anggota yang di-promote
        actorId: String = "",  // ID Pengguna yang melakukan aksi promote
        actorName: String = "" // Nama Pengguna yang melakukan aksi promote
    ) {
        // 1. Perbarui role di database lokal/remote
        memberRepository.updateRole(projectId, userId, UserRole.SECOND_OWNER)

        // 2. Buat objek log sesuai struktur data ActivityLog terbaru
        val log = ActivityLog(
            projectId = projectId,
            actorId = actorId,
            actorName = actorName,
            eventType = LogEvent.MEMBER_PROMOTED,
            message = "Anggota dipromosikan menjadi Second Owner.",
            targetId = userId,
            isSynced = false
        )

        // 3. Simpan ke repositori log aktivitas
        activityLogRepository.addLog(log)
    }
}