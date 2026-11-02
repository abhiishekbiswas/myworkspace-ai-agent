package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TaskEntity
import com.example.ui.components.CreateEditTaskDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TaskCard
import com.example.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: WorkspaceViewModel,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val blockedTaskIds by viewModel.blockedTaskIds.collectAsStateWithLifecycle()
    val activeProjects by viewModel.activeProjects.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var taskToConfirmBlocked by remember { mutableStateOf<TaskEntity?>(null) }

    val filteredTasks = remember(allTasks, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> allTasks.filter { it.status == "PENDING" }
            "IN_PROGRESS" -> allTasks.filter { it.status == "IN_PROGRESS" }
            "COMPLETED" -> allTasks.filter { it.status == "COMPLETED" }
            "BLOCKED" -> allTasks.filter { blockedTaskIds.contains(it.id) }
            else -> allTasks
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                actions = {
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("new_task_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("fab_add_task")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Task")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("ALL", "PENDING", "IN_PROGRESS", "COMPLETED", "BLOCKED")
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.replace("_", " ")) },
                        modifier = Modifier.testTag("task_filter_${filter}")
                    )
                }
            }

            if (filteredTasks.isEmpty()) {
                EmptyStateView(
                    title = "No tasks found",
                    subtitle = "Create a task or change the filter"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        val isBlocked = blockedTaskIds.contains(task.id)
                        TaskCard(
                            task = task,
                            isBlocked = isBlocked,
                            onToggleComplete = {
                                if (isBlocked && task.status != "COMPLETED") {
                                    taskToConfirmBlocked = task
                                } else {
                                    viewModel.toggleTaskCompletion(task)
                                }
                            },
                            onClick = { onTaskClick(task.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateEditTaskDialog(
            projects = activeProjects,
            onDismiss = { showCreateDialog = false },
            onSave = { title, desc, projId, priority ->
                showCreateDialog = false
                viewModel.createTask(title, desc, projId, priority)
            }
        )
    }

    if (taskToConfirmBlocked != null) {
        val task = taskToConfirmBlocked!!
        AlertDialog(
            onDismissRequest = { taskToConfirmBlocked = null },
            title = { Text("Task is Blocked") },
            text = {
                Text("This task has incomplete prerequisites. Completing it now may violate dependency order. Are you sure?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleTaskCompletion(task)
                        taskToConfirmBlocked = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Complete Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToConfirmBlocked = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
