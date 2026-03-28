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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.devsummit.scroll.ui.components.HoldToConfirmButton
import com.devsummit.scroll.ui.theme.UnscrollTheme
import com.devsummit.scroll.ui.theme.BlackOverlay

class OverlayService : Service(), SavedStateRegistryOwner, ViewModelStoreOwner {

    enum class SnoozeOption(val title: String, val minutes: Long) {
        FIVE_MINS("5 Minutes", 5),
        TEN_MINS("10 Minutes", 10),
        FIFTEEN_MINS("15 Minutes", 15),
        THIRTY_MINS("30 Minutes", 30),
        ONE_HOUR("1 Hour", 60),
        RUIN_MY_LIFE("Ruin My Life (No Snooze)", 24 * 60) // Basically today is ruined
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    
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
                            .background(BlackOverlay)
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Reality Check",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "In the time you spent scrolling yesterday, you could have finished reading a book.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                        )

                        var expanded by remember { mutableStateOf(false) }
                        
                        // Load previous snooze selection
                        val prefs = getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
                        val lastSnoozeName = prefs.getString("last_snooze_selection", SnoozeOption.FIFTEEN_MINS.name)
                        
                        var selectedSnooze by remember { 
                            mutableStateOf(
                                try { SnoozeOption.valueOf(lastSnoozeName ?: SnoozeOption.FIFTEEN_MINS.name) } 
                                catch (e: Exception) { SnoozeOption.FIFTEEN_MINS }
                            ) 
                        }

                        Box(modifier = Modifier.padding(bottom = 32.dp)) {
                            Button(onClick = { expanded = true }) {
                                Text("Snooze: ${selectedSnooze.title}")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                SnoozeOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.title) },
                                        onClick = {
                                            selectedSnooze = option
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        HoldToConfirmButton(
                            onConfirm = {
                                val snoozeEnd = System.currentTimeMillis() + (selectedSnooze.minutes * 60 * 1000)
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
