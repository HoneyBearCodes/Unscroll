package com.devsummit.scroll.core.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistedAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: BlacklistedApp)

    @Delete
    suspend fun removeApp(app: BlacklistedApp)

    @Query("SELECT packageName FROM blacklisted_apps")
    fun getAllBlacklistedAppsFlow(): Flow<List<String>>

    @Query("SELECT packageName FROM blacklisted_apps")
    suspend fun getAllBlacklistedApps(): List<String>
}
