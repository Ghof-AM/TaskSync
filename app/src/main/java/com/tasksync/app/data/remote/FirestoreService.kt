package com.tasksync.app.data.remote


import com.google.firebase.firestore.FirebaseFirestore
import com.tasksync.app.data.mapper.toFirestoreMap
import com.tasksync.app.domain.model.ActivityLog
import com.tasksync.app.domain.model.Comment
import com.tasksync.app.domain.model.Project
import com.tasksync.app.domain.model.Task
import com.tasksync.app.domain.model.User
import com.tasksync.app.util.Constants
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Tasks
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

    // Comments
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

    // Users
    suspend fun uploadUser(user: User) {
        firestore.collection(Constants.COLLECTION_USERS)
            .document(user.id)
            .set(user.toFirestoreMap())
            .await()
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

    // Activity Log
    suspend fun uploadLog(log: ActivityLog) {
        firestore.collection(Constants.COLLECTION_ACTIVITY_LOG)
            .document(log.id)
            .set(log.toFirestoreMap())
            .await()
    }
    // Projects
    suspend fun uploadProject(project: Project) {
        firestore.collection(Constants.COLLECTION_TEAMS)
            .document(project.id)
            .set(project.toFirestoreMap())
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
}