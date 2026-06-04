package com.tasksync.app.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tasksync.app.data.local.dao.ProjectDao
import com.tasksync.app.data.local.dao.ProjectMemberDao
import com.tasksync.app.data.local.entity.ProjectEntity
import com.tasksync.app.data.local.entity.ProjectMemberEntity
import com.tasksync.app.data.mapper.toDomain
import com.tasksync.app.data.mapper.toEntity
import com.tasksync.app.data.remote.FirestoreService
import com.tasksync.app.domain.model.Project
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ProjectMemberRepository
import com.tasksync.app.domain.repository.ProjectRepository
import com.tasksync.app.util.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val memberDao: ProjectMemberDao,
    private val firestoreService: FirestoreService,
    private val memberRepository: ProjectMemberRepository,
    private val networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context
) : ProjectRepository {

    // Job listener aktif. Dibatalkan otomatis jika startListening dipanggil ulang
    // (misal: user logout lalu login akun lain).
    private var listenerJob: Job? = null

    override fun getProjectsByUser(userId: String): Flow<List<Project>> =
        projectDao.getProjectsByUser(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getProjectById(projectId: String): Project? =
        projectDao.getProjectById(projectId)?.toDomain()

    /**
     * Mulai mendengarkan Firestore secara real-time untuk userId tertentu.
     *
     * Cara kerja:
     * 1. Firestore mengirim snapshot setiap ada perubahan pada project yang
     *    mengandung userId di field "memberIds".
     * 2. Setiap project dari snapshot di-upsert ke Room (insertProject pakai REPLACE).
     * 3. Member baru yang ada di Firestore tapi belum ada di Room lokal
     *    juga di-insert ke tabel project_members.
     * 4. Karena ProjectDao.getProjectsByUser() adalah Flow dari Room, UI langsung
     *    ter-update otomatis tanpa perlu kode tambahan di ViewModel.
     * 5. Jika project adalah undangan baru (belum ada di Room sebelumnya),
     *    notifikasi lokal ditampilkan.
     *
     * Listener lama dibatalkan dulu jika ada.
     */
    override fun startListening(userId: String) {
        listenerJob?.cancel()
        listenerJob = CoroutineScope(Dispatchers.IO).launch {
            firestoreService.listenToProjectsByUser(userId)
                .collect { projectMaps ->
                    projectMaps.forEach { map ->
                        try {
                            val projectId = map["id"] as? String
                            val projectName = map["name"] as? String ?: ""
                            val projectDesc = map["description"] as? String ?: ""
                            val createdBy = map["createdBy"] as? String ?: ""
                            val createdAt = (map["createdAt"] as? Long) ?: 0L
                            val memberIds = (map["memberIds"] as? List<*>)
                                ?.filterIsInstance<String>() ?: emptyList()

                            if (projectId.isNullOrBlank()) return@forEach

                            // Cek apakah project sudah ada di Room (untuk deteksi undangan baru)
                            val isNewProject = projectDao.getProjectById(projectId) == null

                            // Upsert project ke Room
                            projectDao.insertProject(
                                ProjectEntity(
                                    id = projectId,
                                    name = projectName,
                                    description = projectDesc,
                                    createdBy = createdBy,
                                    isSynced = true,
                                    createdAt = createdAt,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )

                            // Upsert semua member ke Room agar query project_members
                            // yang dipakai di ProjectDao.getProjectsByUser() tetap konsisten
                            memberIds.forEach { memberId ->
                                val alreadyExists = memberDao.getRole(projectId, memberId) != null
                                if (!alreadyExists) {
                                    memberDao.insertMemberLocally(
                                        ProjectMemberEntity(
                                            projectId = projectId,
                                            userId = memberId,
                                            role = if (memberId == createdBy)
                                                UserRole.OWNER.value else UserRole.MEMBER.value,
                                            isSynced = true
                                        )
                                    )
                                }
                            }

                            // Tampilkan notifikasi hanya jika ini adalah undangan baru
                            if (isNewProject && memberIds.contains(userId)) {
                                showLocalNotification(projectName)
                            }

                        } catch (e: Exception) {
                            android.util.Log.e(
                                "ProjectRepo",
                                "Gagal upsert project dari listener: ${e.message}"
                            )
                        }
                    }
                }
        }
    }

    override suspend fun createProject(project: Project, creatorId: String) {
        projectDao.insertProject(project.toEntity())

        memberRepository.addMember(
            projectId = project.id,
            userId = creatorId,
            role = UserRole.OWNER
        )

        val savedRole = memberRepository.getRole(project.id, creatorId)
        android.util.Log.d("ProjectRepo", "Creator role saved: $savedRole for project: ${project.id}")

        if (networkMonitor.isOnline()) {
            try {
                firestoreService.uploadProject(project, listOf(creatorId))
                projectDao.markAsSynced(project.id)
            } catch (e: Exception) {
                android.util.Log.e("ProjectRepo", "Sync failed: ${e.message}")
            }
        }
    }

    override suspend fun deleteProject(projectId: String) {
        projectDao.softDelete(projectId)
    }

    override suspend fun fetchRemoteProjectsAndNotify(userId: String) {
        if (!networkMonitor.isOnline()) return
        try {
            val remoteProjects = firestoreService.getProjectsByUser(userId)
            remoteProjects.forEach { map ->
                val projectId = map["id"] as? String ?: return@forEach
                val projectName = map["name"] as? String ?: "Projek Baru"
                val projectDesc = map["description"] as? String ?: ""

                val localProject = projectDao.getProjectById(projectId)
                if (localProject == null) {
                    projectDao.insertProject(
                        ProjectEntity(
                            id = projectId,
                            name = projectName,
                            description = projectDesc,
                            isSynced = true
                        )
                    )

                    memberDao.insertMemberLocally(
                        ProjectMemberEntity(
                            projectId = projectId,
                            userId = userId,
                            role = UserRole.MEMBER.value,
                            isSynced = true
                        )
                    )

                    showLocalNotification(projectName)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepo", "Error pulling projects: ${e.message}")
        }
    }

    private fun showLocalNotification(projectName: String) {
        val channelId = "tasksync_invitation_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Undangan Projek",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Undangan Projek Baru!")
            .setContentText("Anda telah ditambahkan ke projek: $projectName")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(projectName.hashCode(), notification)
    }

    override suspend fun syncAllPending() {
        projectDao.getUnsyncedProjects().forEach { entity ->
            try {
                val memberIds = memberDao.getMemberIdsForProject(entity.id)
                firestoreService.uploadProject(
                    Project(entity.id, entity.name, entity.description),
                    memberIds
                )
                projectDao.markAsSynced(entity.id)
            } catch (e: Exception) { /* skip */ }
        }
    }
}
