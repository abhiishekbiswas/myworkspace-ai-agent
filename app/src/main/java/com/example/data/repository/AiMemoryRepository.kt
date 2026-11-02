package com.example.data.repository

import com.example.core.database.dao.AiMemoryDao
import com.example.data.local.entity.AiMemoryEntity
import com.example.domain.repository.IAiMemoryRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AiMemoryRepository(
    private val dao: AiMemoryDao
) : IAiMemoryRepository {

    override val allMemories: Flow<List<AiMemoryEntity>> = dao.getAllMemories()

    override fun searchMemories(query: String): Flow<List<AiMemoryEntity>> = dao.searchMemories(query)

    override suspend fun searchMemoriesOnce(query: String): List<AiMemoryEntity> = dao.searchMemoriesOnce(query)

    override suspend fun getAllMemoriesOnce(): List<AiMemoryEntity> = dao.getAllMemoriesOnce()

    override suspend fun saveMemory(category: String, fact: String): AiMemoryEntity {
        val trimmedCat = category.trim().ifEmpty { "General" }
        val trimmedFact = fact.trim()
        val now = System.currentTimeMillis()
        val memory = AiMemoryEntity(
            id = UUID.randomUUID().toString(),
            category = trimmedCat,
            fact = trimmedFact,
            createdAt = now,
            updatedAt = now
        )
        dao.insertMemory(memory)
        return memory
    }

    override suspend fun deleteMemory(memory: AiMemoryEntity) {
        dao.deleteMemory(memory)
    }

    override suspend fun deleteMemoryById(id: String) {
        dao.deleteMemoryById(id)
    }
}
