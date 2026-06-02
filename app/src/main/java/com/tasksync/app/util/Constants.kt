package com.tasksync.app.util

object Constants {
    // Database
    const val DATABASE_NAME = "tasksync.db"

    // Firestore Collections
    const val COLLECTION_TASKS = "tasks"
    const val COLLECTION_COMMENTS = "comments"
    const val COLLECTION_USERS = "users"
    const val COLLECTION_TEAMS = "teams"
    const val COLLECTION_PROJECT_MEMBERS = "project_members"
    const val COLLECTION_ACTIVITY_LOG = "activity_log"

    // WorkManager
    const val SYNC_WORK_NAME = "task_sync"
    const val SYNC_INTERVAL_MINUTES = 15L

    // Notification
    const val NOTIFICATION_CHANNEL_ID = "task_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Task Notifikasi"

    // DataStore
    const val DATASTORE_NAME = "tasksync_prefs"
    const val KEY_DARK_MODE = "dark_mode"
    const val KEY_USER_ID = "user_id"

    // Validation
    const val MAX_COMMENT_LENGTH = 500
    const val MIN_PASSWORD_LENGTH = 6
}