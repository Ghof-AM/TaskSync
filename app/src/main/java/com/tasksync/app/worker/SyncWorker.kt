package com.tasksync.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tasksync.app.domain.repository.ActivityLogRepository
import com.tasksync.app.domain.repository.CommentRepository
import com.tasksync.app.domain.usecase.task.SyncTasksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncTasksUseCase: SyncTasksUseCase,
    private val commentRepository: CommentRepository,
    private val activityLogRepository: ActivityLogRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            syncTasksUseCase()
            commentRepository.syncAllPending()
            activityLogRepository.syncAllPending()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry()
            else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "tasksync_sync_worker"
    }
}