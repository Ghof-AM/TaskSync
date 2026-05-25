package com.tasksync.app.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasksync.app.domain.model.Priority
import com.tasksync.app.domain.model.ProjectMember
import com.tasksync.app.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    onTaskCreated: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val createState by viewModel.createState.collectAsState()
    val projectMembers by viewModel.projectMembers.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Assignee state
    var selectedMember by remember { mutableStateOf<ProjectMember?>(null) }
    var assigneeExpanded by remember { mutableStateOf(false) }

    // Priority state
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var priorityExpanded by remember { mutableStateOf(false) }

    var deadline by remember { mutableLongStateOf(0L) }

    LaunchedEffect(projectId) {
        viewModel.loadProjectMembers(projectId)
    }

    LaunchedEffect(createState) {
        when (createState) {
            is UiState.Success -> {
                viewModel.resetCreateState()
                onTaskCreated()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(
                    (createState as UiState.Error).message
                )
                viewModel.resetCreateState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Buat Task Baru", fontWeight = FontWeight.Bold)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Judul Task*") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // Assignee dropdown
            ExposedDropdownMenuBox(
                expanded = assigneeExpanded,
                onExpandedChange = { assigneeExpanded = it }
            ) {
                OutlinedTextField(
                    value = when {
                        selectedMember == null -> "Pilih anggota..."
                        selectedMember!!.userName.isNotBlank() -> selectedMember!!.userName
                        selectedMember!!.userEmail.isNotBlank() -> selectedMember!!.userEmail
                        else -> selectedMember!!.userId.take(8) + "..."
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Assign ke") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = assigneeExpanded
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = assigneeExpanded,
                    onDismissRequest = { assigneeExpanded = false }
                ) {
                    // Opsi "Tidak di-assign"
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Tidak di-assign",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            selectedMember = null
                            assigneeExpanded = false
                        }
                    )

                    // Daftar anggota
                    projectMembers.forEach { member ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = when {
                                            member.userName.isNotBlank() -> member.userName
                                            member.userEmail.isNotBlank() -> member.userEmail
                                            else -> member.userId.take(8) + "..."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (member.userName.isNotBlank() &&
                                        member.userEmail.isNotBlank()
                                    ) {
                                        Text(
                                            text = member.userEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = when (member.role) {
                                            com.tasksync.app.domain.model.UserRole.OWNER -> "👑 Owner"
                                            com.tasksync.app.domain.model.UserRole.SECOND_OWNER -> "⭐ 2nd Owner"
                                            com.tasksync.app.domain.model.UserRole.MEMBER -> "👤 Member"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                selectedMember = member
                                assigneeExpanded = false
                            }
                        )
                    }
                }
            }

            // Priority dropdown
            ExposedDropdownMenuBox(
                expanded = priorityExpanded,
                onExpandedChange = { priorityExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedPriority.displayName(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Prioritas") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = priorityExpanded
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = priorityExpanded,
                    onDismissRequest = { priorityExpanded = false }
                ) {
                    Priority.entries.forEach { priority ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = priority.displayName(),
                                    color = priority.color()
                                )
                            },
                            onClick = {
                                selectedPriority = priority
                                priorityExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        viewModel.createTask(
                            projectId = projectId,
                            title = title,
                            description = description,
                            assignedTo = selectedMember?.userId ?: "",
                            assignedToName = selectedMember?.userName
                                ?: selectedMember?.userEmail ?: "",
                            priority = selectedPriority,
                            deadline = deadline
                        )
                    },
                    enabled = title.isNotBlank() &&
                            createState !is UiState.Loading
                ) {
                    if (createState is UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Buat Task")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}