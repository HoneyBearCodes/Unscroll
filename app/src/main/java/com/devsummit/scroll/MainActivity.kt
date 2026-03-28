package com.devsummit.scroll

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import android.net.Uri
import android.content.Context
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import com.devsummit.scroll.core.db.AppDatabase
import com.devsummit.scroll.core.db.UsageRepository
import com.devsummit.scroll.core.usage.UsageEngine
import com.devsummit.scroll.service.OverlayService
import com.devsummit.scroll.ui.dashboard.DashboardScreen
import com.devsummit.scroll.ui.settings.AppSelectorScreen
import com.devsummit.scroll.ui.settings.SettingsScreen
import com.devsummit.scroll.ui.theme.UnscrollTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: UsageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = UsageRepository(
            AppDatabase.getDatabase(this).dailyUsageDao(),
            AppDatabase.getDatabase(this).blacklistedAppDao()
        )
        
        // Reset snooze for testing purposes
        getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("global_snooze_until", 0L)
            .apply()
            
        // ... (permissions kept intact)
        // Permissions will be explicitly managed by PermissionsScreen now

        setContent {
            val scope = rememberCoroutineScope()
            val blacklistedApps by repository.allBlacklistedApps.collectAsState(initial = emptyList())
            var currentTab by remember { mutableStateOf("dashboard") }

            val lifecycleOwner = LocalLifecycleOwner.current

            var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(this@MainActivity)) }
            var hasUsage by remember { mutableStateOf(UsageEngine.hasUsageStatsPermission(this@MainActivity)) }
            var hasAccessibility by remember { mutableStateOf(isAccessibilityServiceEnabled()) }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        hasOverlay = Settings.canDrawOverlays(this@MainActivity)
                        hasUsage = UsageEngine.hasUsageStatsPermission(this@MainActivity)
                        hasAccessibility = isAccessibilityServiceEnabled()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            // Sync blacklisted apps to SharedPreferences for the AccessibilityService
            LaunchedEffect(blacklistedApps) {
                val prefs = getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("blacklisted_packages_cache", blacklistedApps.joinToString(",")).apply()
            }

            UnscrollTheme {
                val allPermissionsGranted = hasOverlay && hasUsage && hasAccessibility

                if (!allPermissionsGranted) {
                    com.devsummit.scroll.ui.onboarding.PermissionsScreen(
                        hasOverlay = hasOverlay,
                        hasUsageStats = hasUsage,
                        hasAccessibility = hasAccessibility
                    )
                } else {
                    Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                label = { Text("Dashboard") },
                                selected = currentTab == "dashboard",
                                onClick = { currentTab = "dashboard" }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Apps") },
                                label = { Text("Blocked Apps") },
                                selected = currentTab == "apps",
                                onClick = { currentTab = "apps" }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                selected = currentTab == "settings",
                                onClick = { currentTab = "settings" }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        if (currentTab == "dashboard") {
                            DashboardScreen(
                                blacklistedApps = blacklistedApps.toSet(),
                                onTestOverlayClick = {
                                    startService(Intent(this@MainActivity, OverlayService::class.java))
                                }
                            )
                        } else if (currentTab == "apps") {
                            AppSelectorScreen(
                                blacklistedPackages = blacklistedApps.toSet(),
                                onToggleApp = { appPackage, isBlacklisted ->
                                    scope.launch {
                                        if (isBlacklisted) {
                                            repository.addBlacklistedApp(appPackage)
                                        } else {
                                            repository.removeBlacklistedApp(appPackage)
                                        }
                                    }
                                }
                            )
                        } else {
                            SettingsScreen()
                        }
                    }
                        
                }
            }
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (enabledService in enabledServices) {
            val serviceInfo = enabledService.resolveInfo.serviceInfo
            if (serviceInfo.packageName == packageName && serviceInfo.name == com.devsummit.scroll.service.UnscrollAccessibilityService::class.java.name) {
                return true
            }
        }
        return false
    }
}
