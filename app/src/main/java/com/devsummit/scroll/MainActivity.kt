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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import com.devsummit.scroll.core.db.AppDatabase
import com.devsummit.scroll.core.db.UsageRepository
import com.devsummit.scroll.core.usage.UsageEngine
import com.devsummit.scroll.service.OverlayService
import com.devsummit.scroll.ui.dashboard.DashboardScreen
import com.devsummit.scroll.ui.settings.AppSelectorScreen
import com.devsummit.scroll.ui.theme.UnscrollTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: UsageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = UsageRepository(
            AppDatabase.getDatabase(this).dailyUsageDao(),
            AppDatabase.getDatabase(this).blacklistedAppDao()
        )
        // ... (permissions kept intact)
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
        
        if (!UsageEngine.hasUsageStatsPermission(this)) {
            val usageIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(usageIntent)
        }

        if (!isAccessibilityServiceEnabled()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        setContent {
            val scope = rememberCoroutineScope()
            val blacklistedApps by repository.allBlacklistedApps.collectAsState(initial = emptyList())
            var currentTab by remember { mutableStateOf("dashboard") }

            // Sync blacklisted apps to SharedPreferences for the AccessibilityService
            LaunchedEffect(blacklistedApps) {
                val prefs = getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
                prefs.edit().putStringSet("blacklisted_packages_cache", blacklistedApps.toSet()).apply()
            }

            UnscrollTheme {
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
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        if (currentTab == "dashboard") {
                            DashboardScreen(
                                onTestOverlayClick = {
                                    startService(Intent(this@MainActivity, OverlayService::class.java))
                                }
                            )
                        } else {
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
