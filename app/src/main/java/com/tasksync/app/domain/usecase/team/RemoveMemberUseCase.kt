package com.tasksync.app.domain.usecase.team

import com.tasksync.app.domain.model.ActivityLog
import com.tasksync.app.domain.model.LogEvent
import com.tasksync.app.domain.repository.ActivityLogRepository
import com.tasksync.app.domain.repository.ProjectMemberRepository
import javax.inject.Inject

class RemoveMemberUseCase @Inject constructor(
    private val memberRepository: ProjectMemberRepository,
    private val activityLogRepository: ActivityLogRepository
) {
    suspend operator fun invoke(
        projectId: String,
        userId: String,        // Ini adalah target anggota yang dihapus/keluar
        actorId: String = "",  // ID Pengguna yang melakukan aksi hapus anggota
        actorName: String = "" // Nama Pengguna yang melakukan aksi hapus anggota
    ) {
        // 1. Hapus member dari database
        memberRepository.removeMember(projectId, userId)

        // 2. Buat objek log sesuai struktur data ActivityLog terbaru dengan LogEvent.MEMBER_LEFT
        val log = ActivityLog(
            projectId = projectId,
            actorId = actorId,
            actorName = actorName,
            eventType = LogEvent.MEMBER_LEFT,
            message = "Anggota telah dihapus atau keluar dari project.",
            targetId = userId,
            isSynced = false
        )

        activityLogRepository.addLog(log)
    }
}