package com.example.domain.usecase.task

import com.example.data.local.entity.TaskEntity
import com.example.domain.repository.IWorkspaceRepository
import kotlinx.coroutines.flow.Flow

class GetTasksUseCase(private val repository: IWorkspaceRepository) {
  val allTasks: Flow<List<TaskEntity>> = repository.allTasks
  val tasksWithDeadlines: Flow<List<TaskEntity>> = repository.tasksWithDeadlines
  fun getTasksByProject(projectId: String): Flow<List<TaskEntity>> = repository.getTasksByProject(projectId)
  fun getSubtasks(parentTaskId: String): Flow<List<TaskEntity>> = repository.getSubtasks(parentTaskId)
  fun getTaskById(id: String): Flow<TaskEntity?> = repository.getTaskById(id)
}

class ValidateTaskHierarchyUseCase(private val repository: IWorkspaceRepository) {
  suspend operator fun invoke(taskId: String, newParentTaskId: String?, projectId: String): Result<Unit> {
    return repository.validateTaskHierarchy(taskId, newParentTaskId, projectId)
  }
}

class SaveTaskUseCase(private val repository: IWorkspaceRepository) {
  suspend operator fun invoke(task: TaskEntity) {
    repository.saveTask(task)
  }
}

class UpdateTaskStatusUseCase(private val repository: IWorkspaceRepository) {
  suspend operator fun invoke(taskId: String, newStatus: String) {
    repository.updateTaskStatus(taskId, newStatus)
  }
}

class DeleteTaskUseCase(private val repository: IWorkspaceRepository) {
  suspend operator fun invoke(task: TaskEntity) {
    repository.deleteTask(task)
  }
}
