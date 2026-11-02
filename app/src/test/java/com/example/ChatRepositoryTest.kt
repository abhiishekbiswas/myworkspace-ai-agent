package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.WorkspaceDatabase
import com.example.data.local.entity.ContactEntity
import com.example.data.repository.ChatRepository
import com.example.domain.repository.IChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatRepositoryTest {

  private lateinit var database: WorkspaceDatabase
  private lateinit var repository: IChatRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = WorkspaceDatabase.createInMemory(context)
    repository = ChatRepository(database.chatDao())
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun testConversationCreationAndDuplicateHandling() = runBlocking {
    // 1. Valid conversation creation
    val createResult = repository.createConversation(
      title = "Design Sync",
      isGroup = false,
      avatarColor = "#8B5CF6"
    )
    assertTrue("Should succeed creating conversation", createResult.isSuccess)
    val conv = createResult.getOrThrow()
    assertEquals("Design Sync", conv.title)
    assertFalse(conv.isGroup)

    // 2. Duplicate 1-on-1 conversation handling: should return existing conversation idempotently
    val duplicateResult = repository.createConversation(
      title = "Design Sync",
      isGroup = false
    )
    assertTrue("Duplicate 1-on-1 should resolve safely", duplicateResult.isSuccess)
    assertEquals(conv.id, duplicateResult.getOrThrow().id)

    // 3. Blank title rejected
    val emptyResult = repository.createConversation(title = "   ")
    assertTrue("Blank title must be rejected", emptyResult.isFailure)
  }

  @Test
  fun testPinningMutingAndUnreadCount() = runBlocking {
    val conv = repository.createConversation("Alpha Channel", isGroup = true).getOrThrow()

    // Pinning
    val pinRes = repository.setConversationPinned(conv.id, true)
    assertTrue(pinRes.isSuccess)
    val pinned = repository.getConversationByIdOnce(conv.id)
    assertTrue(pinned?.isPinned == true)

    // Muting
    val muteRes = repository.setConversationMuted(conv.id, true)
    assertTrue(muteRes.isSuccess)
    val muted = repository.getConversationByIdOnce(conv.id)
    assertTrue(muted?.isMuted == true)

    // Unread count reset
    val resetRes = repository.resetUnreadCount(conv.id)
    assertTrue(resetRes.isSuccess)
    val resetConv = repository.getConversationByIdOnce(conv.id)
    assertEquals(0, resetConv?.unreadCount)

    // Non-existent conversation handling
    val fakePinRes = repository.setConversationPinned(UUID.randomUUID().toString(), true)
    assertTrue("Setting pin on non-existent conversation must fail", fakePinRes.isFailure)
  }

  @Test
  fun testMessageInsertionChronologicalOrderAndAtomicPreview() = runBlocking {
    val conv = repository.createConversation("Engineering Daily").getOrThrow()

    // 1. Send first message
    val send1Res = repository.sendLocalTextMessage(
      conversationId = conv.id,
      text = "Starting the morning review",
      senderId = "user_bob",
      senderName = "Bob"
    )
    assertTrue(send1Res.isSuccess)
    val msg1 = send1Res.getOrThrow()
    assertEquals("Starting the morning review", msg1.text)
    assertEquals("LOCAL_SENT", msg1.status)

    // Verify conversation last message preview updated atomically
    var updatedConv = repository.getConversationByIdOnce(conv.id)
    assertEquals(msg1.id, updatedConv?.lastMessageId)
    assertEquals("Starting the morning review", updatedConv?.lastMessageText)

    // 2. Send second message
    val send2Res = repository.sendLocalTextMessage(
      conversationId = conv.id,
      text = "All unit tests are passing now.",
      senderId = "me",
      senderName = "Me"
    )
    assertTrue(send2Res.isSuccess)
    val msg2 = send2Res.getOrThrow()

    // Verify second message updated conversation preview atomically
    updatedConv = repository.getConversationByIdOnce(conv.id)
    assertEquals(msg2.id, updatedConv?.lastMessageId)
    assertEquals("All unit tests are passing now.", updatedConv?.lastMessageText)

    // 3. Verify chronological message ordering
    val messages = repository.getMessagesForConversationOnce(conv.id)
    assertEquals(2, messages.size)
    assertEquals(msg1.id, messages[0].id)
    assertEquals(msg2.id, messages[1].id)
    assertTrue(messages[0].createdAt <= messages[1].createdAt)
  }

  @Test
  fun testReplyLinking() = runBlocking {
    val conv = repository.createConversation("Feature Discussions").getOrThrow()

    val originalMsg = repository.sendLocalTextMessage(
      conversationId = conv.id,
      text = "Can we use Room for local storage?",
      senderId = "user_sarah",
      senderName = "Sarah"
    ).getOrThrow()

    // Reply to original message
    val replyResult = repository.replyToMessage(
      conversationId = conv.id,
      originalMessageId = originalMsg.id,
      replyText = "Yes, Room is already configured and verified.",
      senderId = "me",
      senderName = "Me"
    )
    assertTrue("Reply must succeed", replyResult.isSuccess)
    val replyMsg = replyResult.getOrThrow()

    // Verify reply reference linking
    assertEquals(originalMsg.id, replyMsg.replyToMessageId)
    assertEquals("Sarah", replyMsg.replyToSenderName)
    assertEquals("Can we use Room for local storage?", replyMsg.replyToText)

    // Non-existent original message reply
    val invalidReply = repository.replyToMessage(
      conversationId = conv.id,
      originalMessageId = UUID.randomUUID().toString(),
      replyText = "Ghost reply"
    )
    assertTrue("Replying to non-existent message must fail", invalidReply.isFailure)
  }

  @Test
  fun testMessageEditingDeletionAndStarring() = runBlocking {
    val conv = repository.createConversation("Draft Notes").getOrThrow()

    val msg = repository.sendLocalTextMessage(
      conversationId = conv.id,
      text = "Initial typo text"
    ).getOrThrow()

    // Star message
    val starRes = repository.setMessageStarred(msg.id, true)
    assertTrue(starRes.isSuccess)

    // Edit message
    val editRes = repository.editMessage(msg.id, "Corrected polished text")
    assertTrue(editRes.isSuccess)
    val edited = editRes.getOrThrow()
    assertEquals("Corrected polished text", edited.text)

    // Verify conversation last message preview updated with edited text
    val convAfterEdit = repository.getConversationByIdOnce(conv.id)
    assertEquals("Corrected polished text", convAfterEdit?.lastMessageText)

    // Delete message
    val delRes = repository.deleteLocalMessage(msg.id)
    assertTrue(delRes.isSuccess)
    val remaining = repository.getMessagesForConversationOnce(conv.id)
    assertTrue("Message list should be empty after deletion", remaining.isEmpty())
  }

  @Test
  fun testSearchFunctionality() = runBlocking {
    val conv1 = repository.createConversation("Frontend Architecture").getOrThrow()
    val conv2 = repository.createConversation("Backend Microservices").getOrThrow()

    repository.sendLocalTextMessage(conv1.id, "Jetpack Compose M3 implementation")
    repository.sendLocalTextMessage(conv2.id, "SQLite Room database indexing")

    // Search conversations
    val foundConvs = repository.searchConversations("Frontend").first()
    assertEquals(1, foundConvs.size)
    assertEquals("Frontend Architecture", foundConvs[0].title)

    // Search messages globally
    val foundMsgs = repository.searchMessages("Compose").first()
    assertEquals(1, foundMsgs.size)
    assertEquals("Jetpack Compose M3 implementation", foundMsgs[0].text)

    // Search messages in specific conversation
    val conv1Msgs = repository.searchMessagesInConversation(conv1.id, "indexing").first()
    assertTrue("Conv 1 should not contain 'indexing'", conv1Msgs.isEmpty())

    val conv2Msgs = repository.searchMessagesInConversation(conv2.id, "indexing").first()
    assertEquals(1, conv2Msgs.size)
  }

  @Test
  fun testMarkMessagesLocallyRead() = runBlocking {
    val conv = repository.createConversation("Notifications Channel").getOrThrow()

    repository.sendLocalTextMessage(conv.id, "Hello from peer", senderId = "peer", senderName = "Peer")
    val readRes = repository.markMessagesAsLocallyRead(conv.id)
    assertTrue(readRes.isSuccess)

    val messages = repository.getMessagesForConversationOnce(conv.id)
    assertEquals(1, messages.size)
    assertEquals("LOCAL_READ", messages[0].status)
  }

  @Test
  fun testContactDemoDistinction() = runBlocking {
    val realContact = ContactEntity(
      id = UUID.randomUUID().toString(),
      name = "Neha Sharma",
      role = "QA Engineer",
      isDemo = false
    )
    val demoContact = ContactEntity(
      id = UUID.randomUUID().toString(),
      name = "Demo Preview Contact",
      role = "Sample Persona",
      isDemo = true
    )

    repository.saveContact(realContact)
    repository.saveContact(demoContact)

    val contacts = repository.allContacts.first()
    assertEquals(2, contacts.size)

    val real = contacts.find { it.name == "Neha Sharma" }
    val demo = contacts.find { it.name == "Demo Preview Contact" }

    assertNotNull(real)
    assertNotNull(demo)
    assertFalse("Real contact must have isDemo = false", real!!.isDemo)
    assertTrue("Demo contact must have isDemo = true", demo!!.isDemo)
  }

  @Test
  fun testAttachmentMetadataPersistence() = runBlocking {
    val conv = repository.createConversation("Media Sharing").getOrThrow()
    val msg = repository.sendLocalTextMessage(conv.id, "See attached diagram").getOrThrow()

    val attachResult = repository.saveAttachmentMetadata(
      messageId = msg.id,
      fileName = "architecture_diagram.png",
      fileUri = "content://media/architecture_diagram.png",
      fileType = "image/png",
      fileSizeBytes = 512000L
    )
    assertTrue("Attachment metadata should save successfully", attachResult.isSuccess)
    val attachment = attachResult.getOrThrow()
    assertEquals("architecture_diagram.png", attachment.fileName)

    val fetched = repository.getAttachmentsForMessage(msg.id).first()
    assertEquals(1, fetched.size)
    assertEquals(attachment.id, fetched[0].id)

    // Delete attachment
    val delAttachRes = repository.deleteAttachment(attachment.id)
    assertTrue(delAttachRes.isSuccess)
    val afterDel = repository.getAttachmentsForMessage(msg.id).first()
    assertTrue(afterDel.isEmpty())
  }

  @Test
  fun testInvalidConversationHandling() = runBlocking {
    val invalidId = UUID.randomUUID().toString()

    val sendRes = repository.sendLocalTextMessage(invalidId, "Orphan message")
    assertTrue("Sending to non-existent conversation must fail", sendRes.isFailure)

    val delConvRes = repository.deleteConversation(invalidId)
    assertTrue("Deleting non-existent conversation must fail", delConvRes.isFailure)
  }
}
