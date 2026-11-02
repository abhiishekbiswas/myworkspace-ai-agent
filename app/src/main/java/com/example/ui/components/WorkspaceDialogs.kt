package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditProjectDialog(
    initialProject: ProjectEntity? = null,
    categories: List<CategoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, categoryId: String?, color: String) -> Unit
) {
    var name by remember { mutableStateOf(initialProject?.name ?: "") }
    var description by remember { mutableStateOf(initialProject?.description ?: "") }
    var selectedCategoryId by remember { mutableStateOf(initialProject?.categoryId) }
    var selectedColor by remember { mutableStateOf(initialProject?.color ?: "#4CAF50") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialProject == null) "Create Project" else "Edit Project")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_desc_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), description.trim(), selectedCategoryId, selectedColor)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_project_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_project_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditTaskDialog(
    initialTask: TaskEntity? = null,
    projects: List<ProjectEntity> = emptyList(),
    defaultProjectId: String? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, projectId: String?, priority: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var selectedProjectId by remember { mutableStateOf(initialTask?.projectId ?: defaultProjectId) }
    var selectedPriority by remember { mutableStateOf(initialTask?.priority ?: "MEDIUM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialTask == null) "Create Task" else "Edit Task")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_desc_input")
                )
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("LOW", "MEDIUM", "HIGH", "CRITICAL").forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), description.trim(), selectedProjectId, selectedPriority)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_task_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ProgressUpdateDialog(
    currentProgress: Int,
    onDismiss: () -> Unit,
    onUpdate: (Int) -> Unit
) {
    var progress by remember { mutableFloatStateOf(currentProgress.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Progress") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${progress.toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Slider(
                    value = progress,
                    onValueChange = { progress = it },
                    valueRange = 0f..100f,
                    steps = 19
                )
            }
        },
        confirmButton = {
            Button(onClick = { onUpdate(progress.toInt()) }) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
