package com.example.domain.engine

import com.example.data.local.entity.ChecklistItemEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity

object ProgressCalculationEngine {

  /**
   * Calculates progress for a single task based on its subtasks, checklists, status, or manual input.
   */
  fun calculateTaskProgress(
    task: TaskEntity,
    subtasks: List<TaskEntity> = emptyList(),
    checklistItems: List<ChecklistItemEntity> = emptyList()
  ): Double {
    if (task.status == "Completed") return 100.0
    if (task.status == "Cancelled") return 0.0

    return when (task.progressMode) {
      "Manual" -> task.manualProgress.coerceIn(0.0, 100.0)

      "Weighted" -> {
        val validSubtasks = subtasks.filter { it.status != "Cancelled" && it.archivedAt == null }
        if (validSubtasks.isNotEmpty()) {
          calculateWeightedAverage(
            items = validSubtasks.map { it.weight to it.manualProgress.coerceIn(0.0, 100.0) }
          )
        } else if (checklistItems.isNotEmpty()) {
          val completedCount = checklistItems.count { it.isCompleted }
          (completedCount.toDouble() / checklistItems.size.toDouble()) * 100.0
        } else {
          task.manualProgress.coerceIn(0.0, 100.0)
        }
      }

      "Automatic" -> {
        val validSubtasks = subtasks.filter { it.status != "Cancelled" && it.archivedAt == null }
        if (validSubtasks.isNotEmpty()) {
          val total = validSubtasks.sumOf {
            if (it.status == "Completed") 100.0 else it.manualProgress.coerceIn(0.0, 100.0)
          }
          (total / validSubtasks.size).coerceIn(0.0, 100.0)
        } else if (checklistItems.isNotEmpty()) {
          val completedCount = checklistItems.count { it.isCompleted }
          ((completedCount.toDouble() / checklistItems.size.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        } else {
          task.manualProgress.coerceIn(0.0, 100.0)
        }
      }

      else -> task.manualProgress.coerceIn(0.0, 100.0)
    }
  }

  /**
   * Calculates overall project progress from its tasks and subtasks.
   */
  fun calculateProjectProgress(
    project: ProjectEntity,
    tasks: List<TaskEntity>,
    checklistsMap: Map<String, List<ChecklistItemEntity>> = emptyMap()
  ): Double {
    if (project.status == "Completed") return 100.0
    if (project.status == "Cancelled") return 0.0

    if (project.progressMode == "Manual") {
      return project.manualProgress.coerceIn(0.0, 100.0)
    }

    // Only consider root tasks or all active non-cancelled tasks
    val validTasks = tasks.filter { it.projectId == project.id && it.status != "Cancelled" && it.archivedAt == null }
    if (validTasks.isEmpty()) {
      return project.manualProgress.coerceIn(0.0, 100.0)
    }

    // Filter to root tasks for calculating top-level project progress
    val rootTasks = validTasks.filter { it.parentTaskId == null }.ifEmpty { validTasks }

    return if (project.progressMode == "Weighted") {
      val items = rootTasks.map { task ->
        val subtasks = validTasks.filter { it.parentTaskId == task.id }
        val checklist = checklistsMap[task.id] ?: emptyList()
        val taskProgress = calculateTaskProgress(task, subtasks, checklist)
        (task.weight.coerceAtLeast(0.1)) to taskProgress
      }
      calculateWeightedAverage(items)
    } else {
      // Automatic average
      val totalProgress = rootTasks.sumOf { task ->
        val subtasks = validTasks.filter { it.parentTaskId == task.id }
        val checklist = checklistsMap[task.id] ?: emptyList()
        calculateTaskProgress(task, subtasks, checklist)
      }
      (totalProgress / rootTasks.size).coerceIn(0.0, 100.0)
    }
  }

  /**
   * Calculates dashboard overall progress across active projects using project weights.
   */
  fun calculateDashboardProgress(
    projects: List<ProjectEntity>,
    projectProgressMap: Map<String, Double>
  ): Double {
    val activeProjects = projects.filter {
      it.archivedAt == null && it.status != "Cancelled" && it.status != "Archived"
    }
    if (activeProjects.isEmpty()) return 0.0

    val weightedPairs = activeProjects.map { project ->
      val pWeight = if (project.weight <= 0.0) 1.0 else project.weight
      val pProgress = projectProgressMap[project.id] ?: 0.0
      pWeight to pProgress
    }

    return calculateWeightedAverage(weightedPairs)
  }

  private fun calculateWeightedAverage(items: List<Pair<Double, Double>>): Double {
    if (items.isEmpty()) return 0.0
    val totalWeight = items.sumOf { it.first }
    if (totalWeight <= 0.0) {
      val avg = items.sumOf { it.second } / items.size
      return avg.coerceIn(0.0, 100.0)
    }
    val weightedSum = items.sumOf { it.first * it.second }
    return (weightedSum / totalWeight).coerceIn(0.0, 100.0)
  }
}
