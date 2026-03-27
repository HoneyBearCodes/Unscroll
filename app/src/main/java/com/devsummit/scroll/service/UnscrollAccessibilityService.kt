package com.devsummit.scroll.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.devsummit.scroll.core.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UnscrollAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    
    private var blacklistedApps: Set<String> = emptySet()
    private var currentForegroundPackage: String? = null
    private var checkTimerJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val dao = AppDatabase.getDatabase(this).blacklistedAppDao()
        scope.launch(Dispatchers.IO) {
            dao.getAllBlacklistedAppsFlow().collect { apps ->
                blacklistedApps = apps.toSet()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        currentForegroundPackage = packageName

        if (blacklistedApps.contains(packageName)) {
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
                checkTimerJob?.cancel()
                val delayMs = snoozeUntil - now
                if (delayMs > 0) {
                    checkTimerJob = scope.launch {
                        delay(delayMs + 1000)
                        if (currentForegroundPackage != null && blacklistedApps.contains(currentForegroundPackage)) {
                            try {
                                startService(Intent(this@UnscrollAccessibilityService, OverlayService::class.java))
                            } catch(e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        } else {
            checkTimerJob?.cancel()
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
