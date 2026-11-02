package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.WorkspaceDatabase
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ChecklistItemEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.repository.WorkspaceRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
class DatabaseRepositoryTest {

  private lateinit var database: WorkspaceDatabase
  private lateinit var repository: WorkspaceRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = WorkspaceDatabase.createInMemory(context)
    repository = WorkspaceRepository(database.workspaceDao())
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun testProjectAndTaskCrud() = runBlocking {
    val categoryId = UUID.randomUUID().toString()
    repository.saveCategory(
      CategoryEntity(
        id = categoryId,
        name = "Engineering",
        color = "#2563EB",
        icon = "code"
      )
    )

    val projectId = UUID.randomUUID().toString()
    val project = ProjectEntity(
      id = projectId,
      name = "Core Engine",
      description = "Next gen system",
      categoryId = categoryId,
      status = "Active",
      priority = "High"
    )
    repository.saveProject(project)

    val projects = repository.activeProjects.first()
    assertEquals(1, projects.size)
    assertEquals("Core Engine", projects[0].name)

    // Add task
    val taskId = UUID.randomUUID().toString()
    val task = TaskEntity(
      id = taskId,
      projectId = projectId,
      title = "Implement Security Layer",
      status = "To Do",
      priority = "Urgent"
    )
    repository.saveTask(task)

    val tasks = repository.getTasksByProject(projectId).first()
    assertEquals(1, tasks.size)
    assertEquals("Implement Security Layer", tasks[0].title)

    // Update status
    repository.updateTaskStatus(taskId, "Completed")
    val updatedTask = repository.getTaskById(taskId).first()
    assertNotNull(updatedTask)
    assertEquals("Completed", updatedTask?.status)
    assertEquals(100.0, updatedTask?.progress ?: 0.0, 0.01)
  }

  @Test
  fun testCategorySafeDeletionWhenInUse() = runBlocking {
    val cat1Id = UUID.randomUUID().toString()
    val cat2Id = UUID.randomUUID().toString()
    repository.saveCategory(CategoryEntity(cat1Id, "Frontend", null, "#3B82F6", "web"))
    repository.saveCategory(CategoryEntity(cat2Id, "Backend", null, "#10B981", "server"))

    val projId = UUID.randomUUID().toString()
    repository.saveProject(
      ProjectEntity(
        id = projId,
        name = "Web Dashboard",
        categoryId = cat1Id
      )
    )

    // Attempt to delete cat1 while in use with no reassignment
    val failResult = repository.deleteCategory(cat1Id)
    assertTrue("Should fail deleting category in use", failResult.isFailure)

    // Delete with reassignment to cat2
    val successResult = repository.deleteCategory(cat1Id, reassignToCategoryId = cat2Id)
    assertTrue("Should succeed when reassigning", successResult.isSuccess)

    val updatedProject = repository.getProjectById(projId).first()
    assertEquals(cat2Id, updatedProject?.categoryId)
  }

  @Test
  fun testTaskHierarchyCycleValidation() = runBlocking {
    val proj1Id = UUID.randomUUID().toString()
    val proj2Id = UUID.randomUUID().toString()
    repository.saveProject(ProjectEntity(id = proj1Id, name = "Project Alpha"))
    repository.saveProject(ProjectEntity(id = proj2Id, name = "Project Beta"))

    val taskAId = UUID.randomUUID().toString()
    val taskBId = UUID.randomUUID().toString()
    val taskCId = UUID.randomUUID().toString()

    repository.saveTask(TaskEntity(id = taskAId, projectId = proj1Id, title = "Task A"))
    repository.saveTask(TaskEntity(id = taskBId, projectId = proj1Id, parentTaskId = taskAId, title = "Task B"))
    repository.saveTask(TaskEntity(id = taskCId, projectId = proj1Id, parentTaskId = taskBId, title = "Task C"))

    // 1. Task cannot be its own parent
    val selfParentRes = repository.validateTaskHierarchy(taskAId, taskAId, proj1Id)
    assertTrue(selfParentRes.isFailure)

    // 2. Parent must belong to same project
    val crossProjTaskId = UUID.randomUUID().toString()
    repository.saveTask(TaskEntity(id = crossProjTaskId, projectId = proj2Id, title = "Other Project Task"))
    val crossProjRes = repository.validateTaskHierarchy(taskAId, crossProjTaskId, proj1Id)
    assertTrue(crossProjRes.isFailure)

    // 3. Circular reference: Attempt to make Task A a child of Task C (when C is already a descendant of A)
    val cycleRes = repository.validateTaskHierarchy(taskAId, taskCId, proj1Id)
    assertTrue("Circular hierarchy must be blocked", cycleRes.isFailure)
  }

  @Test
  fun testProjectDuplicationWithSubtasksAndChecklists() = runBlocking {
    val origProjId = UUID.randomUUID().toString()
    repository.saveProject(
      ProjectEntity(
        id = origProjId,
        name = "Alpha Template",
        description = "Reusable blueprint",
        status = "Active"
      )
    )

    val parentTaskId = UUID.randomUUID().toString()
    repository.saveTask(
      TaskEntity(
        id = parentTaskId,
        projectId = origProjId,
        title = "Sprint Planning"
      )
    )

    val subtaskId = UUID.randomUUID().toString()
    repository.saveTask(
      TaskEntity(
        id = subtaskId,
        projectId = origProjId,
        parentTaskId = parentTaskId,
        title = "Assign Story Points"
      )
    )

    val checkItemId = UUID.randomUUID().toString()
    repository.saveChecklistItem(
      ChecklistItemEntity(
        id = checkItemId,
        taskId = subtaskId,
        title = "Review backlog tickets",
        isCompleted = true
      )
    )

    // Duplicate project
    val dupResult = repository.duplicateProject(origProjId)
    assertTrue(dupResult.isSuccess)
    val duplicated = dupResult.getOrThrow()

    assertEquals("Alpha Template (Copy)", duplicated.name)
    assertEquals("Planning", duplicated.status)

    // Check duplicated tasks
    val dupTasks = repository.getTasksByProject(duplicated.id).first()
    assertEquals(2, dupTasks.size)

    val dupParent = dupTasks.find { it.title == "Sprint Planning" }
    val dupSubtask = dupTasks.find { it.title == "Assign Story Points" }
    assertNotNull(dupParent)
    assertNotNull(dupSubtask)

    // Verify subtask hierarchy was properly remapped to the new parent task!
    assertEquals(dupParent?.id, dupSubtask?.parentTaskId)

    // Verify checklist was duplicated
    val dupChecklist = repository.getChecklistForTask(dupSubtask!!.id).first()
    assertEquals(1, dupChecklist.size)
    assertEquals("Review backlog tickets", dupChecklist[0].title)
    assertFalse("Duplicated checklist should reset to not completed", dupChecklist[0].isCompleted)
  }

  @Test
  fun testChecklistAutomaticProgressRecalculation() = runBlocking {
    val projId = UUID.randomUUID().toString()
    repository.saveProject(ProjectEntity(id = projId, name = "Progress Testing"))

    val taskId = UUID.randomUUID().toString()
    repository.saveTask(
      TaskEntity(
        id = taskId,
        projectId = projId,
        title = "Build Feature X",
        status = "To Do",
        progressMode = "Automatic",
        manualProgress = 0.0
      )
    )

    val item1 = ChecklistItemEntity(UUID.randomUUID().toString(), taskId, "Step 1", false, 0)
    val item2 = ChecklistItemEntity(UUID.randomUUID().toString(), taskId, "Step 2", false, 1)
    repository.saveChecklistItem(item1)
    repository.saveChecklistItem(item2)

    // Toggle item 1
    repository.toggleChecklistItem(item1)
    var task = repository.getTaskById(taskId).first()
    assertEquals(50.0, task?.progress ?: 0.0, 0.01)
    assertEquals("In Progress", task?.status)

    // Toggle item 2
    repository.toggleChecklistItem(item2)
    task = repository.getTaskById(taskId).first()
    assertEquals(100.0, task?.progress ?: 0.0, 0.01)
    assertEquals("Completed", task?.status)
    assertNotNull(task?.completedAt)
  }

  @Test
  fun testTaskDependenciesAndBlockedStatus() = runBlocking {
    val projId = UUID.randomUUID().toString()
    repository.saveProject(ProjectEntity(id = projId, name = "Dependency Flow"))

    val taskPrereqId = UUID.randomUUID().toString()
    val taskDependentId = UUID.randomUUID().toString()

    repository.saveTask(
      TaskEntity(
        id = taskPrereqId,
        projectId = projId,
        title = "Prerequisite: Architecture Design",
        status = "To Do"
      )
    )

    repository.saveTask(
      TaskEntity(
        id = taskDependentId,
        projectId = projId,
        title = "Downstream: Implementation",
        status = "To Do"
      )
    )

    // 1. Task cannot depend on itself
    val selfDepRes = repository.addDependency(taskDependentId, taskDependentId)
    assertTrue("Self dependency must be rejected", selfDepRes.isFailure)

    // 2. Add valid dependency (Implementation depends on Architecture Design)
    val addDepRes = repository.addDependency(taskDependentId, taskPrereqId)
    assertTrue("Valid dependency should be added", addDepRes.isSuccess)

    // 3. Circular dependency check: Prerequisite cannot depend on downstream task
    val cycleDepRes = repository.addDependency(taskPrereqId, taskDependentId)
    assertTrue("Cycle dependency must be rejected", cycleDepRes.isFailure)

    // 4. Downstream task must be blocked since prerequisite is "To Do"
    assertTrue("Task should be blocked when prerequisite is To Do", repository.isTaskBlocked(taskDependentId))
    assertFalse("Prerequisite task should not be blocked", repository.isTaskBlocked(taskPrereqId))

    // 5. Query dependencies flow
    val deps = repository.getDependenciesForTask(taskDependentId).first()
    assertEquals(1, deps.size)
    assertEquals(taskPrereqId, deps[0].dependsOnTaskId)

    val dependents = repository.getDependentsOfTask(taskPrereqId).first()
    assertEquals(1, dependents.size)
    assertEquals(taskDependentId, dependents[0].taskId)

    // 6. Complete the prerequisite -> downstream task is unblocked
    repository.updateTaskStatus(taskPrereqId, "Completed")
    assertFalse("Task should be unblocked when prerequisite is completed", repository.isTaskBlocked(taskDependentId))

    // 7. Delete dependency pair
    repository.deleteDependencyPair(taskDependentId, taskPrereqId)
    val remainingDeps = repository.getDependenciesForTask(taskDependentId).first()
    assertTrue("Dependencies should be empty after deletion", remainingDeps.isEmpty())
  }

  @Test
  fun testMultiplePrerequisitesBlockedState() = runBlocking {
    val projId = UUID.randomUUID().toString()
    repository.saveProject(ProjectEntity(id = projId, name = "Multi Prereq Project"))

    val taskAId = UUID.randomUUID().toString()
    val taskBId = UUID.randomUUID().toString()
    val taskCId = UUID.randomUUID().toString()

    repository.saveTask(TaskEntity(id = taskAId, projectId = projId, title = "Task A (Dependent)", status = "To Do"))
    repository.saveTask(TaskEntity(id = taskBId, projectId = projId, title = "Task B (Prereq 1)", status = "To Do"))
    repository.saveTask(TaskEntity(id = taskCId, projectId = projId, title = "Task C (Prereq 2)", status = "To Do"))

    // A depends on B and C
    repository.addDependency(taskAId, taskBId)
    repository.addDependency(taskAId, taskCId)

    // Initially both B and C are To Do -> A must be blocked
    assertTrue("Task A must be blocked when both B and C are incomplete", repository.isTaskBlocked(taskAId))

    // Complete B only -> Task A must STILL be blocked because C is incomplete
    repository.updateTaskStatus(taskBId, "Completed")
    assertTrue("Task A must remain blocked when even one prerequisite (C) is incomplete", repository.isTaskBlocked(taskAId))

    // Complete C -> Both prerequisites are completed -> Task A must be unblocked
    repository.updateTaskStatus(taskCId, "Completed")
    assertFalse("Task A must be unblocked when all prerequisites are completed", repository.isTaskBlocked(taskAId))

    // Reopen B to In Progress -> Task A must immediately become blocked again
    repository.updateTaskStatus(taskBId, "In Progress")
    assertTrue("Task A must become blocked again when prerequisite B is reopened", repository.isTaskBlocked(taskAId))

    // Remove dependency on B (leaving only C which is completed) -> Task A must automatically be unblocked
    repository.deleteDependencyPair(taskAId, taskBId)
    assertFalse("Task A must automatically be unblocked after removing incomplete dependency B", repository.isTaskBlocked(taskAId))
  }

  @Test
  fun testPrerequisiteDeletionPreservesDependentTaskAndUnblocks() = runBlocking {
    val projId = UUID.randomUUID().toString()
    repository.saveProject(ProjectEntity(id = projId, name = "Cascade Deletion Project"))

    val taskDependent = TaskEntity(id = UUID.randomUUID().toString(), projectId = projId, title = "Dependent Task", status = "To Do")
    val taskPrereq = TaskEntity(id = UUID.randomUUID().toString(), projectId = projId, title = "Prerequisite Task", status = "To Do")

    repository.saveTask(taskDependent)
    repository.saveTask(taskPrereq)

    repository.addDependency(taskDependent.id, taskPrereq.id)
    assertTrue("Dependent task must be blocked initially", repository.isTaskBlocked(taskDependent.id))

    // Delete prerequisite task completely
    repository.deleteTask(taskPrereq)

    // 1. Verify prerequisite is removed
    val deletedPrereq = repository.getTaskById(taskPrereq.id).first()
    assertNull("Prerequisite task record must be deleted", deletedPrereq)

    // 2. Verify dependent task record is PRESERVED
    val preservedDependent = repository.getTaskById(taskDependent.id).first()
    assertNotNull("Dependent task record MUST be preserved after prerequisite deletion", preservedDependent)
    assertEquals("Dependent Task", preservedDependent?.title)

    // 3. Verify dependency junction record was cascaded away
    val remainingDeps = repository.getDependenciesForTask(taskDependent.id).first()
    assertTrue("Dependency records for deleted prerequisite must be cascaded away", remainingDeps.isEmpty())

    // 4. Verify dependent task is now unblocked
    assertFalse("Dependent task must be unblocked after prerequisite deletion", repository.isTaskBlocked(taskDependent.id))
  }

  @Test
  fun testConcurrentStatusUpdatesNoStaleBlockedState() = runBlocking {
    val projId = UUID.randomUUID().toString()
    repository.saveProject(ProjectEntity(id = projId, name = "Concurrent Test Project"))

    val taskAId = UUID.randomUUID().toString()
    repository.saveTask(TaskEntity(id = taskAId, projectId = projId, title = "Main Task A", status = "To Do"))

    val prereqIds = (1..5).map { idx ->
      val pId = UUID.randomUUID().toString()
      repository.saveTask(TaskEntity(id = pId, projectId = projId, title = "Prereq $idx", status = "To Do"))
      repository.addDependency(taskAId, pId)
      pId
    }

    assertTrue("Task A should be blocked by 5 prerequisites", repository.isTaskBlocked(taskAId))

    // Complete prerequisites 1 through 4 concurrently
    coroutineScope {
      val jobs = prereqIds.take(4).map { pId ->
        async {
          repository.updateTaskStatus(pId, "Completed")
        }
      }
      jobs.awaitAll()
    }

    // 5th prereq is still To Do -> A must still be blocked
    assertTrue("Task A must still be blocked because 5th prereq is incomplete", repository.isTaskBlocked(taskAId))

    // Complete 5th prereq
    repository.updateTaskStatus(prereqIds.last(), "Completed")
    assertFalse("Task A must be unblocked when all 5 prereqs are completed", repository.isTaskBlocked(taskAId))

    // Rapid toggle of 3rd prereq: To Do -> Completed
    repository.updateTaskStatus(prereqIds[2], "To Do")
    assertTrue("Task A must be blocked when 3rd prereq is set to To Do", repository.isTaskBlocked(taskAId))
    repository.updateTaskStatus(prereqIds[2], "Completed")
    assertFalse("Task A must be unblocked when 3rd prereq is Completed again", repository.isTaskBlocked(taskAId))
  }

  @Test
  fun testReactiveViewModelBlockedStateStream() = runBlocking {
    val viewModel = com.example.viewmodel.WorkspaceViewModel(repository)

    val projId = UUID.randomUUID().toString()
    repository.saveProject(ProjectEntity(id = projId, name = "Reactive VM Project"))

    val taskX = TaskEntity(id = UUID.randomUUID().toString(), projectId = projId, title = "Task X (Prereq)", status = "To Do")
    val taskY = TaskEntity(id = UUID.randomUUID().toString(), projectId = projId, title = "Task Y (Dependent)", status = "To Do")

    repository.saveTask(taskX)
    repository.saveTask(taskY)

    // Add dependency: Y depends on X
    repository.addDependency(taskY.id, taskX.id)

    // Check reactive repository Flow
    val allDeps = repository.allDependencies.first()
    assertEquals(1, allDeps.size)
    assertEquals(taskY.id, allDeps[0].taskId)
    assertEquals(taskX.id, allDeps[0].dependsOnTaskId)

    // Check blocked state via isTaskBlocked
    assertTrue("Task Y is blocked", repository.isTaskBlocked(taskY.id))

    // Complete Task X
    repository.updateTaskStatus(taskX.id, "Completed")
    assertFalse("Task Y is unblocked after Task X is completed", repository.isTaskBlocked(taskY.id))

    // Remove dependency
    repository.deleteDependencyPair(taskY.id, taskX.id)
    val remaining = repository.allDependencies.first()
    assertTrue("All dependencies should now be empty", remaining.isEmpty())
  }
}
