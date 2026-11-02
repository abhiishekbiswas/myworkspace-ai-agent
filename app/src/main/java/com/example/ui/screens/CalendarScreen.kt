package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionHeader
import com.example.ui.components.TaskCard
import com.example.viewmodel.WorkspaceViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: WorkspaceViewModel,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val blockedTaskIds by viewModel.blockedTaskIds.collectAsStateWithLifecycle()

    val (todayTasks, upcomingTasks, unscheduledTasks) = remember(allTasks) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startOfToday = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val endOfToday = cal.timeInMillis

        val today = mutableListOf<com.example.data.local.entity.TaskEntity>()
        val upcoming = mutableListOf<com.example.data.local.entity.TaskEntity>()
        val unscheduled = mutableListOf<com.example.data.local.entity.TaskEntity>()

        for (t in allTasks) {
            val due = t.dueDate
            if (due == null) {
                unscheduled.add(t)
            } else if (due in startOfToday..endOfToday) {
                today.add(t)
            } else {
                upcoming.add(t)
            }
        }
        Triple(today, upcoming, unscheduled)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Calendar & Schedule") })
        },
        modifier = modifier
    ) { innerPadding ->
        if (allTasks.isEmpty()) {
            EmptyStateView(
                title = "No scheduled tasks",
                subtitle = "Add due dates to tasks to organize them here"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (todayTasks.isNotEmpty()) {
                    item { SectionHeader(title = "Today") }
                    items(todayTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            isBlocked = blockedTaskIds.contains(task.id),
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onClick = { onTaskClick(task.id) }
                        )
                    }
                }

                if (upcomingTasks.isNotEmpty()) {
                    item { SectionHeader(title = "Upcoming") }
                    items(upcomingTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            isBlocked = blockedTaskIds.contains(task.id),
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onClick = { onTaskClick(task.id) }
                        )
                    }
                }

                if (unscheduledTasks.isNotEmpty()) {
                    item { SectionHeader(title = "Unscheduled") }
                    items(unscheduledTasks, key = { it.id }) { task ->
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
}
