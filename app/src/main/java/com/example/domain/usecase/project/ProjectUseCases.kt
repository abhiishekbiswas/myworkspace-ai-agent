package com.example.domain.usecase.project

import com.example.data.local.entity.ProjectEntity
import com.example.domain.repository.IWorkspaceRepository
import kotlinx.coroutines.flow.Flow

class GetProjectsUseCase(private val repository: IWorkspaceRepository) {
  val activeProjects: Flow<List<ProjectEntity>> = repository.activeProjects
  val archivedProjects: Flow<List<ProjectEntity>> = repository.archivedProjects
  fun getProjectById(id: String): Flow<ProjectEntity?> = repository.getProjectById(id)
}

class SaveProjectUseCase(private val repository: IWorkspaceRepository) {
  suspend operator fun invoke(project: ProjectEntity) {
    repository.saveProject(project)
  }
}

class DuplicateProjectUseCase(private val repository: IWorkspaceRepository) {
  suspend operator fun invoke(projectId: String): Result<ProjectEntity> {
    return repository.duplicateProject(projectId)
  }
}

class ArchiveProjectUseCase(private val repository: IWorkspaceRepository) {
  suspend fun archive(projectId: String) = repository.archiveProject(projectId)
  suspend fun restore(projectId: String) = repository.restoreProject(projectId)
}

class DeleteProjectUseCase(private val repository: IWorkspaceRepository) {
  suspend operator fun invoke(project: ProjectEntity) {
    repository.deleteProject(project)
  }
}
