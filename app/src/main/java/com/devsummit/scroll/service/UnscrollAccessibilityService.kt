package com.devsummit.scroll.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.devsummit.scroll.core.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class UnscrollAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    private var blacklistedApps: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        val dao = AppDatabase.getDatabase(this).blacklistedAppDao()
        dao.getAllBlacklistedAppsFlow()
            .onEach { apps -> blacklistedApps = apps.toSet() }
            .launchIn(scope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if (blacklistedApps.contains(packageName)) {
            val prefs = getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
            val snoozeUntil = prefs.getLong("global_snooze_until", 0L)
            
            if (System.currentTimeMillis() > snoozeUntil) {
                val intent = Intent(this, OverlayService::class.java)
                startService(intent)
            }
        }
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
