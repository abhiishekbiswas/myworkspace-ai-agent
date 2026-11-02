package com.example.ui.screens

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
import com.example.ui.components.CreateEditTaskDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.TaskCard
import com.example.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    viewModel: WorkspaceViewModel,
    onNavigateBack: () -> Unit,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProjects by viewModel.activeProjects.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val blockedTaskIds by viewModel.blockedTaskIds.collectAsStateWithLifecycle()

    val project = activeProjects.firstOrNull { it.id == projectId }
    val projectTasks = allTasks.filter { it.projectId == projectId }

    var showAddTaskDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Project Details") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("project_detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.duplicateProject(projectId)
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("duplicate_project_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                    }
                    IconButton(
                        onClick = {
                            if (project != null) {
                                viewModel.deleteProject(project)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("delete_project_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                modifier = Modifier.testTag("fab_add_project_task")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (project == null) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Project not found")
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
                                    text = project.name,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                StatusBadge(status = project.status)
                            }
                            if (project.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = project.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Progress: ${project.progress}%")
                            LinearProgressIndicator(
                                progress = { (project.progress / 100.0).toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Tasks (${projectTasks.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (projectTasks.isEmpty()) {
                    item {
                        Text(
                            text = "No tasks in this project yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(projectTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            isBlocked = blockedTaskIds.contains(task.id),
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onClick = { onTaskClick(task.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        CreateEditTaskDialog(
            defaultProjectId = projectId,
            projects = activeProjects,
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, desc, projId, priority ->
                showAddTaskDialog = false
                viewModel.createTask(title, desc, projId ?: projectId, priority)
            }
        )
    }
}
