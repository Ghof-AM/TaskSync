package com.tasksync.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.tasksync.app.domain.repository.UserRepository
import com.tasksync.app.ui.navigation.NavGraph
import com.tasksync.app.ui.navigation.Screen
import com.tasksync.app.ui.profile.ThemeViewModel
import com.tasksync.app.ui.theme.TaskSyncTheme
import com.tasksync.app.util.SyncManager
import com.tasksync.app.util.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var syncManager: SyncManager
    @Inject lateinit var firebaseAuth: FirebaseAuth
    @Inject lateinit var themeManager: ThemeManager
    @Inject lateinit var userRepository: UserRepository

    // Gunakan hiltViewModel factory agar Hilt bisa inject ThemeManager ke ThemeViewModel
    private val themeViewModel: ThemeViewModel by viewModels(
        factoryProducer = { defaultViewModelProviderFactory }
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) Log.d("FCM", "Notification permission granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // PERBAIKAN: jangan pakai ?: return di sini karena akan
        // menghentikan onCreate sebelum setContent dipanggil → layar putih.
        // Jadwalkan SyncWorker hanya jika user sudah login, tanpa return.
        val currentUid = firebaseAuth.currentUser?.uid
        if (currentUid != null) {
            syncManager.schedule(currentUid)
        }

        // Minta izin notifikasi (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Ambil dan simpan FCM token device ini ke Firestore
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "Token device ini: $token")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        userRepository.updateFcmToken(token)
                        Log.d("FCM", "Token berhasil disimpan ke Firestore")
                    } catch (e: Exception) {
                        Log.e("FCM", "Gagal simpan token: ${e.message}")
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