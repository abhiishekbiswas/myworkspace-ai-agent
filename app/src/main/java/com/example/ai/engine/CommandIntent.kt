package com.example.ai.engine

import com.example.ai.domain.ToolExecutionResult

sealed class CommandIntent {
    object GetTodayTasks : CommandIntent()
    object GetTomorrowTasks : CommandIntent()
    object GetPendingTasks : CommandIntent()
    object GetOverdueTasks : CommandIntent()
    data class GetProjectStatus(val projectName: String) : CommandIntent()
    data class CreateTask(
        val title: String,
        val projectName: String? = null,
        val priority: String = "MEDIUM",
        val dueDate: Long? = null
    ) : CommandIntent()
    data class CompleteTask(
        val taskTitleOrId: String,
        val forceIfBlocked: Boolean = false
    ) : CommandIntent()
    data class UpdateTask(
        val taskTitleOrId: String,
        val newStatus: String? = null,
        val newPriority: String? = null,
        val progress: Double? = null
    ) : CommandIntent()
    data class CreateReminder(
        val taskTitleOrId: String,
        val dueDateMs: Long
    ) : CommandIntent()
    data class SaveMemory(
        val category: String,
        val fact: String
    ) : CommandIntent()
    data class SearchMemory(
        val query: String
    ) : CommandIntent()
    data class Unknown(val rawInput: String) : CommandIntent()
}

data class CommandResult(
    val text: String,
    val intent: CommandIntent,
    val toolResult: ToolExecutionResult? = null
)
