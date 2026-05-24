package com.tasksync.app.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tasksync.app.domain.model.ProjectMember
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.domain.repository.ProjectMemberRepository
import com.tasksync.app.domain.repository.UserRepository
import com.tasksync.app.domain.usecase.team.DemoteMemberUseCase
import com.tasksync.app.domain.usecase.team.InviteMemberUseCase
import com.tasksync.app.domain.usecase.team.PromoteMemberUseCase
import com.tasksync.app.domain.usecase.team.RemoveMemberUseCase
import com.tasksync.app.domain.usecase.team.TransferOwnershipUseCase
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
class TeamViewModel @Inject constructor(
    private val memberRepository: ProjectMemberRepository,
    private val userRepository: UserRepository,
    private val inviteMemberUseCase: InviteMemberUseCase,
    private val removeMemberUseCase: RemoveMemberUseCase,
    private val promoteMemberUseCase: PromoteMemberUseCase,
    private val demoteMemberUseCase: DemoteMemberUseCase,
    private val transferOwnershipUseCase: TransferOwnershipUseCase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _membersState = MutableStateFlow<UiState<List<ProjectMember>>>(UiState.Loading)
    val membersState: StateFlow<UiState<List<ProjectMember>>> = _membersState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState.asStateFlow()

    private val _currentUserRole = MutableStateFlow(UserRole.MEMBER)
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""

    fun loadMembers(projectId: String) {
        memberRepository.getMembersByProject(projectId)
            .onEach { members ->
                _membersState.value = UiState.Success(members)
                // Refresh nama untuk member yang belum punya nama
                members.filter { it.userName.isBlank() }.forEach { member ->
                    viewModelScope.launch {
                        val user = userRepository.getUserById(member.userId)
                        if (user != null) {
                            memberRepository.refreshMemberName(
                                projectId,
                                member.userId,
                                user.name,
                                user.email
                            )
                        }
                    }
                }
            }
            .catch { e ->
                _membersState.value = UiState.Error(e.message ?: "Gagal memuat anggota")
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val uid = firebaseAuth.currentUser?.uid ?: return@launch
            _currentUserRole.value = memberRepository.getRole(projectId, uid)
        }
    }

    fun inviteMember(projectId: String, email: String) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                inviteMemberUseCase(projectId, email)
                _actionState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _actionState.value = UiState.Error(
                    e.message ?: "Gagal mengundang anggota"
                )
            }
        }
    }

    fun removeMember(projectId: String, userId: String) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                removeMemberUseCase(projectId, userId)
                _actionState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _actionState.value = UiState.Error(
                    e.message ?: "Gagal menghapus anggota"
                )
            }
        }
    }

    fun promoteMember(projectId: String, userId: String) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                promoteMemberUseCase(projectId, userId)
                _actionState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _actionState.value = UiState.Error(e.message ?: "Gagal promote")
            }
        }
    }

    fun demoteMember(projectId: String, userId: String) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                demoteMemberUseCase(projectId, userId)
                _actionState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _actionState.value = UiState.Error(e.message ?: "Gagal demote")
            }
        }
    }

    fun refreshMemberNames(projectId: String) {
        viewModelScope.launch {
            val state = _membersState.value
            if (state is UiState.Success) {
                state.data.forEach { member ->
                    if (member.userName.isBlank()) {
                        val user = userRepository.getUserById(member.userId)
                        if (user != null) {
                            memberRepository.updateRole(projectId, member.userId, member.role)
                            // update nama via dao langsung — tambahkan fungsi di repository
                        }
                    }
                }
            }
        }
    }

    fun isOwner(): Boolean = _currentUserRole.value == UserRole.OWNER
    fun isAdminOrOwner(): Boolean = _currentUserRole.value == UserRole.OWNER ||
            _currentUserRole.value == UserRole.SECOND_OWNER

    fun resetActionState() {
        _actionState.value = UiState.Idle
    }
}