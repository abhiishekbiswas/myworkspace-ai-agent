package com.example.domain.repository

import com.example.core.database.relation.ProjectWithCategoryAndTasks
import com.example.core.database.relation.ProjectWithTasks
import com.example.core.database.relation.TaskWithSubtasksAndDetails
import com.example.data.local.entity.*
import com.example.domain.model.WorkspaceBackup
import kotlinx.coroutines.flow.Flow

interface IWorkspaceRepository {
    val activeProjects: Flow<List<ProjectEntity>>
    val archivedProjects: Flow<List<ProjectEntity>>
    val categories: Flow<List<CategoryEntity>>
    val allTasks: Flow<List<TaskEntity>>
    val tasksWithDeadlines: Flow<List<TaskEntity>>
    val allNotes: Flow<List<NoteEntity>>
    val recentActivities: Flow<List<ActivityLogEntity>>
    val allDependencies: Flow<List<TaskDependencyEntity>>

    fun getProjectById(id: String): Flow<ProjectEntity?>
    fun getProjectWithTasks(id: String): Flow<ProjectWithTasks?>
    fun getProjectWithCategoryAndTasks(id: String): Flow<ProjectWithCategoryAndTasks?>

    fun getTasksByProject(projectId: String): Flow<List<TaskEntity>>
    fun getRootTasksByProject(projectId: String): Flow<List<TaskEntity>>
    fun getSubtasks(parentTaskId: String): Flow<List<TaskEntity>>
    fun getTaskById(id: String): Flow<TaskEntity?>
    fun getTaskWithDetails(id: String): Flow<TaskWithSubtasksAndDetails?>

    fun getChecklistForTask(taskId: String): Flow<List<ChecklistItemEntity>>
    fun getCommentsForTask(taskId: String): Flow<List<CommentEntity>>
    fun getProgressLogsForProject(projectId: String): Flow<List<ProgressLogEntity>>
    fun getProgressLogsForTask(taskId: String): Flow<List<ProgressLogEntity>>
    fun getNotesForProject(projectId: String): Flow<List<NoteEntity>>
    fun getActivitiesForEntity(entityId: String): Flow<List<ActivityLogEntity>>

    fun getDependenciesForTask(taskId: String): Flow<List<TaskDependencyEntity>>
    fun getDependentsOfTask(taskId: String): Flow<List<TaskDependencyEntity>>

    suspend fun saveProject(project: ProjectEntity)
    suspend fun duplicateProject(projectId: String): Result<ProjectEntity>
    suspend fun archiveProject(projectId: String)
    suspend fun restoreProject(projectId: String)
    suspend fun deleteProject(project: ProjectEntity)

    suspend fun saveCategory(category: CategoryEntity)
    suspend fun deleteCategory(categoryId: String, reassignToCategoryId: String? = null): Result<Unit>

    suspend fun saveTask(task: TaskEntity)
    suspend fun validateTaskHierarchy(taskId: String, newParentTaskId: String?, projectId: String): Result<Unit>
    suspend fun updateTaskStatus(taskId: String, newStatus: String)
    suspend fun recordProgressUpdate(projectId: String, newProgress: Double, updateNote: String = "", source: String = "MANUAL")
    suspend fun deleteTask(task: TaskEntity)
    suspend fun updateTaskSortOrder(taskId: String, newSortOrder: Int)

    suspend fun addDependency(taskId: String, dependsOnTaskId: String, dependencyType: String = "BLOCKS"): Result<Unit>
    suspend fun deleteDependency(dependencyId: String)
    suspend fun deleteDependencyPair(taskId: String, dependsOnTaskId: String)
    suspend fun isTaskBlocked(taskId: String): Boolean

    suspend fun saveChecklistItem(item: ChecklistItemEntity)
    suspend fun toggleChecklistItem(item: ChecklistItemEntity)
    suspend fun deleteChecklistItem(item: ChecklistItemEntity)

    suspend fun addComment(taskId: String, content: String)
    suspend fun deleteComment(comment: CommentEntity)

    suspend fun saveNote(note: NoteEntity)
    suspend fun deleteNote(note: NoteEntity)

    suspend fun exportBackupJson(): String
    suspend fun restoreBackupJson(json: String): Result<WorkspaceBackup>
    suspend fun seedInitialData()
}
