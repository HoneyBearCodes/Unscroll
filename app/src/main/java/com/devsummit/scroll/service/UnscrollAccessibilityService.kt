package com.devsummit.scroll.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class UnscrollAccessibilityService : AccessibilityService() {

    private var currentForegroundPackage: String? = null
    private var scheduledTriggerRunnable: Runnable? = null
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("Unscroll", "Unscroll AccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        currentForegroundPackage = packageName

        // Cancel any pending scheduled re-trigger
        scheduledTriggerRunnable?.let { handler.removeCallbacks(it) }

        // Read blacklist directly from SharedPreferences — fast, no Room needed
        val prefs = getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
        val blacklisted = prefs.getStringSet("blacklisted_packages_cache", emptySet()) ?: emptySet()

        if (blacklisted.contains(packageName)) {
            val snoozeUntil = prefs.getLong("global_snooze_until", 0L)
            val now = System.currentTimeMillis()

            if (now > snoozeUntil) {
                Log.d("Unscroll", "Triggering overlay for $packageName")
                startService(Intent(this, OverlayService::class.java))
            } else {
                val delay = (snoozeUntil - now) + 1000
                Log.d("Unscroll", "Snooze active. Re-check in ${delay}ms")
                val runnable = Runnable {
                    if (currentForegroundPackage == packageName) {
                        Log.d("Unscroll", "Snooze expired. Triggering overlay for $packageName")
                        startService(Intent(this@UnscrollAccessibilityService, OverlayService::class.java))
                    }
                }
                scheduledTriggerRunnable = runnable
                handler.postDelayed(runnable, delay)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
