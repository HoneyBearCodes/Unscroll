package com.devsummit.scroll.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_usage")
data class DailyUsage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long, // Epoch milliseconds for the start of the day
    val totalTime: Long, // Time in milliseconds spent on blacklisted categories
    val appCategory: String // E.g., "Social", "Games", "Entertainment"
)
