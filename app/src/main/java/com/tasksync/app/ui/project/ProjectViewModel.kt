package com.tasksync.app.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tasksync.app.domain.model.Project
import com.tasksync.app.domain.repository.ProjectRepository
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
class ProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _projectsState = MutableStateFlow<UiState<List<Project>>>(UiState.Loading)
    val projectsState: StateFlow<UiState<List<Project>>> = _projectsState.asStateFlow()

    private val _createState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val createState: StateFlow<UiState<Unit>> = _createState.asStateFlow()

    val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""

    init {
        // Langsung load + mulai listener saat ViewModel dibuat.
        // Karena ViewModel di-scope ke NavBackStackEntry, listener ini hidup
        // selama user berada di halaman project list, dan mati otomatis saat
        // user keluar (ViewModel cleared → coroutine dibatalkan).
        loadProjects()
    }

    private fun loadProjects() {
        val uid = firebaseAuth.currentUser?.uid ?: return

        // Mulai listener Firestore real-time untuk user ini.
        // Listener akan push project baru ke Room setiap ada perubahan dari siapapun
        // (termasuk ketika user diundang ke project oleh user lain).
        projectRepository.startListening(uid)

        // Observe Room Flow. Setiap kali listener di atas upsert data baru ke Room,
        // Flow ini akan emit ulang dan UI otomatis refresh.
        projectRepository.getProjectsByUser(uid)
            .onEach { projects ->
                _projectsState.value = UiState.Success(projects)
            }
            .catch { e ->
                _projectsState.value = UiState.Error(e.message ?: "Gagal memuat project")
            }
            .launchIn(viewModelScope)
    }

    fun createProject(name: String, description: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            _createState.value = UiState.Loading
            try {
                require(name.isNotBlank()) { "Nama project tidak boleh kosong" }
                val project = Project(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    description = description.trim(),
                    createdBy = uid,
                    createdAt = System.currentTimeMillis()
                )
                projectRepository.createProject(project, uid)
                _createState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _createState.value = UiState.Error(e.message ?: "Gagal membuat project")
            }
        }
    }

    fun resetCreateState() {
        _createState.value = UiState.Idle
    }
}
