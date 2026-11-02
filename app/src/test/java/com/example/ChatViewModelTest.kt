package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.WorkspaceDatabase
import com.example.data.repository.ChatRepository
import com.example.viewmodel.ChatFilter
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private lateinit var database: WorkspaceDatabase
  private lateinit var repository: ChatRepository
  private lateinit var viewModel: ChatViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = WorkspaceDatabase.createInMemory(context)
    repository = ChatRepository(database.chatDao())
    viewModel = ChatViewModel(repository)
  }

  @After
  fun tearDown() {
    database.close()
    Dispatchers.resetMain()
  }

  @Test
  fun testInitialConversationsState() = runBlocking {
    val conversations = viewModel.allConversations.first()
    assertTrue("Initial conversations should be empty", conversations.isEmpty())
    assertEquals(0, viewModel.unreadConversationsCount.first())
    assertEquals("", viewModel.searchQuery.value)
    assertEquals(ChatFilter.ALL, viewModel.selectedFilter.value)
    assertNull(viewModel.selectedConversationId.value)
  }

  @Test
  fun testReactiveConversationUpdates() = runBlocking {
    viewModel.createConversation("Alpha Project Sync")
    val list = viewModel.allConversations.first { it.isNotEmpty() }
    assertEquals(1, list.size)
    assertEquals("Alpha Project Sync", list[0].title)

    viewModel.createConversation("Beta Team")
    val updatedList = viewModel.allConversations.first { it.size == 2 }
    assertEquals(2, updatedList.size)
  }

  @Test
  fun testSearchFiltering() = runBlocking {
    repository.createConversation("Design Review")
    repository.createConversation("Engineering Daily")
    viewModel.allConversations.first { it.size == 2 }

    // Search for "Design"
    viewModel.setSearchQuery("Design")
    val searchResults = viewModel.filteredConversations.first { it.size == 1 }
    assertEquals("Design Review", searchResults[0].title)

    // Clear search
    viewModel.setSearchQuery("")
    val allResults = viewModel.filteredConversations.first { it.size == 2 }
    assertEquals(2, allResults.size)
  }

  @Test
  fun testUnreadAndPinnedFilters() = runBlocking {
    val conv1 = repository.createConversation("Direct Chat 1", isGroup = false).getOrThrow()
    val conv2 = repository.createConversation("Direct Chat 2", isGroup = false).getOrThrow()
    val groupConv = repository.createConversation("Group Channel", isGroup = true).getOrThrow()
    viewModel.allConversations.first { it.size == 3 }

    // Pin conv1
    viewModel.setConversationPinned(conv1.id, true)
    // Add unread to conv2 directly via repository
    database.chatDao().updateUnreadCount(conv2.id, 3)

    // Filter by PINNED
    viewModel.setChatFilter(ChatFilter.PINNED)
    val pinned = viewModel.filteredConversations.first { it.any { c -> c.id == conv1.id } && it.size == 1 }
    assertEquals(conv1.id, pinned[0].id)

    // Filter by UNREAD
    viewModel.setChatFilter(ChatFilter.UNREAD)
    val unread = viewModel.filteredConversations.first { it.any { c -> c.id == conv2.id } && it.size == 1 }
    assertEquals(conv2.id, unread[0].id)

    // Filter by CHANNELS (Groups)
    viewModel.setChatFilter(ChatFilter.CHANNELS)
    val channels = viewModel.filteredConversations.first { it.any { c -> c.id == groupConv.id } && it.size == 1 }
    assertEquals(groupConv.id, channels[0].id)

    // Filter by DIRECT
    viewModel.setChatFilter(ChatFilter.DIRECT)
    val direct = viewModel.filteredConversations.first { it.size == 2 }
    assertEquals(2, direct.size)
  }

  @Test
  fun testSendingLocalMessages() = runBlocking {
    val conv = repository.createConversation("Task Execution").getOrThrow()
    viewModel.selectConversation(conv.id)

    // Send via draftMessageText
    viewModel.setDraftMessageText("Implementing ChatViewModel tests")
    viewModel.sendLocalTextMessage()

    val messages = viewModel.currentMessages.first { it.isNotEmpty() }
    assertEquals(1, messages.size)
    assertEquals("Implementing ChatViewModel tests", messages[0].text)
    assertEquals("LOCAL_SENT", messages[0].status)
    assertEquals("", viewModel.draftMessageText.value)
  }

  @Test
  fun testReplySelectionAndSending() = runBlocking {
    val conv = repository.createConversation("Architecture Chat").getOrThrow()
    viewModel.selectConversation(conv.id)

    val originalMsg = repository.sendLocalTextMessage(
      conversationId = conv.id,
      text = "Initial architecture question",
      senderId = "colleague",
      senderName = "Alice"
    ).getOrThrow()

    // Select message for reply
    viewModel.selectReplyToMessage(originalMsg)
    assertEquals(originalMsg.id, viewModel.replyingToMessage.value?.id)

    // Send reply
    viewModel.sendLocalTextMessage("Here is the answer to your question")
    val updatedMessages = viewModel.currentMessages.first { it.size == 2 }
    val replyMsg = updatedMessages[1]

    assertEquals(originalMsg.id, replyMsg.replyToMessageId)
    assertEquals("Alice", replyMsg.replyToSenderName)
    assertEquals("Initial architecture question", replyMsg.replyToText)
    assertNull("Replying state must be cleared after sending", viewModel.replyingToMessage.value)
  }

  @Test
  fun testMessageEditingAndDeletion() = runBlocking {
    val conv = repository.createConversation("Notes Chat").getOrThrow()
    viewModel.selectConversation(conv.id)

    val msg = repository.sendLocalTextMessage(conv.id, "Draft text with a typo").getOrThrow()
    viewModel.currentMessages.first { it.isNotEmpty() }

    // Edit message
    viewModel.editMessage(msg.id, "Corrected polished text")
    val edited = viewModel.currentMessages.first { it.any { m -> m.text == "Corrected polished text" } }
    assertEquals("Corrected polished text", edited[0].text)

    // Delete message
    viewModel.deleteLocalMessage(msg.id)
    val remaining = viewModel.currentMessages.first { it.none { m -> m.id == msg.id } }
    assertTrue(remaining.isEmpty())
  }

  @Test
  fun testStarUnstar() = runBlocking {
    val conv = repository.createConversation("Starred Chat").getOrThrow()
    viewModel.selectConversation(conv.id)

    val msg = repository.sendLocalTextMessage(conv.id, "Star this important message").getOrThrow()
    viewModel.currentMessages.first { it.isNotEmpty() }

    viewModel.setMessageStarred(msg.id, true)
    val starred = viewModel.currentMessages.first { it.any { m -> m.id == msg.id && m.isStarred } }
    assertTrue(starred[0].isStarred)

    viewModel.setMessageStarred(msg.id, false)
    val unstarred = viewModel.currentMessages.first { it.any { m -> m.id == msg.id && !m.isStarred } }
    assertFalse(unstarred[0].isStarred)
  }

  @Test
  fun testPinMuteOperations() = runBlocking {
    val conv = repository.createConversation("Operations Channel").getOrThrow()
    viewModel.allConversations.first { it.isNotEmpty() }

    viewModel.setConversationPinned(conv.id, true)
    val pinned = viewModel.allConversations.first { it.any { c -> c.id == conv.id && c.isPinned } }
    assertTrue(pinned[0].isPinned)

    viewModel.setConversationMuted(conv.id, true)
    val muted = viewModel.allConversations.first { it.any { c -> c.id == conv.id && c.isMuted } }
    assertTrue(muted[0].isMuted)
  }

  @Test
  fun testErrorHandling() = runBlocking {
    // 1. Send with no conversation selected
    viewModel.selectConversation(null)
    viewModel.setDraftMessageText("Hello")
    viewModel.sendLocalTextMessage()
    assertEquals("No conversation selected", viewModel.errorMessage.value)

    viewModel.clearError()
    assertNull(viewModel.errorMessage.value)

    // 2. Send blank message
    val conv = repository.createConversation("Error Test").getOrThrow()
    viewModel.selectConversation(conv.id)
    viewModel.setDraftMessageText("   ")
    viewModel.sendLocalTextMessage()
    assertEquals("Message text cannot be blank", viewModel.errorMessage.value)
  }

  @Test
  fun testMarkMessagesLocallyRead() = runBlocking {
    val conv = repository.createConversation("Read Status Channel").getOrThrow()
    val id = conv.id

    // Insert an incoming message with LOCAL_STORED
    repository.sendLocalTextMessage(id, "Unread message from peer", senderId = "peer", senderName = "Peer")
    database.chatDao().updateUnreadCount(id, 1)

    // Verify unread count is 1
    val convBefore = repository.getConversationByIdOnce(id)
    assertEquals(1, convBefore?.unreadCount)

    // Selecting conversation marks messages as locally read
    viewModel.selectConversation(id)

    val convAfter = repository.getConversationById(id).first { it?.unreadCount == 0 }
    assertEquals(0, convAfter?.unreadCount)

    val msgs = viewModel.currentMessages.first { it.any { m -> m.status == "LOCAL_READ" } }
    assertEquals("LOCAL_READ", msgs[0].status)
  }

  @Test
  fun testReactiveMessageUpdatesWithoutAppRestart() = runBlocking {
    val conv = repository.createConversation("Live Stream Test").getOrThrow()
    viewModel.selectConversation(conv.id)

    // Message 1
    viewModel.sendLocalTextMessage("First emission")
    var msgs = viewModel.currentMessages.first { it.size == 1 }
    assertEquals("First emission", msgs[0].text)

    // Message 2
    viewModel.sendLocalTextMessage("Second emission")
    msgs = viewModel.currentMessages.first { it.size == 2 }
    assertEquals("Second emission", msgs[1].text)

    // Message 3
    viewModel.sendLocalTextMessage("Third emission")
    msgs = viewModel.currentMessages.first { it.size == 3 }
    assertEquals("Third emission", msgs[2].text)
  }
}
