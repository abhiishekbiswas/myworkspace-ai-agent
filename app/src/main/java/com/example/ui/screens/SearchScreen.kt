package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProjectCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TaskCard
import com.example.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: WorkspaceViewModel,
    onTaskClick: (String) -> Unit,
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val allProjects by viewModel.activeProjects.collectAsStateWithLifecycle()
    val blockedTaskIds by viewModel.blockedTaskIds.collectAsStateWithLifecycle()

    val matchingTasks = remember(query, allTasks) {
        if (query.isBlank()) emptyList()
        else allTasks.filter {
            it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
        }
    }

    val matchingProjects = remember(query, allProjects) {
        if (query.isBlank()) emptyList()
        else allProjects.filter {
            it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Search Workspace") })
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search tasks and projects...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("workspace_search_input")
            )

            if (query.isNotBlank() && matchingTasks.isEmpty() && matchingProjects.isEmpty()) {
                EmptyStateView(
                    title = "No results found",
                    subtitle = "Try searching with different keywords"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (matchingProjects.isNotEmpty()) {
                        item { SectionHeader(title = "Projects (${matchingProjects.size})") }
                        items(matchingProjects, key = { it.id }) { proj ->
                            ProjectCard(
                                project = proj,
                                onClick = { onProjectClick(proj.id) }
                            )
                        }
                    }

                    if (matchingTasks.isNotEmpty()) {
                        item { SectionHeader(title = "Tasks (${matchingTasks.size})") }
                        items(matchingTasks, key = { it.id }) { task ->
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
}
