package com.tasksync.app.domain.usecase.team

import com.tasksync.app.data.remote.FcmSender
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ProjectMemberRepository
import com.tasksync.app.domain.repository.ProjectRepository
import com.tasksync.app.domain.repository.UserRepository
import javax.inject.Inject

class InviteMemberUseCase @Inject constructor(
    private val memberRepository: ProjectMemberRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val fcmSender: FcmSender
) {
    suspend operator fun invoke(projectId: String, email: String, inviterName: String = "Seseorang") {
        require(projectId.isNotBlank()) { "Project ID tidak boleh kosong" }
        require(email.isNotBlank()) { "Email tidak boleh kosong" }
        require(email.contains("@")) { "Format email tidak valid" }

        // Cari user berdasarkan email
        val user = userRepository.getUserByEmail(email)
            ?: throw Exception("User dengan email '$email' tidak ditemukan. Pastikan mereka sudah terdaftar di TaskSync.")

        // Cek duplikat — poin 4: logika lama salah, sekarang cukup cek isMember saja
        if (memberRepository.isMember(projectId, user.id)) {
            throw Exception("User ini sudah menjadi anggota project")
        }

        // Tambah member ke lokal + Firestore (sudah ada arrayUnion di addMember)
        memberRepository.addMember(projectId, user.id, UserRole.MEMBER)

        // Kirim FCM push notification ke user yang diundang
        // Dilakukan setelah addMember berhasil agar tidak kirim notif kalau gagal
        try {
            val project = projectRepository.getProjectById(projectId)
            val projectName = project?.name ?: "sebuah project"

            fcmSender.sendToUser(
                targetUserId = user.id,
                title = "Undangan Project Baru",
                body = "$inviterName mengundang kamu ke project \"$projectName\"",
                data = mapOf(
                    "type" to "PROJECT_INVITATION",
                    "projectId" to projectId
                )
            )
        } catch (e: Exception) {
            // Notifikasi gagal tidak boleh membatalkan undangan yang sudah berhasil
            android.util.Log.e("InviteMemberUseCase", "FCM gagal: ${e.message}")
        }
    }
}