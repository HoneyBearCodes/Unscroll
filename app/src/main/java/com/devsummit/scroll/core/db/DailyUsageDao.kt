package com.devsummit.scroll.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: DailyUsage): Unit

    @Query("SELECT * FROM daily_usage WHERE date = :date")
    suspend fun getUsageForDate(date: Long): List<DailyUsage>

    @Query("SELECT * FROM daily_usage ORDER BY date DESC")
    fun getAllUsageFlow(): Flow<List<DailyUsage>>
    
    @Query("SELECT SUM(totalTime) FROM daily_usage")
    suspend fun getTotalTimeSpent(): Long?
}
