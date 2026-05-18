package com.tasksync.app.domain.usecase.auth

import com.google.firebase.auth.FirebaseAuth
import com.tasksync.app.domain.model.User
import com.tasksync.app.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(name: String, email: String, password: String) {
        require(name.isNotBlank()) { "Nama tidak boleh kosong" }
        require(email.isNotBlank()) { "Email tidak boleh kosong" }
        require(password.length >= 6) { "Password minimal 6 karakter" }
        val result = firebaseAuth
            .createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: error("Registrasi gagal")
        val user = User(id = uid, name = name, email = email)
        userRepository.saveUser(user)
    }
}