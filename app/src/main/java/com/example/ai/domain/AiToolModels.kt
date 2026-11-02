package com.example.ai.domain

sealed class ToolExecutionResult {
    data class Success(val message: String, val data: Any? = null) : ToolExecutionResult()
    data class Error(val message: String) : ToolExecutionResult()
    data class ClarificationRequired(
        val message: String,
        val candidates: List<DisambiguationCandidate>
    ) : ToolExecutionResult()
    data class ConfirmationRequired(
        val message: String,
        val action: PendingToolAction
    ) : ToolExecutionResult()
}

data class DisambiguationCandidate(
    val id: String,
    val title: String,
    val projectName: String? = null,
    val details: String? = null
)

data class PendingToolAction(
    val actionType: String,
    val entityId: String,
    val title: String,
    val parameters: Map<String, String> = emptyMap()
)

data class TaskSummary(
    val id: String,
    val title: String,
    val projectName: String?,
    val priority: String,
    val status: String,
    val dueDateFormatted: String?
)

data class ProjectStatusSummary(
    val id: String,
    val name: String,
    val status: String,
    val progressPercent: Int,
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int
)

data class TodayScheduleSummary(
    val totalDueToday: Int,
    val completedCount: Int,
    val pendingCount: Int,
    val overdueCount: Int,
    val tasks: List<TaskSummary>
)
