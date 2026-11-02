package com.example.domain.model

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ, FAILED
}

enum class MessageType {
    TEXT, IMAGE, FILE, AUDIO, SYSTEM
}

enum class AttachmentType {
    IMAGE, DOCUMENT, AUDIO
}

data class Conversation(
    val id: String,
    val title: String,
    val isGroup: Boolean = false,
    val avatarUrl: String? = null,
    val avatarColor: String = "#10B981",
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val unreadCount: Int = 0,
    val lastMessageId: String? = null,
    val lastMessageText: String? = null,
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val messageType: String = "TEXT",
    val status: String = "SENT",
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToText: String? = null,
    val isStarred: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class MessageAttachment(
    val id: String,
    val messageId: String,
    val fileName: String,
    val fileUri: String,
    val fileType: String,
    val fileSizeBytes: Long,
    val thumbnailUri: String? = null,
    val durationMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class Contact(
    val id: String,
    val name: String,
    val role: String = "Team Member",
    val email: String? = null,
    val phoneNumber: String? = null,
    val avatarColor: String = "#6366F1",
    val initials: String = "TM",
    val statusBio: String = "Available",
    val isDemo: Boolean = false,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
