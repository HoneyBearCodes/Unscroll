package com.devsummit.scroll.core.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
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
        if (usageStats != null) {
            for ((packageName, stats) in usageStats) {
                if (packageName != null && blacklistedPackages.contains(packageName) && stats != null) {
                    totalTime += stats.totalTimeInForeground
                }
            }
        }
        return totalTime
    }
    fun getWeeklyUsage(blacklistedPackages: Set<String>): List<Float> {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val installTime = packageInfo.firstInstallTime
        
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        val weeklyData = MutableList(7) { 0f }
        
        if (usageStats.isNullOrEmpty()) return weeklyData

        for (i in 6 downTo 0) {
            val dayCalendar = Calendar.getInstance()
            dayCalendar.add(Calendar.DAY_OF_YEAR, -i)
            dayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            dayCalendar.set(Calendar.MINUTE, 0)
            dayCalendar.set(Calendar.SECOND, 0)
            dayCalendar.set(Calendar.MILLISECOND, 0)
            val dayStart = dayCalendar.timeInMillis
            
            dayCalendar.set(Calendar.HOUR_OF_DAY, 23)
            dayCalendar.set(Calendar.MINUTE, 59)
            dayCalendar.set(Calendar.SECOND, 59)
            val dayEnd = dayCalendar.timeInMillis
            
            if (dayEnd < installTime) continue
            
            var totalTimeMs = 0L
            usageStats.forEach { stats ->
                val pkgName = stats?.packageName
                if (pkgName != null && blacklistedPackages.contains(pkgName) && stats.firstTimeStamp >= dayStart && stats.firstTimeStamp <= dayEnd) {
                    totalTimeMs += stats.totalTimeInForeground
                }
            }
            weeklyData[6 - i] = totalTimeMs / (1000f * 60f * 60f)
        }
        return weeklyData
    }

    fun calculateCurrentStreak(blacklistedPackages: Set<String>, dailyGoalMs: Long): Int {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val installTime = packageInfo.firstInstallTime
        
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        if (usageStats.isNullOrEmpty()) return 0
        
        var streak = 0
        for (i in 0..30) {
            val dayCalendar = Calendar.getInstance()
            dayCalendar.add(Calendar.DAY_OF_YEAR, -i)
            dayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            dayCalendar.set(Calendar.MINUTE, 0)
            dayCalendar.set(Calendar.SECOND, 0)
            dayCalendar.set(Calendar.MILLISECOND, 0)
            val dayStart = dayCalendar.timeInMillis
            
            dayCalendar.set(Calendar.HOUR_OF_DAY, 23)
            dayCalendar.set(Calendar.MINUTE, 59)
            dayCalendar.set(Calendar.SECOND, 59)
            val dayEnd = dayCalendar.timeInMillis
            
            if (dayEnd < installTime) break
            
            var totalTimeMs = 0L
            usageStats.forEach { stats ->
                val pkgName = stats?.packageName
                if (pkgName != null && blacklistedPackages.contains(pkgName) && stats.firstTimeStamp >= dayStart && stats.firstTimeStamp <= dayEnd) {
                    totalTimeMs += stats.totalTimeInForeground
                }
            }
            
            if (totalTimeMs <= dailyGoalMs) streak++ else break
        }
        return streak
    }
    companion object {
        fun hasUsageStatsPermission(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }
}
