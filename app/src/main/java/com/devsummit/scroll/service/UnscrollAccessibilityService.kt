package com.devsummit.scroll.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.devsummit.scroll.core.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class UnscrollAccessibilityService : AccessibilityService() {

    private var currentForegroundPackage: String? = null
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var scheduledTriggerRunnable: Runnable? = null
    
    // In-memory cache of blacklisted apps to avoid querying DB on the main thread
    @Volatile
    private var blacklistedApps: Set<String> = emptySet()
    
    // Safety: Initialize coroutines strictly inside complete Android lifecycles!
    private var serviceScope: CoroutineScope? = null

    override fun onServiceConnected() {
        try {
            super.onServiceConnected()
            Log.d("Unscroll", "Accessibility Service actively connected cleanly.")
            
            // Cleanly startup background sync process
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            serviceScope = scope
            
            val dao = AppDatabase.getDatabase(this.applicationContext).blacklistedAppDao()
            
            scope.launch {
                try {
                    dao.getAllBlacklistedAppsFlow().collect { apps ->
                        blacklistedApps = apps.toSet()
                    }
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

            val packageName = event.packageName?.toString() ?: return
            currentForegroundPackage = packageName

            // Safe memory read, zero blocking issues!
            val localApps = blacklistedApps

            scheduledTriggerRunnable?.let { handler.removeCallbacks(it) }

            if (localApps.contains(packageName)) {
                checkOverlayTrigger(localApps)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkOverlayTrigger(localApps: Set<String>) {
        val prefs = getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
        val snoozeUntil = prefs.getLong("global_snooze_until", 0L)
        val now = System.currentTimeMillis()
        
        if (now > snoozeUntil) {
            try {
                startService(Intent(this, OverlayService::class.java))
            } catch(e: Exception) {
                e.printStackTrace()
            }
        } else {
            val delayMs = snoozeUntil - now
            if (delayMs > 0) {
                val runnable = Runnable {
                    val currentPkg = currentForegroundPackage
                    if (currentPkg != null && localApps.contains(currentPkg)) {
                        try {
                            startService(Intent(this@UnscrollAccessibilityService, OverlayService::class.java))
                        } catch(e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                scheduledTriggerRunnable = runnable
                handler.postDelayed(runnable, delayMs + 1000)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        scheduledTriggerRunnable?.let { handler.removeCallbacks(it) }
        serviceScope?.cancel()
    }
}
