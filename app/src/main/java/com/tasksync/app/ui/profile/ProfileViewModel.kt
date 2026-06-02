package com.tasksync.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tasksync.app.domain.model.User
import com.tasksync.app.domain.repository.UserRepository
import com.tasksync.app.domain.usecase.auth.LogoutUseCase
import com.tasksync.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val logoutUseCase: LogoutUseCase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _userState = MutableStateFlow<UiState<User>>(UiState.Loading)
    val userState: StateFlow<UiState<User>> = _userState.asStateFlow()

    private val _updateState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val updateState: StateFlow<UiState<Unit>> = _updateState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                if (user != null) {
                    _userState.value = UiState.Success(user)
                } else {
                    _userState.value = UiState.Error("User tidak ditemukan")
                }
            } catch (e: Exception) {
                _userState.value = UiState.Error(e.message ?: "Gagal memuat profil")
            }
        }
    }

    fun updateProfile(name: String) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            try {
                require(name.isNotBlank()) { "Nama tidak boleh kosong" }
                userRepository.updateProfile(name, "")
                loadProfile()
                _updateState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _updateState.value = UiState.Error(e.message ?: "Gagal update profil")
            }
        }
    }

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            try {
                require(newPassword.length >= 6) { "Password minimal 6 karakter" }
                firebaseAuth.currentUser?.updatePassword(newPassword)
                    ?.addOnSuccessListener {
                        _updateState.value = UiState.Success(Unit)
                    }
                    ?.addOnFailureListener { e ->
                        _updateState.value = UiState.Error(
                            e.message ?: "Gagal ganti password"
                        )
                    }
            } catch (e: Exception) {
                _updateState.value = UiState.Error(e.message ?: "Gagal ganti password")
            }
        }
    }

    fun logout() = logoutUseCase()

    fun resetUpdateState() {
        _updateState.value = UiState.Idle
    }

    val currentUserEmail: String
        get() = firebaseAuth.currentUser?.email ?: ""
}