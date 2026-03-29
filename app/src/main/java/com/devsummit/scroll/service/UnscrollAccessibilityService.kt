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

            // Ignore system-level popups, dialogs, and keyboards
            if (pkg == "android" || pkg == "com.android.systemui" || pkg.contains("inputmethod") || 
                classNameStr.contains("Popup") || classNameStr.contains("Dialog")) {
                return
            }
            
            currentPackage = pkg
            pendingTrigger?.let { h.removeCallbacks(it) }
            pendingTrigger = null

            val prefs = getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
            val blacklistedStr = prefs.getString("blacklisted_packages_cache", "") ?: ""
            
            // Standardizing the split to avoid accidental spaces
            val blacklistedApps = blacklistedStr.split(",").filter { it.isNotBlank() }.map { it.trim() }.toSet()
            val isBlacklisted = blacklistedApps.contains(pkg)

            // Protect our own app from being killed
            if (pkg == "com.devsummit.scroll") {
                if (!classNameStr.contains("MainActivity")) return
            }

            if (!isBlacklisted) {
                // Not a blocked app, ensure overlay is gone
                stopService(Intent(this, OverlayService::class.java))
                return
            }

            val globalSnoozeUntil = prefs.getLong("global_snooze_until", 0L)
            val now = System.currentTimeMillis()

            if (now > globalSnoozeUntil) {
                Log.d("Unscroll", "Triggering overlay for blacklisted: $pkg")
                try {
                    val intent = Intent(this, OverlayService::class.java).apply {
                        setPackage(packageName)
                    }
                    startService(intent)
                } catch (e: Exception) {
                    Log.e("Unscroll", "StartService failed for $pkg", e)
                }
            } else {
                val remainingSec = (globalSnoozeUntil - now) / 1000
                Log.d("Unscroll", "Snooze active for $pkg ($remainingSec s left)")
                val delay = (globalSnoozeUntil - now) + 200
                val runnable = Runnable {
                    if (currentPackage == pkg) {
                        try {
                            startService(Intent(this@UnscrollAccessibilityService, OverlayService::class.java))
                        } catch (_: Exception) {}
                    }
                }
                pendingTrigger = runnable
                h.postDelayed(runnable, delay)
            }
        } catch (t: Throwable) {
            Log.e("Unscroll", "onAccessibilityEvent crash", t)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        pendingTrigger?.let { handler?.removeCallbacks(it) }
        handler = null
    }
}
