package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AiMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMemoryDao {

  @Query("SELECT * FROM ai_memories ORDER BY createdAt DESC")
  fun getAllMemories(): Flow<List<AiMemoryEntity>>

  @Query("SELECT * FROM ai_memories ORDER BY createdAt DESC")
  suspend fun getAllMemoriesOnce(): List<AiMemoryEntity>

  @Query("SELECT * FROM ai_memories WHERE id = :id LIMIT 1")
  suspend fun getMemoryById(id: String): AiMemoryEntity?

  @Query("SELECT * FROM ai_memories WHERE category = :category ORDER BY createdAt DESC")
  fun getMemoriesByCategory(category: String): Flow<List<AiMemoryEntity>>

  @Query("SELECT * FROM ai_memories WHERE fact LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY createdAt DESC")
  fun searchMemories(query: String): Flow<List<AiMemoryEntity>>

  @Query("SELECT * FROM ai_memories WHERE fact LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY createdAt DESC")
  suspend fun searchMemoriesOnce(query: String): List<AiMemoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMemory(memory: AiMemoryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMemoriesBulk(memories: List<AiMemoryEntity>)

  @Update
  suspend fun updateMemory(memory: AiMemoryEntity)

  @Delete
  suspend fun deleteMemory(memory: AiMemoryEntity)

  @Query("DELETE FROM ai_memories WHERE id = :id")
  suspend fun deleteMemoryById(id: String)

  @Query("DELETE FROM ai_memories")
  suspend fun clearAllMemories()
}
