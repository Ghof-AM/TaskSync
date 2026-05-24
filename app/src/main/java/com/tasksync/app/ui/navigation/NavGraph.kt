package com.tasksync.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tasksync.app.ui.auth.AuthViewModel
import com.tasksync.app.ui.auth.LoginScreen
import com.tasksync.app.ui.auth.RegisterScreen
import com.tasksync.app.ui.project.ProjectListScreen
import com.tasksync.app.ui.task.CreateTaskScreen
import com.tasksync.app.ui.task.TaskListScreen
import com.tasksync.app.ui.task.TaskDetailScreen
import com.tasksync.app.ui.profile.ProfileScreen
import com.tasksync.app.ui.team.TeamScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.ProjectList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.ProjectList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ProjectList.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            ProjectListScreen(
                onNavigateToTaskList = { projectId ->
                    navController.navigate(Screen.TaskList.createRoute(projectId))
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.TaskList.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            TaskListScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                },
                onNavigateToCreate = {
                    navController.navigate(Screen.CreateTask.createRoute(projectId))
                },
                onNavigateToTeam = {
                    navController.navigate(Screen.Team.createRoute(projectId))
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(
            route = Screen.CreateTask.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            CreateTaskScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
                onTaskCreated = { navController.popBackStack() }
            )
        }

        // Profile
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Team
        composable(
            route = Screen.Team.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            TeamScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Placeholder untuk TaskDetail
        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            TaskDetailScreen(
                taskId = taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}