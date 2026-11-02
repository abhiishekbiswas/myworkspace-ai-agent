package com.example.data.repository

import com.example.core.database.relation.ProjectWithCategoryAndTasks
import com.example.core.database.relation.ProjectWithTasks
import com.example.core.database.relation.TaskWithSubtasksAndDetails
import com.example.data.local.dao.WorkspaceDao
import com.example.data.local.entity.*
import com.example.domain.model.WorkspaceBackup
import com.example.domain.repository.IWorkspaceRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

class WorkspaceRepository(
    private val dao: WorkspaceDao
) : IWorkspaceRepository {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    override val activeProjects: Flow<List<ProjectEntity>> = dao.getActiveProjects()
    override val archivedProjects: Flow<List<ProjectEntity>> = dao.getArchivedProjects()
    override val categories: Flow<List<CategoryEntity>> = dao.getCategories()
    override val allTasks: Flow<List<TaskEntity>> = dao.getAllTasks()
    override val tasksWithDeadlines: Flow<List<TaskEntity>> = dao.getTasksWithDeadlines()
    override val allNotes: Flow<List<NoteEntity>> = dao.getAllNotes()
    override val recentActivities: Flow<List<ActivityLogEntity>> = dao.getRecentActivities(50)
    override val allDependencies: Flow<List<TaskDependencyEntity>> = dao.getAllDependencies()

    override fun getProjectById(id: String): Flow<ProjectEntity?> = dao.getProjectById(id)
    override fun getProjectWithTasks(id: String): Flow<ProjectWithTasks?> = dao.getProjectWithTasks(id)
    override fun getProjectWithCategoryAndTasks(id: String): Flow<ProjectWithCategoryAndTasks?> = dao.getProjectWithCategoryAndTasks(id)

    override fun getTasksByProject(projectId: String): Flow<List<TaskEntity>> = dao.getTasksByProject(projectId)
    override fun getRootTasksByProject(projectId: String): Flow<List<TaskEntity>> = dao.getRootTasksByProject(projectId)
    override fun getSubtasks(parentTaskId: String): Flow<List<TaskEntity>> = dao.getSubtasks(parentTaskId)
    override fun getTaskById(id: String): Flow<TaskEntity?> = dao.getTaskById(id)
    override fun getTaskWithDetails(id: String): Flow<TaskWithSubtasksAndDetails?> = dao.getTaskWithDetails(id)

    override fun getChecklistForTask(taskId: String): Flow<List<ChecklistItemEntity>> = dao.getChecklistForTask(taskId)
    override fun getCommentsForTask(taskId: String): Flow<List<CommentEntity>> = dao.getCommentsForTask(taskId)
    override fun getProgressLogsForProject(projectId: String): Flow<List<ProgressLogEntity>> = dao.getProgressLogsForProject(projectId)
    override fun getProgressLogsForTask(taskId: String): Flow<List<ProgressLogEntity>> = dao.getProgressLogsForTask(taskId)
    override fun getNotesForProject(projectId: String): Flow<List<NoteEntity>> = dao.getNotesForProject(projectId)
    override fun getActivitiesForEntity(entityId: String): Flow<List<ActivityLogEntity>> = dao.getActivitiesForEntity(entityId)

    override fun getDependenciesForTask(taskId: String): Flow<List<TaskDependencyEntity>> = dao.getDependenciesForTask(taskId)
    override fun getDependentsOfTask(taskId: String): Flow<List<TaskDependencyEntity>> = dao.getDependentsOfTask(taskId)

    override suspend fun saveProject(project: ProjectEntity) {
        dao.insertProject(project)
        dao.insertActivityLog(
            ActivityLogEntity(
                entityType = "PROJECT",
                entityId = project.id,
                action = "SAVED",
                details = "Saved project ${project.name}"
            )
        )
    }

    override suspend fun duplicateProject(projectId: String): Result<ProjectEntity> {
        val original = dao.getProjectByIdOnce(projectId)
            ?: return Result.failure(IllegalArgumentException("Project not found"))
        val newProjectId = UUID.randomUUID().toString()
        val duplicatedProject = original.copy(
            id = newProjectId,
            name = "${original.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isArchived = false,
            archivedAt = null
        )
        dao.insertProject(duplicatedProject)

        val originalTasks = dao.getTasksByProjectOnce(projectId)
        val idMap = mutableMapOf<String, String>()
        val duplicatedTasks = originalTasks.map { task ->
            val newTaskId = UUID.randomUUID().toString()
            idMap[task.id] = newTaskId
            task.copy(
                id = newTaskId,
                projectId = newProjectId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }.map { task ->
            if (task.parentTaskId != null && idMap.containsKey(task.parentTaskId)) {
                task.copy(parentTaskId = idMap[task.parentTaskId])
            } else {
                task
            }
        }
        dao.insertTasksBulk(duplicatedTasks)

        val allDeps = dao.getAllDependenciesOnce()
        val duplicatedDeps = allDeps.filter { idMap.containsKey(it.taskId) && idMap.containsKey(it.dependsOnTaskId) }
            .map { dep ->
                TaskDependencyEntity(
                    id = UUID.randomUUID().toString(),
                    taskId = idMap[dep.taskId]!!,
                    dependsOnTaskId = idMap[dep.dependsOnTaskId]!!,
                    dependencyType = dep.dependencyType,
                    createdAt = System.currentTimeMillis()
                )
            }
        if (duplicatedDeps.isNotEmpty()) {
            dao.insertDependenciesBulk(duplicatedDeps)
        }

        return Result.success(duplicatedProject)
    }

    override suspend fun archiveProject(projectId: String) {
        val project = dao.getProjectByIdOnce(projectId) ?: return
        dao.updateProject(
            project.copy(
                isArchived = true,
                archivedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun restoreProject(projectId: String) {
        val project = dao.getProjectByIdOnce(projectId) ?: return
        dao.updateProject(
            project.copy(
                isArchived = false,
                archivedAt = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteProject(project: ProjectEntity) {
        dao.deleteProject(project)
    }

    override suspend fun saveCategory(category: CategoryEntity) {
        dao.insertCategory(category)
    }

    override suspend fun deleteCategory(categoryId: String, reassignToCategoryId: String?): Result<Unit> {
        val count = dao.countProjectsWithCategory(categoryId)
        if (count > 0 && reassignToCategoryId == null) {
            return Result.failure(IllegalStateException("Category contains $count active projects"))
        }
        if (count > 0 && reassignToCategoryId != null) {
            dao.reassignCategory(categoryId, reassignToCategoryId, System.currentTimeMillis())
        }
        dao.deleteCategoryById(categoryId)
        return Result.success(Unit)
    }

    override suspend fun saveTask(task: TaskEntity) {
        dao.insertTask(task)
        recalculateParentTaskFromSubtasks(task.parentTaskId)
        recalculateProjectProgress(task.projectId)
    }

    override suspend fun validateTaskHierarchy(taskId: String, newParentTaskId: String?, projectId: String): Result<Unit> {
        if (newParentTaskId == null) return Result.success(Unit)
        if (taskId == newParentTaskId) {
            return Result.failure(IllegalArgumentException("A task cannot be its own parent"))
        }
        val parent = dao.getTaskByIdOnce(newParentTaskId)
            ?: return Result.failure(IllegalArgumentException("Parent task not found"))
        if (parent.projectId != projectId) {
            return Result.failure(IllegalArgumentException("Parent task must belong to the same project"))
        }
        var curr: String? = parent.parentTaskId
        while (curr != null) {
            if (curr == taskId) {
                return Result.failure(IllegalArgumentException("Circular hierarchy detected"))
            }
            curr = dao.getTaskByIdOnce(curr)?.parentTaskId
        }
        return Result.success(Unit)
    }

    override suspend fun updateTaskStatus(taskId: String, newStatus: String) {
        val task = dao.getTaskByIdOnce(taskId) ?: return
        val completedAt = if (newStatus == "COMPLETED") System.currentTimeMillis() else null
        val progress = if (newStatus == "COMPLETED") 100.0 else if (task.progress == 100.0) 0.0 else task.progress
        dao.updateTask(
            task.copy(
                status = newStatus,
                completedAt = completedAt,
                progress = progress,
                updatedAt = System.currentTimeMillis()
            )
        )
        recalculateParentTaskFromSubtasks(task.parentTaskId)
        recalculateProjectProgress(task.projectId)
    }

    override suspend fun recordProgressUpdate(projectId: String, newProgress: Double, updateNote: String, source: String) {
        val project = dao.getProjectByIdOnce(projectId) ?: return
        dao.insertProgressLog(
            ProgressLogEntity(
                projectId = projectId,
                oldProgress = project.progress,
                newProgress = newProgress,
                updateNote = updateNote,
                source = source
            )
        )
        dao.updateProject(
            project.copy(
                progress = newProgress,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteTask(task: TaskEntity) {
        dao.deleteTask(task)
        recalculateParentTaskFromSubtasks(task.parentTaskId)
        recalculateProjectProgress(task.projectId)
    }

    override suspend fun updateTaskSortOrder(taskId: String, newSortOrder: Int) {
        dao.updateTaskSortOrder(taskId, newSortOrder)
    }

    override suspend fun addDependency(taskId: String, dependsOnTaskId: String, dependencyType: String): Result<Unit> {
        if (taskId == dependsOnTaskId) {
            return Result.failure(IllegalArgumentException("A task cannot depend on itself"))
        }
        val taskA = dao.getTaskByIdOnce(taskId) ?: return Result.failure(IllegalArgumentException("Task not found"))
        val taskB = dao.getTaskByIdOnce(dependsOnTaskId) ?: return Result.failure(IllegalArgumentException("Prerequisite task not found"))
        if (taskA.projectId != taskB.projectId) {
            return Result.failure(IllegalArgumentException("Tasks must belong to the same project"))
        }

        // BFS cycle check: check if taskA can reach dependsOnTaskId
        val allDeps = dao.getAllDependenciesOnce()
        val adj = mutableMapOf<String, MutableList<String>>()
        for (dep in allDeps) {
            adj.getOrPut(dep.taskId) { mutableListOf() }.add(dep.dependsOnTaskId)
        }
        val queue: Queue<String> = LinkedList()
        val visited = mutableSetOf<String>()
        queue.add(dependsOnTaskId)
        visited.add(dependsOnTaskId)
        while (queue.isNotEmpty()) {
            val curr = queue.poll()!!
            if (curr == taskId) {
                return Result.failure(IllegalArgumentException("Circular dependency detected"))
            }
            for (neighbor in adj[curr] ?: emptyList()) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor)
                }
            }
        }

        dao.insertDependency(
            TaskDependencyEntity(
                taskId = taskId,
                dependsOnTaskId = dependsOnTaskId,
                dependencyType = dependencyType
            )
        )
        return Result.success(Unit)
    }

    override suspend fun deleteDependency(dependencyId: String) {
        dao.deleteDependencyById(dependencyId)
    }

    override suspend fun deleteDependencyPair(taskId: String, dependsOnTaskId: String) {
        dao.deleteDependencyPair(taskId, dependsOnTaskId)
    }

    override suspend fun isTaskBlocked(taskId: String): Boolean {
        val deps = dao.getDependenciesForTaskOnce(taskId)
        for (dep in deps) {
            val prerequisite = dao.getTaskByIdOnce(dep.dependsOnTaskId)
            if (prerequisite != null && prerequisite.status != "COMPLETED") {
                return true
            }
        }
        return false
    }

    override suspend fun saveChecklistItem(item: ChecklistItemEntity) {
        dao.insertChecklistItem(item)
        recalculateTaskFromChecklist(item.taskId)
    }

    override suspend fun toggleChecklistItem(item: ChecklistItemEntity) {
        val updated = item.copy(
            isCompleted = !item.isCompleted,
            completedAt = if (!item.isCompleted) System.currentTimeMillis() else null,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateChecklistItem(updated)
        recalculateTaskFromChecklist(item.taskId)
    }

    override suspend fun deleteChecklistItem(item: ChecklistItemEntity) {
        dao.deleteChecklistItem(item)
        recalculateTaskFromChecklist(item.taskId)
    }

    override suspend fun addComment(taskId: String, content: String) {
        dao.insertComment(
            CommentEntity(
                taskId = taskId,
                content = content
            )
        )
    }

    override suspend fun deleteComment(comment: CommentEntity) {
        dao.deleteComment(comment)
    }

    override suspend fun saveNote(note: NoteEntity) {
        dao.insertNote(note)
    }

    override suspend fun deleteNote(note: NoteEntity) {
        dao.deleteNote(note)
    }

    override suspend fun exportBackupJson(): String {
        val backup = WorkspaceBackup(
            projects = dao.getAllProjectsForBackup(),
            tasks = dao.getAllTasksForBackup(),
            checklistItems = dao.getAllChecklistItemsForBackup(),
            comments = dao.getAllCommentsForBackup(),
            progressLogs = dao.getAllProgressLogsForBackup(),
            notes = dao.getAllNotesForBackup(),
            categories = dao.getAllCategoriesForBackup(),
            dependencies = dao.getAllDependenciesOnce()
        )
        val adapter = moshi.adapter(WorkspaceBackup::class.java)
        return adapter.toJson(backup)
    }

    override suspend fun restoreBackupJson(json: String): Result<WorkspaceBackup> {
        return try {
            val adapter = moshi.adapter(WorkspaceBackup::class.java)
            val backup = adapter.fromJson(json) ?: return Result.failure(IllegalArgumentException("Invalid JSON"))
            if (backup.categories.isNotEmpty()) dao.insertCategoriesBulk(backup.categories)
            if (backup.projects.isNotEmpty()) dao.insertProjectsBulk(backup.projects)
            if (backup.tasks.isNotEmpty()) dao.insertTasksBulk(backup.tasks)
            if (backup.checklistItems.isNotEmpty()) dao.insertChecklistBulk(backup.checklistItems)
            if (backup.comments.isNotEmpty()) dao.insertCommentsBulk(backup.comments)
            if (backup.progressLogs.isNotEmpty()) dao.insertProgressLogsBulk(backup.progressLogs)
            if (backup.notes.isNotEmpty()) dao.insertNotesBulk(backup.notes)
            if (backup.dependencies.isNotEmpty()) dao.insertDependenciesBulk(backup.dependencies)
            Result.success(backup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun seedInitialData() {
        if (dao.getAllProjectsForBackup().isNotEmpty()) return
        val catId = UUID.randomUUID().toString()
        val projId = UUID.randomUUID().toString()
        dao.insertCategory(
            CategoryEntity(
                id = catId,
                name = "General",
                description = "Default category",
                color = "#3B82F6"
            )
        )
        dao.insertProject(
            ProjectEntity(
                id = projId,
                name = "Sample Project",
                description = "Get started with My Workspace",
                categoryId = catId,
                status = "ACTIVE",
                progressMode = "CALCULATED"
            )
        )
        val task1 = TaskEntity(
            projectId = projId,
            title = "Explore Workspace features",
            description = "Check out projects, tasks, dependencies and AI assistant",
            status = "TODO",
            priority = "HIGH"
        )
        val task2 = TaskEntity(
            projectId = projId,
            title = "Ask AI Assistant for help",
            description = "Try natural language queries in Hindi, Hinglish or English",
            status = "TODO",
            priority = "MEDIUM"
        )
        dao.insertTask(task1)
        dao.insertTask(task2)
    }

    private suspend fun recalculateTaskFromChecklist(taskId: String) {
        val items = dao.getChecklistForTaskOnce(taskId)
        if (items.isEmpty()) return
        val completed = items.count { it.isCompleted }
        val newProgress = (completed.toDouble() / items.size.toDouble()) * 100.0
        val task = dao.getTaskByIdOnce(taskId) ?: return
        val newStatus = if (completed == items.size) "COMPLETED" else if (completed > 0) "IN_PROGRESS" else task.status
        dao.updateTask(task.copy(progress = newProgress, status = newStatus, updatedAt = System.currentTimeMillis()))
        recalculateParentTaskFromSubtasks(task.parentTaskId)
        recalculateProjectProgress(task.projectId)
    }

    private suspend fun recalculateParentTaskFromSubtasks(parentTaskId: String?) {
        if (parentTaskId == null) return
        val subtasks = dao.getSubtasksOnce(parentTaskId)
        if (subtasks.isEmpty()) return
        val avgProgress = subtasks.map { it.progress }.average()
        val allCompleted = subtasks.all { it.status == "COMPLETED" }
        val anyInProgress = subtasks.any { it.status == "IN_PROGRESS" || it.progress > 0.0 }
        val parent = dao.getTaskByIdOnce(parentTaskId) ?: return
        val newStatus = if (allCompleted) "COMPLETED" else if (anyInProgress) "IN_PROGRESS" else parent.status
        dao.updateTask(parent.copy(progress = avgProgress, status = newStatus, updatedAt = System.currentTimeMillis()))
        recalculateParentTaskFromSubtasks(parent.parentTaskId)
    }

    private suspend fun recalculateProjectProgress(projectId: String) {
        val project = dao.getProjectByIdOnce(projectId) ?: return
        if (project.progressMode != "CALCULATED") return
        val tasks = dao.getTasksByProjectOnce(projectId)
        if (tasks.isEmpty()) {
            dao.updateProject(project.copy(progress = 0.0, updatedAt = System.currentTimeMillis()))
            return
        }
        val avg = tasks.map { it.progress }.average()
        dao.updateProject(project.copy(progress = avg, updatedAt = System.currentTimeMillis()))
    }
}
