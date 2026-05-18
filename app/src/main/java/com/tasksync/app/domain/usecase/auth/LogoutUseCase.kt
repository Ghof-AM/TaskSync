package com.tasksync.app.domain.usecase.auth

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    operator fun invoke() {
        firebaseAuth.signOut()
    }
}