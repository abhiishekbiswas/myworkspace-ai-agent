package com.example.core.database.dao

import androidx.room.*
import com.example.core.database.relation.ProjectWithCategoryAndTasks
import com.example.core.database.relation.ProjectWithTasks
import com.example.core.database.relation.TaskWithSubtasksAndDetails
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllActiveProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isArchived = 1 ORDER BY archivedAt DESC")
    fun getArchivedProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectById(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectByIdOnce(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getProjectByNameOnce(name: String): ProjectEntity?

    @Transaction
    @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getProjectsWithTasks(): Flow<List<ProjectWithTasks>>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectWithCategoryAndTasks(id: String): Flow<ProjectWithCategoryAndTasks?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isArchived = 0 ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isArchived = 0 ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun getAllActiveTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND isArchived = 0 ORDER BY sortOrder ASC")
    fun getTasksForProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND isArchived = 0 ORDER BY sortOrder ASC")
    suspend fun getTasksForProjectOnce(projectId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskByIdOnce(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isArchived = 0 AND (dueDate BETWEEN :startMs AND :endMs)")
    suspend fun getTasksDueBetween(startMs: Long, endMs: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isArchived = 0 AND dueDate < :currentMs AND status != 'COMPLETED'")
    suspend fun getOverdueTasks(currentMs: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isArchived = 0 AND status != 'COMPLETED' ORDER BY dueDate ASC")
    suspend fun getPendingTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isArchived = 0 AND (LOWER(title) LIKE '%' || LOWER(:query) || '%' OR LOWER(description) LIKE '%' || LOWER(:query) || '%')")
    suspend fun searchTasks(query: String): List<TaskEntity>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskWithSubtasksAndDetails(id: String): Flow<TaskWithSubtasksAndDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasksBulk(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)
}

@Dao
interface TaskDependencyDao {
    @Query("SELECT * FROM task_dependencies WHERE taskId = :taskId")
    fun getDependenciesForTask(taskId: String): Flow<List<TaskDependencyEntity>>

    @Query("SELECT * FROM task_dependencies WHERE taskId = :taskId")
    suspend fun getDependenciesForTaskOnce(taskId: String): List<TaskDependencyEntity>

    @Query("SELECT * FROM task_dependencies WHERE dependsOnTaskId = :taskId")
    fun getDependentsOfTask(taskId: String): Flow<List<TaskDependencyEntity>>

    @Query("SELECT * FROM task_dependencies WHERE dependsOnTaskId = :taskId")
    suspend fun getDependentsOfTaskOnce(taskId: String): List<TaskDependencyEntity>

    @Query("SELECT * FROM task_dependencies")
    fun getAllDependencies(): Flow<List<TaskDependencyEntity>>

    @Query("SELECT * FROM task_dependencies")
    suspend fun getAllDependenciesOnce(): List<TaskDependencyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDependency(dependency: TaskDependencyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDependenciesBulk(dependencies: List<TaskDependencyEntity>)

    @Delete
    suspend fun deleteDependency(dependency: TaskDependencyEntity)

    @Query("DELETE FROM task_dependencies WHERE id = :id")
    suspend fun deleteDependencyById(id: String)

    @Query("DELETE FROM task_dependencies WHERE taskId = :taskId AND dependsOnTaskId = :dependsOnTaskId")
    suspend fun deleteDependencyPair(taskId: String, dependsOnTaskId: String)
}

@Dao
interface ChecklistItemDao {
    @Query("SELECT * FROM checklist_items WHERE taskId = :taskId ORDER BY sortOrder ASC")
    fun getItemsForTask(taskId: String): Flow<List<ChecklistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ChecklistItemEntity)

    @Update
    suspend fun updateItem(item: ChecklistItemEntity)

    @Delete
    suspend fun deleteItem(item: ChecklistItemEntity)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun getCommentsForTask(taskId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Update
    suspend fun updateComment(comment: CommentEntity)

    @Delete
    suspend fun deleteComment(comment: CommentEntity)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE archivedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE projectId = :projectId AND archivedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesForProject(projectId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE archivedAt IS NULL AND (LOWER(title) LIKE '%' || LOWER(:query) || '%' OR LOWER(content) LIKE '%' || LOWER(:query) || '%')")
    suspend fun searchNotes(query: String): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}

@Dao
interface ProgressLogDao {
    @Query("SELECT * FROM progress_logs WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getLogsForProject(projectId: String): Flow<List<ProgressLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ProgressLogEntity)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentActivities(limit: Int = 50): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityLogEntity)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun getConversationById(id: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationByIdOnce(id: String): ConversationEntity?

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessagesForConversationOnce(conversationId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContactsBulk(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Transaction
    suspend fun insertMessageAndUpdateConversation(message: ChatMessageEntity) {
        insertMessage(message)
        val conversation = getConversationByIdOnce(message.conversationId)
        if (conversation != null) {
            updateConversation(
                conversation.copy(
                    lastMessageId = message.id,
                    lastMessageText = message.text,
                    lastMessageTimestamp = message.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}

@Dao
interface AiMemoryDao {
    @Query("SELECT * FROM ai_memories ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<AiMemoryEntity>>

    @Query("SELECT * FROM ai_memories ORDER BY createdAt DESC")
    suspend fun getAllMemoriesOnce(): List<AiMemoryEntity>

    @Query("SELECT * FROM ai_memories WHERE id = :id LIMIT 1")
    suspend fun getMemoryById(id: String): AiMemoryEntity?

    @Query("SELECT * FROM ai_memories WHERE category = :category ORDER BY createdAt DESC")
    fun getMemoriesByCategory(category: String): Flow<List<AiMemoryEntity>>

    @Query("SELECT * FROM ai_memories WHERE fact LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchMemories(query: String): Flow<List<AiMemoryEntity>>

    @Query("SELECT * FROM ai_memories WHERE fact LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun searchMemoriesOnce(query: String): List<AiMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AiMemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoriesBulk(memories: List<AiMemoryEntity>)

    @Update
    suspend fun updateMemory(memory: AiMemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: AiMemoryEntity)

    @Query("DELETE FROM ai_memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String)

    @Query("DELETE FROM ai_memories")
    suspend fun clearAllMemories()
}
