package com.tasksync.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.tasksync.app.ui.navigation.NavGraph
import com.tasksync.app.ui.navigation.Screen
import com.tasksync.app.ui.theme.TaskSyncTheme
import com.tasksync.app.util.SyncManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Jadwalkan background sync
        syncManager.schedule()

        setContent {
            TaskSyncTheme {
                val navController = rememberNavController()

                // Tentukan start destination berdasarkan status login
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