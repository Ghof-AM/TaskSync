package com.tasksync.app.domain.model
enum class Priority(val value: String) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    companion object {
        fun fromValue(value: String): Priority =
            entries.find { it.value == value } ?: MEDIUM
    }
}