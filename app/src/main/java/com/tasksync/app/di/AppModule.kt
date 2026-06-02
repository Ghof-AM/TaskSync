package com.tasksync.app.di

import android.content.Context
import androidx.room.Room
import com.tasksync.app.data.local.AppDatabase
import com.tasksync.app.data.local.dao.ActivityLogDao
import com.tasksync.app.data.local.dao.CommentDao
import com.tasksync.app.data.local.dao.ProjectDao
import com.tasksync.app.data.local.dao.ProjectMemberDao
import com.tasksync.app.data.local.dao.TaskDao
import com.tasksync.app.data.local.dao.UserDao
import com.tasksync.app.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        Constants.DATABASE_NAME
    ).fallbackToDestructiveMigration(false).build()

    @Provides
    @Singleton
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    @Singleton
    fun provideCommentDao(db: AppDatabase): CommentDao = db.commentDao()

    @Provides
    @Singleton
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    @Singleton
    fun provideProjectMemberDao(db: AppDatabase): ProjectMemberDao = db.projectMemberDao()

    @Provides
    @Singleton
    fun provideActivityLogDao(db: AppDatabase): ActivityLogDao = db.activityLogDao()

    @Provides
    @Singleton
    fun provideProjectDao(db: AppDatabase): ProjectDao = db.projectDao()
}