package com.devsummit.scroll.core.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

class UsageEngine(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun getTodayUsageInMilliseconds(blacklistedPackages: Set<String>): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)

        var totalTime = 0L
        for ((packageName, stats) in usageStats) {
            if (blacklistedPackages.contains(packageName)) {
                totalTime += stats.totalTimeInForeground
            }
        }
        return totalTime
    }
}
