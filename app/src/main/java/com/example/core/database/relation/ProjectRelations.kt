package com.example.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity

/**
 * 1-to-N relationship between a Project and its Tasks.
 */
data class ProjectWithTasks(
  @Embedded val project: ProjectEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "projectId"
  )
  val tasks: List<TaskEntity>
)

/**
 * Composite relationship between Project, its assigned Category, and Tasks.
 */
data class ProjectWithCategoryAndTasks(
  @Embedded val project: ProjectEntity,
  @Relation(
    parentColumn = "categoryId",
    entityColumn = "id"
  )
  val category: CategoryEntity?,
  @Relation(
    parentColumn = "id",
    entityColumn = "projectId"
  )
  val tasks: List<TaskEntity>
)
