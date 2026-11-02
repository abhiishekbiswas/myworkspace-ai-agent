package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemDao {

  @Query("SELECT * FROM checklist_items WHERE taskId = :taskId ORDER BY sortOrder ASC, createdAt ASC")
  fun getChecklistForTask(taskId: String): Flow<List<ChecklistItemEntity>>

  @Query("SELECT * FROM checklist_items WHERE taskId = :taskId ORDER BY sortOrder ASC, createdAt ASC")
  suspend fun getChecklistForTaskOnce(taskId: String): List<ChecklistItemEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertChecklistItem(item: ChecklistItemEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertChecklistBulk(items: List<ChecklistItemEntity>)

  @Update
  suspend fun updateChecklistItem(item: ChecklistItemEntity)

  @Delete
  suspend fun deleteChecklistItem(item: ChecklistItemEntity)

  @Query("DELETE FROM checklist_items WHERE taskId = :taskId")
  suspend fun deleteChecklistForTask(taskId: String)

  @Query("SELECT * FROM checklist_items")
  suspend fun getAllChecklistItemsForBackup(): List<ChecklistItemEntity>
}
