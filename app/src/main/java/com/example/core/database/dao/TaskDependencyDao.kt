package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.TaskDependencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDependencyDao {

  @Query("SELECT * FROM task_dependencies WHERE taskId = :taskId")
  fun getDependenciesForTask(taskId: String): Flow<List<TaskDependencyEntity>>

  @Query("SELECT * FROM task_dependencies WHERE taskId = :taskId")
  suspend fun getDependenciesForTaskOnce(taskId: String): List<TaskDependencyEntity>

  @Query("SELECT * FROM task_dependencies WHERE dependsOnTaskId = :taskId")
  fun getDependentsOfTask(taskId: String): Flow<List<TaskDependencyEntity>>

  @Query("SELECT * FROM task_dependencies WHERE dependsOnTaskId = :taskId")
  suspend fun getDependentsOfTaskOnce(taskId: String): List<TaskDependencyEntity>

  @Query("SELECT * FROM task_dependencies")
  fun getAllDependencies(): Flow<List<TaskDependencyEntity>>

  @Query("SELECT * FROM task_dependencies")
  suspend fun getAllDependenciesOnce(): List<TaskDependencyEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDependency(dependency: TaskDependencyEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDependenciesBulk(dependencies: List<TaskDependencyEntity>)

  @Delete
  suspend fun deleteDependency(dependency: TaskDependencyEntity)

  @Query("DELETE FROM task_dependencies WHERE id = :id")
  suspend fun deleteDependencyById(id: String)

  @Query("DELETE FROM task_dependencies WHERE taskId = :taskId AND dependsOnTaskId = :dependsOnTaskId")
  suspend fun deleteDependencyPair(taskId: String, dependsOnTaskId: String)
}
