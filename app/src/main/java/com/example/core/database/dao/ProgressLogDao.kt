package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ProgressLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressLogDao {

  @Query("SELECT * FROM progress_logs WHERE projectId = :projectId ORDER BY createdAt DESC")
  fun getProgressLogsForProject(projectId: String): Flow<List<ProgressLogEntity>>

  @Query("SELECT * FROM progress_logs WHERE taskId = :taskId ORDER BY createdAt DESC")
  fun getProgressLogsForTask(taskId: String): Flow<List<ProgressLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProgressLog(log: ProgressLogEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProgressLogsBulk(logs: List<ProgressLogEntity>)

  @Delete
  suspend fun deleteProgressLog(log: ProgressLogEntity)

  @Query("SELECT * FROM progress_logs")
  suspend fun getAllProgressLogsForBackup(): List<ProgressLogEntity>
}
