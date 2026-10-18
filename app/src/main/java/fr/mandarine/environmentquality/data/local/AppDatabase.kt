package fr.mandarine.environmentquality.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedResponseEntity::class,
        FollowedCommuneEntity::class,
        RecentCommuneEntity::class,
        AlertSettingsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedResponses(): CachedResponseDao
    abstract fun followedCommunes(): FollowedCommuneDao
    abstract fun recentCommunes(): RecentCommuneDao
    abstract fun alertSettings(): AlertSettingsDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "environment-quality.db").build()
    }
}
