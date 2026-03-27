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
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    private var currentForegroundPackage: String? = null
    private var checkTimerJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        currentForegroundPackage = packageName

        scope.launch {
            val dao = AppDatabase.getDatabase(this@UnscrollAccessibilityService).blacklistedAppDao()
            val blacklistedApps = dao.getAllBlacklistedApps()

            if (blacklistedApps.contains(packageName)) {
                checkOverlayTrigger(blacklistedApps)
            } else {
                checkTimerJob?.cancel()
            }
        }
    }

    private fun checkOverlayTrigger(blacklistedApps: List<String>) {
        val prefs = getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
        val snoozeUntil = prefs.getLong("global_snooze_until", 0L)
        val now = System.currentTimeMillis()
        
        if (now > snoozeUntil) {
            val intent = Intent(this, OverlayService::class.java)
            startService(intent)
        } else {
            checkTimerJob?.cancel()
            val delayMs = snoozeUntil - now
            if (delayMs > 0) {
                checkTimerJob = scope.launch {
                    delay(delayMs + 1000)
                    
                    val currentPkg = currentForegroundPackage
                    if (currentPkg != null && blacklistedApps.contains(currentPkg)) {
                        val intent = Intent(this@UnscrollAccessibilityService, OverlayService::class.java)
                        startService(intent)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
