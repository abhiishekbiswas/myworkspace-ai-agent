package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    greeting: String = "Hello!",
    dateString: String = "",
    unreadNotifications: Int = 0,
    unreadChats: Int = 0,
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onChatClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (dateString.isNotBlank()) {
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.testTag("topbar_search_button")
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            if (onChatClick != null) {
                IconButton(
                    onClick = onChatClick,
                    modifier = Modifier.testTag("topbar_chat_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadChats > 0) {
                                Badge { Text("$unreadChats") }
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Chat, contentDescription = "Chats")
                    }
                }
            }
            if (onSettingsClick != null) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.testTag("topbar_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}
