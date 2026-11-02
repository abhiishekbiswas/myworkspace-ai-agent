package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    viewModel: WorkspaceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val blockedTaskIds by viewModel.blockedTaskIds.collectAsStateWithLifecycle()
    val allDependencies by viewModel.allDependencies.collectAsStateWithLifecycle()

    val task = allTasks.firstOrNull { it.id == taskId }
    val isBlocked = blockedTaskIds.contains(taskId)

    val prerequisites = remember(allDependencies, allTasks, taskId) {
        val prereqIds = allDependencies.filter { it.taskId == taskId }.map { it.dependsOnTaskId }
        allTasks.filter { prereqIds.contains(it.id) }
    }

    val availablePrereqCandidates = remember(allTasks, taskId, prerequisites) {
        val currentPrereqIds = prerequisites.map { it.id }.toSet()
        allTasks.filter { it.id != taskId && !currentPrereqIds.contains(it.id) && it.projectId == task?.projectId }
    }

    var showAddPrereqDialog by remember { mutableStateOf(false) }
    var dependencyErrorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(task?.title ?: "Task Details") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("task_detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (task != null) {
                                viewModel.deleteTask(task)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("delete_task_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (task == null) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Task not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusBadge(status = task.status)
                            }

                            if (task.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PriorityBadge(priority = task.priority)
                                if (isBlocked) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = "BLOCKED",
                                            color = MaterialTheme.colorScheme.onError,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { viewModel.toggleTaskCompletion(task) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_complete_detail_button")
                    ) {
                        Text(if (task.status == "COMPLETED") "Mark as Incomplete" else "Mark as Completed")
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Prerequisites (${prerequisites.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(
                            onClick = { showAddPrereqDialog = true },
                            modifier = Modifier.testTag("add_prerequisite_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }

                if (prerequisites.isEmpty()) {
                    item {
                        Text(
                            text = "No prerequisite tasks. This task can be completed anytime.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(prerequisites, key = { it.id }) { prereq ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prereq.title,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Status: ${prereq.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.removeDependency(taskId, prereq.id) },
                                    modifier = Modifier.testTag("remove_dep_${prereq.id}")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove dependency")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPrereqDialog) {
        AlertDialog(
            onDismissRequest = { showAddPrereqDialog = false },
            title = { Text("Add Prerequisite") },
            text = {
                if (availablePrereqCandidates.isEmpty()) {
                    Text("No other tasks in this project are eligible to be prerequisites.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(availablePrereqCandidates) { candidate ->
                            ListItem(
                                headlineContent = { Text(candidate.title) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showAddPrereqDialog = false
                                        viewModel.addDependency(
                                            taskId = taskId,
                                            dependsOnTaskId = candidate.id,
                                            onCycleDetected = {
                                                dependencyErrorMessage = "Circular dependency detected! Cannot make this task depend on '${candidate.title}'."
                                            }
                                        )
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddPrereqDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (dependencyErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { dependencyErrorMessage = null },
            title = { Text("Dependency Error") },
            text = { Text(dependencyErrorMessage ?: "") },
            confirmButton = {
                Button(onClick = { dependencyErrorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}
