package com.devsummit.scroll.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devsummit.scroll.ui.theme.*

@Composable
fun PermissionsScreen(
    hasOverlay: Boolean,
    hasUsageStats: Boolean,
    hasAccessibility: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showInfoDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    val grantedCount = listOf(hasOverlay, hasUsageStats, hasAccessibility).count { it }

    Surface(
        color = DeepNavy,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Shield icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Teal400.copy(alpha = 0.3f), Teal400.copy(alpha = 0.05f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = "Shield",
                    tint = Teal400,
                    modifier = Modifier.size(44.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Welcome to Unscroll",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = OffWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We need a few permissions to protect your focus and intercept distracting apps.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MutedGray
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (index < grantedCount) 32.dp else 24.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (index < grantedCount) Teal400 else ElevatedSlate
                            )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$grantedCount/3",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (grantedCount == 3) Teal400 else MutedGray
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            PermissionCard(
                title = "Display Over Apps",
                description = "Show intervention overlay",
                icon = Icons.Outlined.Layers,
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
                description = "Track screen time data",
                icon = Icons.Outlined.QueryStats,
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
                description = "Detect app switches",
                icon = Icons.Outlined.Accessibility,
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

        showInfoDialog?.let { (title, message) ->
            AlertDialog(
                onDismissRequest = { showInfoDialog = null },
                containerColor = CardSurface,
                titleContentColor = OffWhite,
                textContentColor = MutedGray,
                title = { Text(title) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = null }) {
                        Text("Got it", color = Teal400)
                    }
                }
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onInfoClick: () -> Unit,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) SafeGreen.copy(alpha = 0.08f) else CardSurface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) SafeGreen.copy(alpha = 0.15f) else ElevatedSlate
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isGranted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Granted",
                        tint = SafeGreen,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MutedGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OffWhite
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray
                )
            }
            
            if (!isGranted) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onInfoClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Info",
                        tint = SubtleGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Button(
                    onClick = onGrantClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Teal400,
                        contentColor = DeepNavy
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Grant", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
