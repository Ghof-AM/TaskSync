package com.tasksync.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.tasksync.app.data.mapper.toFirestoreMap
import com.tasksync.app.domain.model.ActivityLog
import com.tasksync.app.domain.model.Comment
import com.tasksync.app.domain.model.Project
import com.tasksync.app.domain.model.Task
import com.tasksync.app.domain.model.User
import com.tasksync.app.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // ─── REAL-TIME LISTENERS ──────────────────────────────────────────────────

    /**
     * Mendengarkan perubahan task secara real-time untuk satu project.
     * Setiap kali ada task yang dibuat/diubah/dihapus di Firestore oleh siapapun,
     * Flow ini akan emit list terbaru ke collector-nya (TaskRepositoryImpl).
     */
    fun listenToTasksByProject(projectId: String): Flow<List<Map<String, Any?>>> =
        callbackFlow {
            val listener = firestore.collection(Constants.COLLECTION_TASKS)
                .whereEqualTo("projectId", projectId)
                .whereEqualTo("isDeleted", false)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreService", "Task listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    val result = snapshot?.documents?.map { it.data ?: emptyMap() } ?: emptyList()
                    trySend(result)
                }
            // Listener dibersihkan otomatis saat Flow di-cancel (misal: ViewModel cleared)
            awaitClose { listener.remove() }
        }

    /**
     * Mendengarkan perubahan project secara real-time untuk satu user.
     * Akan trigger setiap kali user diundang ke project baru, atau project diubah.
     */
    fun listenToProjectsByUser(userId: String): Flow<List<Map<String, Any?>>> =
        callbackFlow {
            val listener = firestore.collection(Constants.COLLECTION_TEAMS)
                .whereArrayContains("memberIds", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreService", "Project listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    val result = snapshot?.documents?.map { it.data ?: emptyMap() } ?: emptyList()
                    trySend(result)
                }
            awaitClose { listener.remove() }
        }

    // ─── TASKS ────────────────────────────────────────────────────────────────

    suspend fun uploadTask(task: Task) {
        firestore.collection(Constants.COLLECTION_TASKS)
            .document(task.id)
            .set(task.toFirestoreMap())
            .await()
    }

    suspend fun deleteTaskRemote(taskId: String) {
        firestore.collection(Constants.COLLECTION_TASKS)
            .document(taskId)
            .update("isDeleted", true)
            .await()
    }

    suspend fun getTasksByProject(projectId: String): List<Map<String, Any?>> {
        return firestore.collection(Constants.COLLECTION_TASKS)
            .whereEqualTo("projectId", projectId)
            .whereEqualTo("isDeleted", false)
            .get()
            .await()
            .documents
            .map { it.data ?: emptyMap() }
    }

    // ─── COMMENTS ─────────────────────────────────────────────────────────────

    suspend fun uploadComment(comment: Comment) {
        firestore.collection(Constants.COLLECTION_COMMENTS)
            .document(comment.id)
            .set(comment.toFirestoreMap())
            .await()
    }

    suspend fun deleteCommentRemote(commentId: String) {
        firestore.collection(Constants.COLLECTION_COMMENTS)
            .document(commentId)
            .delete()
            .await()
    }

    // ─── USERS ────────────────────────────────────────────────────────────────

    suspend fun uploadUser(user: User) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(user.id)
            .set(user.toFirestoreMap())
            .await()
    }

    suspend fun getUserByEmail(email: String): Map<String, Any?>? {
        return firestore.collection(Constants.COLLECTION_USERS)
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.data
    }

    suspend fun getUserById(userId: String): Map<String, Any?>? {
        return firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .get()
            .await()
            .data
    }

    suspend fun updateFcmToken(userId: String, token: String) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .update("fcmToken", token)
            .await()
    }

    // ─── ACTIVITY LOG ─────────────────────────────────────────────────────────

    suspend fun uploadLog(log: ActivityLog) {
        android.util.Log.d("FirestoreService", "Uploading log id: '${log.id}', msg: ${log.message}")
        if (log.id.isBlank()) {
            android.util.Log.e("FirestoreService", "Log ID is blank!")
            return
        }
        firestore.collection(Constants.COLLECTION_ACTIVITY_LOG)
            .document(log.id)
            .set(log.toFirestoreMap())
            .await()
    }

    // ─── PROJECTS ─────────────────────────────────────────────────────────────

    suspend fun uploadProject(project: Project, memberIds: List<String>) {
        val projectMap = project.toFirestoreMap().toMutableMap().apply {
            put("memberIds", memberIds)
        }
        firestore.collection(Constants.COLLECTION_TEAMS)
            .document(project.id)
            .set(projectMap)
            .await()
    }

    suspend fun getProjectsByUser(userId: String): List<Map<String, Any?>> {
        return firestore.collection(Constants.COLLECTION_TEAMS)
            .whereArrayContains("memberIds", userId)
            .get()
            .await()
            .documents
            .map { it.data ?: emptyMap() }
    }

    suspend fun addProjectMemberRemote(projectId: String, userId: String) {
        firestore.collection(Constants.COLLECTION_TEAMS)
            .document(projectId)
            .update("memberIds", FieldValue.arrayUnion(userId))
            .await()
    }

    suspend fun getCommentsByTask(taskId: String): List<Map<String, Any?>> {
        return firestore.collection(Constants.COLLECTION_COMMENTS)
            .whereEqualTo("taskId", taskId)
            .get()
            .await()
            .documents
            .map { it.data ?: emptyMap() }
    }
}