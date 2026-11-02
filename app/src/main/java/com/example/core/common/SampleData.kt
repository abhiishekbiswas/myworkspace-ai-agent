package com.example.core.common

import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.model.DashboardStats

/**
 * Isolated sample / preview models used strictly for UI testing, component previews,
 * and early Phase 1 presentation verification without polluting persistent storage.
 */
object SampleData {

  val previewStats = DashboardStats(
    totalProjects = 4,
    activeProjects = 3,
    completedProjects = 1,
    pendingTasks = 8,
    overdueTasks = 2,
    completedTasks = 12,
    overallProgress = 68.0
  )

  val previewProjects = listOf(
    ProjectEntity(
      id = "sample_p1",
      name = "Android Application Development",
      description = "Build full native workspace management command center",
      color = "#2563EB",
      icon = "code",
      status = "Active",
      priority = "High",
      progressMode = "Weighted",
      manualProgress = 75.0,
      dueDate = System.currentTimeMillis() + 86400000L * 10
    ),
    ProjectEntity(
      id = "sample_p2",
      name = "YouTube Tech Content Series",
      description = "Video scripts, voiceover, and editing for 5 episodes",
      color = "#EF4444",
      icon = "video",
      status = "Active",
      priority = "High",
      progressMode = "Automatic",
      manualProgress = 50.0,
      dueDate = System.currentTimeMillis() + 86400000L * 5
    ),
    ProjectEntity(
      id = "sample_p3",
      name = "Instagram Automation Pipeline",
      description = "Batch schedule reels and track weekly reach metrics",
      color = "#8B5CF6",
      icon = "instagram",
      status = "Active",
      priority = "Medium",
      progressMode = "Automatic",
      manualProgress = 30.0,
      dueDate = System.currentTimeMillis() + 86400000L * 14
    )
  )

  val previewTasks = listOf(
    TaskEntity(
      id = "sample_t1",
      projectId = "sample_p1",
      title = "Implement Phase 1 UI & Architecture",
      description = "Theme system, top navigation, bottom bar, and dashboard cards",
      status = "In Progress",
      priority = "Urgent",
      manualProgress = 85.0,
      dueDate = System.currentTimeMillis() + 86400000L
    ),
    TaskEntity(
      id = "sample_t2",
      projectId = "sample_p2",
      title = "Record Audio Voiceover for Video #1",
      description = "Check audio levels and eliminate background noise",
      status = "To Do",
      priority = "High",
      manualProgress = 0.0,
      dueDate = System.currentTimeMillis() + 86400000L * 2
    ),
    TaskEntity(
      id = "sample_t3",
      projectId = "sample_p3",
      title = "Generate 10 Carousel Posts",
      description = "Export high-resolution PNGs and captions",
      status = "To Do",
      priority = "Medium",
      manualProgress = 20.0,
      dueDate = System.currentTimeMillis() + 86400000L * 3
    )
  )

  val previewActivities = listOf(
    ActivityLogEntity(
      id = "act_1",
      entityType = "Task",
      entityId = "sample_t1",
      action = "Updated",
      details = "Task 'Implement Phase 1 UI & Architecture' progress updated to 85%",
      createdAt = System.currentTimeMillis() - 1000L * 60 * 30
    ),
    ActivityLogEntity(
      id = "act_2",
      entityType = "Project",
      entityId = "sample_p1",
      action = "Created",
      details = "Project 'Android Application Development' initialized",
      createdAt = System.currentTimeMillis() - 1000L * 60 * 180
    )
  )
}
