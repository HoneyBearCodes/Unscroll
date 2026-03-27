package com.devsummit.scroll.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blacklisted_apps")
data class BlacklistedApp(
    @PrimaryKey val packageName: String
)
