package com.tasksync.app.data.remote

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import com.tasksync.app.domain.repository.UserRepository
import com.tasksync.app.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmSender @Inject constructor(
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context  // ← @ApplicationContext wajib ada
) {
    companion object {
        private const val TAG = "FcmSender"
    }

    suspend fun sendToUser(
        targetUserId: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending to user: $targetUserId")

            val targetUser = userRepository.getUserById(targetUserId)
            Log.d(TAG, "Target user found: ${targetUser?.name}, token: ${targetUser?.fcmToken}")

            if (targetUser == null) {
                Log.w(TAG, "User tidak ditemukan: $targetUserId")
                return@withContext
            }

            val fcmToken = targetUser.fcmToken
            if (fcmToken.isBlank()) {
                Log.w(TAG, "FCM token KOSONG untuk user: $targetUserId")
                return@withContext
            }

            Log.d(TAG, "FCM token valid, getting access token...")
            val accessToken = getAccessToken()
            if (accessToken == null) {
                Log.e(TAG, "Gagal mendapat access token dari service-account.json")
                return@withContext
            }

            Log.d(TAG, "Access token obtained, sending notification...")

            val fcmUrl = "https://fcm.googleapis.com/v1/projects/" +
                    "${Constants.FIREBASE_PROJECT_ID}/messages:send"

            val dataJson = JSONObject()
            data.forEach { (k, v) -> dataJson.put(k, v) }

            val payload = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("token", fcmToken)
                    put("notification", JSONObject().apply {
                        put("title", title)
                        put("body", body)
                    })
                    put("data", dataJson)
                })
            }

            val client = OkHttpClient()
            val request = Request.Builder()
                .url(fcmUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(
                    payload.toString()
                        .toRequestBody("application/json".toMediaType())
                )
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d(TAG, "Notifikasi terkirim ke $targetUserId")
            } else {
                Log.e(TAG, "Gagal: ${response.code} - ${response.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error kirim notifikasi: ${e.message}")
        }
    }

    private fun getAccessToken(): String? {
        return try {
            val stream = context.assets.open("service-account.json")
            val credentials = GoogleCredentials
                .fromStream(stream)
                .createScoped(
                    listOf("https://www.googleapis.com/auth/firebase.messaging")
                )
            credentials.refreshIfExpired()
            credentials.accessToken.tokenValue
        } catch (e: Exception) {
            Log.e(TAG, "Get access token failed: ${e.message}")
            null
        }
    }
}