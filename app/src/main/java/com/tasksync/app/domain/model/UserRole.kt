package com.tasksync.app.domain.model

enum class UserRole(val value: String) {
    OWNER("owner"),
    SECOND_OWNER("2nd_owner"),
    MEMBER("member");

    companion object {
        fun fromValue(value: String): UserRole =
            entries.find { it.value == value } ?: MEMBER
    }
}