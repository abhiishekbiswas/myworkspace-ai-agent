package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.core.database.relation.ProjectWithCategoryAndTasks
import com.example.core.database.relation.ProjectWithTasks
import com.example.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

  @Query("SELECT * FROM projects WHERE isArchived = 0 AND archivedAt IS NULL ORDER BY createdAt DESC")
  fun getActiveProjects(): Flow<List<ProjectEntity>>

  @Query("SELECT * FROM projects WHERE isArchived = 1 OR archivedAt IS NOT NULL ORDER BY updatedAt DESC")
  fun getArchivedProjects(): Flow<List<ProjectEntity>>

  @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
  fun getProjectById(id: String): Flow<ProjectEntity?>

  @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
  suspend fun getProjectByIdOnce(id: String): ProjectEntity?

  @Transaction
  @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
  fun getProjectWithTasks(id: String): Flow<ProjectWithTasks?>

  @Transaction
  @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
  fun getProjectWithCategoryAndTasks(id: String): Flow<ProjectWithCategoryAndTasks?>

  @Query("SELECT COUNT(*) FROM projects WHERE categoryId = :categoryId")
  suspend fun countProjectsWithCategory(categoryId: String): Int

  @Query("UPDATE projects SET categoryId = :newCategoryId, updatedAt = :updatedAt WHERE categoryId = :oldCategoryId")
  suspend fun reassignCategory(oldCategoryId: String, newCategoryId: String?, updatedAt: Long = System.currentTimeMillis())

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProject(project: ProjectEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProjectsBulk(projects: List<ProjectEntity>)

  @Update
  suspend fun updateProject(project: ProjectEntity)

  @Delete
  suspend fun deleteProject(project: ProjectEntity)

  @Query("DELETE FROM projects WHERE id = :id")
  suspend fun deleteProjectById(id: String)

  @Query("SELECT * FROM projects")
  suspend fun getAllProjectsForBackup(): List<ProjectEntity>
}
