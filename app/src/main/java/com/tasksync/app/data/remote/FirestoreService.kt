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
            awaitClose { listener.remove() }
        }

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

    /**
     * Listener real-time untuk member sebuah project.
     * Firestore menyimpan member di sub-collection "members" di dalam document project.
     * Setiap kali ada perubahan role atau member dihapus/ditambah,
     * Flow ini emit list terbaru ke collector (ProjectMemberRepositoryImpl).
     */
    fun listenToMembersByProject(projectId: String): Flow<List<Map<String, Any?>>> =
        callbackFlow {
            val listener = firestore
                .collection(Constants.COLLECTION_TEAMS)
                .document(projectId)
                .collection("members")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreService", "Member listener error: ${error.message}")
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
            .get().await()
            .documents.map { it.data ?: emptyMap() }
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

    suspend fun getCommentsByTask(taskId: String): List<Map<String, Any?>> {
        return firestore.collection(Constants.COLLECTION_COMMENTS)
            .whereEqualTo("taskId", taskId)
            .get().await()
            .documents.map { it.data ?: emptyMap() }
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
            .get().await()
            .documents.firstOrNull()?.data
    }

    suspend fun getUserById(userId: String): Map<String, Any?>? {
        return firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .get().await()
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
        if (log.id.isBlank()) return
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
            .get().await()
            .documents.map { it.data ?: emptyMap() }
    }

    suspend fun addProjectMemberRemote(projectId: String, userId: String) {
        firestore.collection(Constants.COLLECTION_TEAMS)
            .document(projectId)
            .update("memberIds", FieldValue.arrayUnion(userId))
            .await()
        // Simpan juga di sub-collection members agar listener bisa detect perubahan role
        firestore.collection(Constants.COLLECTION_TEAMS)
            .document(projectId)
            .collection("members")
            .document(userId)
            .set(mapOf("userId" to userId, "role" to "member"))
            .await()
    }

    /**
     * Update role member di sub-collection members Firestore.
     * Ini yang akan di-detect oleh listenToMembersByProject di user lain.
     */
    suspend fun updateMemberRoleRemote(projectId: String, userId: String, role: String) {
        firestore.collection(Constants.COLLECTION_TEAMS)
            .document(projectId)
            .collection("members")
            .document(userId)
            .update("role", role)
            .await()
    }

    /**
     * Hapus member dari sub-collection members dan arrayUnion memberIds.
     */
    suspend fun removeMemberRemote(projectId: String, userId: String) {
        firestore.collection(Constants.COLLECTION_TEAMS)
            .document(projectId)
            .collection("members")
            .document(userId)
            .delete()
            .await()

        firestore.collection(Constants.COLLECTION_TEAMS)
            .document(projectId)
            .update("memberIds", FieldValue.arrayRemove(userId))
            .await()
    }
}