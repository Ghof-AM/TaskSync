package com.tasksync.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid

    fun isLoggedIn(): Boolean = firebaseAuth.currentUser != null

    fun signOut() = firebaseAuth.signOut()
}