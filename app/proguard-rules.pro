# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-keep class * extends androidx.room.Entity
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *

# Compose rules are usually handled by the compose-plugin, but we ensure common models are kept
-keep class com.devsummit.scroll.core.db.** { *; }
-keep class com.devsummit.scroll.core.usage.** { *; }
