package com.example.domain.model

enum class ProjectStatus(val displayName: String) {
  PLANNING("Planning"),
  ACTIVE("Active"),
  ON_HOLD("On Hold"),
  COMPLETED("Completed"),
  CANCELLED("Cancelled"),
  ARCHIVED("Archived");

  companion object {
    fun fromString(value: String): ProjectStatus =
      entries.find { it.displayName.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) } ?: ACTIVE
  }
}

enum class TaskStatus(val displayName: String) {
  BACKLOG("Backlog"),
  TODO("To Do"),
  IN_PROGRESS("In Progress"),
  ON_HOLD("On Hold"),
  IN_REVIEW("In Review"),
  COMPLETED("Completed"),
  CANCELLED("Cancelled");

  companion object {
    fun fromString(value: String): TaskStatus =
      entries.find { it.displayName.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) } ?: TODO
  }
}

enum class TaskPriority(val displayName: String) {
  LOW("Low"),
  MEDIUM("Medium"),
  HIGH("High"),
  URGENT("Urgent");

  companion object {
    fun fromString(value: String): TaskPriority =
      entries.find { it.displayName.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) } ?: MEDIUM
  }
}

enum class ProgressMode(val displayName: String) {
  AUTOMATIC("Automatic"),
  MANUAL("Manual"),
  WEIGHTED("Weighted");

  companion object {
    fun fromString(value: String): ProgressMode =
      entries.find { it.displayName.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) } ?: AUTOMATIC
  }
}
