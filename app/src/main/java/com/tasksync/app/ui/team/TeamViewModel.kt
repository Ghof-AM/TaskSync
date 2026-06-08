package com.tasksync.app.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tasksync.app.data.repository.ProjectMemberRepositoryImpl
import com.tasksync.app.domain.model.ProjectMember
import com.tasksync.app.domain.model.User
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

    private var currentProjectId: String? = null

    val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""

    fun loadMembers(projectId: String) {
        currentProjectId = projectId
        memberRepository.startListening(projectId)
        memberRepository.getMembersByProject(projectId)
            .onEach { members ->
                _membersState.value = UiState.Success(members)
                members.filter { it.userName.isBlank() }.forEach { member ->
                    viewModelScope.launch {
                        val user = userRepository.getUserById(member.userId)
                        if (user != null) {
                            memberRepository.refreshMemberName(
                                projectId, member.userId, user.name, user.email
                            )
                        }
                    }
                }
            }
            .catch { e ->
                _membersState.value = UiState.Error(e.message ?: "Gagal memuat anggota")
            }
            .launchIn(viewModelScope)

        // Listener Firestore hanya sebagai pelengkap real-time sync
        // Jika offline, listener tidak berjalan tapi data Room tetap tampil
        memberRepository.startListening(projectId)

        viewModelScope.launch {
            val uid = firebaseAuth.currentUser?.uid ?: return@launch
            _currentUserRole.value = memberRepository.getRole(projectId, uid)
        }
    }

    fun inviteMember(projectId: String, email: String) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                val inviterName = userRepository.getCurrentUser()?.name ?: "Seseorang"
                inviteMemberUseCase(projectId, email, inviterName)
                _actionState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _actionState.value = UiState.Error(e.message ?: "Gagal mengundang anggota")
            }
        }
    }

    fun promoteMember(projectId: String, userId: String) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                val actor = userRepository.getCurrentUser()
                promoteMemberUseCase(projectId = projectId, userId = userId, actorId = actor?.id ?: "", actorName = actor?.name ?: "")
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
                val actor = userRepository.getCurrentUser()
                demoteMemberUseCase(projectId = projectId, userId = userId, actorId = actor?.id ?: "", actorName = actor?.name ?: "")
                _actionState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _actionState.value = UiState.Error(e.message ?: "Gagal demote")
            }
        }
    }

    fun removeMember(projectId: String, userId: String) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                val actor = userRepository.getCurrentUser()
                removeMemberUseCase(projectId = projectId, userId = userId, actorId = actor?.id ?: "", actorName = actor?.name ?: "")
                _actionState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _actionState.value = UiState.Error(e.message ?: "Gagal hapus anggota")
            }
        }
    }

    fun transferOwnership(projectId: String, newOwnerId: String) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                val currentUser = userRepository.getCurrentUser()
                    ?: throw Exception("User tidak ditemukan")
                val newOwnerMember = (membersState.value as? UiState.Success)
                    ?.data?.find { it.userId == newOwnerId }
                    ?: throw Exception("Member tidak ditemukan")
                val newOwnerUser = User(id = newOwnerMember.userId, name = newOwnerMember.userName)
                transferOwnershipUseCase(projectId = projectId, currentOwner = currentUser, newOwner = newOwnerUser)
                _actionState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _actionState.value = UiState.Error(e.message ?: "Gagal transfer ownership")
            }
        }
    }

    fun isOwner(): Boolean = _currentUserRole.value == UserRole.OWNER
    fun isAdminOrOwner(): Boolean =
        _currentUserRole.value == UserRole.OWNER || _currentUserRole.value == UserRole.SECOND_OWNER

    override fun onCleared() {
        super.onCleared()
        currentProjectId?.let { memberRepository.stopListening(it) }
    }

    fun resetActionState() {
        _actionState.value = UiState.Idle
    }
}