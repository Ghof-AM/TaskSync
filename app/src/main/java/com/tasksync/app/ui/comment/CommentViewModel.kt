package com.tasksync.app.ui.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tasksync.app.domain.model.Comment
import com.tasksync.app.domain.model.User
import com.tasksync.app.domain.repository.CommentRepository
import com.tasksync.app.domain.repository.UserRepository
import com.tasksync.app.domain.usecase.comment.AddCommentUseCase
import com.tasksync.app.domain.usecase.comment.DeleteCommentUseCase
import com.tasksync.app.domain.usecase.comment.GetCommentsUseCase
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
class CommentViewModel @Inject constructor(
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _commentsState = MutableStateFlow<UiState<List<Comment>>>(UiState.Loading)
    val commentsState: StateFlow<UiState<List<Comment>>> = _commentsState.asStateFlow()

    private val _addCommentState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val addCommentState: StateFlow<UiState<Unit>> = _addCommentState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""

    fun loadComments(taskId: String) {
        // Observe Room (realtime lokal)
        getCommentsUseCase(taskId)
            .onEach { comments ->
                _commentsState.value = UiState.Success(comments)
            }
            .catch { e ->
                _commentsState.value = UiState.Error(
                    e.message ?: "Gagal memuat komentar"
                )
            }
            .launchIn(viewModelScope)

        // Pull dari Firestore sekali saat pertama buka
        viewModelScope.launch {
            try {
                commentRepository.pullFromFirestore(taskId)
            } catch (e: Exception) {
                android.util.Log.e("CommentViewModel", "Pull failed: ${e.message}")
            }
        }
    }

    private suspend fun pullCommentsFromFirestore(taskId: String) {
        // Inject FirestoreService dan CommentDao langsung di ViewModel
        // Tapi karena kita pakai repository pattern, kita bisa tambahkan
        // fungsi pull di repository
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                _currentUser.value = user
                android.util.Log.d("CommentVM", "Current user loaded: ${user?.name}")
            } catch (e: Exception) {
                android.util.Log.e("CommentVM", "Load user failed: ${e.message}")
            }
        }
    }

    fun addComment(taskId: String, content: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _addCommentState.value = UiState.Loading
            try {
                addCommentUseCase(taskId, content, user)
                _addCommentState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _addCommentState.value = UiState.Error(
                    e.message ?: "Gagal menambahkan komentar"
                )
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                deleteCommentUseCase(commentId)
            } catch (e: Exception) {
                _commentsState.value = UiState.Error(
                    e.message ?: "Gagal menghapus komentar"
                )
            }
        }
    }

    fun resetAddCommentState() {
        _addCommentState.value = UiState.Idle
    }
}