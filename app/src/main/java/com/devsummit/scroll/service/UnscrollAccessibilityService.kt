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
            val h = handler ?: run {
                Log.w("Unscroll", "Handler is null, service may not be connected yet")
                return
            }
            if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

            val pkg = event.packageName?.toString() ?: return
            val classNameStr = event.className?.toString() ?: ""
            
            Log.d("Unscroll", "Event: pkg=$pkg, class=$classNameStr")
            
            // Ignore system-level popups, dialogs, and Compose DropdownMenus.
            // These sub-windows shouldn't trigger an app-switched context reset.
            if (classNameStr.contains("Popup") || classNameStr.contains("Dialog") || pkg == "android" || pkg == "com.android.systemui" || pkg.contains("inputmethod")) {
                Log.d("Unscroll", "Ignoring system/popup event: $pkg / $classNameStr")
                return
            }
            
            currentPackage = pkg

            pendingTrigger?.let { h.removeCallbacks(it) }
            pendingTrigger = null

            val prefs = getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
            val blacklistedStr = prefs.getString("blacklisted_packages_cache", "") ?: ""
            if (blacklistedStr.isEmpty()) {
                Log.w("Unscroll", "Blacklist cache is EMPTY — no apps to block")
                return
            }
            
            val blacklistedApps = blacklistedStr.split(",").filter { it.isNotBlank() }.toSet()
            val isBlacklisted = blacklistedApps.contains(pkg)
            
            Log.d("Unscroll", "Package $pkg blacklisted=$isBlacklisted (blocklist has ${blacklistedApps.size} apps: $blacklistedStr)")
            
            // Protect our own popup windows (like dropdown menus) from killing our own overlay! 
            // We only want to kill the overlay if they explicitly opened our MainActivity dashboard.
            if (pkg == "com.devsummit.scroll") {
                if (!classNameStr.contains("MainActivity")) {
                    Log.d("Unscroll", "Ignoring our own non-MainActivity window: $classNameStr")
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
                Log.d("Unscroll", "Launching overlay for $pkg (snooze expired or not set)")
                try {
                    startService(Intent(this, OverlayService::class.java))
                    Log.d("Unscroll", "startService called successfully for OverlayService")
                } catch (e: Exception) {
                    Log.e("Unscroll", "FAILED to start OverlayService", e)
                }
            } else {
                val delay = (globalSnoozeUntil - now) + 500
                Log.d("Unscroll", "Snooze active for ${delay}ms, scheduling delayed trigger")
                val runnable = Runnable {
                    if (currentPackage == pkg) {
                        Log.d("Unscroll", "Snooze done. Launching overlay for $pkg")
                        try {
                            startService(Intent(this@UnscrollAccessibilityService, OverlayService::class.java))
                        } catch (e: Exception) {
                            Log.e("Unscroll", "FAILED to start OverlayService after snooze", e)
                        }
                    } else {
                        Log.d("Unscroll", "Snooze done but user left $pkg (now on $currentPackage), skipping overlay")
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
