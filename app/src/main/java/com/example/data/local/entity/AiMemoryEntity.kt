package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "ai_memories",
    indices = [
        Index("category"),
        Index("createdAt")
    ]
)
data class AiMemoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val category: String = "General",
    val fact: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
