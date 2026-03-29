package com.devsummit.scroll

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhonelinkLock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Context
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.devsummit.scroll.core.db.AppDatabase
import com.devsummit.scroll.core.db.UsageRepository
import com.devsummit.scroll.core.usage.UsageEngine
import com.devsummit.scroll.service.OverlayService
import com.devsummit.scroll.ui.about.AboutScreen
import com.devsummit.scroll.ui.dashboard.DashboardScreen
import com.devsummit.scroll.ui.settings.AppSelectorScreen
import com.devsummit.scroll.ui.settings.SettingsScreen
import com.devsummit.scroll.ui.splash.SplashScreen
import com.devsummit.scroll.ui.theme.UnscrollTheme
import com.devsummit.scroll.ui.theme.*

class MainActivity : ComponentActivity() {

    private lateinit var repository: UsageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = UsageRepository(
            AppDatabase.getDatabase(this).dailyUsageDao(),
            AppDatabase.getDatabase(this).blacklistedAppDao()
        )

        // One-time snooze reset after update (clears stale snooze from old versions)
        val prefs = getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("snooze_migrated_v2", false)) {
            prefs.edit()
                .putLong("global_snooze_until", 0L)
                .putBoolean("snooze_migrated_v2", true)
                .apply()
        }

        setContent {
            val scope = rememberCoroutineScope()
            val blacklistedApps by repository.allBlacklistedApps.collectAsState(initial = emptyList())
            var currentTab by remember { mutableStateOf("dashboard") }
            var showSplash by remember { mutableStateOf(true) }

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
                if (showSplash) {
                    SplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    val allPermissionsGranted = hasOverlay && hasUsage && hasAccessibility

                    if (!allPermissionsGranted) {
                        com.devsummit.scroll.ui.onboarding.PermissionsScreen(
                            hasOverlay = hasOverlay,
                            hasUsageStats = hasUsage,
                            hasAccessibility = hasAccessibility
                        )
                    } else {
                        Scaffold(
                            containerColor = DeepNavy,
                            bottomBar = {
                                NavigationBar(
                                    containerColor = DarkSlate,
                                    contentColor = OffWhite,
                                    tonalElevation = 0.dp
                                ) {
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Outlined.Dashboard, contentDescription = "Dashboard") },
                                        label = { Text("Dashboard") },
                                        selected = currentTab == "dashboard",
                                        onClick = { currentTab = "dashboard" },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Teal400,
                                            selectedTextColor = Teal400,
                                            indicatorColor = Teal400.copy(alpha = 0.12f),
                                            unselectedIconColor = MutedGray,
                                            unselectedTextColor = MutedGray
                                        )
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Outlined.PhonelinkLock, contentDescription = "Apps") },
                                        label = { Text("Blocked") },
                                        selected = currentTab == "apps",
                                        onClick = { currentTab = "apps" },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Teal400,
                                            selectedTextColor = Teal400,
                                            indicatorColor = Teal400.copy(alpha = 0.12f),
                                            unselectedIconColor = MutedGray,
                                            unselectedTextColor = MutedGray
                                        )
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
                                        label = { Text("Settings") },
                                        selected = currentTab == "settings",
                                        onClick = { currentTab = "settings" },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Teal400,
                                            selectedTextColor = Teal400,
                                            indicatorColor = Teal400.copy(alpha = 0.12f),
                                            unselectedIconColor = MutedGray,
                                            unselectedTextColor = MutedGray
                                        )
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Outlined.Info, contentDescription = "About") },
                                        label = { Text("About") },
                                        selected = currentTab == "about",
                                        onClick = { currentTab = "about" },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Teal400,
                                            selectedTextColor = Teal400,
                                            indicatorColor = Teal400.copy(alpha = 0.12f),
                                            unselectedIconColor = MutedGray,
                                            unselectedTextColor = MutedGray
                                        )
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                                when (currentTab) {
                                    "dashboard" -> DashboardScreen(
                                        blacklistedApps = blacklistedApps.toSet(),
                                        onTestOverlayClick = {
                                            startService(Intent(this@MainActivity, OverlayService::class.java).apply {
                                                putExtra(OverlayService.EXTRA_PREVIEW_MODE, true)
                                            })
                                        }
                                    )
                                    "apps" -> AppSelectorScreen(
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
                                    "settings" -> SettingsScreen()
                                    "about" -> AboutScreen()
                                }
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
