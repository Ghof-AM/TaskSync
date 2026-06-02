package com.tasksync.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tasksync.app.domain.repository.ActivityLogRepository
import com.tasksync.app.domain.repository.CommentRepository
import com.tasksync.app.domain.repository.ProjectMemberRepository // Tambahan
import com.tasksync.app.domain.repository.ProjectRepository // Tambahan
import com.tasksync.app.domain.usecase.task.SyncTasksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncTasksUseCase: SyncTasksUseCase,
    private val commentRepository: CommentRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val projectRepository: ProjectRepository,       // Tambahan Isu #1
    private val memberRepository: ProjectMemberRepository // Tambahan Isu #3
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Jalankan sync Project & Members terlebih dahulu agar relasi data di Firestore aman
            projectRepository.syncAllPending()   // Mengatasi Isu #1
            memberRepository.syncAllPending()    // Mengatasi Isu #3

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