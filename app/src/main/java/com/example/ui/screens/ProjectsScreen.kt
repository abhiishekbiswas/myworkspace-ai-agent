package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CreateEditProjectDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProjectCard
import com.example.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: WorkspaceViewModel,
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProjects by viewModel.activeProjects.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects") },
                actions = {
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("new_project_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Project")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("fab_add_project")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Project")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (activeProjects.isEmpty()) {
            EmptyStateView(
                title = "No projects yet",
                subtitle = "Organize tasks by creating your first project"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeProjects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onProjectClick(project.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateEditProjectDialog(
            categories = categories,
            onDismiss = { showCreateDialog = false },
            onSave = { name, desc, catId, color ->
                showCreateDialog = false
                viewModel.createProject(name, desc, catId, color)
            }
        )
    }
}
