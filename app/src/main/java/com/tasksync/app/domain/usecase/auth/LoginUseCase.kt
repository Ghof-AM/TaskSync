package com.tasksync.app.domain.usecase.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    suspend operator fun invoke(email: String, password: String) {
        require(email.isNotBlank()) { "Email tidak boleh kosong" }
        require(password.isNotBlank()) { "Password tidak boleh kosong" }
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }
}