package com.tasksync.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tasksync.app.domain.repository.ActivityLogRepository
import com.tasksync.app.domain.repository.CommentRepository
import com.tasksync.app.domain.repository.ProjectMemberRepository
import com.tasksync.app.domain.repository.ProjectRepository
import com.tasksync.app.domain.usecase.task.SyncTasksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val syncTasksUseCase: SyncTasksUseCase,
    private val commentRepository: CommentRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val projectRepository: ProjectRepository,
    private val memberRepository: ProjectMemberRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val currentUserId = inputData.getString(KEY_USER_ID)
            ?: return Result.failure()

        return try {
            // 1. PULL: Ambil project baru dari Firestore (undangan, dll)
            projectRepository.fetchRemoteProjectsAndNotify(currentUserId)

            // 2. PUSH: Upload semua data offline yang belum tersync
            projectRepository.syncAllPending()
            memberRepository.syncAllPending()
            syncTasksUseCase()
            commentRepository.syncAllPending()
            activityLogRepository.syncAllPending()

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "tasksync_sync_worker"
        const val KEY_USER_ID = "USER_ID"
    }
}