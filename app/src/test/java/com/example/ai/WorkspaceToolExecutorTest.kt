package com.example.ai

import com.example.ai.domain.DisambiguationCandidate
import com.example.ai.domain.ToolExecutionResult
import com.example.ai.tools.WorkspaceToolExecutor
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskDependencyEntity
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class WorkspaceToolExecutorTest {

    private lateinit var workspaceRepo: MockWorkspaceRepository
    private lateinit var memoryRepo: MockAiMemoryRepository
    private lateinit var executor: WorkspaceToolExecutor

    @Before
    fun setUp() {
        workspaceRepo = MockWorkspaceRepository()
        memoryRepo = MockAiMemoryRepository()
        executor = WorkspaceToolExecutor(workspaceRepo, memoryRepo)
    }

    @Test
    fun testGetTodayTasks() = runBlocking {
        val now = System.currentTimeMillis()
        val task1 = TaskEntity(id = "t1", title = "Task Today", projectId = "p1", dueDate = now, status = "PENDING")
        val task2 = TaskEntity(id = "t2", title = "Task Other Day", projectId = "p1", dueDate = now + 86400000L * 5, status = "PENDING")
        workspaceRepo.allTasksList.value = listOf(task1, task2)

        val result = executor.getTodayTasks()
        assertTrue(result is ToolExecutionResult.Success)
        val success = result as ToolExecutionResult.Success
        assertTrue(success.message.contains("Today's Agenda"))
    }

    @Test
    fun testGetPendingTasks() = runBlocking {
        val task1 = TaskEntity(id = "t1", title = "Pending 1", projectId = "p1", status = "PENDING")
        val task2 = TaskEntity(id = "t2", title = "Pending 2", projectId = "p1", status = "IN_PROGRESS")
        val task3 = TaskEntity(id = "t3", title = "Completed", projectId = "p1", status = "COMPLETED")
        workspaceRepo.allTasksList.value = listOf(task1, task2, task3)

        val result = executor.getPendingTasks()
        assertTrue(result is ToolExecutionResult.Success)
        val success = result as ToolExecutionResult.Success
        assertTrue(success.message.contains("Found 2 pending tasks"))
    }

    @Test
    fun testGetOverdueTasks() = runBlocking {
        val past = System.currentTimeMillis() - 86400000L * 2
        val task1 = TaskEntity(id = "t1", title = "Overdue Task", projectId = "p1", dueDate = past, status = "PENDING")
        val task2 = TaskEntity(id = "t2", title = "Future Task", projectId = "p1", dueDate = System.currentTimeMillis() + 86400000L, status = "PENDING")
        workspaceRepo.allTasksList.value = listOf(task1, task2)

        val result = executor.getOverdueTasks()
        assertTrue(result is ToolExecutionResult.Success)
        val success = result as ToolExecutionResult.Success
        assertTrue(success.message.contains("1 overdue task"))
    }

    @Test
    fun testGetTomorrowTasks() = runBlocking {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        val tomorrowMs = cal.timeInMillis

        val task1 = TaskEntity(id = "t1", title = "Tomorrow Task", projectId = "p1", dueDate = tomorrowMs, status = "PENDING")
        workspaceRepo.allTasksList.value = listOf(task1)

        val result = executor.getTomorrowTasks()
        assertTrue(result is ToolExecutionResult.Success)
        val success = result as ToolExecutionResult.Success
        assertTrue(success.message.contains("1 tasks scheduled for tomorrow") || success.message.contains("1 task"))
    }

    @Test
    fun testGetProjectStatus() = runBlocking {
        val project = ProjectEntity(id = "p-beta", name = "Project Beta", progress = 75.0, status = "ACTIVE")
        workspaceRepo.activeProjectsList.value = listOf(project)
        val task1 = TaskEntity(id = "t1", title = "Task 1", projectId = "p-beta", status = "COMPLETED")
        val task2 = TaskEntity(id = "t2", title = "Task 2", projectId = "p-beta", status = "PENDING")
        workspaceRepo.allTasksList.value = listOf(task1, task2)

        val result = executor.getProjectStatus("Beta")
        assertTrue(result is ToolExecutionResult.Success)
        val success = result as ToolExecutionResult.Success
        assertTrue(success.message.contains("Project 'Project Beta'"))
        assertTrue(success.message.contains("75%"))
    }

    @Test
    fun testCompleteTaskSafetyWhenBlocked() = runBlocking {
        val prereq = TaskEntity(id = "t-prereq", title = "Prereq Task", projectId = "p1", status = "PENDING")
        val dependent = TaskEntity(id = "t-dep", title = "Dependent Task", projectId = "p1", status = "PENDING")
        val dep = TaskDependencyEntity(id = "d1", taskId = "t-dep", dependsOnTaskId = "t-prereq")

        workspaceRepo.allTasksList.value = listOf(prereq, dependent)
        workspaceRepo.dependenciesList.value = listOf(dep)

        // Completing without force should require confirmation
        val result = executor.completeTask("Dependent Task", forceIfBlocked = false)
        assertTrue("Expected ConfirmationRequired, got $result", result is ToolExecutionResult.ConfirmationRequired)

        // Completing with force should succeed
        val forceResult = executor.completeTask("Dependent Task", forceIfBlocked = true)
        assertTrue("Expected Success on force complete, got $forceResult", forceResult is ToolExecutionResult.Success)
    }

    @Test
    fun testTaskDisambiguationOnMultipleMatches() = runBlocking {
        val task1 = TaskEntity(id = "t1", title = "Submit Report Alpha", projectId = "p1", status = "PENDING")
        val task2 = TaskEntity(id = "t2", title = "Submit Report Beta", projectId = "p1", status = "PENDING")
        workspaceRepo.allTasksList.value = listOf(task1, task2)

        val result = executor.completeTask("Submit Report")
        assertTrue("Expected ClarificationRequired, got $result", result is ToolExecutionResult.ClarificationRequired)
        val clarification = result as ToolExecutionResult.ClarificationRequired
        assertEquals(2, clarification.candidates.size)
    }

    @Test
    fun testSaveAndSearchMemory() = runBlocking {
        val saveResult = executor.saveMemory(category = "Preferences", fact = "Prefers dark theme and compact view")
        assertTrue(saveResult is ToolExecutionResult.Success)

        val searchResult = executor.searchMemory("dark theme")
        assertTrue(searchResult is ToolExecutionResult.Success)
        val success = searchResult as ToolExecutionResult.Success
        assertTrue(success.message.contains("Prefers dark theme"))
    }
}
