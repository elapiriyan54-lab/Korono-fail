package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.model.NotificationCategory
import com.example.model.NotificationSeverity

class KronoConverters {
  @TypeConverter
  fun fromCategory(value: NotificationCategory): String = value.name

  @TypeConverter
  fun toCategory(value: String): NotificationCategory = NotificationCategory.valueOf(value)

  @TypeConverter
  fun fromSeverity(value: NotificationSeverity): String = value.name

  @TypeConverter
  fun toSeverity(value: String): NotificationSeverity = NotificationSeverity.valueOf(value)
}

@Database(
  entities =
    [
      PlaylistEntity::class,
      PlaylistTrackEntity::class,
      VaultItemEntity::class,
      NotificationEntity::class,
      SearchHistoryEntity::class,
      FavoriteTrackEntity::class,
      SyncedFileEntity::class,
    ],
  version = 2,
  exportSchema = false,
)
@TypeConverters(KronoConverters::class)
abstract class KronoDatabase : RoomDatabase() {
  abstract fun playlistDao(): PlaylistDao

  abstract fun vaultDao(): VaultDao

  abstract fun notificationDao(): NotificationDao

  abstract fun searchHistoryDao(): SearchHistoryDao

  abstract fun favoriteTrackDao(): FavoriteTrackDao

  abstract fun syncedFileDao(): SyncedFileDao

  companion object {
    @Volatile private var INSTANCE: KronoDatabase? = null

    fun getInstance(context: Context): KronoDatabase {
      return INSTANCE
        ?: synchronized(this) {
          val instance =
            Room.databaseBuilder(
                context.applicationContext,
                KronoDatabase::class.java,
                "kronofiles_db",
              )
              .fallbackToDestructiveMigration(dropAllTables = true)
              .build()
          INSTANCE = instance
          instance
        }
    }
  }
}
