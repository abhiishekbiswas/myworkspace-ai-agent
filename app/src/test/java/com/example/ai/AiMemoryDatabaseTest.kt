package com.example.ai

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.dao.AiMemoryDao
import com.example.data.local.database.WorkspaceDatabase
import com.example.data.local.entity.AiMemoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiMemoryDatabaseTest {

    private lateinit var database: WorkspaceDatabase
    private lateinit var dao: AiMemoryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WorkspaceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.aiMemoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveMemory() = runBlocking {
        val mem = AiMemoryEntity(
            id = UUID.randomUUID().toString(),
            category = "Preferences",
            fact = "Prefers dark mode and daily summaries at 9am",
            createdAt = System.currentTimeMillis()
        )
        dao.insertMemory(mem)

        val retrieved = dao.getMemoryById(mem.id)
        assertNotNull(retrieved)
        assertEquals("Preferences", retrieved?.category)
        assertEquals("Prefers dark mode and daily summaries at 9am", retrieved?.fact)
    }

    @Test
    fun testSearchMemoriesKeyword() = runBlocking {
        val mem1 = AiMemoryEntity(id = "m1", category = "Work", fact = "Client server is hosted on AWS Mumbai")
        val mem2 = AiMemoryEntity(id = "m2", category = "Personal", fact = "Doctor appointment every 6 months")
        dao.insertMemory(mem1)
        dao.insertMemory(mem2)

        val results = dao.searchMemoriesOnce("Mumbai")
        assertEquals(1, results.size)
        assertEquals("m1", results[0].id)
    }

    @Test
    fun testSearchMemoriesByCategory() = runBlocking {
        val mem1 = AiMemoryEntity(id = "m1", category = "Tax", fact = "GST return due on 20th")
        val mem2 = AiMemoryEntity(id = "m2", category = "Tax", fact = "ITR deadline is July 31")
        val mem3 = AiMemoryEntity(id = "m3", category = "General", fact = "Office key with guard")
        dao.insertMemory(mem1)
        dao.insertMemory(mem2)
        dao.insertMemory(mem3)

        val taxMemories = dao.getMemoriesByCategory("Tax").first()
        assertEquals(2, taxMemories.size)
    }


    @Test
    fun testDeleteMemory() = runBlocking {
        val mem = AiMemoryEntity(id = "m-del", category = "Temp", fact = "Temporary code 9988")
        dao.insertMemory(mem)
        assertNotNull(dao.getMemoryById("m-del"))

        dao.deleteMemoryById("m-del")
        assertNull(dao.getMemoryById("m-del"))
    }
}
