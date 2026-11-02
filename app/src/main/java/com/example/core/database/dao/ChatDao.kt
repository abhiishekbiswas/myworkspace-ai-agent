package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

  // --- CONVERSATIONS ---

  @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
  fun getAllConversations(): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
  suspend fun getAllConversationsOnce(): List<ConversationEntity>

  @Query("SELECT * FROM conversations WHERE id = :id")
  fun getConversationById(id: String): Flow<ConversationEntity?>

  @Query("SELECT * FROM conversations WHERE id = :id")
  suspend fun getConversationByIdOnce(id: String): ConversationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConversation(conversation: ConversationEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConversationsBulk(conversations: List<ConversationEntity>)

  @Update
  suspend fun updateConversation(conversation: ConversationEntity)

  @Query("UPDATE conversations SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :id")
  suspend fun setConversationPinned(id: String, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE conversations SET isMuted = :isMuted, updatedAt = :updatedAt WHERE id = :id")
  suspend fun setConversationMuted(id: String, isMuted: Boolean, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE conversations SET unreadCount = :unreadCount, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateUnreadCount(id: String, unreadCount: Int, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE conversations SET lastMessageId = :lastMessageId, lastMessageText = :lastMessageText, lastMessageTimestamp = :timestamp, updatedAt = :timestamp WHERE id = :conversationId")
  suspend fun updateConversationLastMessage(
    conversationId: String,
    lastMessageId: String,
    lastMessageText: String,
    timestamp: Long
  )

  @Delete
  suspend fun deleteConversation(conversation: ConversationEntity)

  @Query("DELETE FROM conversations WHERE id = :id")
  suspend fun deleteConversationById(id: String)

  // --- MESSAGES ---

  @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
  fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>>

  @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
  suspend fun getMessagesForConversationOnce(conversationId: String): List<ChatMessageEntity>

  @Query("SELECT * FROM chat_messages WHERE id = :id")
  fun getMessageById(id: String): Flow<ChatMessageEntity?>

  @Query("SELECT * FROM chat_messages WHERE id = :id")
  suspend fun getMessageByIdOnce(id: String): ChatMessageEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: ChatMessageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessagesBulk(messages: List<ChatMessageEntity>)

  @Update
  suspend fun updateMessage(message: ChatMessageEntity)

  @Delete
  suspend fun deleteMessage(message: ChatMessageEntity)

  @Query("DELETE FROM chat_messages WHERE id = :id")
  suspend fun deleteMessageById(id: String)

  @Query("SELECT * FROM chat_messages WHERE text LIKE '%' || :query || '%' ORDER BY createdAt DESC")
  fun searchMessages(query: String): Flow<List<ChatMessageEntity>>

  @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId AND text LIKE '%' || :query || '%' ORDER BY createdAt DESC")
  fun searchMessagesInConversation(conversationId: String, query: String): Flow<List<ChatMessageEntity>>

  @Query("UPDATE chat_messages SET status = :status, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateMessageStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE chat_messages SET isStarred = :isStarred, updatedAt = :updatedAt WHERE id = :id")
  suspend fun setMessageStarred(id: String, isStarred: Boolean, updatedAt: Long = System.currentTimeMillis())

  @Transaction
  suspend fun insertMessageAndUpdateConversation(message: ChatMessageEntity) {
    insertMessage(message)
    updateConversationLastMessage(
      conversationId = message.conversationId,
      lastMessageId = message.id,
      lastMessageText = message.text,
      timestamp = message.createdAt
    )
  }

  // --- ATTACHMENTS ---

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttachment(attachment: MessageAttachmentEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttachmentsBulk(attachments: List<MessageAttachmentEntity>)

  @Query("SELECT * FROM message_attachments WHERE messageId = :messageId")
  fun getAttachmentsForMessage(messageId: String): Flow<List<MessageAttachmentEntity>>

  @Query("SELECT * FROM message_attachments WHERE messageId = :messageId")
  suspend fun getAttachmentsForMessageOnce(messageId: String): List<MessageAttachmentEntity>

  @Query("SELECT * FROM message_attachments WHERE id = :id")
  suspend fun getAttachmentByIdOnce(id: String): MessageAttachmentEntity?

  @Delete
  suspend fun deleteAttachment(attachment: MessageAttachmentEntity)

  @Query("DELETE FROM message_attachments WHERE id = :id")
  suspend fun deleteAttachmentById(id: String)

  @Query("DELETE FROM message_attachments WHERE messageId = :messageId")
  suspend fun deleteAttachmentsForMessage(messageId: String)

  // --- CONTACTS ---

  @Query("SELECT * FROM contacts ORDER BY name ASC")
  fun getAllContacts(): Flow<List<ContactEntity>>

  @Query("SELECT * FROM contacts ORDER BY name ASC")
  suspend fun getAllContactsOnce(): List<ContactEntity>

  @Query("SELECT * FROM contacts WHERE id = :id")
  fun getContactById(id: String): Flow<ContactEntity?>

  @Query("SELECT * FROM contacts WHERE id = :id")
  suspend fun getContactByIdOnce(id: String): ContactEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertContact(contact: ContactEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertContactsBulk(contacts: List<ContactEntity>)

  @Update
  suspend fun updateContact(contact: ContactEntity)

  @Query("SELECT * FROM contacts WHERE name LIKE '%' || :query || '%' OR role LIKE '%' || :query || '%' ORDER BY name ASC")
  fun searchContacts(query: String): Flow<List<ContactEntity>>

  @Delete
  suspend fun deleteContact(contact: ContactEntity)

  @Query("DELETE FROM contacts WHERE id = :id")
  suspend fun deleteContactById(id: String)
}
