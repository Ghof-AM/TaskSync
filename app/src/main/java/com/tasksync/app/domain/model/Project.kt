package com.tasksync.app.domain.model

data class Project(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val memberIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)