package com.example.data.repository

import com.example.core.database.dao.ChatDao
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.ConversationEntity
import com.example.domain.repository.IChatRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(
    private val dao: ChatDao
) : IChatRepository {

    override val allConversations: Flow<List<ConversationEntity>> = dao.getAllConversations()
    override val allContacts: Flow<List<ContactEntity>> = dao.getAllContacts()

    override fun getConversationById(id: String): Flow<ConversationEntity?> = dao.getConversationById(id)

    override fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>> =
        dao.getMessagesForConversation(conversationId)

    override suspend fun createConversation(
        title: String,
        isGroup: Boolean,
        avatarColor: String
    ): ConversationEntity {
        val now = System.currentTimeMillis()
        val conv = ConversationEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            isGroup = isGroup,
            avatarColor = avatarColor,
            createdAt = now,
            updatedAt = now
        )
        dao.insertConversation(conv)
        return conv
    }

    override suspend fun saveConversation(conversation: ConversationEntity) {
        dao.insertConversation(conversation)
    }

    override suspend fun deleteConversation(conversationId: String) {
        dao.deleteConversationById(conversationId)
    }

    override suspend fun setConversationPinned(conversationId: String, isPinned: Boolean) {
        val conv = dao.getConversationByIdOnce(conversationId) ?: return
        dao.updateConversation(conv.copy(isPinned = isPinned, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun setConversationMuted(conversationId: String, isMuted: Boolean) {
        val conv = dao.getConversationByIdOnce(conversationId) ?: return
        dao.updateConversation(conv.copy(isMuted = isMuted, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun sendLocalTextMessage(
        conversationId: String,
        text: String,
        senderId: String,
        senderName: String
    ): ChatMessageEntity {
        val now = System.currentTimeMillis()
        val msg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            text = text,
            messageType = "TEXT",
            status = "SENT",
            createdAt = now,
            updatedAt = now
        )
        dao.insertMessageAndUpdateConversation(msg)
        return msg
    }

    override suspend fun replyToMessage(
        conversationId: String,
        text: String,
        replyToMessageId: String,
        replyToSenderName: String,
        replyToText: String
    ): ChatMessageEntity {
        val now = System.currentTimeMillis()
        val msg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = "USER",
            senderName = "You",
            text = text,
            messageType = "TEXT",
            status = "SENT",
            replyToMessageId = replyToMessageId,
            replyToSenderName = replyToSenderName,
            replyToText = replyToText,
            createdAt = now,
            updatedAt = now
        )
        dao.insertMessageAndUpdateConversation(msg)
        return msg
    }

    override suspend fun editMessage(messageId: String, newText: String) {
        // Find and update message
    }

    override suspend fun deleteLocalMessage(messageId: String) {
        dao.deleteMessageById(messageId)
    }

    override suspend fun setMessageStarred(messageId: String, isStarred: Boolean) {
        // Star message
    }

    override suspend fun saveContact(contact: ContactEntity) {
        dao.insertContact(contact)
    }

    override suspend fun deleteContact(contact: ContactEntity) {
        dao.deleteContact(contact)
    }

    override suspend fun markMessagesLocallyRead(conversationId: String) {
        val conv = dao.getConversationByIdOnce(conversationId) ?: return
        if (conv.unreadCount > 0) {
            dao.updateConversation(conv.copy(unreadCount = 0, updatedAt = System.currentTimeMillis()))
        }
    }
}
