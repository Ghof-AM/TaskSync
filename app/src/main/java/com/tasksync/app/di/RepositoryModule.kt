package com.tasksync.app.di

import com.tasksync.app.data.repository.ActivityLogRepositoryImpl
import com.tasksync.app.data.repository.CommentRepositoryImpl
import com.tasksync.app.data.repository.ProjectMemberRepositoryImpl
import com.tasksync.app.data.repository.TaskRepositoryImpl
import com.tasksync.app.data.repository.UserRepositoryImpl
import com.tasksync.app.domain.repository.ActivityLogRepository
import com.tasksync.app.domain.repository.CommentRepository
import com.tasksync.app.domain.repository.ProjectMemberRepository
import com.tasksync.app.domain.repository.TaskRepository
import com.tasksync.app.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        impl: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindCommentRepository(
        impl: CommentRepositoryImpl
    ): CommentRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindProjectMemberRepository(
        impl: ProjectMemberRepositoryImpl
    ): ProjectMemberRepository

    @Binds
    @Singleton
    abstract fun bindActivityLogRepository(
        impl: ActivityLogRepositoryImpl
    ): ActivityLogRepository
}