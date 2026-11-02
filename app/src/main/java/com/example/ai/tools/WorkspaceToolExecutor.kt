package com.example.ai.tools

import com.example.ai.domain.*
import com.example.data.local.entity.TaskEntity
import com.example.domain.repository.IAiMemoryRepository
import com.example.domain.repository.IWorkspaceRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class WorkspaceToolExecutor(
    private val workspaceRepository: IWorkspaceRepository,
    private val aiMemoryRepository: IAiMemoryRepository
) {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private sealed class TaskResolutionResult {
        data class Resolved(val task: TaskEntity) : TaskResolutionResult()
        data class Ambiguous(val candidates: List<DisambiguationCandidate>) : TaskResolutionResult()
        object NotFound : TaskResolutionResult()
    }

    suspend fun getTodayTasks(): ToolExecutionResult {
        return try {
            val (startMs, endMs) = getTodayBounds()
            val allTasks = workspaceRepository.allTasks.first()
            val projects = workspaceRepository.activeProjects.first().associateBy { it.id }

            val todayTasks = allTasks.filter { task ->
                val due = task.dueDate
                due != null && due in startMs..endMs
            }
            val overdueTasks = allTasks.filter { task ->
                val due = task.dueDate
                due != null && due < startMs && task.status != "COMPLETED"
            }

            val combined = (todayTasks + overdueTasks).distinctBy { it.id }
            val taskSummaries = combined.map { task ->
                TaskSummary(
                    id = task.id,
                    title = task.title,
                    projectName = projects[task.projectId]?.name,
                    priority = task.priority,
                    status = task.status,
                    dueDateFormatted = task.dueDate?.let { dateFormat.format(Date(it)) }
                )
            }

            val summary = TodayScheduleSummary(
                totalDueToday = todayTasks.size,
                completedCount = todayTasks.count { it.status == "COMPLETED" },
                pendingCount = todayTasks.count { it.status != "COMPLETED" },
                overdueCount = overdueTasks.size,
                tasks = taskSummaries
            )

            val msg = buildString {
                append("📅 Today's Agenda: ${summary.pendingCount} pending, ${summary.completedCount} completed.")
                if (summary.overdueCount > 0) {
                    append(" ⚠️ ${summary.overdueCount} overdue tasks require attention.")
                }
            }
            ToolExecutionResult.Success(msg, summary)
        } catch (e: Exception) {
            ToolExecutionResult.Error("Failed to fetch today's tasks: ${e.message}")
        }
    }

    suspend fun getPendingTasks(): ToolExecutionResult {
        return try {
            val allTasks = workspaceRepository.allTasks.first()
            val projects = workspaceRepository.activeProjects.first().associateBy { it.id }
            val pending = allTasks.filter { it.status != "COMPLETED" }
                .sortedWith(compareByDescending<TaskEntity> { priorityWeight(it.priority) }.thenBy { it.dueDate ?: Long.MAX_VALUE })

            val summaries = pending.map { task ->
                TaskSummary(
                    id = task.id,
                    title = task.title,
                    projectName = projects[task.projectId]?.name,
                    priority = task.priority,
                    status = task.status,
                    dueDateFormatted = task.dueDate?.let { dateFormat.format(Date(it)) }
                )
            }
            ToolExecutionResult.Success("Found ${pending.size} pending tasks across your workspace.", summaries)
        } catch (e: Exception) {
            ToolExecutionResult.Error("Failed to fetch pending tasks: ${e.message}")
        }
    }

    suspend fun getOverdueTasks(): ToolExecutionResult {
        return try {
            val now = System.currentTimeMillis()
            val allTasks = workspaceRepository.allTasks.first()
            val projects = workspaceRepository.activeProjects.first().associateBy { it.id }

            val overdue = allTasks.filter { task ->
                val due = task.dueDate
                due != null && due < now && task.status != "COMPLETED"
            }.sortedBy { it.dueDate }

            val summaries = overdue.map { task ->
                TaskSummary(
                    id = task.id,
                    title = task.title,
                    projectName = projects[task.projectId]?.name,
                    priority = task.priority,
                    status = task.status,
                    dueDateFormatted = task.dueDate?.let { dateFormat.format(Date(it)) }
                )
            }

            val msg = if (overdue.isEmpty()) {
                "🎉 Excellent! You have no overdue tasks."
            } else {
                "⚠️ You have ${overdue.size} overdue tasks that missed their deadlines."
            }
            ToolExecutionResult.Success(msg, summaries)
        } catch (e: Exception) {
            ToolExecutionResult.Error("Failed to fetch overdue tasks: ${e.message}")
        }
    }

    suspend fun getTomorrowTasks(): ToolExecutionResult {
        return try {
            val (startMs, endMs) = getTomorrowBounds()
            val allTasks = workspaceRepository.allTasks.first()
            val projects = workspaceRepository.activeProjects.first().associateBy { it.id }

            val tomorrowTasks = allTasks.filter { task ->
                val due = task.dueDate
                due != null && due in startMs..endMs
            }

            val summaries = tomorrowTasks.map { task ->
                TaskSummary(
                    id = task.id,
                    title = task.title,
                    projectName = projects[task.projectId]?.name,
                    priority = task.priority,
                    status = task.status,
                    dueDateFormatted = task.dueDate?.let { dateFormat.format(Date(it)) }
                )
            }
            ToolExecutionResult.Success("Found ${tomorrowTasks.size} tasks scheduled for tomorrow.", summaries)
        } catch (e: Exception) {
            ToolExecutionResult.Error("Failed to fetch tomorrow's tasks: ${e.message}")
        }
    }

    suspend fun getProjectStatus(projectName: String): ToolExecutionResult {
        return try {
            val projects = workspaceRepository.activeProjects.first()
            val query = projectName.trim().lowercase(Locale.getDefault())

            val matched = projects.filter { it.name.lowercase(Locale.getDefault()).contains(query) }
            if (matched.isEmpty()) {
                return ToolExecutionResult.Error("Project '$projectName' not found in your workspace.")
            }
            if (matched.size > 1) {
                val candidates = matched.map { p ->
                    DisambiguationCandidate(
                        id = p.id,
                        title = p.name,
                        details = "Status: ${p.status}, Progress: ${p.progress.toInt()}%"
                    )
                }
                return ToolExecutionResult.ClarificationRequired(
                    "Multiple projects matched '$projectName'. Please select one:",
                    candidates
                )
            }

            val targetProject = matched.first()
            val tasks = workspaceRepository.getTasksByProject(targetProject.id).first()
            val completed = tasks.count { it.status == "COMPLETED" }
            val pending = tasks.count { it.status != "COMPLETED" }

            val summary = ProjectStatusSummary(
                id = targetProject.id,
                name = targetProject.name,
                status = targetProject.status,
                progressPercent = targetProject.progress.toInt(),
                totalTasks = tasks.size,
                completedTasks = completed,
                pendingTasks = pending
            )
            val msg = "📊 Project '${targetProject.name}' is ${targetProject.status} at ${targetProject.progress.toInt()}% progress (${completed}/${tasks.size} tasks completed)."
            ToolExecutionResult.Success(msg, summary)
        } catch (e: Exception) {
            ToolExecutionResult.Error("Failed to fetch project status: ${e.message}")
        }
    }

    suspend fun searchMemory(query: String): ToolExecutionResult {
        return try {
            val trimmed = query.trim()
            val memories = if (trimmed.isEmpty()) {
                aiMemoryRepository.getAllMemoriesOnce()
            } else {
                aiMemoryRepository.searchMemoriesOnce(trimmed)
            }
            if (memories.isEmpty()) {
                ToolExecutionResult.Success("No memory records found matching '$query'.", emptyList<String>())
            } else {
                val items = memories.map { "[${it.category}] ${it.fact}" }
                val msg = "Found ${memories.size} remembered item(s):\n" + items.joinToString("\n") { "• $it" }
                ToolExecutionResult.Success(msg, items)
            }
        } catch (e: Exception) {
            ToolExecutionResult.Error("Failed to search memory: ${e.message}")
        }
    }

    suspend fun createTask(
        title: String,
        projectName: String? = null,
        priority: String = "MEDIUM",
        dueDate: Long? = null
    ): ToolExecutionResult {
        return try {
            val cleanTitle = title.trim()
            if (cleanTitle.isEmpty()) {
                return ToolExecutionResult.Error("Task title cannot be empty.")
            }

            val projects = workspaceRepository.activeProjects.first()
            if (projects.isEmpty()) {
                return ToolExecutionResult.Error("No active project available. Please create a project first.")
            }

            val targetProject = if (projectName != null && projectName.isNotBlank()) {
                val matched = projects.filter { it.name.lowercase(Locale.getDefault()).contains(projectName.trim().lowercase(Locale.getDefault())) }
                if (matched.isEmpty()) {
                    return ToolExecutionResult.Error("Project '$projectName' not found.")
                }
                if (matched.size > 1) {
                    val candidates = matched.map { p ->
                        DisambiguationCandidate(id = p.id, title = p.name, details = "Status: ${p.status}")
                    }
                    return ToolExecutionResult.ClarificationRequired(
                        "Multiple projects matched '$projectName'. Where should '$cleanTitle' be created?",
                        candidates
                    )
                }
                matched.first()
            } else {
                projects.first()
            }

            val cleanPriority = when (priority.uppercase(Locale.getDefault())) {
                "HIGH", "URGENT", "LOW" -> priority.uppercase(Locale.getDefault())
                else -> "MEDIUM"
            }

            val task = TaskEntity(
                id = UUID.randomUUID().toString(),
                projectId = targetProject.id,
                title = cleanTitle,
                priority = cleanPriority,
                dueDate = dueDate,
                status = "TODO",
                progressMode = "MANUAL",
                progress = 0.0
            )

            workspaceRepository.saveTask(task)
            val dueInfo = dueDate?.let { " (Due: ${dateFormat.format(Date(it))})" } ?: ""
            ToolExecutionResult.Success(
                "✅ Created task '$cleanTitle' in project '${targetProject.name}' with $cleanPriority priority$dueInfo.",
                task
            )
        } catch (e: Exception) {
            ToolExecutionResult.Error("Failed to create task: ${e.message}")
        }
    }

    suspend fun completeTask(
        taskTitleOrId: String,
        forceIfBlocked: Boolean = false
    ): ToolExecutionResult {
        return try {
            val resolution = resolveTask(taskTitleOrId)
            when (resolution) {
                is TaskResolutionResult.NotFound -> {
                    ToolExecutionResult.Error("Task '$taskTitleOrId' not found.")
                }
                is TaskResolutionResult.Ambiguous -> {
                    ToolExecutionResult.ClarificationRequired(
                        "Multiple tasks matched '$taskTitleOrId'. Which one would you like to complete?",
                        resolution.candidates
                    )
                }
                is TaskResolutionResult.Resolved -> {
                    val task = resolution.task
                    if (task.status == "COMPLETED") {
                        return ToolExecutionResult.Success("Task '${task.title}' is already marked as completed.", task)
                    }

                    val isBlocked = workspaceRepository.isTaskBlocked(task.id)
                    if (isBlocked && !forceIfBlocked) {
                        return ToolExecutionResult.ConfirmationRequired(
                            "⚠️ Task '${task.title}' is currently BLOCKED by incomplete prerequisites. Are you sure you want to complete it anyway?",
                            PendingToolAction(
                                actionType = "FORCE_COMPLETE_TASK",
                                entityId = task.id,
                                title = task.title
                            )
                        )
                    }

                    workspaceRepository.updateTaskStatus(task.id, "COMPLETED")
                    ToolExecutionResult.Success("✅ Completed task '${task.title}'.", task)
                }
            }
        } catch (e: Exception) {
            ToolExecutionResult.Error("Failed to complete task: ${e.message}")
        }
    }

    suspend fun updateTask(
        taskTitleOrId: String,
        newStatus: String? = null,
        newPriority: String? = null,
        progress: Double? = null
    ): ToolExecutionResult {
        return try {
            val resolution = resolveTask(taskTitleOrId)
            when (resolution) {
                is TaskResolutionResult.NotFound -> {
                    ToolExecutionResult.Error("Task '$taskTitleOrId' not found.")
                }
                is TaskResolutionResult.Ambiguous -> {
                    ToolExecutionResult.ClarificationRequired(
                        "Multiple tasks matched '$taskTitleOrId'. Which one would you like to update?",
                        resolution.candidates
                    )
                }
                is TaskResolutionResult.Resolved -> {
                    val task = resolution.task
                    var updated = task

                    if (newStatus != null) {
                        workspaceRepository.updateTaskStatus(task.id, newStatus.uppercase(Locale.getDefault()))
                        updated = updated.copy(status = newStatus.uppercase(Locale.getDefault()))
                    }
                    if (newPriority != null) {
                        updated = updated.copy(priority = newPriority.uppercase(Locale.getDefault()))
                        workspaceRepository.saveTask(updated)
                    }
                    if (progress != null) {
                        updated = updated.copy(progress = progress.coerceIn(0.0, 100.0), manualProgress = progress.coerceIn(0.0, 100.0))
                        workspaceRepository.saveTask(updated)
                    }
                    ToolExecutionResult.Success("✅ Updated task '${task.title}'.", updated)
                }
            }
        } catch (e: Exception) {
            ToolExecutionResult.Error("Failed to update task: ${e.message}")
        }
    }

    suspend fun createReminder(
        taskTitleOrId: String,
        dueDateMs: Long
    ): ToolExecutionResult {
        return try {
            val resolution = resolveTask(taskTitleOrId)
            when (resolution) {
                is TaskResolutionResult.NotFound -> {
                    ToolExecutionResult.Error("Task '$taskTitleOrId' not found.")
                }
                is TaskResolutionResult.Ambiguous -> {
                    ToolExecutionResult.ClarificationRequired(
                        "Multiple tasks matched '$taskTitleOrId'. Which task should the reminder be set for?",
                        resolution.candidates
                    )
                }
                is TaskResolutionResult.Resolved -> {
                    val task = resolution.task
                    val updated = task.copy(dueDate = dueDateMs, updatedAt = System.currentTimeMillis())
                    workspaceRepository.saveTask(updated)
                    val formatted = "${dateFormat.format(Date(dueDateMs))} at ${timeFormat.format(Date(dueDateMs))}"
                    // Explicitly document notification limitation as required by prompt
                    ToolExecutionResult.Success(
                        "⏰ Reminder scheduled for '${task.title}' on $formatted. (Note: In-app schedule updated; system push notifications require upcoming OS alarm integration).",
                        updated
                    )
                }
            }
        } catch (e: Exception) {
            ToolExecutionResult.Error("Failed to create reminder: ${e.message}")
        }
    }

    suspend fun saveMemory(
        category: String,
        fact: String
    ): ToolExecutionResult {
        return try {
            val cleanFact = fact.trim()
            if (cleanFact.isEmpty()) {
                return ToolExecutionResult.Error("Memory fact cannot be empty.")
            }
            val cleanCat = category.trim().ifEmpty { "General" }
            val saved = aiMemoryRepository.saveMemory(cleanCat, cleanFact)
            ToolExecutionResult.Success("🧠 Remembered: [${saved.category}] ${saved.fact}", saved)
        } catch (e: Exception) {
            ToolExecutionResult.Error("Failed to save memory: ${e.message}")
        }
    }

    private suspend fun resolveTask(taskTitleOrId: String): TaskResolutionResult {
        val allTasks = workspaceRepository.allTasks.first()
        val projects = workspaceRepository.activeProjects.first().associateBy { it.id }

        // Check exact ID match first
        val exactId = allTasks.firstOrNull { it.id == taskTitleOrId }
        if (exactId != null) return TaskResolutionResult.Resolved(exactId)

        val query = taskTitleOrId.trim().lowercase(Locale.getDefault())

        // Check exact title match
        val exactTitle = allTasks.filter { it.title.trim().lowercase(Locale.getDefault()) == query }
        if (exactTitle.size == 1) return TaskResolutionResult.Resolved(exactTitle.first())

        // Check substring match
        val partial = allTasks.filter { it.title.lowercase(Locale.getDefault()).contains(query) }
        return when {
            partial.isEmpty() -> TaskResolutionResult.NotFound
            partial.size == 1 -> TaskResolutionResult.Resolved(partial.first())
            else -> {
                val candidates = partial.map { t ->
                    DisambiguationCandidate(
                        id = t.id,
                        title = t.title,
                        projectName = projects[t.projectId]?.name,
                        details = "Priority: ${t.priority}, Status: ${t.status}"
                    )
                }
                TaskResolutionResult.Ambiguous(candidates)
            }
        }
    }

    private fun priorityWeight(priority: String): Int {
        return when (priority.uppercase(Locale.getDefault())) {
            "URGENT" -> 4
            "HIGH" -> 3
            "MEDIUM" -> 2
            "LOW" -> 1
            else -> 0
        }
    }

    private fun getTodayBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    private fun getTomorrowBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }
}
