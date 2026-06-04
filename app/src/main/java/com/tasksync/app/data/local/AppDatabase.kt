package com.tasksync.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tasksync.app.data.local.dao.ActivityLogDao
import com.tasksync.app.data.local.dao.CommentDao
import com.tasksync.app.data.local.dao.ProjectDao
import com.tasksync.app.data.local.dao.ProjectMemberDao
import com.tasksync.app.data.local.dao.TaskDao
import com.tasksync.app.data.local.dao.UserDao
import com.tasksync.app.data.local.entity.ActivityLogEntity
import com.tasksync.app.data.local.entity.CommentEntity
import com.tasksync.app.data.local.entity.ProjectEntity
import com.tasksync.app.data.local.entity.ProjectMemberEntity
import com.tasksync.app.data.local.entity.TaskEntity
import com.tasksync.app.data.local.entity.UserEntity

@Database(
    entities = [
        TaskEntity::class,
        CommentEntity::class,
        UserEntity::class,
        ProjectMemberEntity::class,
        ActivityLogEntity::class,
        ProjectEntity::class
    ],
    version = 5,  // naik dari 3 ke 4
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun commentDao(): CommentDao
    abstract fun userDao(): UserDao
    abstract fun projectMemberDao(): ProjectMemberDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun projectDao(): ProjectDao
}