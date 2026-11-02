package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.viewmodel.WorkspaceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: WorkspaceViewModel,
    onNavigateToProjects: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    onNavigateToProjectDetail: (String) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToChats: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val blockedTaskIds by viewModel.blockedTaskIds.collectAsStateWithLifecycle()
    val recentActivities by viewModel.recentActivities.collectAsStateWithLifecycle()
    val activeProjects by viewModel.activeProjects.collectAsStateWithLifecycle()

    var showQuickAdd by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var showCreateProjectDialog by remember { mutableStateOf(false) }

    val todayCalendar = Calendar.getInstance()
    val todayFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val dateString = todayFormat.format(todayCalendar.time)

    val todayTasks = remember(allTasks) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val endOfDay = cal.timeInMillis

        allTasks.filter { task ->
            task.dueDate != null && task.dueDate in startOfDay..endOfDay
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                greeting = "My Workspace",
                dateString = dateString,
                onSearchClick = onNavigateToSearch,
                onChatClick = onNavigateToChats,
                onSettingsClick = onNavigateToSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickAdd = true },
                modifier = Modifier.testTag("fab_quick_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Pending Tasks",
                        value = "${stats.pendingTasks}",
                        icon = Icons.Default.CheckCircle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Overdue",
                        value = "${stats.overdueTasks}",
                        icon = Icons.Default.Warning,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Projects",
                        value = "${stats.activeProjects}",
                        icon = Icons.Default.Folder,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Done",
                        value = "${stats.completedTasks}",
                        icon = Icons.Default.DoneAll,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Today's Schedule",
                    actionText = "See All",
                    onActionClick = onNavigateToTasks
                )
            }

            if (todayTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "No tasks due today. Enjoy your day or add new tasks!",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(todayTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        isBlocked = blockedTaskIds.contains(task.id),
                        onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                        onClick = { onNavigateToTaskDetail(task.id) }
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Active Projects",
                    actionText = "View All",
                    onActionClick = onNavigateToProjects
                )
            }

            if (activeProjects.isEmpty()) {
                item {
                    Text(
                        text = "No active projects. Tap + to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(activeProjects.take(3), key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onNavigateToProjectDetail(project.id) }
                    )
                }
            }

            if (recentActivities.isNotEmpty()) {
                item {
                    SectionHeader(title = "Recent Activity")
                }
                items(recentActivities.take(5), key = { it.id }) { act ->
                    ListItem(
                        headlineContent = { Text(act.action) },
                        supportingContent = { Text(act.details) },
                        leadingContent = { Icon(Icons.Default.History, contentDescription = null) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (showQuickAdd) {
        QuickAddMenu(
            onDismiss = { showQuickAdd = false },
            onAddTask = {
                showQuickAdd = false
                showCreateTaskDialog = true
            },
            onAddProject = {
                showQuickAdd = false
                showCreateProjectDialog = true
            },
            onAddNote = {
                showQuickAdd = false
                viewModel.createNote("Quick Note", "")
            }
        )
    }

    if (showCreateTaskDialog) {
        CreateEditTaskDialog(
            projects = activeProjects,
            onDismiss = { showCreateTaskDialog = false },
            onSave = { title, desc, projId, priority ->
                showCreateTaskDialog = false
                viewModel.createTask(title, desc, projId, priority)
            }
        )
    }

    if (showCreateProjectDialog) {
        CreateEditProjectDialog(
            onDismiss = { showCreateProjectDialog = false },
            onSave = { name, desc, catId, color ->
                showCreateProjectDialog = false
                viewModel.createProject(name, desc, catId, color)
            }
        )
    }
}
