package com.tasksync.app.domain.usecase.team

import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ProjectMemberRepository
import com.tasksync.app.domain.repository.UserRepository
import javax.inject.Inject

class InviteMemberUseCase @Inject constructor(
    private val memberRepository: ProjectMemberRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(projectId: String, email: String) {
        require(projectId.isNotBlank()) { "Project ID tidak boleh kosong" }
        require(email.isNotBlank()) { "Email tidak boleh kosong" }
        require(email.contains("@")) { "Format email tidak valid" }

        // Cari user berdasarkan email
        val user = userRepository.getUserByEmail(email)
            ?: throw Exception("User dengan email '$email' tidak ditemukan. Pastikan mereka sudah terdaftar di TaskSync.")

        // Cek apakah sudah jadi member
        val existingRole = memberRepository.getRole(projectId, user.id)
        if (existingRole != com.tasksync.app.domain.model.UserRole.MEMBER ||
            memberRepository.isMember(projectId, user.id)) {
            throw Exception("User ini sudah menjadi anggota project")
        }

        memberRepository.addMember(projectId, user.id, UserRole.MEMBER)
    }
}