package com.devsummit.scroll.core.db

import kotlinx.coroutines.flow.Flow

class UsageRepository(private val dailyUsageDao: DailyUsageDao) {
    
    val allUsages: Flow<List<DailyUsage>> = dailyUsageDao.getAllUsageFlow()

    suspend fun insertUsage(usage: DailyUsage) {
        dailyUsageDao.insertUsage(usage)
    }

    suspend fun getUsageForDate(date: Long): List<DailyUsage> {
        return dailyUsageDao.getUsageForDate(date)
    }
    
    suspend fun getTotalTimeSpent(): Long {
        return dailyUsageDao.getTotalTimeSpent() ?: 0L
    }
}
