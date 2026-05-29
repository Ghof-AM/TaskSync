package com.tasksync.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface FcmApi {
    @POST("v1/projects/{projectId}/messages:send")
    suspend fun sendNotification(
        @Header("Authorization") token: String,
        @Path("projectId") projectId: String,
        @Body body: FcmMessage
    ): Response<Unit>
}

data class FcmMessage(val message: FcmPayload)

data class FcmPayload(
    val token: String,
    val notification: FcmNotification,
    val data: Map<String, String> = emptyMap()
)

data class FcmNotification(
    val title: String,
    val body: String
)