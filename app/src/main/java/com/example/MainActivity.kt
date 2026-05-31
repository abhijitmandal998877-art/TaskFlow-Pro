package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.TaskRepository
import com.example.ui.compositions.DeveloperScreen
import com.example.ui.compositions.GoalScreen
import com.example.ui.compositions.LoadingScreen
import com.example.ui.compositions.TaskScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TaskViewModel
import com.example.ui.viewmodel.TaskViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init local persistence layer
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TaskRepository(database.taskDao())
        val sharedPrefs = getSharedPreferences("taskflow_prefs", Context.MODE_PRIVATE)

        setContent {
            // Instantiate View Model with safe constructor parameters
            val viewModel: TaskViewModel by viewModels {
                TaskViewModelFactory(repository, sharedPrefs)
            }

            val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            val isAppLoading by viewModel.isAppLoading.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDark) {
                // Request Notification Permission dynamically on start
                NotificationPermissionContract()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Crossfade(
                        targetState = isAppLoading,
                        label = "StartupCrossfade"
                    ) { loading ->
                        if (loading) {
                            LoadingScreen()
                        } else {
                            MainWorkspaceApp(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationPermissionContract() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission denied. Task alert reminders cannot trigger.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun MainWorkspaceApp(viewModel: TaskViewModel) {
    var selectedScreenIndex by remember { mutableStateOf(0) } // 0 = Tasks, 1 = Goals, 2 = Developer

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("app_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedScreenIndex == 0,
                    onClick = { selectedScreenIndex = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = "Tasks tab icon",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Tasks", fontSize = 12.sp) },
                    modifier = Modifier.testTag("nav_tasks_tab")
                )

                NavigationBarItem(
                    selected = selectedScreenIndex == 1,
                    onClick = { selectedScreenIndex = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = "Goals tab icon",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Goals", fontSize = 12.sp) },
                    modifier = Modifier.testTag("nav_goals_tab")
                )

                NavigationBarItem(
                    selected = selectedScreenIndex == 2,
                    onClick = { selectedScreenIndex = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DeveloperMode,
                            contentDescription = "Developer tab icon",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Developer", fontSize = 12.sp) },
                    modifier = Modifier.testTag("nav_developer_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedScreenIndex) {
                0 -> TaskScreen(viewModel = viewModel)
                1 -> GoalScreen(viewModel = viewModel)
                2 -> DeveloperScreen(viewModel = viewModel)
            }
        }
    }
}
