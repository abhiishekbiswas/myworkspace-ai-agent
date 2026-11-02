package com.example.data.local.dao

import androidx.room.*
import com.example.core.database.relation.ProjectWithCategoryAndTasks
import com.example.core.database.relation.ProjectWithTasks
import com.example.core.database.relation.TaskWithSubtasksAndDetails
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM projects WHERE isArchived = 0 AND archivedAt IS NULL ORDER BY createdAt DESC")
    fun getActiveProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isArchived = 1 OR archivedAt IS NOT NULL ORDER BY updatedAt DESC")
    fun getArchivedProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectById(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectByIdOnce(id: String): ProjectEntity?

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectWithTasks(id: String): Flow<ProjectWithTasks?>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectWithCategoryAndTasks(id: String): Flow<ProjectWithCategoryAndTasks?>

    @Query("SELECT COUNT(*) FROM projects WHERE categoryId = :categoryId")
    suspend fun countProjectsWithCategory(categoryId: String): Int

    @Query("UPDATE projects SET categoryId = :newCategoryId, updatedAt = :updatedAt WHERE categoryId = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: String, newCategoryId: String?, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectsBulk(projects: List<ProjectEntity>)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    fun getCategoryById(id: String): Flow<CategoryEntity?>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryByIdOnce(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoriesBulk(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    @Query("SELECT * FROM tasks WHERE isArchived = 0 AND archivedAt IS NULL ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND isArchived = 0 AND archivedAt IS NULL ORDER BY sortOrder ASC, createdAt DESC")
    fun getTasksByProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId")
    suspend fun getTasksByProjectOnce(projectId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND parentTaskId IS NULL AND isArchived = 0 AND archivedAt IS NULL ORDER BY sortOrder ASC, createdAt DESC")
    fun getRootTasksByProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentTaskId AND isArchived = 0 AND archivedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    fun getSubtasks(parentTaskId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentTaskId")
    suspend fun getSubtasksOnce(parentTaskId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskByIdOnce(id: String): TaskEntity?

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskWithDetails(id: String): Flow<TaskWithSubtasksAndDetails?>

    @Query("SELECT * FROM tasks WHERE isArchived = 0 AND archivedAt IS NULL AND dueDate IS NOT NULL ORDER BY dueDate ASC")
    fun getTasksWithDeadlines(): Flow<List<TaskEntity>>

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

    @Query("UPDATE tasks SET sortOrder = :newSortOrder, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateTaskSortOrder(taskId: String, newSortOrder: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM checklist_items WHERE taskId = :taskId ORDER BY sortOrder ASC, createdAt ASC")
    fun getChecklistForTask(taskId: String): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE taskId = :taskId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getChecklistForTaskOnce(taskId: String): List<ChecklistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: ChecklistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistBulk(items: List<ChecklistItemEntity>)

    @Update
    suspend fun updateChecklistItem(item: ChecklistItemEntity)

    @Delete
    suspend fun deleteChecklistItem(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE taskId = :taskId")
    suspend fun deleteChecklistForTask(taskId: String)

    @Query("SELECT * FROM comments WHERE taskId = :taskId AND deletedAt IS NULL ORDER BY createdAt ASC")
    fun getCommentsForTask(taskId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE taskId = :taskId AND deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getCommentsForTaskOnce(taskId: String): List<CommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommentsBulk(comments: List<CommentEntity>)

    @Update
    suspend fun updateComment(comment: CommentEntity)

    @Delete
    suspend fun deleteComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE taskId = :taskId")
    suspend fun deleteCommentsForTask(taskId: String)

    @Query("SELECT * FROM progress_logs WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getProgressLogsForProject(projectId: String): Flow<List<ProgressLogEntity>>

    @Query("SELECT * FROM progress_logs WHERE taskId = :taskId ORDER BY createdAt DESC")
    fun getProgressLogsForTask(taskId: String): Flow<List<ProgressLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressLog(log: ProgressLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressLogsBulk(logs: List<ProgressLogEntity>)

    @Delete
    suspend fun deleteProgressLog(log: ProgressLogEntity)

    @Query("SELECT * FROM notes WHERE archivedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE projectId = :projectId AND archivedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesForProject(projectId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE taskId = :taskId AND archivedAt IS NULL ORDER BY updatedAt DESC")
    fun getNotesForTask(taskId: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotesBulk(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("SELECT * FROM activity_logs ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentActivities(limit: Int = 50): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE entityId = :entityId OR details LIKE '%' || :entityId || '%' ORDER BY createdAt DESC LIMIT 50")
    fun getActivitiesForEntity(entityId: String): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(activity: ActivityLogEntity)

    @Query("SELECT * FROM projects")
    suspend fun getAllProjectsForBackup(): List<ProjectEntity>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksForBackup(): List<TaskEntity>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllChecklistItemsForBackup(): List<ChecklistItemEntity>

    @Query("SELECT * FROM comments")
    suspend fun getAllCommentsForBackup(): List<CommentEntity>

    @Query("SELECT * FROM progress_logs")
    suspend fun getAllProgressLogsForBackup(): List<ProgressLogEntity>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesForBackup(): List<NoteEntity>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesForBackup(): List<CategoryEntity>

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
