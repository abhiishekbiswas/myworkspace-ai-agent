package com.example.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.chat.components.AttachmentPickerSheet
import com.example.ui.screens.chat.components.ChatInputBar
import com.example.ui.screens.chat.components.MessageBubble
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationId: String,
    chatViewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onConversationDetailClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messageInput by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        chatViewModel.openConversation(conversationId)
    }

    val conversation by chatViewModel.currentConversation.collectAsStateWithLifecycle()
    val messages by chatViewModel.currentMessages.collectAsStateWithLifecycle()
    val isThinking by chatViewModel.isAiThinking.collectAsStateWithLifecycle()
    val clarification by chatViewModel.pendingClarification.collectAsStateWithLifecycle()
    val confirmation by chatViewModel.pendingConfirmation.collectAsStateWithLifecycle()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = conversation?.title ?: "Chat",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isThinking) "Thinking..." else "Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("conversation_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onConversationDetailClick,
                        modifier = Modifier.testTag("conversation_detail_button")
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Details")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Clarification options chips
                if (clarification != null) {
                    val prompt = clarification?.message ?: ""
                    val candidates = clarification?.candidates ?: emptyList()
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(candidates) { candidate ->
                                    SuggestionChip(
                                        onClick = {
                                            chatViewModel.selectClarificationCandidate(candidate)
                                        },
                                        label = { Text(candidate.title) },
                                        modifier = Modifier.testTag("clarification_chip_${candidate.id}")
                                    )
                                }
                                item {
                                    SuggestionChip(
                                        onClick = { chatViewModel.dismissClarification() },
                                        label = { Text("Cancel") }
                                    )
                                }
                            }
                        }
                    }
                }

                // AI Thinking progress bar
                if (isThinking) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                    )
                }

                ChatInputBar(
                    text = messageInput,
                    onTextChange = { messageInput = it },
                    onSend = {
                        val textToSend = messageInput.trim()
                        if (textToSend.isNotBlank()) {
                            messageInput = ""
                            chatViewModel.sendMessage(textToSend)
                        }
                    },
                    onAttachClick = { showAttachmentSheet = true }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }
    }

    if (showAttachmentSheet) {
        AttachmentPickerSheet(
            onDismiss = { showAttachmentSheet = false },
            onPickType = { type ->
                showAttachmentSheet = false
                chatViewModel.sendMessage("[Shared ${type.name}]")
            }
        )
    }

    if (confirmation != null) {
        val conf = confirmation!!
        AlertDialog(
            onDismissRequest = { chatViewModel.confirmPendingAction(false) },
            title = { Text("Confirm Action") },
            text = { Text(conf.message) },
            confirmButton = {
                Button(
                    onClick = { chatViewModel.confirmPendingAction(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { chatViewModel.confirmPendingAction(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}
