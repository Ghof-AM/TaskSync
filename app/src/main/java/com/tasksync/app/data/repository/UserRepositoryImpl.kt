package com.tasksync.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.tasksync.app.data.local.dao.UserDao
import com.tasksync.app.data.mapper.toDomain
import com.tasksync.app.data.mapper.toEntity
import com.tasksync.app.data.mapper.toFirestoreMap
import com.tasksync.app.data.mapper.toUser
import com.tasksync.app.data.remote.FirestoreService
import com.tasksync.app.domain.model.User
import com.tasksync.app.domain.repository.UserRepository
import com.tasksync.app.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val firestoreService: FirestoreService,
    private val firebaseAuth: FirebaseAuth,
    private val networkMonitor: NetworkMonitor
) : UserRepository {

    override suspend fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        val uid = firebaseUser.uid

        // Cek lokal dulu
        val local = userDao.getUserById(uid)
        if (local != null && local.name.isNotBlank()) return local.toDomain()

        // Ambil dari Firestore
        if (networkMonitor.isOnline()) {
            try {
                val remote = firestoreService.getUserById(uid)
                if (remote != null) {
                    val user = remote.toUser()
                    if (user.name.isNotBlank()) {
                        userDao.insertUser(user.toEntity())
                        return user
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("UserRepo", "Firestore fetch failed: ${e.message}")
            }
        }

        // Fallback dari Firebase Auth
        val name = firebaseUser.displayName?.takeIf { it.isNotBlank() }
            ?: firebaseUser.email?.substringBefore("@")
            ?: "User"

        val user = User(
            id = uid,
            name = name,
            email = firebaseUser.email ?: ""
        )

        // Simpan ke Room agar tidak perlu fetch ulang
        userDao.insertUser(user.toEntity())

        // Sync ke Firestore
        if (networkMonitor.isOnline()) {
            try {
                firestoreService.uploadUser(user)
            } catch (e: Exception) { /* skip */ }
        }

        return user
    }

    override suspend fun getUserByEmail(email: String): User? {
        // Cari di Firestore berdasarkan email
        if (networkMonitor.isOnline()) {
            try {
                val result = firestoreService.getUserByEmail(email)
                if (result != null) {
                    val user = result.toUser()
                    userDao.insertUser(user.toEntity())
                    return user
                }
            } catch (e: Exception) {
                android.util.Log.e("UserRepo", "getUserByEmail error: ${e.message}")
            }
        }
        return null
    }

    override suspend fun getUserById(userId: String): User? {
        val local = userDao.getUserById(userId)
        if (local != null) return local.toDomain()
        if (networkMonitor.isOnline()) {
            val remote = firestoreService.getUserById(userId)
            if (remote != null) {
                val user = remote.toUser()
                userDao.insertUser(user.toEntity())
                return user
            }
        }
        return null
    }

    override fun getUsersByIds(userIds: List<String>): Flow<List<User>> =
        userDao.getUsersByIds(userIds).map { list -> list.map { it.toDomain() } }

    override suspend fun saveUser(user: User) {
        userDao.insertUser(user.toEntity())
        if (networkMonitor.isOnline()) {
            try {
                firestoreService.uploadUser(user)
            } catch (e: Exception) { /* retry nanti */ }
        }
    }

    override suspend fun updateFcmToken(token: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        userDao.updateFcmToken(uid, token)
        if (networkMonitor.isOnline()) {
            try {
                firestoreService.updateFcmToken(uid, token)
            } catch (e: Exception) { /* retry nanti */ }
        }
    }

    override suspend fun updateProfile(name: String, photoUrl: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val current = userDao.getUserById(uid) ?: return
        val updated = current.copy(name = name, photoUrl = photoUrl)
        userDao.insertUser(updated)
        if (networkMonitor.isOnline()) {
            try {
                firestoreService.uploadUser(updated.toDomain())
            } catch (e: Exception) { /* retry nanti */ }
        }
    }
}