package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.dao.ChatDao
import com.example.data.local.database.WorkspaceDatabase
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageAttachmentEntity
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
class ChatDatabaseTest {

  private lateinit var database: WorkspaceDatabase
  private lateinit var chatDao: ChatDao
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext<Context>()
    database = WorkspaceDatabase.createInMemory(context)
    chatDao = database.chatDao()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun testConversationCrudAndPinning() = runBlocking {
    val convId = UUID.randomUUID().toString()
    val conv = ConversationEntity(
      id = convId,
      title = "Engineering Core Team",
      isGroup = true,
      avatarColor = "#8B5CF6",
      isPinned = false,
      isMuted = false,
      unreadCount = 2,
      lastMessageText = "Deployment scheduled for 5 PM",
      lastMessageTimestamp = System.currentTimeMillis()
    )

    // 1. Insert
    chatDao.insertConversation(conv)

    // 2. Query
    val fetched = chatDao.getConversationByIdOnce(convId)
    assertNotNull(fetched)
    assertEquals("Engineering Core Team", fetched?.title)
    assertTrue(fetched?.isGroup == true)
    assertEquals(2, fetched?.unreadCount)
    assertFalse(fetched?.isPinned == true)

    // 3. Pin conversation
    chatDao.setConversationPinned(convId, true)
    val pinnedConv = chatDao.getConversationByIdOnce(convId)
    assertTrue("Conversation should be pinned", pinnedConv?.isPinned == true)

    // 4. Mute conversation
    chatDao.setConversationMuted(convId, true)
    val mutedConv = chatDao.getConversationByIdOnce(convId)
    assertTrue("Conversation should be muted", mutedConv?.isMuted == true)

    // 5. Update unread count
    chatDao.updateUnreadCount(convId, 0)
    val readConv = chatDao.getConversationByIdOnce(convId)
    assertEquals(0, readConv?.unreadCount)

    // 6. Delete
    chatDao.deleteConversationById(convId)
    val deletedConv = chatDao.getConversationByIdOnce(convId)
    assertNull("Conversation should be deleted", deletedConv)
  }

  @Test
  fun testMessageInsertionAndChronologicalOrdering() = runBlocking {
    val convId = UUID.randomUUID().toString()
    chatDao.insertConversation(
      ConversationEntity(
        id = convId,
        title = "Sprint Planning Chat",
        isGroup = false
      )
    )

    val baseTime = 1700000000000L
    val msg1 = ChatMessageEntity(
      id = UUID.randomUUID().toString(),
      conversationId = convId,
      senderId = "user_1",
      senderName = "Alice",
      text = "Good morning team!",
      status = "LOCAL_STORED",
      createdAt = baseTime + 1000
    )
    val msg2 = ChatMessageEntity(
      id = UUID.randomUUID().toString(),
      conversationId = convId,
      senderId = "me",
      senderName = "Me",
      text = "Morning Alice, starting on the database layer now.",
      status = "LOCAL_SENT",
      createdAt = baseTime + 2000
    )
    val msg3 = ChatMessageEntity(
      id = UUID.randomUUID().toString(),
      conversationId = convId,
      senderId = "user_1",
      senderName = "Alice",
      text = "Great! Let me know if you need review.",
      status = "LOCAL_STORED",
      createdAt = baseTime + 3000
    )

    // Insert out of chronological order
    chatDao.insertMessage(msg2)
    chatDao.insertMessage(msg1)
    chatDao.insertMessage(msg3)

    // Retrieve via Flow and verify ascending order by createdAt
    val messages = chatDao.getMessagesForConversation(convId).first()
    assertEquals(3, messages.size)
    assertEquals("Good morning team!", messages[0].text)
    assertEquals("Morning Alice, starting on the database layer now.", messages[1].text)
    assertEquals("Great! Let me know if you need review.", messages[2].text)

    // Search messages
    val searchResults = chatDao.searchMessagesInConversation(convId, "database").first()
    assertEquals(1, searchResults.size)
    assertEquals(msg2.id, searchResults[0].id)
  }

  @Test
  fun testConversationDeletionCascadesToMessagesAndAttachments() = runBlocking {
    val convId = UUID.randomUUID().toString()
    chatDao.insertConversation(
      ConversationEntity(id = convId, title = "Ephemeral Project Chat")
    )

    val msgId = UUID.randomUUID().toString()
    chatDao.insertMessage(
      ChatMessageEntity(
        id = msgId,
        conversationId = convId,
        senderId = "me",
        senderName = "Me",
        text = "Here is the schema PDF"
      )
    )

    val attachId = UUID.randomUUID().toString()
    chatDao.insertAttachment(
      MessageAttachmentEntity(
        id = attachId,
        messageId = msgId,
        fileName = "schema.pdf",
        fileUri = "content://media/schema.pdf",
        fileType = "application/pdf",
        fileSizeBytes = 204800L
      )
    )

    // Verify presence
    val initialMessages = chatDao.getMessagesForConversationOnce(convId)
    assertEquals(1, initialMessages.size)
    val initialAttachments = chatDao.getAttachmentsForMessageOnce(msgId)
    assertEquals(1, initialAttachments.size)

    // Delete conversation
    chatDao.deleteConversationById(convId)

    // Verify conversation is gone
    assertNull(chatDao.getConversationByIdOnce(convId))

    // Verify messages cascaded away
    val remainingMessages = chatDao.getMessagesForConversationOnce(convId)
    assertTrue("Messages must be cascaded away when conversation is deleted", remainingMessages.isEmpty())

    // Verify attachments cascaded away
    val remainingAttachments = chatDao.getAttachmentsForMessageOnce(msgId)
    assertTrue("Attachments must be cascaded away when message/conversation is deleted", remainingAttachments.isEmpty())
  }

  @Test
  fun testContactCrudAndDemoDistinction() = runBlocking {
    val realContactId = UUID.randomUUID().toString()
    val demoContactId = UUID.randomUUID().toString()

    val realContact = ContactEntity(
      id = realContactId,
      name = "Vikram Patel",
      role = "Engineering Manager",
      email = "vikram@workspace.internal",
      isDemo = false
    )

    val demoContact = ContactEntity(
      id = demoContactId,
      name = "Demo Assistant Bot",
      role = "Workspace Assistant",
      statusBio = "Simulated preview assistant",
      isDemo = true
    )

    chatDao.insertContact(realContact)
    chatDao.insertContact(demoContact)

    val allContacts = chatDao.getAllContactsOnce()
    assertEquals(2, allContacts.size)

    val fetchedReal = chatDao.getContactByIdOnce(realContactId)
    assertNotNull(fetchedReal)
    assertFalse("Real contact must not be flagged as demo", fetchedReal!!.isDemo)

    val fetchedDemo = chatDao.getContactByIdOnce(demoContactId)
    assertNotNull(fetchedDemo)
    assertTrue("Demo contact must be clearly marked as demo", fetchedDemo!!.isDemo)

    // Search contacts
    val searchResults = chatDao.searchContacts("Vikram").first()
    assertEquals(1, searchResults.size)
    assertEquals("Vikram Patel", searchResults[0].name)

    // Delete contact
    chatDao.deleteContactById(demoContactId)
    val remaining = chatDao.getAllContactsOnce()
    assertEquals(1, remaining.size)
    assertEquals(realContactId, remaining[0].id)
  }

  @Test
  fun testDatabaseMigrationFromVersion3To4AndDataPreservation() {
    val helper = FrameworkSQLiteOpenHelperFactory().create(
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .name("test_migration_3_4.db")
        .callback(object : SupportSQLiteOpenHelper.Callback(3) {
          override fun onCreate(db: SupportSQLiteDatabase) {
            // 1. Create existing Phase 3 tables
            db.execSQL(
              """
              CREATE TABLE IF NOT EXISTS `projects` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `name` TEXT NOT NULL,
                `description` TEXT,
                `categoryId` TEXT,
                `color` TEXT NOT NULL,
                `icon` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `priority` TEXT NOT NULL,
                `progressMode` TEXT NOT NULL,
                `manualProgress` REAL NOT NULL,
                `startDate` INTEGER,
                `dueDate` INTEGER,
                `completedAt` INTEGER,
                `isArchived` INTEGER NOT NULL,
                `archivedAt` INTEGER,
                `sortOrder` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
              )
              """.trimIndent()
            )

            db.execSQL(
              """
              CREATE TABLE IF NOT EXISTS `tasks` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `projectId` TEXT NOT NULL,
                `parentTaskId` TEXT,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `status` TEXT NOT NULL,
                `priority` TEXT NOT NULL,
                `progressMode` TEXT NOT NULL,
                `manualProgress` REAL NOT NULL,
                `progress` REAL NOT NULL,
                `startDate` INTEGER,
                `dueDate` INTEGER,
                `completedAt` INTEGER,
                `estimatedHours` REAL,
                `actualHours` REAL,
                `isArchived` INTEGER NOT NULL,
                `archivedAt` INTEGER,
                `sortOrder` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
              )
              """.trimIndent()
            )

            db.execSQL(
              """
              CREATE TABLE IF NOT EXISTS `task_dependencies` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `taskId` TEXT NOT NULL,
                `dependsOnTaskId` TEXT NOT NULL,
                `dependencyType` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
              )
              """.trimIndent()
            )

            // Seed Phase 3 data
            db.execSQL("INSERT INTO projects (id, name, color, icon, status, priority, progressMode, manualProgress, isArchived, sortOrder, createdAt, updatedAt) VALUES ('p1', 'Phase 3 Core', '#2563EB', 'folder', 'Active', 'High', 'Automatic', 50.0, 0, 0, 1000, 1000)")
            db.execSQL("INSERT INTO tasks (id, projectId, title, status, priority, progressMode, manualProgress, progress, isArchived, sortOrder, createdAt, updatedAt) VALUES ('t1', 'p1', 'Prereq Task', 'Completed', 'High', 'Manual', 100.0, 100.0, 0, 0, 1000, 1000)")
            db.execSQL("INSERT INTO tasks (id, projectId, title, status, priority, progressMode, manualProgress, progress, isArchived, sortOrder, createdAt, updatedAt) VALUES ('t2', 'p1', 'Dependent Task', 'To Do', 'High', 'Manual', 0.0, 0.0, 0, 1, 1000, 1000)")
            db.execSQL("INSERT INTO task_dependencies (id, taskId, dependsOnTaskId, dependencyType, createdAt) VALUES ('dep1', 't2', 't1', 'FINISH_TO_START', 1000)")
          }

          override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // No-op here; test manually applies MIGRATION_3_4
          }
        })
        .build()
    )

    val db = helper.writableDatabase

    // 1. Verify Phase 3 data exists before migration
    val projectCursorBefore = db.query("SELECT name FROM projects WHERE id = 'p1'")
    assertTrue(projectCursorBefore.moveToFirst())
    assertEquals("Phase 3 Core", projectCursorBefore.getString(0))
    projectCursorBefore.close()

    // 2. Apply MIGRATION_3_4
    WorkspaceDatabase.MIGRATION_3_4.migrate(db)

    // 3. Verify Phase 3 data is 100% PRESERVED after migration
    val projectCursorAfter = db.query("SELECT name FROM projects WHERE id = 'p1'")
    assertTrue(projectCursorAfter.moveToFirst())
    assertEquals("Phase 3 Core", projectCursorAfter.getString(0))
    projectCursorAfter.close()

    val taskCursorAfter = db.query("SELECT title FROM tasks WHERE id = 't2'")
    assertTrue(taskCursorAfter.moveToFirst())
    assertEquals("Dependent Task", taskCursorAfter.getString(0))
    taskCursorAfter.close()

    val depCursorAfter = db.query("SELECT dependencyType FROM task_dependencies WHERE id = 'dep1'")
    assertTrue(depCursorAfter.moveToFirst())
    assertEquals("FINISH_TO_START", depCursorAfter.getString(0))
    depCursorAfter.close()

    // 4. Verify new chat tables were created and can be queried/inserted
    db.execSQL(
      "INSERT INTO conversations (id, title, isGroup, avatarColor, isPinned, isMuted, unreadCount, lastMessageTimestamp, createdAt, updatedAt) VALUES ('c1', 'Migrated Chat', 0, '#2563EB', 1, 0, 0, 2000, 2000, 2000)"
    )
    val convCursor = db.query("SELECT title, isPinned FROM conversations WHERE id = 'c1'")
    assertTrue(convCursor.moveToFirst())
    assertEquals("Migrated Chat", convCursor.getString(0))
    assertEquals(1, convCursor.getInt(1))
    convCursor.close()

    db.execSQL(
      "INSERT INTO chat_messages (id, conversationId, senderId, senderName, text, messageType, status, isStarred, createdAt, updatedAt) VALUES ('m1', 'c1', 'me', 'Me', 'Hello Post Migration', 'TEXT', 'LOCAL_STORED', 0, 2000, 2000)"
    )
    val msgCursor = db.query("SELECT text, status FROM chat_messages WHERE id = 'm1'")
    assertTrue(msgCursor.moveToFirst())
    assertEquals("Hello Post Migration", msgCursor.getString(0))
    assertEquals("LOCAL_STORED", msgCursor.getString(1))
    msgCursor.close()

    db.close()
  }
}
