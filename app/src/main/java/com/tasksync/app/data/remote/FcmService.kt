package com.tasksync.app.data.remote

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tasksync.app.domain.repository.UserRepository
import com.tasksync.app.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onNewToken(token: String) {
        Log.d("FCM", "New token: $token")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                userRepository.updateFcmToken(token)
                Log.d("FCM", "Token updated successfully")
            } catch (e: Exception) {
                Log.e("FCM", "Update token failed: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("FCM", "Message received from: ${message.from}")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "TaskSync"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "Ada notifikasi baru"
        val taskId = message.data["taskId"] ?: ""
        val projectId = message.data["projectId"] ?: ""

        notificationHelper.showNotification(
            title = title,
            body = body,
            taskId = taskId,
            projectId = projectId
        )
    }
}