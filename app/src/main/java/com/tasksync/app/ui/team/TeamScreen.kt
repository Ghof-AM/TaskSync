package com.tasksync.app.ui.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasksync.app.domain.model.ProjectMember
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.util.UiState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    viewModel: TeamViewModel = hiltViewModel()
) {
    val membersState by viewModel.membersState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteUserId by remember { mutableStateOf("") }

    val isAdmin = currentUserRole == UserRole.OWNER ||
            currentUserRole == UserRole.SECOND_OWNER

    LaunchedEffect(projectId) {
        viewModel.loadMembers(projectId)
    }

    LaunchedEffect(actionState) {
        when (actionState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar("Berhasil!")
                showInviteDialog = false
                inviteUserId = ""
                viewModel.resetActionState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(
                    (actionState as UiState.Error).message
                )
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Manajemen Tim", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showInviteDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Undang anggota",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (val state = membersState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "${state.data.size} Anggota",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(
                        items = state.data,
                        key = { "${it.projectId}_${it.userId}" }
                    ) { member ->
                        MemberCard(
                            member = member,
                            isCurrentUser = member.userId == viewModel.currentUserId,
                            canManage = isAdmin && member.userId != viewModel.currentUserId,
                            isOwner = currentUserRole == UserRole.OWNER,
                            onPromote = {
                                viewModel.promoteMember(projectId, member.userId)
                            },
                            onDemote = {
                                viewModel.demoteMember(projectId, member.userId)
                            },
                            onRemove = {
                                viewModel.removeMember(projectId, member.userId)
                            }
                        )
                    }
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {}
        }
    }
    var inviteEmail by remember { mutableStateOf("") }
    // Invite Dialog
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = {
                showInviteDialog = false
                inviteEmail = ""
            },
            title = { Text("Undang Anggota") },
            text = {
                Column {
                    Text(
                        text = "Masukkan email anggota yang sudah terdaftar di TaskSync",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Email") },
                        placeholder = { Text("contoh@email.com") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.inviteMember(projectId, inviteEmail)
                    },
                    enabled = inviteEmail.contains("@") &&
                            actionState !is UiState.Loading
                ) {
                    if (actionState is UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Undang")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showInviteDialog = false
                    inviteEmail = ""
                }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun MemberCard(
    member: ProjectMember,
    isCurrentUser: Boolean,
    canManage: Boolean,
    isOwner: Boolean,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = when (member.role) {
                    UserRole.OWNER -> MaterialTheme.colorScheme.primary
                    UserRole.SECOND_OWNER -> MaterialTheme.colorScheme.secondary
                    UserRole.MEMBER -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (member.userName.isNotBlank())
                            member.userName.first().uppercaseChar().toString()
                        else "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (member.role) {
                            UserRole.OWNER -> MaterialTheme.colorScheme.onPrimary
                            UserRole.SECOND_OWNER -> MaterialTheme.colorScheme.onSecondary
                            UserRole.MEMBER -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        // Tampilkan userName jika ada, fallback ke email, lalu userId
                        text = when {
                            member.userName.isNotBlank() -> member.userName
                            member.userEmail.isNotBlank() -> member.userEmail
                            else -> member.userId.take(8) + "..."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isCurrentUser) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Kamu",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                // Tampilkan email jika userName ada
                if (member.userName.isNotBlank() && member.userEmail.isNotBlank()) {
                    Text(
                        text = member.userEmail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = when (member.role) {
                        UserRole.OWNER -> "👑 Owner"
                        UserRole.SECOND_OWNER -> "⭐ 2nd Owner"
                        UserRole.MEMBER -> "👤 Member"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Menu actions (admin only, not for self)
            if (canManage) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Opsi"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (member.role == UserRole.MEMBER) {
                            DropdownMenuItem(
                                text = { Text("Angkat jadi 2nd Owner") },
                                onClick = {
                                    showMenu = false
                                    onPromote()
                                }
                            )
                        }
                        if (member.role == UserRole.SECOND_OWNER) {
                            DropdownMenuItem(
                                text = { Text("Turunkan jadi Member") },
                                onClick = {
                                    showMenu = false
                                    onDemote()
                                }
                            )
                        }
                        if (isOwner) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Hapus dari Tim",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onRemove()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}