package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "conversations",
    indices = [
        Index("lastMessageTimestamp"),
        Index("isPinned")
    ]
)
data class ConversationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("conversationId"),
        Index("createdAt"),
        Index("replyToMessageId")
    ]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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

@Entity(
    tableName = "message_attachments",
    foreignKeys = [
        ForeignKey(
            entity = ChatMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("messageId")
    ]
)
data class MessageAttachmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val messageId: String,
    val fileName: String,
    val fileUri: String,
    val fileType: String,
    val fileSizeBytes: Long,
    val thumbnailUri: String? = null,
    val durationMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "contacts",
    indices = [
        Index("name"),
        Index("isDemo")
    ]
)
data class ContactEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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
