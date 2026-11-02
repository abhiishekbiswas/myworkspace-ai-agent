package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

  @Query("SELECT * FROM categories ORDER BY name ASC")
  fun getAllCategories(): Flow<List<CategoryEntity>>

  @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
  fun getCategoryById(id: String): Flow<CategoryEntity?>

  @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
  suspend fun getCategoryByIdOnce(id: String): CategoryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCategory(category: CategoryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCategoriesBulk(categories: List<CategoryEntity>)

  @Update
  suspend fun updateCategory(category: CategoryEntity)

  @Delete
  suspend fun deleteCategory(category: CategoryEntity)

  @Query("DELETE FROM categories WHERE id = :id")
  suspend fun deleteCategoryById(id: String)

  @Query("SELECT * FROM categories")
  suspend fun getAllCategoriesForBackup(): List<CategoryEntity>
}
