package com.example.ai.tools

import com.example.ai.cloud.AiToolCall
import com.example.ai.cloud.AiToolDefinition
import com.example.ai.cloud.AiToolParameter
import com.example.ai.domain.ToolExecutionResult

/**
 * Registry of safe Workspace tools exposed to Gemini function calling.
 * Validates parameters and routes calls to WorkspaceToolExecutor locally without SQL injection risk.
 */
class AiToolRegistry(
  private val executor: WorkspaceToolExecutor
) {

  val availableTools: List<AiToolDefinition> = listOf(
    AiToolDefinition(
      name = "getTodayTasks",
      description = "Retrieves tasks scheduled for today and overdue tasks from the Workspace.",
      parameters = emptyList()
    ),
    AiToolDefinition(
      name = "getTomorrowTasks",
      description = "Retrieves tasks scheduled for tomorrow from the Workspace.",
      parameters = emptyList()
    ),
    AiToolDefinition(
      name = "getPendingTasks",
      description = "Retrieves all uncompleted tasks in the Workspace ordered by priority and due date.",
      parameters = emptyList()
    ),
    AiToolDefinition(
      name = "getOverdueTasks",
      description = "Retrieves all uncompleted tasks whose deadline has already passed.",
      parameters = emptyList()
    ),
    AiToolDefinition(
      name = "getProjectStatus",
      description = "Retrieves status, task completion count, progress percentage, and blocked task count for a named project.",
      parameters = listOf(
        AiToolParameter("projectName", "string", "The name of the project to check", isRequired = true)
      )
    ),
    AiToolDefinition(
      name = "createTask",
      description = "Creates a new task in the Workspace database.",
      parameters = listOf(
        AiToolParameter("title", "string", "The title of the task to create", isRequired = true),
        AiToolParameter("projectName", "string", "The optional project to assign the task to", isRequired = false),
        AiToolParameter("priority", "string", "Priority level: Low, Medium, High, or Urgent", isRequired = false),
        AiToolParameter("dueDate", "number", "Optional due date timestamp in milliseconds", isRequired = false)
      )
    ),
    AiToolDefinition(
      name = "updateTask",
      description = "Updates status, priority, or progress of an existing task.",
      parameters = listOf(
        AiToolParameter("taskTitleOrId", "string", "The title or ID of the task to update", isRequired = true),
        AiToolParameter("newStatus", "string", "New status (e.g. To Do, In Progress, Completed)", isRequired = false),
        AiToolParameter("newPriority", "string", "New priority (Low, Medium, High, Urgent)", isRequired = false),
        AiToolParameter("progress", "number", "Progress percentage between 0.0 and 100.0", isRequired = false)
      )
    ),
    AiToolDefinition(
      name = "completeTask",
      description = "Marks an existing task as Completed. Checks prerequisite dependencies safely.",
      parameters = listOf(
        AiToolParameter("taskTitleOrId", "string", "The title or ID of the task to mark completed", isRequired = true),
        AiToolParameter("forceIfBlocked", "boolean", "Whether to force completion if prerequisites are incomplete", isRequired = false)
      )
    ),
    AiToolDefinition(
      name = "createReminder",
      description = "Schedules a task deadline in the Workspace database. Note: OS notifications are not scheduled at system level.",
      parameters = listOf(
        AiToolParameter("taskTitleOrId", "string", "The title or ID of the task to schedule", isRequired = true),
        AiToolParameter("dueDateMs", "number", "Target timestamp in milliseconds", isRequired = true)
      )
    ),
    AiToolDefinition(
      name = "saveMemory",
      description = "Saves a user preference, working instruction, or personal fact into persistent AI memory.",
      parameters = listOf(
        AiToolParameter("fact", "string", "The fact or instruction to remember", isRequired = true),
        AiToolParameter("category", "string", "Category: Preference, Schedule, Contact, Instruction, or Fact", isRequired = false)
      )
    ),
    AiToolDefinition(
      name = "searchMemory",
      description = "Searches persistent AI memory for remembered user facts and preferences.",
      parameters = listOf(
        AiToolParameter("query", "string", "Search keyword or phrase", isRequired = true)
      )
    )
  )

  /**
   * Executes a requested tool call safely locally with parameter extraction and type casting.
   */
  suspend fun executeTool(call: AiToolCall): ToolExecutionResult {
    val args = call.arguments
    return try {
      when (call.name) {
        "getTodayTasks" -> executor.getTodayTasks()
        "getTomorrowTasks" -> executor.getTomorrowTasks()
        "getPendingTasks" -> executor.getPendingTasks()
        "getOverdueTasks" -> executor.getOverdueTasks()

        "getProjectStatus" -> {
          val proj = args["projectName"]?.toString() ?: ""
          if (proj.isBlank()) {
            ToolExecutionResult.Error("Missing required parameter: projectName")
          } else {
            executor.getProjectStatus(proj)
          }
        }

        "createTask" -> {
          val title = args["title"]?.toString() ?: ""
          if (title.isBlank()) {
            ToolExecutionResult.Error("Missing required parameter: title")
          } else {
            val proj = args["projectName"]?.toString()
            val priority = args["priority"]?.toString() ?: "Medium"
            val dueDate = (args["dueDate"] as? Number)?.toLong()
            executor.createTask(title = title, projectName = proj, priority = priority, dueDate = dueDate)
          }
        }

        "updateTask" -> {
          val titleOrId = args["taskTitleOrId"]?.toString() ?: ""
          if (titleOrId.isBlank()) {
            ToolExecutionResult.Error("Missing required parameter: taskTitleOrId")
          } else {
            val newStatus = args["newStatus"]?.toString()
            val newPriority = args["newPriority"]?.toString()
            val progress = (args["progress"] as? Number)?.toDouble()
            executor.updateTask(
              taskTitleOrId = titleOrId,
              newStatus = newStatus,
              newPriority = newPriority,
              progress = progress
            )
          }
        }

        "completeTask" -> {
          val titleOrId = args["taskTitleOrId"]?.toString() ?: ""
          if (titleOrId.isBlank()) {
            ToolExecutionResult.Error("Missing required parameter: taskTitleOrId")
          } else {
            val force = (args["forceIfBlocked"] as? Boolean) ?: false
            executor.completeTask(taskTitleOrId = titleOrId, forceIfBlocked = force)
          }
        }

        "createReminder" -> {
          val titleOrId = args["taskTitleOrId"]?.toString() ?: ""
          val dueMs = (args["dueDateMs"] as? Number)?.toLong()
          if (titleOrId.isBlank() || dueMs == null) {
            ToolExecutionResult.Error("Missing required parameters: taskTitleOrId and dueDateMs")
          } else {
            executor.createReminder(taskTitleOrId = titleOrId, dueDateMs = dueMs)
          }
        }

        "saveMemory" -> {
          val fact = args["fact"]?.toString() ?: ""
          val category = args["category"]?.toString() ?: "Preference"
          if (fact.isBlank()) {
            ToolExecutionResult.Error("Missing required parameter: fact")
          } else {
            executor.saveMemory(category = category, fact = fact)
          }
        }

        "searchMemory" -> {
          val query = args["query"]?.toString() ?: ""
          executor.searchMemory(query)
        }

        else -> {
          ToolExecutionResult.Error("Unknown tool: ${call.name}")
        }
      }
    } catch (e: Exception) {
      ToolExecutionResult.Error("Tool '${call.name}' failed: ${e.message}", e)
    }
  }

  /**
   * Converts a ToolExecutionResult into a structured map suitable for model feedback.
   */
  fun formatToolResultForModel(result: ToolExecutionResult): Map<String, Any?> {
    return when (result) {
      is ToolExecutionResult.Success -> mapOf(
        "status" to "success",
        "message" to result.message,
        "data" to (result.data?.toString() ?: "")
      )
      is ToolExecutionResult.Error -> mapOf(
        "status" to "error",
        "error" to result.message
      )
      is ToolExecutionResult.ClarificationRequired -> mapOf(
        "status" to "clarification_required",
        "prompt" to result.prompt,
        "candidates" to result.candidates.map {
          mapOf("id" to it.id, "title" to it.title, "project" to (it.projectName ?: ""))
        }
      )
      is ToolExecutionResult.ConfirmationRequired -> mapOf(
        "status" to "confirmation_required",
        "prompt" to result.prompt,
        "warning" to result.warning,
        "action" to result.pendingAction.actionType
      )
    }
  }
}
