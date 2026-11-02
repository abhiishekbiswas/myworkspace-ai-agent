package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.database.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        ProjectEntity::class,
        CategoryEntity::class,
        TaskEntity::class,
        TaskDependencyEntity::class,
        ChecklistItemEntity::class,
        CommentEntity::class,
        NoteEntity::class,
        ProgressLogEntity::class,
        ActivityLogEntity::class,
        ConversationEntity::class,
        ChatMessageEntity::class,
        MessageAttachmentEntity::class,
        ContactEntity::class,
        AiMemoryEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class WorkspaceDatabase : RoomDatabase() {

    abstract fun workspaceDao(): com.example.data.local.dao.WorkspaceDao
    abstract fun projectDao(): ProjectDao
    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun taskDependencyDao(): TaskDependencyDao
    abstract fun checklistItemDao(): ChecklistItemDao
    abstract fun commentDao(): CommentDao
    abstract fun noteDao(): NoteDao
    abstract fun progressLogDao(): ProgressLogDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun chatDao(): ChatDao
    abstract fun aiMemoryDao(): AiMemoryDao


    companion object {
        @Volatile
        private var INSTANCE: WorkspaceDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 1 to 2 schema updates if applicable
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `task_dependencies` (
                      `id` TEXT NOT NULL PRIMARY KEY,
                      `taskId` TEXT NOT NULL,
                      `dependsOnTaskId` TEXT NOT NULL,
                      `dependencyType` TEXT NOT NULL,
                      `createdAt` INTEGER NOT NULL,
                      FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON DELETE CASCADE,
                      FOREIGN KEY(`dependsOnTaskId`) REFERENCES `tasks`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_dependencies_taskId` ON `task_dependencies` (`taskId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_dependencies_dependsOnTaskId` ON `task_dependencies` (`dependsOnTaskId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `conversations` (
                      `id` TEXT NOT NULL PRIMARY KEY,
                      `title` TEXT NOT NULL,
                      `isGroup` INTEGER NOT NULL,
                      `avatarUrl` TEXT,
                      `avatarColor` TEXT NOT NULL,
                      `isPinned` INTEGER NOT NULL,
                      `isMuted` INTEGER NOT NULL,
                      `unreadCount` INTEGER NOT NULL,
                      `lastMessageId` TEXT,
                      `lastMessageText` TEXT,
                      `lastMessageTimestamp` INTEGER NOT NULL,
                      `createdAt` INTEGER NOT NULL,
                      `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversations_lastMessageTimestamp` ON `conversations` (`lastMessageTimestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversations_isPinned` ON `conversations` (`isPinned`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                      `id` TEXT NOT NULL PRIMARY KEY,
                      `conversationId` TEXT NOT NULL,
                      `senderId` TEXT NOT NULL,
                      `senderName` TEXT NOT NULL,
                      `text` TEXT NOT NULL,
                      `messageType` TEXT NOT NULL,
                      `status` TEXT NOT NULL,
                      `replyToMessageId` TEXT,
                      `replyToSenderName` TEXT,
                      `replyToText` TEXT,
                      `isStarred` INTEGER NOT NULL,
                      `createdAt` INTEGER NOT NULL,
                      `updatedAt` INTEGER NOT NULL,
                      FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_conversationId` ON `chat_messages` (`conversationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_createdAt` ON `chat_messages` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_replyToMessageId` ON `chat_messages` (`replyToMessageId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `message_attachments` (
                      `id` TEXT NOT NULL PRIMARY KEY,
                      `messageId` TEXT NOT NULL,
                      `fileName` TEXT NOT NULL,
                      `fileUri` TEXT NOT NULL,
                      `fileType` TEXT NOT NULL,
                      `fileSizeBytes` INTEGER NOT NULL,
                      `thumbnailUri` TEXT,
                      `durationMs` INTEGER,
                      `createdAt` INTEGER NOT NULL,
                      FOREIGN KEY(`messageId`) REFERENCES `chat_messages`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_message_attachments_messageId` ON `message_attachments` (`messageId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `contacts` (
                      `id` TEXT NOT NULL PRIMARY KEY,
                      `name` TEXT NOT NULL,
                      `role` TEXT NOT NULL,
                      `email` TEXT,
                      `phoneNumber` TEXT,
                      `avatarColor` TEXT NOT NULL,
                      `initials` TEXT NOT NULL,
                      `statusBio` TEXT NOT NULL,
                      `isDemo` INTEGER NOT NULL,
                      `lastActiveTimestamp` INTEGER NOT NULL,
                      `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_name` ON `contacts` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_isDemo` ON `contacts` (`isDemo`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_memories` (
                      `id` TEXT NOT NULL PRIMARY KEY,
                      `category` TEXT NOT NULL,
                      `fact` TEXT NOT NULL,
                      `createdAt` INTEGER NOT NULL,
                      `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_memories_category` ON `ai_memories` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_memories_createdAt` ON `ai_memories` (`createdAt`)")
            }
        }

        fun getInstance(context: Context): WorkspaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkspaceDatabase::class.java,
                    "workspace_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
