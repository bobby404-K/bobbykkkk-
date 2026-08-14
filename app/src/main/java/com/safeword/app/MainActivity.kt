package com.safeword.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.safeword.app.alert.AlertManager
import com.safeword.app.data.AppDatabase
import com.safeword.app.data.ContactsRepository
import com.safeword.app.data.IncidentLogRepository
import com.safeword.app.service.ListeningService
import com.safeword.app.ui.history.HistoryScreen
import com.safeword.app.ui.onboarding.OnboardingScreen
import com.safeword.app.ui.settings.SettingsScreen
import com.safeword.app.ui.theme.SafeWordTheme

class MainActivity : ComponentActivity() {

    private lateinit var contactsRepository: ContactsRepository
    private lateinit var incidentLogRepository: IncidentLogRepository
    private lateinit var alertManager: AlertManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize database repositories and managers
        val database = AppDatabase.getDatabase(this)
        contactsRepository = ContactsRepository(database.contactDao())
        incidentLogRepository = IncidentLogRepository(database.incidentLogDao())
        alertManager = AlertManager(this)

        setContent {
            SafeWordTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var onboardingComplete by remember {
                        mutableStateOf(
                            getSharedPreferences("safeword_prefs", Context.MODE_PRIVATE)
                                .getBoolean("onboarding_complete", false)
                        )
                    }

                    if (!onboardingComplete) {
                        OnboardingScreen(onFinished = { onboardingComplete = true })
                    } else {
                        MainAppContent()
                    }
                }
            }
        }
    }

    @Composable
    fun MainAppContent() {
        var currentTab by remember { mutableStateOf(0) }
        
        val context = LocalContext.current
        val prefs = remember { context.getSharedPreferences("safeword_prefs", Context.MODE_PRIVATE) }
        var isServiceRunning by remember {
            mutableStateOf(prefs.getBoolean(ListeningService.KEY_SERVICE_RUNNING, false))
        }

        DisposableEffect(Unit) {
            val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == ListeningService.KEY_SERVICE_RUNNING) {
                    isServiceRunning = prefs.getBoolean(ListeningService.KEY_SERVICE_RUNNING, false)
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("SafeWord", style = MaterialTheme.typography.titleMedium) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Shield, contentDescription = "Protection") },
                        label = { Text("Protection") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.History, contentDescription = "Logs") },
                        label = { Text("Logs") }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (currentTab) {
                    0 -> SettingsScreen(
                        contactsRepository = contactsRepository,
                        isServiceRunning = isServiceRunning,
                        onToggleService = { start ->
                            toggleService(start)
                            isServiceRunning = start
                        },
                        onTriggerPanic = {
                            val intent = Intent(this@MainActivity, ListeningService::class.java).apply {
                                action = ListeningService.ACTION_PANIC
                            }
                            startService(intent)
                        }
                    )
                    1 -> HistoryScreen(incidentLogRepository = incidentLogRepository)
                }
            }
        }
    }

    private fun toggleService(start: Boolean) {
        val intent = Intent(this, ListeningService::class.java).apply {
            action = if (start) ListeningService.ACTION_START else ListeningService.ACTION_STOP
        }
        if (start) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            startService(intent)
        }
    }
}
