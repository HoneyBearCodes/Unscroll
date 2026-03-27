package com.devsummit.scroll.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.devsummit.scroll.core.db.AppDatabase

class UnscrollAccessibilityService : AccessibilityService() {

    private var currentForegroundPackage: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var scheduledTriggerRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("Unscroll", "Accessibility Service actively connected cleanly.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

            val packageName = event.packageName?.toString() ?: return
            currentForegroundPackage = packageName

            // Because we enabled allowMainThreadQueries(), this synchronous DB hit is perfectly safe and lightning fast
            // for our tiny table. It completely eliminates Flow/Coroutine process crashes!
            val dao = AppDatabase.getDatabase(this).blacklistedAppDao()
            val blacklistedApps = dao.getAllBlacklistedAppsSync()

            // Cancel any old trap
            scheduledTriggerRunnable?.let { handler.removeCallbacks(it) }

            if (blacklistedApps.contains(packageName)) {
                checkOverlayTrigger(blacklistedApps)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkOverlayTrigger(blacklistedApps: List<String>) {
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
                    if (currentPkg != null && blacklistedApps.contains(currentPkg)) {
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
    }
}
