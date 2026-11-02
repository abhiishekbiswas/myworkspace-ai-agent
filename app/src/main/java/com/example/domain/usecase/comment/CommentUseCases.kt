package com.example.domain.usecase.comment

import com.example.data.local.entity.CommentEntity
import com.example.domain.repository.IWorkspaceRepository
import kotlinx.coroutines.flow.Flow

class ManageCommentsUseCase(private val repository: IWorkspaceRepository) {
  fun getCommentsForTask(taskId: String): Flow<List<CommentEntity>> =
    repository.getCommentsForTask(taskId)

  suspend fun addComment(taskId: String, content: String) =
    repository.addComment(taskId, content)

  suspend fun deleteComment(comment: CommentEntity) =
    repository.deleteComment(comment)
}
