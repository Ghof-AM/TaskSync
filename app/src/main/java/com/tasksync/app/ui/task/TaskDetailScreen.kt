package com.tasksync.app.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasksync.app.domain.model.Task
import com.tasksync.app.domain.model.TaskStatus
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.ui.comment.CommentViewModel
import com.tasksync.app.util.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (taskId: String, projectId: String) -> Unit, // tambahkan
    taskDetailViewModel: TaskDetailViewModel = hiltViewModel(),
    commentViewModel: CommentViewModel = hiltViewModel()
) {
    val taskState by taskDetailViewModel.taskState.collectAsState()
    val userRole by taskDetailViewModel.userRole.collectAsState()
    val commentsState by commentViewModel.commentsState.collectAsState()
    val addCommentState by commentViewModel.addCommentState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var commentText by remember { mutableStateOf("") }
    val currentUser by commentViewModel.currentUser.collectAsState()

    val isAdmin = userRole == UserRole.OWNER || userRole == UserRole.SECOND_OWNER

    LaunchedEffect(taskId) {
        taskDetailViewModel.loadTask(taskId)
        commentViewModel.loadComments(taskId)
        commentViewModel.loadCurrentUser()
    }

    LaunchedEffect(addCommentState) {
        when (addCommentState) {
            is UiState.Success -> {
                commentText = ""
                commentViewModel.resetAddCommentState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(
                    (addCommentState as UiState.Error).message
                )
                commentViewModel.resetAddCommentState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                actions = {
                    if (isAdmin) {
                        IconButton(
                            onClick = {
                                val task = (taskState as? UiState.Success)?.data
                                if (task != null) {
                                    onNavigateToEdit(taskId, task.projectId)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Task",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                title = {
                    Text(
                        "Detail Task",
                        fontWeight = FontWeight.Bold
                    )
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (val state = taskState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .imePadding()
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Task info section
                        item {
                            TaskInfoSection(
                                task = state.data,
                                isAdmin = isAdmin,
                                onStatusChange = { newStatus ->
                                    taskDetailViewModel.updateStatus(taskId, newStatus)
                                }
                            )
                        }

                        // Comments header
                        item {
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Komentar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Comments list
                        when (val commentState = commentsState) {
                            is UiState.Success -> {
                                if (commentState.data.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Belum ada komentar",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(
                                        items = commentState.data,
                                        key = { it.id }
                                    ) { comment ->
                                        CommentItem(
                                            userName = comment.userName,
                                            content = comment.content,
                                            createdAt = comment.createdAt,
                                            isOwner = comment.userId == commentViewModel.currentUserId,
                                            onDelete = {
                                                commentViewModel.deleteComment(comment.id)
                                            }
                                        )
                                    }
                                }
                            }
                            is UiState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }

                    // Comment input
                    // Comment input — disable jika user belum loaded
                    HorizontalDivider()
                    CommentInputBar(
                        value = commentText,
                        onValueChange = { commentText = it },
                        onSend = {
                            if (commentText.isNotBlank()) {
                                commentViewModel.addComment(taskId, commentText)
                            }
                        },
                        isSending = addCommentState is UiState.Loading,
                        isEnabled = currentUser != null  // tambahkan parameter ini
                    )
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
}

@Composable
fun TaskInfoSection(
    task: Task,
    isAdmin: Boolean,
    onStatusChange: (TaskStatus) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Priority & Status row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = task.priority.color().copy(alpha = 0.15f)
                ) {
                    Text(
                        text = task.priority.displayName(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = task.priority.color(),
                        fontWeight = FontWeight.Medium
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = task.status.color().copy(alpha = 0.15f)
                ) {
                    Text(
                        text = task.status.displayName(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = task.status.color(),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Description
            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Setelah description, tambahkan:
            if (task.assignedToName.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ditugaskan ke: ${task.assignedToName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Deadline
            if (task.deadline > 0) {
                val isOverdue = task.deadline < System.currentTimeMillis()
                        && task.status != TaskStatus.DONE
                Text(
                    text = "📅 Deadline: ${
                        SimpleDateFormat("dd MMMM yyyy", Locale("id"))
                            .format(Date(task.deadline))
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status change buttons (semua user bisa update status)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Ubah Status:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskStatus.entries.forEach { status ->
                    val isSelected = task.status == status
                    Surface(
                        onClick = { if (!isSelected) onStatusChange(status) },
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) status.color()
                        else status.color().copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = status.displayName(),
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected)
                                androidx.compose.ui.graphics.Color.White
                            else status.color(),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    userName: String,
    content: String,
    createdAt: Long,
    isOwner: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar
        Surface(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = SimpleDateFormat("dd MMM, HH:mm", Locale("id"))
                            .format(Date(createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isOwner) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus komentar",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CommentInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
    isEnabled: Boolean = true  // tambahkan
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    if (isEnabled) "Tulis komentar..."
                    else "Memuat profil..."
                )
            },
            enabled = isEnabled,
            maxLines = 3,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onSend,
            enabled = value.isNotBlank() && !isSending && isEnabled
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim komentar",
                    tint = if (value.isNotBlank() && isEnabled)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}