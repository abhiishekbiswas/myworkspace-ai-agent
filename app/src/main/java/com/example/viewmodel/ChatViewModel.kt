package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.domain.DisambiguationCandidate
import com.example.ai.domain.ToolExecutionResult
import com.example.ai.engine.LocalCommandEngine
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.ConversationEntity
import com.example.domain.repository.IChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: IChatRepository,
    private val localCommandEngine: LocalCommandEngine? = null
) : ViewModel() {

    companion object {
        const val AI_ASSISTANT_CONVERSATION_ID = "ai-assistant-conversation-id"
        const val AI_ASSISTANT_CONTACT_ID = "ai-assistant-contact-id"
        const val AI_ASSISTANT_SENDER_ID = "AI_ASSISTANT"
        const val AI_ASSISTANT_NAME = "Personal AI Assistant"
    }

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _pendingClarification = MutableStateFlow<ToolExecutionResult.ClarificationRequired?>(null)
    val pendingClarification: StateFlow<ToolExecutionResult.ClarificationRequired?> = _pendingClarification.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<ToolExecutionResult.ConfirmationRequired?>(null)
    val pendingConfirmation: StateFlow<ToolExecutionResult.ConfirmationRequired?> = _pendingConfirmation.asStateFlow()

    val allConversations: StateFlow<List<ConversationEntity>> = chatRepository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allContacts: StateFlow<List<ContactEntity>> = chatRepository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ChatFilter.ALL)
    val selectedFilter: StateFlow<ChatFilter> = _selectedFilter.asStateFlow()

    val filteredConversations: StateFlow<List<ConversationEntity>> = combine(
        allConversations,
        _searchQuery,
        _selectedFilter
    ) { convs, query, filter ->
        convs.filter { conv ->
            val matchesQuery = query.isBlank() || conv.title.contains(query, ignoreCase = true) ||
                    (conv.lastMessageText?.contains(query, ignoreCase = true) == true)
            val matchesFilter = when (filter) {
                ChatFilter.ALL -> true
                ChatFilter.UNREAD -> conv.unreadCount > 0
                ChatFilter.PINNED -> conv.isPinned
                ChatFilter.DIRECT -> !conv.isGroup
                ChatFilter.CHANNELS -> conv.isGroup
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadConversationsCount: StateFlow<Int> = allConversations.map { convs ->
        convs.count { it.unreadCount > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    val selectedConversation: StateFlow<ConversationEntity?> = _selectedConversationId.flatMapLatest { id ->
        if (id == null) flowOf(null) else chatRepository.getConversationById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentConversation: StateFlow<ConversationEntity?> get() = selectedConversation

    val currentMessages: StateFlow<List<ChatMessageEntity>> = _selectedConversationId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else chatRepository.getMessagesForConversation(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _inConversationSearchQuery = MutableStateFlow("")
    val inConversationSearchQuery: StateFlow<String> = _inConversationSearchQuery.asStateFlow()

    val displayedMessages: StateFlow<List<ChatMessageEntity>> = combine(
        currentMessages,
        _inConversationSearchQuery
    ) { msgs, q ->
        if (q.isBlank()) msgs else msgs.filter { it.text.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _draftMessageText = MutableStateFlow("")
    val draftMessageText: StateFlow<String> = _draftMessageText.asStateFlow()

    private val _replyingToMessage = MutableStateFlow<ChatMessageEntity?>(null)
    val replyingToMessage: StateFlow<ChatMessageEntity?> = _replyingToMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setChatFilter(filter: ChatFilter) {
        _selectedFilter.value = filter
    }

    fun setInConversationSearchQuery(query: String) {
        _inConversationSearchQuery.value = query
    }

    fun setDraftMessageText(text: String) {
        _draftMessageText.value = text
    }

    fun selectReplyToMessage(message: ChatMessageEntity) {
        _replyingToMessage.value = message
    }

    fun clearReplySelection() {
        _replyingToMessage.value = null
    }

    fun selectConversation(conversationId: String) {
        _selectedConversationId.value = conversationId
        _inConversationSearchQuery.value = ""
        _draftMessageText.value = ""
        _replyingToMessage.value = null
        viewModelScope.launch {
            chatRepository.markMessagesLocallyRead(conversationId)
        }
    }

    fun createConversation(
        title: String,
        isGroup: Boolean = false,
        avatarColor: String = "#10B981",
        onResult: (Result<ConversationEntity>) -> Unit = {}
    ) = viewModelScope.launch {
        try {
            val conv = chatRepository.createConversation(title, isGroup, avatarColor)
            onResult(Result.success(conv))
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun sendLocalTextMessage(
        text: String,
        senderId: String = "USER",
        senderName: String = "You",
        onResult: (Result<ChatMessageEntity>) -> Unit = {}
    ) = viewModelScope.launch {
        val convId = _selectedConversationId.value ?: return@launch
        val clean = text.trim()
        if (clean.isEmpty()) return@launch

        try {
            val msg = chatRepository.sendLocalTextMessage(convId, clean, senderId, senderName)
            _draftMessageText.value = ""
            onResult(Result.success(msg))

            if (convId == AI_ASSISTANT_CONVERSATION_ID && localCommandEngine != null) {
                processAiMessage(clean)
            }
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun processAiMessage(prompt: String) = viewModelScope.launch {
        if (localCommandEngine == null) return@launch
        _isAiThinking.value = true
        try {
            val result = localCommandEngine.execute(prompt)
            when (result.toolResult) {
                is ToolExecutionResult.ClarificationRequired -> {
                    _pendingClarification.value = result.toolResult
                }
                is ToolExecutionResult.ConfirmationRequired -> {
                    _pendingConfirmation.value = result.toolResult
                }
                else -> {
                    _pendingClarification.value = null
                    _pendingConfirmation.value = null
                }
            }

            chatRepository.sendLocalTextMessage(
                conversationId = AI_ASSISTANT_CONVERSATION_ID,
                text = result.text,
                senderId = AI_ASSISTANT_SENDER_ID,
                senderName = AI_ASSISTANT_NAME
            )
        } catch (e: Exception) {
            chatRepository.sendLocalTextMessage(
                conversationId = AI_ASSISTANT_CONVERSATION_ID,
                text = "❌ Error processing command: ${e.message}",
                senderId = AI_ASSISTANT_SENDER_ID,
                senderName = AI_ASSISTANT_NAME
            )
        } finally {
            _isAiThinking.value = false
        }
    }

    fun confirmPendingAction(confirmed: Boolean) = viewModelScope.launch {
        val action = _pendingConfirmation.value ?: return@launch
        _pendingConfirmation.value = null
        if (!confirmed) {
            chatRepository.sendLocalTextMessage(
                conversationId = AI_ASSISTANT_CONVERSATION_ID,
                text = "Action cancelled.",
                senderId = AI_ASSISTANT_SENDER_ID,
                senderName = AI_ASSISTANT_NAME
            )
            return@launch
        }
        if (action.action.actionType == "FORCE_COMPLETE_TASK") {
            processAiMessage("complete task ${action.action.entityId}")
        }
    }

    fun selectClarificationCandidate(candidate: DisambiguationCandidate) = viewModelScope.launch {
        _pendingClarification.value = null
        processAiMessage("task id ${candidate.id}")
    }

    fun clearPendingAiPrompts() {
        _pendingClarification.value = null
        _pendingConfirmation.value = null
    }

    fun replyToMessage(
        originalMessage: ChatMessageEntity,
        replyText: String,
        senderId: String = "USER",
        senderName: String = "You",
        onResult: (Result<ChatMessageEntity>) -> Unit = {}
    ) = viewModelScope.launch {
        val convId = _selectedConversationId.value ?: return@launch
        try {
            val msg = chatRepository.replyToMessage(
                conversationId = convId,
                text = replyText,
                replyToMessageId = originalMessage.id,
                replyToSenderName = originalMessage.senderName,
                replyToText = originalMessage.text
            )
            _replyingToMessage.value = null
            onResult(Result.success(msg))
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun deleteLocalMessage(messageId: String) = viewModelScope.launch {
        chatRepository.deleteLocalMessage(messageId)
    }

    fun setConversationPinned(conversationId: String, isPinned: Boolean) = viewModelScope.launch {
        chatRepository.setConversationPinned(conversationId, isPinned)
    }

    fun togglePinConversation(conversationId: String) = viewModelScope.launch {
        val conv = allConversations.value.firstOrNull { it.id == conversationId } ?: return@launch
        chatRepository.setConversationPinned(conversationId, !conv.isPinned)
    }

    fun setConversationMuted(conversationId: String, isMuted: Boolean) = viewModelScope.launch {
        chatRepository.setConversationMuted(conversationId, isMuted)
    }

    fun openConversation(id: String) = selectConversation(id)

    fun sendMessage(text: String) = sendLocalTextMessage(text)

    fun setFilter(filter: ChatFilter) = setChatFilter(filter)

    fun dismissClarification() = clearPendingAiPrompts()

    fun clearConversationMessages(conversationId: String) = viewModelScope.launch {
        val msgs = chatRepository.getMessagesForConversation(conversationId).first()
        for (m in msgs) {
            chatRepository.deleteLocalMessage(m.id)
        }
    }

    init {
        // Gate creation of assistant conversation only when engine is provided
        if (localCommandEngine != null) {
            viewModelScope.launch {
                ensureAiAssistantConversationExists()
            }
        }
    }

    private suspend fun ensureAiAssistantConversationExists() {
        val conversations = chatRepository.allConversations.first()
        if (conversations.none { it.id == AI_ASSISTANT_CONVERSATION_ID }) {
            val conv = ConversationEntity(
                id = AI_ASSISTANT_CONVERSATION_ID,
                title = AI_ASSISTANT_NAME,
                isGroup = false,
                avatarColor = "#6366F1",
                isPinned = true,
                lastMessageText = "Namaste! I am your Workspace AI Assistant. Ask me anything in Hindi, Hinglish or English.",
                lastMessageTimestamp = System.currentTimeMillis()
            )
            chatRepository.saveConversation(conv)
            chatRepository.sendLocalTextMessage(
                conversationId = AI_ASSISTANT_CONVERSATION_ID,
                text = "Namaste! I am your personal Workspace Assistant. You can ask me:\n" +
                        "• 'आज कितने काम बाकी हैं?' or 'Aaj ke pending tasks batao'\n" +
                        "• 'Create a task called Electricity Bill'\n" +
                        "• 'Project Alpha ka status kya hai?'\n" +
                        "• 'Yaad rakhna mujhe weekly reports pasand hain'",
                senderId = AI_ASSISTANT_SENDER_ID,
                senderName = AI_ASSISTANT_NAME
            )
        }
    }
}

class ChatViewModelFactory(
    private val chatRepository: IChatRepository,
    private val localCommandEngine: LocalCommandEngine? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(chatRepository, localCommandEngine) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
