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
        for ((packageName, stats) in usageStats) {
            if (blacklistedPackages.contains(packageName)) {
                totalTime += stats.totalTimeInForeground
            }
        }
        return totalTime
    }
    fun getWeeklyUsage(blacklistedPackages: Set<String>): List<Float> {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val installTime = packageInfo.firstInstallTime
        
        val weeklyData = mutableListOf<Float>()
        
        for (i in 6 downTo 0) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -i)

            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endTime = calendar.timeInMillis
            
            if (endTime < installTime) {
                weeklyData.add(0f)
            } else {
                val queryEnd = if (i == 0) System.currentTimeMillis() else endTime
                val usageStats = usageStatsManager.queryAndAggregateUsageStats(startTime, queryEnd)
                
                var totalTimeMs = 0L
                for ((packageName, stats) in usageStats) {
                    if (blacklistedPackages.contains(packageName)) {
                        totalTimeMs += stats.totalTimeInForeground
                    }
                }
                val hours = totalTimeMs / (1000f * 60f * 60f)
                weeklyData.add(hours)
            }
        }
        return weeklyData
    }

    fun calculateCurrentStreak(blacklistedPackages: Set<String>, dailyGoalMs: Long): Int {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val installTime = packageInfo.firstInstallTime
        
        var streak = 0
        
        for (i in 0..30) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endTime = calendar.timeInMillis
            
            if (endTime < installTime) {
                break 
            }
            
            val queryEnd = if (i == 0) System.currentTimeMillis() else endTime
            val usageStats = usageStatsManager.queryAndAggregateUsageStats(startTime, queryEnd)
            
            var totalTimeMs = 0L
            for ((packageName, stats) in usageStats) {
                if (blacklistedPackages.contains(packageName)) {
                    totalTimeMs += stats.totalTimeInForeground
                }
            }
            
            if (totalTimeMs <= dailyGoalMs) {
                streak++
            } else {
                break
            }
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
