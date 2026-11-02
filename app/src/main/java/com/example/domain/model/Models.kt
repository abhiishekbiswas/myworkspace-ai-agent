package com.example.domain.model

import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ChecklistItemEntity
import com.example.data.local.entity.CommentEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ProgressLogEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskDependencyEntity
import com.example.data.local.entity.TaskEntity
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WorkspaceBackup(
  val version: Int = 1,
  val exportDate: Long = System.currentTimeMillis(),
  val appVersion: String = "1.0",
  val projects: List<ProjectEntity> = emptyList(),
  val categories: List<CategoryEntity> = emptyList(),
  val tasks: List<TaskEntity> = emptyList(),
  val dependencies: List<TaskDependencyEntity> = emptyList(),
  val checklistItems: List<ChecklistItemEntity> = emptyList(),
  val comments: List<CommentEntity> = emptyList(),
  val progressLogs: List<ProgressLogEntity> = emptyList(),
  val notes: List<NoteEntity> = emptyList()
)


data class DashboardStats(
  val totalProjects: Int = 0,
  val activeProjects: Int = 0,
  val completedProjects: Int = 0,
  val pendingTasks: Int = 0,
  val overdueTasks: Int = 0,
  val completedTasks: Int = 0,
  val overallProgress: Double = 0.0
)

enum class TaskSortOption {
  DUE_DATE_ASC,
  DUE_DATE_DESC,
  PRIORITY_DESC,
  PROGRESS_DESC,
  CREATED_DESC,
  TITLE_ASC
}

enum class ProjectSortOption {
  RECENT,
  DEADLINE,
  PROGRESS_DESC,
  NAME_ASC,
  PRIORITY_DESC
}
