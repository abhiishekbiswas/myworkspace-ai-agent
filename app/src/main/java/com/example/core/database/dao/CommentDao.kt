package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {

  @Query("SELECT * FROM comments WHERE taskId = :taskId AND deletedAt IS NULL ORDER BY createdAt ASC")
  fun getCommentsForTask(taskId: String): Flow<List<CommentEntity>>

  @Query("SELECT * FROM comments WHERE taskId = :taskId AND deletedAt IS NULL ORDER BY createdAt ASC")
  suspend fun getCommentsForTaskOnce(taskId: String): List<CommentEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertComment(comment: CommentEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCommentsBulk(comments: List<CommentEntity>)

  @Update
  suspend fun updateComment(comment: CommentEntity)

  @Delete
  suspend fun deleteComment(comment: CommentEntity)

  @Query("DELETE FROM comments WHERE taskId = :taskId")
  suspend fun deleteCommentsForTask(taskId: String)

  @Query("SELECT * FROM comments")
  suspend fun getAllCommentsForBackup(): List<CommentEntity>
}
