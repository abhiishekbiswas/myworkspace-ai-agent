package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

  @Query("SELECT * FROM notes WHERE archivedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
  fun getAllNotes(): Flow<List<NoteEntity>>

  @Query("SELECT * FROM notes WHERE projectId = :projectId AND archivedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
  fun getNotesForProject(projectId: String): Flow<List<NoteEntity>>

  @Query("SELECT * FROM notes WHERE taskId = :taskId AND archivedAt IS NULL ORDER BY updatedAt DESC")
  fun getNotesForTask(taskId: String): Flow<List<NoteEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNote(note: NoteEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotesBulk(notes: List<NoteEntity>)

  @Update
  suspend fun updateNote(note: NoteEntity)

  @Delete
  suspend fun deleteNote(note: NoteEntity)

  @Query("DELETE FROM notes WHERE id = :id")
  suspend fun deleteNoteById(id: String)

  @Query("SELECT * FROM notes")
  suspend fun getAllNotesForBackup(): List<NoteEntity>
}
