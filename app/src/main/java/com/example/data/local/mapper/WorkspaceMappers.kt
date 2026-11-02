package com.example.data.local.mapper

import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ChecklistItemEntity
import com.example.data.local.entity.CommentEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.model.Category
import com.example.domain.model.ChecklistItem
import com.example.domain.model.Comment
import com.example.domain.model.ProgressMode
import com.example.domain.model.Project
import com.example.domain.model.ProjectStatus
import com.example.domain.model.Task
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskStatus

fun ProjectEntity.toDomain(): Project = Project(
  id = id,
  name = name,
  description = description,
  categoryId = categoryId,
  color = color,
  icon = icon,
  status = ProjectStatus.fromString(status),
  priority = TaskPriority.fromString(priority),
  progress = progress,
  progressMode = ProgressMode.fromString(progressMode),
  weight = weight,
  isArchived = isArchived || archivedAt != null,
  startDate = startDate,
  dueDate = dueDate,
  createdAt = createdAt,
  updatedAt = updatedAt,
  completedAt = completedAt
)

fun Project.toEntity(): ProjectEntity = ProjectEntity(
  id = id,
  name = name,
  description = description,
  categoryId = categoryId,
  color = color,
  icon = icon,
  status = status.displayName,
  priority = priority.displayName,
  progressMode = progressMode.displayName,
  manualProgress = progress,
  progress = progress,
  weight = weight,
  isArchived = isArchived,
  startDate = startDate,
  dueDate = dueDate,
  createdAt = createdAt,
  updatedAt = updatedAt,
  archivedAt = if (isArchived) updatedAt else null,
  completedAt = completedAt
)

fun TaskEntity.toDomain(): Task = Task(
  id = id,
  projectId = projectId,
  parentTaskId = parentTaskId,
  title = title,
  description = description,
  status = TaskStatus.fromString(status),
  priority = TaskPriority.fromString(priority),
  progress = progress,
  progressMode = ProgressMode.fromString(progressMode),
  weight = weight,
  sortOrder = sortOrder,
  isArchived = isArchived || archivedAt != null,
  startDate = startDate,
  dueDate = dueDate,
  createdAt = createdAt,
  updatedAt = updatedAt,
  completedAt = completedAt
)

fun Task.toEntity(): TaskEntity = TaskEntity(
  id = id,
  projectId = projectId,
  parentTaskId = parentTaskId,
  title = title,
  description = description,
  status = status.displayName,
  priority = priority.displayName,
  progressMode = progressMode.displayName,
  manualProgress = progress,
  progress = progress,
  weight = weight,
  sortOrder = sortOrder,
  isArchived = isArchived,
  startDate = startDate,
  dueDate = dueDate,
  completedAt = completedAt,
  createdAt = createdAt,
  updatedAt = updatedAt,
  archivedAt = if (isArchived) updatedAt else null
)

fun CategoryEntity.toDomain(): Category = Category(
  id = id,
  name = name,
  description = description,
  color = color,
  icon = icon,
  createdAt = createdAt,
  updatedAt = updatedAt
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
  id = id,
  name = name,
  description = description,
  color = color,
  icon = icon,
  createdAt = createdAt,
  updatedAt = updatedAt
)

fun ChecklistItemEntity.toDomain(): ChecklistItem = ChecklistItem(
  id = id,
  taskId = taskId,
  title = title,
  isCompleted = isCompleted,
  sortOrder = sortOrder,
  createdAt = createdAt,
  updatedAt = updatedAt,
  completedAt = completedAt
)

fun ChecklistItem.toEntity(): ChecklistItemEntity = ChecklistItemEntity(
  id = id,
  taskId = taskId,
  title = title,
  isCompleted = isCompleted,
  sortOrder = sortOrder,
  createdAt = createdAt,
  updatedAt = updatedAt,
  completedAt = completedAt
)

fun CommentEntity.toDomain(): Comment = Comment(
  id = id,
  taskId = taskId,
  content = content,
  createdAt = createdAt,
  updatedAt = updatedAt
)

fun Comment.toEntity(): CommentEntity = CommentEntity(
  id = id,
  taskId = taskId,
  content = content,
  createdAt = createdAt,
  updatedAt = updatedAt,
  deletedAt = null
)
