package com.example.ai

import com.example.ai.domain.ToolExecutionResult
import com.example.ai.engine.CommandIntent
import com.example.ai.engine.LocalCommandEngine
import com.example.ai.tools.WorkspaceToolExecutor
import com.example.data.local.entity.AiMemoryEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.repository.IAiMemoryRepository
import com.example.domain.repository.IWorkspaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class LocalCommandEngineTest {

    private lateinit var mockWorkspaceRepo: MockWorkspaceRepository
    private lateinit var mockMemoryRepo: MockAiMemoryRepository
    private lateinit var toolExecutor: WorkspaceToolExecutor
    private lateinit var engine: LocalCommandEngine

    @Before
    fun setUp() {
        mockWorkspaceRepo = MockWorkspaceRepository()
        mockMemoryRepo = MockAiMemoryRepository()
        toolExecutor = WorkspaceToolExecutor(mockWorkspaceRepo, mockMemoryRepo)
        engine = LocalCommandEngine(toolExecutor)
    }

    @Test
    fun testParseHindiQueries() {
        val intent1 = engine.parse("आज कितने काम बाकी हैं?")
        assertTrue("Expected GetTodayTasks, got $intent1", intent1 is CommandIntent.GetTodayTasks)

        val intent2 = engine.parse("मेरे पेंडिंग टास्क दिखाओ")
        assertTrue("Expected GetPendingTasks, got $intent2", intent2 is CommandIntent.GetPendingTasks)

        val intent3 = engine.parse("कल क्या करना है?")
        assertTrue("Expected GetTomorrowTasks, got $intent3", intent3 is CommandIntent.GetTomorrowTasks)
    }

    @Test
    fun testParseHinglishQueries() {
        val intent1 = engine.parse("Aaj ke pending tasks batao")
        assertTrue("Expected GetPendingTasks, got $intent1", intent1 is CommandIntent.GetPendingTasks)

        val intent2 = engine.parse("Electricity bill ko complete mark karo")
        assertTrue("Expected CompleteTask, got $intent2", intent2 is CommandIntent.CompleteTask)
        if (intent2 is CommandIntent.CompleteTask) {
            assertTrue(intent2.taskTitleOrId.contains("Electricity bill", ignoreCase = true))
        }

        val intent3 = engine.parse("Project Alpha ka status kya hai?")
        assertTrue("Expected GetProjectStatus, got $intent3", intent3 is CommandIntent.GetProjectStatus)
        if (intent3 is CommandIntent.GetProjectStatus) {
            assertEquals("Alpha", intent3.projectName.trim())
        }

        val intent4 = engine.parse("Yaad rakhna mujhe weekly reports pasand hain")
        assertTrue("Expected SaveMemory, got $intent4", intent4 is CommandIntent.SaveMemory)

        val intent5 = engine.parse("Maine kya yaad rakhne ko bola tha?")
        assertTrue("Expected SearchMemory, got $intent5", intent5 is CommandIntent.SearchMemory)
    }

    @Test
    fun testParseEnglishQueries() {
        val intent1 = engine.parse("Show my overdue tasks")
        assertTrue("Expected GetOverdueTasks, got $intent1", intent1 is CommandIntent.GetOverdueTasks)

        val intent2 = engine.parse("What are my tasks for today?")
        assertTrue("Expected GetTodayTasks, got $intent2", intent2 is CommandIntent.GetTodayTasks)

        val intent3 = engine.parse("Create a task called Submit GST return")
        assertTrue("Expected CreateTask, got $intent3", intent3 is CommandIntent.CreateTask)
        if (intent3 is CommandIntent.CreateTask) {
            assertEquals("Submit GST return", intent3.title)
        }

        val intent4 = engine.parse("Mark Electricity Bill as done")
        assertTrue("Expected CompleteTask, got $intent4", intent4 is CommandIntent.CompleteTask)

        val intent5 = engine.parse("Remember that client meeting is on Friday")
        assertTrue("Expected SaveMemory, got $intent5", intent5 is CommandIntent.SaveMemory)

        val intent6 = engine.parse("What did I ask you to remember?")
        assertTrue("Expected SearchMemory, got $intent6", intent6 is CommandIntent.SearchMemory)
    }

    @Test
    fun testExecuteUnknownCommand() = runBlocking {
        val result = engine.execute("Some random gibberish 12345")
        assertTrue(result.intent is CommandIntent.Unknown)
        assertTrue(
            result.text.contains("didn't recognize", ignoreCase = true) ||
            result.text.contains("ask me", ignoreCase = true)
        )
    }

    @Test
    fun testExecuteCreateAndCompleteTask() = runBlocking {
        mockWorkspaceRepo.activeProjectsList.value = listOf(ProjectEntity(id = "p1", name = "Finance"))

        val createResult = engine.execute("Create a task called Pay Wifi Bill")
        assertTrue(createResult.intent is CommandIntent.CreateTask)
        assertTrue(createResult.toolResult is ToolExecutionResult.Success)

        val completeResult = engine.execute("Mark Pay Wifi Bill as complete")
        assertTrue(completeResult.intent is CommandIntent.CompleteTask)
        assertTrue(completeResult.toolResult is ToolExecutionResult.Success)
    }

    @Test
    fun testExecuteMemorySaveAndSearch() = runBlocking {
        val saveResult = engine.execute("Remember that my server IP is 192.168.1.10")
        assertTrue(saveResult.intent is CommandIntent.SaveMemory)
        assertTrue(saveResult.toolResult is ToolExecutionResult.Success)

        val searchResult = engine.execute("What did I ask you to remember?")
        assertTrue(searchResult.intent is CommandIntent.SearchMemory)
        assertTrue(searchResult.toolResult is ToolExecutionResult.Success)
        assertTrue(searchResult.text.contains("192.168.1.10"))
    }
}

class MockAiMemoryRepository : IAiMemoryRepository {
    private val memories = mutableListOf<AiMemoryEntity>()
    private val _flow = MutableStateFlow<List<AiMemoryEntity>>(emptyList())
    override val allMemories: Flow<List<AiMemoryEntity>> = _flow.asStateFlow()

    override fun searchMemories(query: String): Flow<List<AiMemoryEntity>> {
        val filtered = memories.filter { it.fact.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
        return flowOf(filtered)
    }

    override suspend fun searchMemoriesOnce(query: String): List<AiMemoryEntity> {
        return memories.filter { it.fact.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
    }

    override suspend fun getAllMemoriesOnce(): List<AiMemoryEntity> = memories.toList()

    override suspend fun saveMemory(category: String, fact: String): AiMemoryEntity {
        val mem = AiMemoryEntity(
            id = UUID.randomUUID().toString(),
            category = category,
            fact = fact,
            createdAt = System.currentTimeMillis()
        )
        memories.add(mem)
        _flow.value = memories.toList()
        return mem
    }

    override suspend fun deleteMemory(memory: AiMemoryEntity) {
        memories.removeAll { it.id == memory.id }
        _flow.value = memories.toList()
    }

    override suspend fun deleteMemoryById(id: String) {
        memories.removeAll { it.id == id }
        _flow.value = memories.toList()
    }
}

class MockWorkspaceRepository : IWorkspaceRepository {
    val activeProjectsList = MutableStateFlow<List<ProjectEntity>>(listOf(ProjectEntity(id = "p1", name = "Default Project")))
    val allTasksList = MutableStateFlow<List<TaskEntity>>(emptyList())
    val dependenciesList = MutableStateFlow<List<com.example.data.local.entity.TaskDependencyEntity>>(emptyList())

    override val activeProjects: Flow<List<ProjectEntity>> = activeProjectsList.asStateFlow()
    override val archivedProjects: Flow<List<ProjectEntity>> = flowOf(emptyList())
    override val categories: Flow<List<com.example.data.local.entity.CategoryEntity>> = flowOf(emptyList())
    override val allTasks: Flow<List<TaskEntity>> = allTasksList.asStateFlow()
    override val tasksWithDeadlines: Flow<List<TaskEntity>> = flowOf(emptyList())
    override val allNotes: Flow<List<com.example.data.local.entity.NoteEntity>> = flowOf(emptyList())
    override val recentActivities: Flow<List<com.example.data.local.entity.ActivityLogEntity>> = flowOf(emptyList())
    override val allDependencies: Flow<List<com.example.data.local.entity.TaskDependencyEntity>> = dependenciesList.asStateFlow()

    override fun getProjectById(id: String): Flow<ProjectEntity?> = flowOf(activeProjectsList.value.firstOrNull { it.id == id })
    override fun getProjectWithTasks(id: String): Flow<com.example.core.database.relation.ProjectWithTasks?> = flowOf(null)
    override fun getProjectWithCategoryAndTasks(id: String): Flow<com.example.core.database.relation.ProjectWithCategoryAndTasks?> = flowOf(null)
    override fun getTasksByProject(projectId: String): Flow<List<TaskEntity>> = flowOf(allTasksList.value.filter { it.projectId == projectId })
    override fun getRootTasksByProject(projectId: String): Flow<List<TaskEntity>> = flowOf(emptyList())
    override fun getSubtasks(parentTaskId: String): Flow<List<TaskEntity>> = flowOf(emptyList())
    override fun getTaskById(id: String): Flow<TaskEntity?> = flowOf(allTasksList.value.firstOrNull { it.id == id })
    override fun getTaskWithDetails(id: String): Flow<com.example.core.database.relation.TaskWithSubtasksAndDetails?> = flowOf(null)
    override fun getChecklistForTask(taskId: String): Flow<List<com.example.data.local.entity.ChecklistItemEntity>> = flowOf(emptyList())
    override fun getCommentsForTask(taskId: String): Flow<List<com.example.data.local.entity.CommentEntity>> = flowOf(emptyList())
    override fun getProgressLogsForProject(projectId: String): Flow<List<com.example.data.local.entity.ProgressLogEntity>> = flowOf(emptyList())
    override fun getProgressLogsForTask(taskId: String): Flow<List<com.example.data.local.entity.ProgressLogEntity>> = flowOf(emptyList())
    override fun getNotesForProject(projectId: String): Flow<List<com.example.data.local.entity.NoteEntity>> = flowOf(emptyList())
    override fun getDependenciesForTask(taskId: String): Flow<List<com.example.data.local.entity.TaskDependencyEntity>> =
        flowOf(dependenciesList.value.filter { it.taskId == taskId })
    override fun getDependentsOfTask(taskId: String): Flow<List<com.example.data.local.entity.TaskDependencyEntity>> =
        flowOf(dependenciesList.value.filter { it.dependsOnTaskId == taskId })

    override suspend fun saveProject(project: ProjectEntity) {
        activeProjectsList.value = activeProjectsList.value.filter { it.id != project.id } + project
    }
    override suspend fun duplicateProject(projectId: String): Result<ProjectEntity> = Result.success(activeProjectsList.value.first())
    override suspend fun archiveProject(projectId: String) {}
    override suspend fun restoreProject(projectId: String) {}
    override suspend fun deleteProject(project: ProjectEntity) {
        activeProjectsList.value = activeProjectsList.value.filter { it.id != project.id }
    }
    override suspend fun saveCategory(category: com.example.data.local.entity.CategoryEntity) {}
    override suspend fun deleteCategory(categoryId: String, reassignToCategoryId: String?): Result<Unit> = Result.success(Unit)
    override suspend fun saveTask(task: TaskEntity) {
        allTasksList.value = allTasksList.value.filter { it.id != task.id } + task
    }
    override suspend fun updateTaskStatus(taskId: String, newStatus: String) {
        allTasksList.value = allTasksList.value.map {
            if (it.id == taskId) it.copy(status = newStatus) else it
        }
    }
    override suspend fun updateTaskSortOrder(taskId: String, newSortOrder: Int) {}
    override suspend fun deleteTask(task: TaskEntity) {
        allTasksList.value = allTasksList.value.filter { it.id != task.id }
    }
    override suspend fun addDependency(taskId: String, dependsOnTaskId: String, dependencyType: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteDependency(dependencyId: String) {}
    override suspend fun deleteDependencyPair(taskId: String, dependsOnTaskId: String) {
        dependenciesList.value = dependenciesList.value.filterNot { it.taskId == taskId && it.dependsOnTaskId == dependsOnTaskId }
    }
    override suspend fun saveChecklistItem(item: com.example.data.local.entity.ChecklistItemEntity) {}
    override suspend fun toggleChecklistItem(item: com.example.data.local.entity.ChecklistItemEntity) {}
    override suspend fun deleteChecklistItem(item: com.example.data.local.entity.ChecklistItemEntity) {}
    override suspend fun addComment(taskId: String, content: String) {}
    override suspend fun deleteComment(comment: com.example.data.local.entity.CommentEntity) {}
    override suspend fun saveNote(note: com.example.data.local.entity.NoteEntity) {}
    override suspend fun deleteNote(note: com.example.data.local.entity.NoteEntity) {}
    override suspend fun recordProgressUpdate(projectId: String, newProgress: Double, updateNote: String, source: String) {}
    override suspend fun exportBackupJson(): String = "{}"
    override suspend fun restoreBackupJson(jsonString: String): Result<com.example.domain.model.WorkspaceBackup> =
        Result.failure(Exception("Not implemented"))
    override fun getActivitiesForEntity(entityId: String): Flow<List<com.example.data.local.entity.ActivityLogEntity>> = flowOf(emptyList())
    override suspend fun isTaskBlocked(taskId: String): Boolean {
        val deps = dependenciesList.value.filter { it.taskId == taskId }
        if (deps.isEmpty()) return false
        val taskMap = allTasksList.value.associateBy { it.id }
        return deps.any { dep ->
            val prereq = taskMap[dep.dependsOnTaskId]
            prereq != null && prereq.status != "COMPLETED"
        }
    }
    override suspend fun validateTaskHierarchy(taskId: String, parentTaskId: String?, projectId: String): Result<Unit> = Result.success(Unit)
    override suspend fun seedInitialData() {}
}

