package com.tasksync.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tasksync.app.domain.model.Priority
import com.tasksync.app.domain.model.Task
import com.tasksync.app.domain.model.TaskStatus
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ProjectMemberRepository
import com.tasksync.app.domain.usecase.task.CreateTaskUseCase
import com.tasksync.app.domain.usecase.task.DeleteTaskUseCase
import com.tasksync.app.domain.usecase.task.GetAllTasksUseCase
import com.tasksync.app.domain.usecase.task.UpdateTaskStatusUseCase
import com.tasksync.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val getAllTasksUseCase: GetAllTasksUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskStatusUseCase: UpdateTaskStatusUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val memberRepository: ProjectMemberRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _tasksState = MutableStateFlow<UiState<List<Task>>>(UiState.Loading)
    val tasksState: StateFlow<UiState<List<Task>>> = _tasksState.asStateFlow()

    private val _createState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val createState: StateFlow<UiState<Unit>> = _createState.asStateFlow()

    private val _userRole = MutableStateFlow(UserRole.MEMBER)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _selectedFilter = MutableStateFlow<TaskStatus?>(null)
    val selectedFilter: StateFlow<TaskStatus?> = _selectedFilter.asStateFlow()

    val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""

    fun loadTasks(projectId: String) {
        getAllTasksUseCase(projectId)
            .onEach { tasks ->
                _tasksState.value = UiState.Success(tasks)
            }
            .catch { e ->
                _tasksState.value = UiState.Error(e.message ?: "Gagal memuat task")
            }
            .launchIn(viewModelScope)
    }

    fun loadUserRole(projectId: String) {
        viewModelScope.launch {
            val uid = firebaseAuth.currentUser?.uid ?: return@launch
            val role = memberRepository.getRole(projectId, uid)
            android.util.Log.d("TaskViewModel", "User: $uid, Project: $projectId, Role: $role")
            _userRole.value = role
        }
    }

    fun setFilter(status: TaskStatus?) {
        _selectedFilter.value = status
    }

    fun createTask(
        projectId: String,
        title: String,
        description: String,
        assignedTo: String,
        priority: Priority,
        deadline: Long
    ) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            _createState.value = UiState.Loading
            try {
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    teamId = projectId,
                    title = title.trim(),
                    description = description.trim(),
                    assignedTo = assignedTo,
                    createdBy = uid,
                    priority = priority,
                    deadline = deadline
                )
                createTaskUseCase(task)
                _createState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _createState.value = UiState.Error(
                    e.message ?: "Gagal membuat task"
                )
            }
        }
    }

    fun updateStatus(taskId: String, status: TaskStatus) {
        viewModelScope.launch {
            try {
                updateTaskStatusUseCase(taskId, status)
            } catch (e: Exception) {
                _tasksState.value = UiState.Error(e.message ?: "Gagal update status")
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                deleteTaskUseCase(taskId)
            } catch (e: Exception) {
                _tasksState.value = UiState.Error(e.message ?: "Gagal menghapus task")
            }
        }
    }

    fun resetCreateState() {
        _createState.value = UiState.Idle
    }

    fun isAdminOrOwner(): Boolean =
        _userRole.value == UserRole.OWNER || _userRole.value == UserRole.SECOND_OWNER
}