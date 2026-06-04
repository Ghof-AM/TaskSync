package com.tasksync.app.util

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tasksync.app.worker.SyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val workManager: WorkManager   // ← inject, bukan getInstance()
) {
    fun schedule(userId: String) {
        if (userId.isBlank()) return

        val inputData = Data.Builder()
            .putString(SyncWorker.KEY_USER_ID, userId)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            Constants.SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setInputData(inputData)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1,
                TimeUnit.MINUTES
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
    }
}