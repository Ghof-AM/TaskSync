package com.tasksync.app.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasksync.app.domain.model.Priority
import com.tasksync.app.domain.model.Task
import com.tasksync.app.domain.model.TaskStatus
import com.tasksync.app.domain.model.UserRole
import com.tasksync.app.util.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    projectId: String,
    projectName: String = "Project",
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (taskId: String) -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val tasksState by viewModel.tasksState.collectAsState()
    val createState by viewModel.createState.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Tentukan apakah user adalah admin berdasarkan StateFlow
    val isAdmin = userRole == UserRole.OWNER || userRole == UserRole.SECOND_OWNER

    LaunchedEffect(projectId) {
        viewModel.loadTasks(projectId)
        viewModel.loadUserRole(projectId)
    }

    LaunchedEffect(createState) {
        if (createState is UiState.Error) {
            snackbarHostState.showSnackbar(
                (createState as UiState.Error).message
            )
            viewModel.resetCreateState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        projectName,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
        floatingActionButton = {
            // Gunakan isAdmin dari StateFlow, bukan fungsi biasa
            if (isAdmin) {
                FloatingActionButton(
                    onClick = onNavigateToCreate,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Buat Task",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { viewModel.setFilter(null) },
                        label = { Text("Semua") }
                    )
                }
                items(TaskStatus.entries) { status ->
                    FilterChip(
                        selected = selectedFilter == status,
                        onClick = { viewModel.setFilter(status) },
                        label = { Text(status.displayName()) }
                    )
                }
            }

            // Task list
            when (val state = tasksState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Success -> {
                    val filtered = if (selectedFilter == null) {
                        state.data
                    } else {
                        state.data.filter { it.status == selectedFilter }
                    }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (selectedFilter == null)
                                        "Belum ada task"
                                    else "Tidak ada task ${selectedFilter?.displayName()}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // Gunakan isAdmin dari StateFlow
                                if (isAdmin) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tap + untuk membuat task baru",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = filtered,
                                key = { it.id }
                            ) { task ->
                                TaskCard(
                                    task = task,
                                    // Gunakan isAdmin dari StateFlow
                                    isAdmin = isAdmin,
                                    onClick = { onNavigateToDetail(task.id) },
                                    onStatusToggle = {
                                        val newStatus = when (task.status) {
                                            TaskStatus.TODO -> TaskStatus.IN_PROGRESS
                                            TaskStatus.IN_PROGRESS -> TaskStatus.DONE
                                            TaskStatus.DONE -> TaskStatus.TODO
                                        }
                                        viewModel.updateStatus(task.id, newStatus)
                                    },
                                    onDelete = {
                                        viewModel.deleteTask(task.id)
                                    }
                                )
                            }
                        }
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
}

@Composable
fun TaskCard(
    task: Task,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onStatusToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status toggle button
            IconButton(
                onClick = onStatusToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (task.status == TaskStatus.DONE)
                        Icons.Default.CheckCircle
                    else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle status",
                    tint = when (task.status) {
                        TaskStatus.DONE -> Color(0xFF4CAF50)
                        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                        TaskStatus.TODO -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Task info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Priority badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(task.priority.color().copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.priority.displayName(),
                            style = MaterialTheme.typography.labelSmall,
                            color = task.priority.color(),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(task.status.color().copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.status.displayName(),
                            style = MaterialTheme.typography.labelSmall,
                            color = task.status.color(),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.status == TaskStatus.DONE)
                        TextDecoration.LineThrough
                    else TextDecoration.None
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (task.deadline > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val isOverdue = task.deadline < System.currentTimeMillis()
                            && task.status != TaskStatus.DONE
                    Text(
                        text = "📅 ${
                            SimpleDateFormat("dd MMM yyyy", Locale("id"))
                                .format(Date(task.deadline))
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Delete button (admin only)
            if (isAdmin) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus task",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Extension functions untuk display
fun TaskStatus.displayName(): String = when (this) {
    TaskStatus.TODO -> "Todo"
    TaskStatus.IN_PROGRESS -> "In Progress"
    TaskStatus.DONE -> "Selesai"
}

fun TaskStatus.color(): Color = when (this) {
    TaskStatus.TODO -> Color(0xFF9E9E9E)
    TaskStatus.IN_PROGRESS -> Color(0xFF2196F3)
    TaskStatus.DONE -> Color(0xFF4CAF50)
}

fun Priority.displayName(): String = when (this) {
    Priority.LOW -> "Low"
    Priority.MEDIUM -> "Medium"
    Priority.HIGH -> "High"
}

fun Priority.color(): Color = when (this) {
    Priority.LOW -> Color(0xFF4CAF50)
    Priority.MEDIUM -> Color(0xFFFF9800)
    Priority.HIGH -> Color(0xFFF44336)
}