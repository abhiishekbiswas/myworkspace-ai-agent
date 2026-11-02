package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: WorkspaceViewModel,
    onNavigateToCalendar: () -> Unit,
    onNavigateToKanban: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("More Features") })
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            item {
                MoreItem(
                    title = "Calendar & Schedule",
                    subtitle = "View tasks by due date",
                    icon = Icons.Default.CalendarToday,
                    onClick = onNavigateToCalendar,
                    testTag = "more_calendar"
                )
            }
            item {
                MoreItem(
                    title = "Kanban Board",
                    subtitle = "Visualize tasks across columns",
                    icon = Icons.Default.ViewKanban,
                    onClick = onNavigateToKanban,
                    testTag = "more_kanban"
                )
            }
            item {
                MoreItem(
                    title = "Notes & Docs",
                    subtitle = "Personal notes and scratchpad",
                    icon = Icons.Default.Edit,
                    onClick = onNavigateToNotes,
                    testTag = "more_notes"
                )
            }
            item {
                MoreItem(
                    title = "Analytics",
                    subtitle = "Completion rates and project progress",
                    icon = Icons.Default.BarChart,
                    onClick = onNavigateToAnalytics,
                    testTag = "more_analytics"
                )
            }
            item {
                MoreItem(
                    title = "Search Workspace",
                    subtitle = "Find tasks, projects, and notes",
                    icon = Icons.Default.Search,
                    onClick = onNavigateToSearch,
                    testTag = "more_search"
                )
            }
            item {
                MoreItem(
                    title = "Backup & Restore",
                    subtitle = "Export or restore JSON database backup",
                    icon = Icons.Default.Backup,
                    onClick = onNavigateToBackup,
                    testTag = "more_backup"
                )
            }
            item {
                MoreItem(
                    title = "Settings",
                    subtitle = "Theme and application preferences",
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToSettings,
                    testTag = "more_settings"
                )
            }
        }
    }
}

@Composable
private fun MoreItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    )
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}
