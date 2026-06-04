package com.tasksync.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tasksync.app.domain.repository.ActivityLogRepository
import com.tasksync.app.domain.repository.CommentRepository
import com.tasksync.app.domain.repository.ProjectMemberRepository
import com.tasksync.app.domain.repository.ProjectRepository
import com.tasksync.app.domain.usecase.task.SyncTasksUseCase
import androidx.hilt.work.HiltWorker
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
        val currentUserId = inputData.getString("USER_ID") ?: return Result.failure()
        return try {
            // PERBAIKAN: Menghapus argumen 'context' karena interface hanya membutuhkan 'currentUserId'
            projectRepository.fetchRemoteProjectsAndNotify(currentUserId)

            // 2. PUSH DATA: Jalankan antrean upload offline seperti biasa
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
    }
}