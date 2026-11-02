package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: WorkspaceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var backupJson by remember { mutableStateOf("") }
    var restoreInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Export Workspace", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Generate a JSON snapshot of all your projects, tasks, and notes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.exportBackup { json ->
                                backupJson = json
                                statusMessage = "Backup generated (${json.length} bytes)"
                            }
                        },
                        modifier = Modifier.testTag("generate_backup_button")
                    ) {
                        Text("Generate Backup JSON")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Restore Workspace", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreInput,
                        onValueChange = { restoreInput = it },
                        label = { Text("Paste Backup JSON") },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_json_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (restoreInput.isNotBlank()) {
                                viewModel.restoreBackup(restoreInput) { result ->
                                    statusMessage = if (result.isSuccess) {
                                        "Backup restored successfully!"
                                    } else {
                                        "Failed to restore backup: ${result.exceptionOrNull()?.message}"
                                    }
                                }
                            }
                        },
                        enabled = restoreInput.isNotBlank(),
                        modifier = Modifier.testTag("restore_backup_button")
                    ) {
                        Text("Restore Data")
                    }
                }
            }

            if (statusMessage != null) {
                Text(
                    text = statusMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
