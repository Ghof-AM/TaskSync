package com.tasksync.app.domain.model

enum class TaskStatus(val value: String) {
    TODO("todo"),
    IN_PROGRESS("in_progress"),
    DONE("done");

    companion object {
        fun fromValue(value: String): TaskStatus =
            entries.find { it.value == value } ?: TODO
    }
}