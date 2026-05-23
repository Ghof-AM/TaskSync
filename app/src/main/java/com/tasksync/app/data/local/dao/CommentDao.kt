package com.tasksync.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tasksync.app.data.local.entity.CommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {

    @Query("SELECT * FROM comments WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun getCommentsByTask(taskId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE isSynced = 0")
    suspend fun getUnsyncedComments(): List<CommentEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)  // ganti REPLACE → IGNORE
    suspend fun insertComment(comment: CommentEntity)

    @Query("UPDATE comments SET isSynced = 1 WHERE id = :commentId")
    suspend fun markAsSynced(commentId: String)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: String)

    @Query("DELETE FROM comments WHERE taskId = :taskId")
    suspend fun deleteAllByTask(taskId: String)

    @Query("SELECT * FROM comments WHERE id = :commentId LIMIT 1")
    suspend fun getCommentById(commentId: String): CommentEntity?
}