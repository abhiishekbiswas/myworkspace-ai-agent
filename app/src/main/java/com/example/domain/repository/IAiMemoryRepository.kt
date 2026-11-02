package com.example.domain.repository

import com.example.data.local.entity.AiMemoryEntity
import kotlinx.coroutines.flow.Flow

interface IAiMemoryRepository {
    val allMemories: Flow<List<AiMemoryEntity>>

    fun searchMemories(query: String): Flow<List<AiMemoryEntity>>
    suspend fun searchMemoriesOnce(query: String): List<AiMemoryEntity>
    suspend fun getAllMemoriesOnce(): List<AiMemoryEntity>
    suspend fun saveMemory(category: String, fact: String): AiMemoryEntity
    suspend fun deleteMemory(memory: AiMemoryEntity)
    suspend fun deleteMemoryById(id: String)
}
