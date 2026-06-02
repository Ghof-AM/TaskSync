package com.tasksync.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.tasksync.app.ui.navigation.NavGraph
import com.tasksync.app.ui.navigation.Screen
import com.tasksync.app.ui.profile.ThemeViewModel
import com.tasksync.app.ui.theme.TaskSyncTheme
import com.tasksync.app.util.SyncManager
import com.tasksync.app.util.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var themeManager: ThemeManager

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        syncManager.schedule()

        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()

            TaskSyncTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val startDestination = if (firebaseAuth.currentUser != null) {
                    Screen.ProjectList.route
                } else {
                    Screen.Login.route
                }
                NavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}