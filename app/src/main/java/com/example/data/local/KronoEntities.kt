package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.NotificationCategory
import com.example.model.NotificationSeverity

@Entity(tableName = "playlists")
data class PlaylistEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "playlist_tracks")
data class PlaylistTrackEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val playlistId: Long,
  val filePath: String,
  val title: String,
  val artist: String,
  val album: String,
  val durationMs: Long,
  val sortOrder: Int,
)

@Entity(tableName = "vault_items")
data class VaultItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val fileName: String,
  val vaultFilePath: String,
  val originalRelativePath: String,
  val extension: String,
  val sizeBytes: Long,
  val lockedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "notifications")
data class NotificationEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val message: String,
  val category: NotificationCategory,
  val severity: NotificationSeverity,
  val actionLabel: String?,
  val actionRoute: String?,
  val isRead: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
  @PrimaryKey val query: String,
  val searchedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
  @PrimaryKey val filePath: String,
  val favoritedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "synced_files")
data class SyncedFileEntity(
  @PrimaryKey val filePath: String,
  val sizeBytes: Long,
  val lastModifiedMs: Long,
  val sha256Hash: String,
  val driveFileId: String,
  val syncedAtMs: Long = System.currentTimeMillis(),
)
