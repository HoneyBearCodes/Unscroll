package com.devsummit.scroll.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class UnscrollAccessibilityService : AccessibilityService() {

    private var currentPackage: String? = null
    private var pendingTrigger: Runnable? = null
    private var handler: Handler? = null

    override fun onServiceConnected() {
        try {
            super.onServiceConnected()
            handler = Handler(Looper.getMainLooper())
            Log.d("Unscroll", "AccessibilityService CONNECTED successfully")
        } catch (t: Throwable) {
            getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
                .edit().putString("last_accessibility_error", "onServiceConnected: ${t.message}").apply()
            Log.e("Unscroll", "onServiceConnected failed", t)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            val h = handler ?: return
            if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

            val pkg = event.packageName?.toString() ?: return
            currentPackage = pkg

            pendingTrigger?.let { h.removeCallbacks(it) }
            pendingTrigger = null

            val prefs = getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
            val blacklistedStr = prefs.getString("blacklisted_packages_cache", "") ?: ""
            if (blacklistedStr.isEmpty()) return
            
            val isBlacklisted = blacklistedStr.split(",").contains(pkg)
            
            // Protect our own popup windows (like dropdown menus) from killing our own overlay! 
            // We only want to kill the overlay if they explicitly opened our MainActivity dashboard.
            if (pkg == "com.devsummit.scroll") {
                val classNameStr = event.className?.toString() ?: ""
                if (!classNameStr.contains("MainActivity")) {
                    return // Ignore our own popups/services!
                }
            }

            if (!isBlacklisted) {
                // If they go to home screen, recents, or another app, immediately hide the overlay
                Log.d("Unscroll", "Moved to safe app: $pkg, stopping overlay")
                stopService(Intent(this, OverlayService::class.java))
                return
            }

            val globalSnoozeUntil = prefs.getLong("global_snooze_until", 0L)
            val now = System.currentTimeMillis()

            if (now > globalSnoozeUntil) {
                Log.d("Unscroll", "Launching overlay for $pkg")
                startService(Intent(this, OverlayService::class.java))
            } else {
                val delay = (globalSnoozeUntil - now) + 500
                Log.d("Unscroll", "Snooze active for ${delay}ms")
                val runnable = Runnable {
                    if (currentPackage == pkg) {
                        Log.d("Unscroll", "Snooze done. Launching overlay for $pkg")
                        startService(Intent(this@UnscrollAccessibilityService, OverlayService::class.java))
                    }
                }
                pendingTrigger = runnable
                h.postDelayed(runnable, delay)
            }
        } catch (t: Throwable) {
            getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
                .edit().putString("last_accessibility_error", "onAccessibilityEvent: ${t.message}").apply()
            Log.e("Unscroll", "onAccessibilityEvent error", t)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        pendingTrigger?.let { handler?.removeCallbacks(it) }
        handler = null
        Log.d("Unscroll", "AccessibilityService destroyed")
    }
}
