package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddMenu(
    onDismiss: () -> Unit,
    onAddTask: () -> Unit,
    onAddProject: () -> Unit,
    onAddNote: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium
            )
            ListItem(
                headlineContent = { Text("New Task") },
                leadingContent = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_task")
                    .clickable {
                        onDismiss()
                        onAddTask()
                    }
            )
            ListItem(
                headlineContent = { Text("New Project") },
                leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_project")
                    .clickable {
                        onDismiss()
                        onAddProject()
                    }
            )
            ListItem(
                headlineContent = { Text("New Note") },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_note")
                    .clickable {
                        onDismiss()
                        onAddNote()
                    }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
