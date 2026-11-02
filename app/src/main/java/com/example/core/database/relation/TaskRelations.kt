package com.example.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.ChecklistItemEntity
import com.example.data.local.entity.CommentEntity
import com.example.data.local.entity.TaskEntity

/**
 * Composite relationship between a Task, its Subtasks, Checklist Items, and Comments.
 */
data class TaskWithSubtasksAndDetails(
  @Embedded val task: TaskEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "parentTaskId"
  )
  val subtasks: List<TaskEntity>,
  @Relation(
    parentColumn = "id",
    entityColumn = "taskId"
  )
  val checklistItems: List<ChecklistItemEntity>,
  @Relation(
    parentColumn = "id",
    entityColumn = "taskId"
  )
  val comments: List<CommentEntity>
)
