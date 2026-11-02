package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.core.database.relation.TaskWithSubtasksAndDetails
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

  @Query("SELECT * FROM tasks WHERE isArchived = 0 AND archivedAt IS NULL ORDER BY sortOrder ASC, createdAt DESC")
  fun getAllTasks(): Flow<List<TaskEntity>>

  @Query("SELECT * FROM tasks WHERE projectId = :projectId AND isArchived = 0 AND archivedAt IS NULL ORDER BY sortOrder ASC, createdAt DESC")
  fun getTasksByProject(projectId: String): Flow<List<TaskEntity>>

  @Query("SELECT * FROM tasks WHERE projectId = :projectId")
  suspend fun getTasksByProjectOnce(projectId: String): List<TaskEntity>

  @Query("SELECT * FROM tasks WHERE projectId = :projectId AND parentTaskId IS NULL AND isArchived = 0 AND archivedAt IS NULL ORDER BY sortOrder ASC, createdAt DESC")
  fun getRootTasksByProject(projectId: String): Flow<List<TaskEntity>>

  @Query("SELECT * FROM tasks WHERE parentTaskId = :parentTaskId AND isArchived = 0 AND archivedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
  fun getSubtasks(parentTaskId: String): Flow<List<TaskEntity>>

  @Query("SELECT * FROM tasks WHERE parentTaskId = :parentTaskId")
  suspend fun getSubtasksOnce(parentTaskId: String): List<TaskEntity>

  @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
  fun getTaskById(id: String): Flow<TaskEntity?>

  @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
  suspend fun getTaskByIdOnce(id: String): TaskEntity?

  @Transaction
  @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
  fun getTaskWithDetails(id: String): Flow<TaskWithSubtasksAndDetails?>

  @Query("SELECT * FROM tasks WHERE isArchived = 0 AND archivedAt IS NULL AND dueDate IS NOT NULL ORDER BY dueDate ASC")
  fun getTasksWithDeadlines(): Flow<List<TaskEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTask(task: TaskEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTasksBulk(tasks: List<TaskEntity>)

  @Update
  suspend fun updateTask(task: TaskEntity)

  @Delete
  suspend fun deleteTask(task: TaskEntity)

  @Query("DELETE FROM tasks WHERE id = :id")
  suspend fun deleteTaskById(id: String)

  @Query("UPDATE tasks SET sortOrder = :newSortOrder, updatedAt = :updatedAt WHERE id = :taskId")
  suspend fun updateTaskSortOrder(taskId: String, newSortOrder: Int, updatedAt: Long = System.currentTimeMillis())

  @Query("SELECT * FROM tasks")
  suspend fun getAllTasksForBackup(): List<TaskEntity>
}
