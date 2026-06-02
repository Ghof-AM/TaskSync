package com.tasksync.app.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasksync.app.domain.model.ActivityLog
import com.tasksync.app.domain.usecase.log.GetActivityLogUseCase
import com.tasksync.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ActivityLogViewModel @Inject constructor(
    private val getActivityLogUseCase: GetActivityLogUseCase
) : ViewModel() {

    private val _logState = MutableStateFlow<UiState<List<ActivityLog>>>(UiState.Loading)
    val logState: StateFlow<UiState<List<ActivityLog>>> = _logState.asStateFlow()

    fun loadLogs(projectId: String) {
        android.util.Log.d("ActivityLogVM", "Loading logs for project: $projectId")
        getActivityLogUseCase(projectId)
            .onEach { logs ->
                android.util.Log.d("ActivityLogVM", "Logs loaded: ${logs.size}")
                logs.forEach { android.util.Log.d("ActivityLogVM", "Log: ${it.message}") }
                _logState.value = UiState.Success(logs)
            }
            .catch { e ->
                android.util.Log.e("ActivityLogVM", "Error: ${e.message}")
                _logState.value = UiState.Error(e.message ?: "Gagal memuat activity log")
            }
            .launchIn(viewModelScope)
    }
}