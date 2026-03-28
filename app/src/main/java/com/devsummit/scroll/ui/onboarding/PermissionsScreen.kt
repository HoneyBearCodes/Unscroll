package com.devsummit.scroll.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PermissionsScreen(
    hasOverlay: Boolean,
    hasUsageStats: Boolean,
    hasAccessibility: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showInfoDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Unscroll",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "To protect your focus, we need a few permissions to seamlessly intercept apps you want to block.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            PermissionCard(
                title = "Display Over Other Apps",
                isGranted = hasOverlay,
                onInfoClick = {
                    showInfoDialog = "Display Overlays" to "We need this to show the Unscroll intervention screen exactly when you try to open a blocked app."
                },
                onGrantClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    context.startActivity(intent)
                }
            )

            PermissionCard(
                title = "Usage Access",
                isGranted = hasUsageStats,
                onInfoClick = {
                    showInfoDialog = "Usage Access" to "We use this to analyze your daily phone usage history to accurately populate the dashboard charts."
                },
                onGrantClick = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }
            )

            PermissionCard(
                title = "Accessibility Service",
                isGranted = hasAccessibility,
                onInfoClick = {
                    showInfoDialog = "Accessibility Service" to "This runs efficiently in the background to detect which app you just opened so we can block it instantly."
                },
                onGrantClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }
            )
        }

        // Info Dialog Box
        showInfoDialog?.let { (title, message) ->
            AlertDialog(
                onDismissRequest = { showInfoDialog = null },
                title = { Text(title) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = null }) {
                        Text("Got it")
                    }
                }
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    isGranted: Boolean,
    onInfoClick: () -> Unit,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onInfoClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Button(onClick = onGrantClick) {
                    Text("Grant")
                }
            }
        }
    }
}
