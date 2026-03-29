package com.devsummit.scroll.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
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
            Log.d("Unscroll", "AccessibilityService connected")
        } catch (t: Throwable) {
            Log.e("Unscroll", "onServiceConnected failed", t)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            val h = handler ?: return
            if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

            val pkg = event.packageName?.toString() ?: return
            val classNameStr = event.className?.toString() ?: ""

            // Ignore system-level popups, dialogs, and Compose DropdownMenus
            if (classNameStr.contains("Popup") || classNameStr.contains("Dialog") || pkg == "android" || pkg == "com.android.systemui" || pkg.contains("inputmethod")) {
                return
            }
            
            currentPackage = pkg

            pendingTrigger?.let { h.removeCallbacks(it) }
            pendingTrigger = null

            val prefs = getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
            val blacklistedStr = prefs.getString("blacklisted_packages_cache", "") ?: ""
            if (blacklistedStr.isEmpty()) {
                Log.w("Unscroll", "Blacklist cache empty")
                return
            }
            
            val blacklistedApps = blacklistedStr.split(",").filter { it.isNotBlank() }.toSet()
            val isBlacklisted = blacklistedApps.contains(pkg)

            // Protect our own popup windows from killing our own overlay
            if (pkg == "com.devsummit.scroll") {
                if (!classNameStr.contains("MainActivity")) return
            }

            if (!isBlacklisted) {
                stopService(Intent(this, OverlayService::class.java))
                return
            }

            val globalSnoozeUntil = prefs.getLong("global_snooze_until", 0L)
            val now = System.currentTimeMillis()

            if (now > globalSnoozeUntil) {
                Log.d("Unscroll", "Triggering overlay for $pkg")
                try {
                    startService(Intent(this, OverlayService::class.java))
                } catch (e: Exception) {
                    Log.e("Unscroll", "Failed to start OverlayService", e)
                }
            } else {
                val remainingSec = (globalSnoozeUntil - now) / 1000
                Log.d("Unscroll", "Snooze active for ${remainingSec}s more")
                val delay = (globalSnoozeUntil - now) + 500
                val runnable = Runnable {
                    if (currentPackage == pkg) {
                        try {
                            startService(Intent(this@UnscrollAccessibilityService, OverlayService::class.java))
                        } catch (e: Exception) {
                            Log.e("Unscroll", "Failed to start OverlayService after snooze", e)
                        }
                    }
                }
                pendingTrigger = runnable
                h.postDelayed(runnable, delay)
            }
        } catch (t: Throwable) {
            Log.e("Unscroll", "onAccessibilityEvent error", t)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        pendingTrigger?.let { handler?.removeCallbacks(it) }
        handler = null
    }
}
