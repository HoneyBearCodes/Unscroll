package com.devsummit.scroll.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class UnscrollAccessibilityService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    // Declare fields as null - initialized only after system binds us
    private var handler: Handler? = null
    private var currentPackage: String? = null
    private var pendingTrigger: Runnable? = null

    private lateinit var prefs: SharedPreferences
    private val blacklistedCache = mutableSetOf<String>()
    private var globalSnoozeUntil: Long = 0L

    override fun onServiceConnected() {
        try {
            super.onServiceConnected()
            // Safe to initialize Handler here - system has fully bound us
            handler = Handler(Looper.getMainLooper())
            prefs = getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
            prefs.registerOnSharedPreferenceChangeListener(this)
            
            updateCacheFromPrefs()
            
            Log.d("Unscroll", "AccessibilityService CONNECTED successfully")
        } catch (t: Throwable) {
            Log.e("Unscroll", "onServiceConnected failed", t)
        }
    }

    private fun updateCacheFromPrefs() {
        blacklistedCache.clear()
        prefs.getString("blacklisted_packages_cache", "")?.let { str ->
            if (str.isNotEmpty()) {
                blacklistedCache.addAll(str.split(","))
            }
        }
        globalSnoozeUntil = prefs.getLong("global_snooze_until", 0L)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "blacklisted_packages_cache" || key == "global_snooze_until") {
            updateCacheFromPrefs()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            val h = handler ?: return
            if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

            val pkg = event.packageName?.toString() ?: return
            currentPackage = pkg

            // Remove any previously scheduled trigger
            pendingTrigger?.let { h.removeCallbacks(it) }
            pendingTrigger = null

            if (!blacklistedCache.contains(pkg)) return

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
            Log.e("Unscroll", "onAccessibilityEvent error", t)
        }
    }

    override fun onInterrupt() {
        Log.d("Unscroll", "AccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingTrigger?.let { handler?.removeCallbacks(it) }
        handler = null
        if (::prefs.isInitialized) {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
        }
        Log.d("Unscroll", "AccessibilityService destroyed")
    }
}
