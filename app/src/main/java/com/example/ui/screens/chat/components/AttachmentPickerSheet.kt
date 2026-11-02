package com.example.ui.screens.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AttachmentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentPickerSheet(
    onDismiss: () -> Unit,
    onPickType: (AttachmentType) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Share Content",
                style = MaterialTheme.typography.titleMedium
            )
            ListItem(
                headlineContent = { Text("Document / Note") },
                leadingContent = { Icon(Icons.Default.Description, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("attach_document")
                    .clickable {
                        onDismiss()
                        onPickType(AttachmentType.DOCUMENT)
                    }
            )
            ListItem(
                headlineContent = { Text("Image / Photo") },
                leadingContent = { Icon(Icons.Default.Image, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("attach_image")
                    .clickable {
                        onDismiss()
                        onPickType(AttachmentType.IMAGE)
                    }
            )
            ListItem(
                headlineContent = { Text("Audio Note") },
                leadingContent = { Icon(Icons.Default.Mic, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("attach_audio")
                    .clickable {
                        onDismiss()
                        onPickType(AttachmentType.AUDIO)
                    }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
