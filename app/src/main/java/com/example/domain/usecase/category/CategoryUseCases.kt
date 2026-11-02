package com.example.domain.usecase.category

import com.example.data.local.entity.CategoryEntity
import com.example.domain.repository.IWorkspaceRepository
import kotlinx.coroutines.flow.Flow

class GetCategoriesUseCase(private val repository: IWorkspaceRepository) {
  val categories: Flow<List<CategoryEntity>> = repository.categories
}

class SaveCategoryUseCase(private val repository: IWorkspaceRepository) {
  suspend operator fun invoke(category: CategoryEntity) {
    repository.saveCategory(category)
  }
}

class DeleteCategoryUseCase(private val repository: IWorkspaceRepository) {
  suspend operator fun invoke(categoryId: String, reassignToCategoryId: String? = null): Result<Unit> {
    return repository.deleteCategory(categoryId, reassignToCategoryId)
  }
}
