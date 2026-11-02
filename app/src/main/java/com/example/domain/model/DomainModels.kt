package com.example.domain.model

enum class ProjectStatus {
    ACTIVE, ON_HOLD, COMPLETED, CANCELLED
}

enum class TaskStatus {
    TODO, IN_PROGRESS, REVIEW, COMPLETED
}

enum class TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}

enum class ProgressMode {
    MANUAL, CALCULATED
}

enum class ProjectSortOption {
    NAME, CREATED_DATE, DUE_DATE, PROGRESS, PRIORITY
}

enum class TaskSortOption {
    SORT_ORDER, TITLE, DUE_DATE, PRIORITY, STATUS
}

data class DashboardStats(
    val totalProjects: Int = 0,
    val activeProjects: Int = 0,
    val completedProjects: Int = 0,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val overdueTasks: Int = 0,
    val overallProgress: Double = 0.0
)

data class Project(
    val id: String,
    val name: String,
    val description: String = "",
    val categoryId: String? = null,
    val color: String = "#3B82F6",
    val icon: String = "Folder",
    val status: String = "ACTIVE",
    val priority: String = "MEDIUM",
    val progressMode: String = "CALCULATED",
    val manualProgress: Double = 0.0,
    val progress: Double = 0.0,
    val weight: Double = 1.0,
    val isArchived: Boolean = false,
    val startDate: Long? = null,
    val dueDate: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val archivedAt: Long? = null
)

data class Task(
    val id: String,
    val projectId: String,
    val parentTaskId: String? = null,
    val title: String,
    val description: String = "",
    val status: String = "TODO",
    val priority: String = "MEDIUM",
    val progressMode: String = "MANUAL",
    val manualProgress: Double = 0.0,
    val progress: Double = 0.0,
    val weight: Double = 1.0,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val startDate: Long? = null,
    val dueDate: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val archivedAt: Long? = null
)

data class Category(
    val id: String,
    val name: String,
    val description: String = "",
    val color: String = "#6366F1",
    val icon: String = "Bookmark",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ChecklistItem(
    val id: String,
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

data class Comment(
    val id: String,
    val taskId: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val deletedAt: Long? = null
)
