package com.devsummit.scroll.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.devsummit.scroll.ui.components.HoldToConfirmButton
import com.devsummit.scroll.ui.theme.*

class OverlayService : Service(), SavedStateRegistryOwner, ViewModelStoreOwner {

    companion object {
        const val EXTRA_PREVIEW_MODE = "extra_preview_mode"
    }

    enum class SnoozeOption(val title: String, val minutes: Long) {
        FIVE_MINS("5 Minutes", 5),
        TEN_MINS("10 Minutes", 10),
        FIFTEEN_MINS("15 Minutes", 15),
        THIRTY_MINS("30 Minutes", 30),
        ONE_HOUR("1 Hour", 60),
        RUIN_MY_LIFE("Ruin My Life (24hrs)", 24 * 60)
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var isPreviewMode = false
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isPreviewMode = intent?.getBooleanExtra(EXTRA_PREVIEW_MODE, false) ?: false
        if (composeView == null) {
            showOverlay()
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        return START_STICKY
    }

    private fun showOverlay() {
        if (composeView != null) return

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            setContent {
                UnscrollTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        DarkOverlay,
                                        Color(0xFF0A0E18)
                                    )
                                )
                            )
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        val preview = isPreviewMode
                        var isOverLimit by remember { mutableStateOf(false) }
                        var holdDurationMs by remember { mutableStateOf(5000L) }
                        
                        LaunchedEffect(Unit) {
                            if (preview) return@LaunchedEffect
                            
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                val prefs = getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
                                val blockedAppsStr = prefs.getString("blacklisted_packages_cache", "") ?: ""
                                val apps = if (blockedAppsStr.isEmpty()) emptySet() else blockedAppsStr.split(",").toSet()
                                val goalMs = prefs.getLong("daily_goal_ms", 60L * 60 * 1000)
                                
                                val engine = com.devsummit.scroll.core.usage.UsageEngine(this@OverlayService)
                                val todayUsage = engine.getTodayUsageInMilliseconds(apps)
                                
                                if (todayUsage >= goalMs) {
                                    isOverLimit = true
                                    holdDurationMs = 10000L
                                }
                            }
                        }
                        
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOverLimit) 
                                        Brush.radialGradient(listOf(LimitRed.copy(alpha = 0.3f), LimitRed.copy(alpha = 0.05f)))
                                    else 
                                        Brush.radialGradient(listOf(Teal400.copy(alpha = 0.3f), Teal400.copy(alpha = 0.05f)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isOverLimit) Icons.Outlined.WarningAmber else Icons.Outlined.PauseCircle,
                                contentDescription = null,
                                tint = if (isOverLimit) LimitRed else Teal400,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = if (isOverLimit) "Limit Exceeded" else "Reality Check",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isOverLimit) LimitRed else OffWhite
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isOverLimit) 
                                "You've used up your daily allowance for blocked apps. Unscroll is fully active." 
                            else 
                                "Take a moment to reconsider. Is this how you want to spend your time right now?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MutedGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                        )

                        val prefs = getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
                        val lastSnoozeName = prefs.getString("last_snooze_selection", SnoozeOption.FIFTEEN_MINS.name)
                        
                        var selectedSnooze by remember { 
                            mutableStateOf(
                                try { SnoozeOption.valueOf(lastSnoozeName ?: SnoozeOption.FIFTEEN_MINS.name) } 
                                catch (e: Exception) { SnoozeOption.FIFTEEN_MINS }
                            ) 
                        }

                        if (!isOverLimit) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(bottom = 32.dp)
                            ) {
                                Button(
                                    onClick = { expanded = !expanded },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElevatedSlate,
                                        contentColor = OffWhite
                                    )
                                ) {
                                    Text("Snooze: ${selectedSnooze.title}")
                                }
                                if (expanded) {
                                    Column(
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(CardSurface)
                                            .padding(8.dp)
                                    ) {
                                        SnoozeOption.values().forEach { option ->
                                            TextButton(
                                                onClick = {
                                                    selectedSnooze = option
                                                    expanded = false
                                                    prefs.edit().putString("last_snooze_selection", option.name).apply()
                                                }
                                            ) {
                                                Text(
                                                    option.title, 
                                                    color = if (option == selectedSnooze) Teal400 else OffWhite,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Emergency Snooze (2.5 mins)",
                                style = MaterialTheme.typography.titleSmall,
                                color = LimitRed.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 32.dp)
                            )
                        }
                        
                        HoldToConfirmButton(
                            durationMs = holdDurationMs,
                            onConfirm = {
                                val selectedMs = if (isOverLimit) (2.5 * 60 * 1000).toLong() else (selectedSnooze.minutes * 60 * 1000)
                                val snoozeEnd = System.currentTimeMillis() + selectedMs
                                prefs.edit()
                                    .putLong("global_snooze_until", snoozeEnd)
                                    .putString("last_snooze_selection", selectedSnooze.name)
                                    .apply()
                                removeOverlay()
                            }
                        )
                    }
                }
            }
        }

        val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        windowManager.addView(composeView, params)
    }

    private fun removeOverlay() {
        composeView?.let {
            windowManager.removeView(it)
            composeView = null
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
        
    override val viewModelStore: ViewModelStore
        get() = store
}
