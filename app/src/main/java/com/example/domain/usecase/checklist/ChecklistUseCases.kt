package com.example.domain.usecase.checklist

import com.example.data.local.entity.ChecklistItemEntity
import com.example.domain.repository.IWorkspaceRepository
import kotlinx.coroutines.flow.Flow

class ManageChecklistUseCase(private val repository: IWorkspaceRepository) {
  fun getChecklistForTask(taskId: String): Flow<List<ChecklistItemEntity>> =
    repository.getChecklistForTask(taskId)

  suspend fun saveItem(item: ChecklistItemEntity) = repository.saveChecklistItem(item)

  suspend fun toggleItem(item: ChecklistItemEntity) = repository.toggleChecklistItem(item)

  suspend fun deleteItem(item: ChecklistItemEntity) = repository.deleteChecklistItem(item)
}
