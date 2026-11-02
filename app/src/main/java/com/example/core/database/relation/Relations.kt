package com.example.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ChecklistItemEntity
import com.example.data.local.entity.CommentEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity

data class ProjectWithTasks(
    @Embedded val project: ProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val tasks: List<TaskEntity>
)

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

data class TaskWithSubtasksAndDetails(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentTaskId"
    )
    val subtasks: List<TaskEntity> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val checklistItems: List<ChecklistItemEntity> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val comments: List<CommentEntity> = emptyList()
)
