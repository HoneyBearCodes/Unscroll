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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import com.devsummit.scroll.ui.components.HoldToConfirmButton
import com.devsummit.scroll.ui.theme.UnscrollTheme
import com.devsummit.scroll.ui.theme.BlackOverlay

class OverlayService : Service(), SavedStateRegistryOwner, ViewModelStoreOwner {

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
        showOverlay()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        return START_STICKY
    }

    private fun showOverlay() {
        if (composeView != null) return

        composeView = ComposeView(this).apply {
            ViewTreeLifecycleOwner.set(this, this@OverlayService)
            ViewTreeViewModelStoreOwner.set(this, this@OverlayService)
            ViewTreeSavedStateRegistryOwner.set(this, this@OverlayService)

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
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "In the time you spent scrolling yesterday, you could have finished reading a book.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.LightGray,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                        
                        HoldToConfirmButton(
                            onConfirm = { removeOverlay() }
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
