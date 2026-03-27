package com.devsummit.scroll.core.db

import kotlinx.coroutines.flow.Flow

class UsageRepository(
    private val dailyUsageDao: DailyUsageDao,
    private val blacklistedAppDao: BlacklistedAppDao
) {
    
    val allUsages: Flow<List<DailyUsage>> = dailyUsageDao.getAllUsageFlow()
    val allBlacklistedApps: Flow<List<String>> = blacklistedAppDao.getAllBlacklistedAppsFlow()

    suspend fun insertUsage(usage: DailyUsage) {
        dailyUsageDao.insertUsage(usage)
    }

    suspend fun getUsageForDate(date: Long): List<DailyUsage> {
        return dailyUsageDao.getUsageForDate(date)
    }
    
    suspend fun getTotalTimeSpent(): Long {
        return dailyUsageDao.getTotalTimeSpent() ?: 0L
    }

    suspend fun addBlacklistedApp(packageName: String) {
        blacklistedAppDao.insertApp(BlacklistedApp(packageName))
    }

    suspend fun removeBlacklistedApp(packageName: String) {
        blacklistedAppDao.removeApp(BlacklistedApp(packageName))
    }

    suspend fun getBlacklistedAppsSync(): List<String> {
        return blacklistedAppDao.getAllBlacklistedApps()
    }
}
