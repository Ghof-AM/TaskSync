package com.tasksync.app.ui.navigation

sealed class Screen(val route: String) {
    // Auth
    data object Login : Screen("login")
    data object Register : Screen("register")

    // Main
    data object TaskList : Screen("task_list/{projectId}") {
        fun createRoute(projectId: String) = "task_list/$projectId"
    }
    data object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }
    data object CreateTask : Screen("create_task/{projectId}") {
        fun createRoute(projectId: String) = "create_task/$projectId"
    }
    data object Profile : Screen("profile")
    data object Team : Screen("team/{projectId}") {
        fun createRoute(projectId: String) = "team/$projectId"
    }
    data object ActivityLog : Screen("activity_log/{projectId}") {
        fun createRoute(projectId: String) = "activity_log/$projectId"
    }
    data object ProjectList : Screen("project_list")
}