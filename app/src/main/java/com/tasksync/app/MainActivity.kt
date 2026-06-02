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
import com.tasksync.app.domain.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log // Pastikan Log diimport
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging // Pastikan FirebaseMessaging diimport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var userRepository: UserRepository

    private val themeViewModel: ThemeViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("FCM", "Notification permission granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        syncManager.schedule()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Ambil FCM token saat pertama buka
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                android.util.Log.d("FCM", "Token device ini: $token")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        userRepository.updateFcmToken(token)
                        android.util.Log.d("FCM", "Token berhasil disimpan ke Firestore")
                    } catch (e: Exception) {
                        android.util.Log.e("FCM", "Gagal simpan token: ${e.message}")
                    }
                }
            }
        }

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