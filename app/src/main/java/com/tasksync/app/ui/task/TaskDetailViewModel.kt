package com.tasksync.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tasksync.app.domain.model.Task
import com.tasksync.app.domain.model.TaskStatus
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ProjectMemberRepository
import com.tasksync.app.domain.repository.TaskRepository
import com.tasksync.app.domain.usecase.task.UpdateTaskStatusUseCase
import com.tasksync.app.util.NetworkMonitor
import com.tasksync.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val memberRepository: ProjectMemberRepository,
    private val updateTaskStatusUseCase: UpdateTaskStatusUseCase,
    private val networkMonitor: NetworkMonitor,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _taskState = MutableStateFlow<UiState<Task>>(UiState.Loading)
    val taskState: StateFlow<UiState<Task>> = _taskState.asStateFlow()

    private val _userRole = MutableStateFlow(UserRole.MEMBER)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        // Observe koneksi jaringan — tampilkan banner offline di UI
        networkMonitor.isOnlineFlow
            .onEach { online ->
                _isOnline.value = online
                // Instant sync saat kembali online — tidak perlu tunggu SyncWorker 15 menit
                if (online) {
                    taskRepository.syncAllPending()
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadTask(taskId: String) {
        // Gunakan Flow dari Room agar UI otomatis update saat data berubah
        // (baik dari listener Firestore maupun dari updateStatus lokal)
        taskRepository.getTaskFlow(taskId)
            .onEach { task ->
                if (task != null) {
                    _taskState.value = UiState.Success(task)
                    loadUserRole(task.projectId)
                } else {
                    _taskState.value = UiState.Error("Task tidak ditemukan")
                }
            }
            .catch { e ->
                _taskState.value = UiState.Error(e.message ?: "Gagal memuat task")
            }
            .launchIn(viewModelScope)
    }

    private fun loadUserRole(projectId: String) {
        viewModelScope.launch {
            val uid = firebaseAuth.currentUser?.uid ?: return@launch
            _userRole.value = memberRepository.getRole(projectId, uid)
        }
    }

    fun updateStatus(taskId: String, status: TaskStatus) {
        viewModelScope.launch {
            try {
                updateTaskStatusUseCase(taskId, status)
                // Tidak perlu loadTask() lagi — Flow di atas akan emit otomatis
            } catch (e: Exception) {
                _taskState.value = UiState.Error(e.message ?: "Gagal update status")
            }
        }
    }
}