package com.tasksync.app.domain.model

enum class LogEvent {
    PROJECT_CREATED,
    OWNER_TRANSFERRED,
    MEMBER_PROMOTED,
    MEMBER_DEMOTED,
    TASK_CREATED,
    TASK_EDITED,
    TASK_ASSIGNED,
    TASK_STATUS_CHANGED,
    TASK_COMMENTED,
    TASK_DELETED,       // tambahkan ini
    MEMBER_JOINED,
    MEMBER_LEFT,
    PROJECT_DELETED
}