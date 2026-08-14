package com.safeword.app.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var userName by remember { mutableStateOf("") }
    
    // Permission states
    var hasMic by remember { mutableStateOf(hasPerm(context, Manifest.permission.RECORD_AUDIO)) }
    var hasSms by remember { mutableStateOf(hasPerm(context, Manifest.permission.SEND_SMS)) }
    var hasFineLoc by remember { mutableStateOf(hasPerm(context, Manifest.permission.ACCESS_FINE_LOCATION)) }
    var hasBgLoc by remember { mutableStateOf(hasPerm(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) }
    var hasNotif by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hasPerm(context, Manifest.permission.POST_NOTIFICATIONS)
            } else {
                true
            }
        )
    }

    // Launchers
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasMic = it }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasSms = it }

    val locLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasFineLoc = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    val bgLocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasBgLoc = it }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasNotif = it }

    // Colors
    val brush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        // Icon / Logo
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = "SafeWord Logo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Welcome to SafeWord",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Your voice-triggered emergency assistant. SafeWord runs silently in the background and sends alerts when you call for help.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Username Input
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Personalize Your Alert",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "This name will be included in the emergency message sent to your contacts.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Your Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Permissions Section
        Text(
            text = "Required Permissions",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            text = "SafeWord requires the following permissions to function in emergencies. None of your data leaves this device.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
        )

        // 1. Microphone
        PermissionRow(
            title = "Record Audio (Microphone)",
            description = "Required to run local keyword detection. Audio is processed fully offline on your device.",
            isGranted = hasMic,
            icon = Icons.Default.Mic,
            onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        )

        // 2. SMS
        PermissionRow(
            title = "Send SMS Messages",
            description = "Used to dispatch emergency alerts to your contacts instantly without needing mobile data.",
            isGranted = hasSms,
            icon = Icons.Default.Sms,
            onRequest = { smsLauncher.launch(Manifest.permission.SEND_SMS) }
        )

        // 3. Foreground Location
        PermissionRow(
            title = "Location Access",
            description = "Required to pinpoint your precise GPS coordinates and include a Google Maps link in your alert.",
            isGranted = hasFineLoc,
            icon = Icons.Default.LocationOn,
            onRequest = {
                locLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        )

        // 4. Background Location
        if (hasFineLoc) {
            PermissionRow(
                title = "Background Location Access",
                description = "Critical so that SafeWord can fetch location updates and track movement even if the screen is locked.",
                isGranted = hasBgLoc,
                icon = Icons.Default.Map,
                onRequest = { bgLocLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
            )
        }

        // 5. Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRow(
                title = "Send Notifications",
                description = "Required to display the active foreground service notification so Android doesn't stop the background listening.",
                isGranted = hasNotif,
                icon = Icons.Default.Notifications,
                onRequest = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Complete Button
        val coreGranted = hasMic && hasSms && hasFineLoc && userName.trim().isNotEmpty()
        Button(
            onClick = {
                if (coreGranted) {
                    val prefs = context.getSharedPreferences("safeword_prefs", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("user_name", userName.trim())
                        putBoolean("onboarding_complete", true)
                        apply()
                    }
                    onFinished()
                } else {
                    Toast.makeText(context, "Please enter your name and grant Microphone, SMS, and Location permissions.", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (coreGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
        ) {
            Text("Finish Setup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Button(
                    onClick = onRequest,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Grant", fontSize = 12.sp)
                }
            }
        }
    }
}

private fun hasPerm(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
