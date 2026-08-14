package com.safeword.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeword.app.data.Contact
import com.safeword.app.data.ContactsRepository
import com.safeword.app.service.ListeningService
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    contactsRepository: ContactsRepository,
    isServiceRunning: Boolean,
    onToggleService: (Boolean) -> Unit,
    onTriggerPanic: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Preferences & Config States
    val prefs = remember { context.getSharedPreferences("safeword_prefs", Context.MODE_PRIVATE) }
    
    var userName by remember { mutableStateOf(prefs.getString("user_name", "Someone") ?: "Someone") }
    var trackingEnabled by remember { mutableStateOf(prefs.getBoolean("tracking_enabled", true)) }
    var durationMinutes by remember { mutableStateOf(prefs.getInt("tracking_duration_minutes", 10)) }
    var intervalSeconds by remember { mutableStateOf(prefs.getInt("tracking_interval_seconds", 30)) }

    // Custom Trigger Words State
    var triggerWords by remember {
        mutableStateOf(
            prefs.getStringSet("trigger_words", setOf("help", "help me"))?.toMutableList() ?: mutableListOf("help", "help me")
        )
    }

    // Database Contacts State
    val contacts by contactsRepository.allContacts.collectAsState(initial = emptyList())

    // Dialog state
    var showAddContactDialog by remember { mutableStateOf(false) }
    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }
    var newTriggerWord by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Protection Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceRunning) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isServiceRunning) "SafeWord Protection ACTIVE" else "SafeWord Protection INACTIVE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isServiceRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isServiceRunning) "Running in background, listening for triggers." else "Speech trigger monitoring is paused.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { onToggleService(it) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Fast manual panic fallback button
                Button(
                    onClick = onTriggerPanic,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Panic Alert (Manual)", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Personal Profile Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Profile Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(10.dp))
                
                OutlinedTextField(
                    value = userName,
                    onValueChange = {
                        userName = it
                        prefs.edit().putString("user_name", it).apply()
                    },
                    label = { Text("Your Name (included in alerts)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 3. Emergency Contacts Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Emergency Contacts (${contacts.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    IconButton(onClick = { showAddContactDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Contact")
                    }
                }
                
                if (contacts.isEmpty()) {
                    Text(
                        text = "No emergency contacts configured yet. Add at least one contact to receive alert SMS.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    contacts.forEach { contact ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(contact.phoneNumber, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        contactsRepository.delete(contact)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // 4. Trigger Words Management
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Trigger Keywords", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = "SafeWord listens for these phrases offline. Keep custom words short and distinct.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Render current trigger list
                triggerWords.forEach { word ->
                    val isDefault = word == "help" || word == "help me"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "\"$word\"",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        if (!isDefault) {
                            IconButton(
                                onClick = {
                                    val newList = triggerWords.toMutableList().apply { remove(word) }
                                    triggerWords = newList
                                    prefs.edit().putStringSet("trigger_words", newList.toSet()).apply()
                                    // Refresh listening service if it's active
                                    if (isServiceRunning) restartService(context)
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Word", tint = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Text("System Default", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTriggerWord,
                        onValueChange = { newTriggerWord = it },
                        label = { Text("Add custom trigger word") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val word = newTriggerWord.trim().lowercase()
                            if (word.isNotEmpty() && !triggerWords.contains(word)) {
                                val newList = triggerWords.toMutableList().apply { add(word) }
                                triggerWords = newList
                                prefs.edit().putStringSet("trigger_words", newList.toSet()).apply()
                                newTriggerWord = ""
                                Toast.makeText(context, "Added custom trigger: $word", Toast.LENGTH_SHORT).show()
                                if (isServiceRunning) restartService(context)
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        // 5. Tracking Settings Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Location Share Tracking", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Switch(
                        checked = trackingEnabled,
                        onCheckedChange = {
                            trackingEnabled = it
                            prefs.edit().putBoolean("tracking_enabled", it).apply()
                        }
                    )
                }
                Text(
                    text = "If enabled, SafeWord periodically texts your contacts with location updates for a specified window after the initial alert.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                if (trackingEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text("Tracking Duration (minutes): $durationMinutes", fontSize = 13.sp)
                    Slider(
                        value = durationMinutes.toFloat(),
                        onValueChange = { durationMinutes = it.toInt() },
                        onValueChangeFinished = {
                            prefs.edit().putInt("tracking_duration_minutes", durationMinutes).apply()
                        },
                        valueRange = 2f..60f,
                        steps = 58
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("SMS Ping Interval (seconds): $intervalSeconds", fontSize = 13.sp)
                    Slider(
                        value = intervalSeconds.toFloat(),
                        onValueChange = { intervalSeconds = it.toInt() },
                        onValueChangeFinished = {
                            prefs.edit().putInt("tracking_interval_seconds", intervalSeconds).apply()
                        },
                        valueRange = 10f..300f,
                        steps = 290
                    )
                }
            }
        }

        // 6. Device Compatibility settings (Battery)
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Device Optimization", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = "To ensure SafeWord remains active and is not killed in the background, please disable battery optimization for this application.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        try {
                            val intent = Intent().apply {
                                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Request direct settings panel fallback
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.BatteryAlert, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disable Battery Optimizations", fontSize = 13.sp)
                }
            }
        }
    }

    // Add Contact Dialog
    if (showAddContactDialog) {
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text("Add Emergency Contact") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newContactName,
                        onValueChange = { newContactName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newContactPhone,
                        onValueChange = { newContactPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newContactName.trim()
                        val phone = newContactPhone.trim()
                        if (name.isNotEmpty() && phone.isNotEmpty()) {
                            coroutineScope.launch {
                                contactsRepository.insert(Contact(name = name, phoneNumber = phone))
                                showAddContactDialog = false
                                newContactName = ""
                                newContactPhone = ""
                            }
                        } else {
                            Toast.makeText(context, "Please enter name and phone number.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun restartService(context: Context) {
    val stopIntent = Intent(context, ListeningService::class.java).apply {
        action = ListeningService.ACTION_STOP
    }
    context.startService(stopIntent)
    
    val startIntent = Intent(context, ListeningService::class.java).apply {
        action = ListeningService.ACTION_START
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(startIntent)
    } else {
        context.startService(startIntent)
    }
}
