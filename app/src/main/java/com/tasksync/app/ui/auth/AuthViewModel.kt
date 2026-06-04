package com.tasksync.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tasksync.app.domain.usecase.auth.LoginUseCase
import com.tasksync.app.domain.usecase.auth.LogoutUseCase
import com.tasksync.app.domain.usecase.auth.RegisterUseCase
import com.tasksync.app.util.SyncManager
import com.tasksync.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val firebaseAuth: FirebaseAuth,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val loginState: StateFlow<UiState<Unit>> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val registerState: StateFlow<UiState<Unit>> = _registerState.asStateFlow()

    val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = UiState.Loading
            try {
                loginUseCase(email, password)
                // Jadwalkan SyncWorker segera setelah login berhasil,
                // tidak perlu tunggu app di-restart
                val uid = firebaseAuth.currentUser?.uid
                if (uid != null) syncManager.schedule(uid)
                _loginState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _loginState.value = UiState.Error(
                    e.message ?: "Login gagal, periksa email dan password"
                )
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _registerState.value = UiState.Loading
            try {
                registerUseCase(name, email, password)
                // Sama seperti login — jadwalkan sync langsung setelah register
                val uid = firebaseAuth.currentUser?.uid
                if (uid != null) syncManager.schedule(uid)
                _registerState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _registerState.value = UiState.Error(
                    e.message ?: "Registrasi gagal, coba lagi"
                )
            }
        }
    }

    fun logout() {
        syncManager.cancelAll()
        logoutUseCase()
        _loginState.value = UiState.Idle
        _registerState.value = UiState.Idle
    }

    fun resetLoginState() {
        _loginState.value = UiState.Idle
    }

    fun resetRegisterState() {
        _registerState.value = UiState.Idle
    }
}