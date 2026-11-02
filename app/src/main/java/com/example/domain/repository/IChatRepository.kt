package com.example.domain.repository

import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

interface IChatRepository {
    val allConversations: Flow<List<ConversationEntity>>
    val allContacts: Flow<List<ContactEntity>>

    fun getConversationById(id: String): Flow<ConversationEntity?>
    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    suspend fun createConversation(
        title: String,
        isGroup: Boolean = false,
        avatarColor: String = "#10B981"
    ): ConversationEntity

    suspend fun saveConversation(conversation: ConversationEntity)
    suspend fun deleteConversation(conversationId: String)
    suspend fun setConversationPinned(conversationId: String, isPinned: Boolean)
    suspend fun setConversationMuted(conversationId: String, isMuted: Boolean)

    suspend fun sendLocalTextMessage(
        conversationId: String,
        text: String,
        senderId: String = "USER",
        senderName: String = "You"
    ): ChatMessageEntity

    suspend fun replyToMessage(
        conversationId: String,
        text: String,
        replyToMessageId: String,
        replyToSenderName: String,
        replyToText: String
    ): ChatMessageEntity

    suspend fun editMessage(messageId: String, newText: String)
    suspend fun deleteLocalMessage(messageId: String)
    suspend fun setMessageStarred(messageId: String, isStarred: Boolean)

    suspend fun saveContact(contact: ContactEntity)
    suspend fun deleteContact(contact: ContactEntity)
    suspend fun markMessagesLocallyRead(conversationId: String)
}
