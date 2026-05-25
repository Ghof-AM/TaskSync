package com.tasksync.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tasksync.app.domain.model.Priority
import com.tasksync.app.domain.model.ProjectMember
import com.tasksync.app.domain.model.Task
import com.tasksync.app.domain.model.TaskStatus
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ProjectMemberRepository
import com.tasksync.app.domain.repository.TaskRepository
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
    private val taskRepository: TaskRepository,  // tambahkan ini
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

    // Tambahkan ini
    private val _projectMembers = MutableStateFlow<List<ProjectMember>>(emptyList())
    val projectMembers: StateFlow<List<ProjectMember>> = _projectMembers.asStateFlow()

    val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""

    private val _taskDetail = MutableStateFlow<Task?>(null)
    val taskDetail: StateFlow<Task?> = _taskDetail.asStateFlow()

    private val _editState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val editState: StateFlow<UiState<Unit>> = _editState.asStateFlow()

    fun loadTaskForEdit(taskId: String) {
        viewModelScope.launch {
            try {
                val task = taskRepository.getTaskById(taskId)
                _taskDetail.value = task
            } catch (e: Exception) {
                android.util.Log.e("TaskViewModel", "Load task failed: ${e.message}")
            }
        }
    }

    fun editTask(
        taskId: String,
        projectId: String,
        title: String,
        description: String,
        assignedTo: String,
        assignedToName: String,
        priority: Priority,
        deadline: Long
    ) {
        viewModelScope.launch {
            _editState.value = UiState.Loading
            try {
                require(title.isNotBlank()) { "Judul task tidak boleh kosong" }
                val current = taskRepository.getTaskById(taskId)
                    ?: throw Exception("Task tidak ditemukan")
                val updated = current.copy(
                    title = title.trim(),
                    description = description.trim(),
                    assignedTo = assignedTo,
                    assignedToName = assignedToName,
                    priority = priority,
                    deadline = deadline,
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )
                taskRepository.updateTask(updated)
                _editState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _editState.value = UiState.Error(e.message ?: "Gagal mengubah task")
            }
        }
    }

    fun resetEditState() {
        _editState.value = UiState.Idle
    }

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

    // Tambahkan fungsi ini
    fun loadProjectMembers(projectId: String) {
        memberRepository.getMembersByProject(projectId)
            .onEach { members ->
                _projectMembers.value = members
            }
            .catch { e ->
                android.util.Log.e("TaskViewModel", "Load members failed: ${e.message}")
            }
            .launchIn(viewModelScope)
    }

    fun setFilter(status: TaskStatus?) {
        _selectedFilter.value = status
    }

    fun createTask(
        projectId: String,
        title: String,
        description: String,
        assignedTo: String,
        assignedToName: String,
        priority: Priority,
        deadline: Long
    ) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            _createState.value = UiState.Loading
            try {
                val task = Task(
                    id = java.util.UUID.randomUUID().toString(),
                    projectId = projectId,
                    teamId = projectId,
                    title = title.trim(),
                    description = description.trim(),
                    assignedTo = assignedTo,
                    assignedToName = assignedToName,
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