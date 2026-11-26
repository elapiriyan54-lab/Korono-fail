package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
  @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
  fun observePlaylists(): Flow<List<PlaylistEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlaylist(playlist: PlaylistEntity): Long

  @Query("DELETE FROM playlists WHERE id = :playlistId")
  suspend fun deletePlaylist(playlistId: Long)

  @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
  fun observeTracksForPlaylist(playlistId: Long): Flow<List<PlaylistTrackEntity>>

  @Query("SELECT * FROM playlist_tracks ORDER BY playlistId ASC, sortOrder ASC")
  fun observeAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>>

  @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM playlist_tracks WHERE playlistId = :playlistId")
  suspend fun getMaxOrderForPlaylist(playlistId: Long): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTrack(track: PlaylistTrackEntity): Long

  @Update
  suspend fun updateTracks(tracks: List<PlaylistTrackEntity>)

  @Query("DELETE FROM playlist_tracks WHERE id = :trackId")
  suspend fun removeTrackFromPlaylist(trackId: Long)

  @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
  suspend fun clearTracksForPlaylist(playlistId: Long)
}

@Dao
interface VaultDao {
  @Query("SELECT * FROM vault_items ORDER BY lockedAt DESC")
  fun observeVaultItems(): Flow<List<VaultItemEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVaultItem(item: VaultItemEntity): Long

  @Query("DELETE FROM vault_items WHERE id = :id")
  suspend fun deleteVaultItem(id: Long)
}

@Dao
interface NotificationDao {
  @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
  fun observeNotifications(): Flow<List<NotificationEntity>>

  @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
  fun observeUnreadCount(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotification(notification: NotificationEntity): Long

  @Query("UPDATE notifications SET isRead = 1")
  suspend fun markAllRead()

  @Query("DELETE FROM notifications WHERE id = :id")
  suspend fun dismissNotification(id: Long)

  @Query("DELETE FROM notifications")
  suspend fun clearAll()
}

@Dao
interface SearchHistoryDao {
  @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 12")
  fun observeRecentSearches(): Flow<List<SearchHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSearch(entry: SearchHistoryEntity)

  @Query("DELETE FROM search_history")
  suspend fun clearHistory()
}

@Dao
interface FavoriteTrackDao {
  @Query("SELECT filePath FROM favorite_tracks")
  fun observeFavoritePaths(): Flow<List<String>>

  @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE filePath = :path)")
  suspend fun isFavorite(path: String): Boolean

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun addFavorite(item: FavoriteTrackEntity)

  @Query("DELETE FROM favorite_tracks WHERE filePath = :path")
  suspend fun removeFavorite(path: String)
}

@Dao
interface SyncedFileDao {
  @Query("SELECT COUNT(*) FROM synced_files")
  fun observeSyncedCount(): Flow<Int>

  @Query("SELECT * FROM synced_files")
  suspend fun getAllSynced(): List<SyncedFileEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSynced(item: SyncedFileEntity)

  @Query("DELETE FROM synced_files")
  suspend fun clearAll()
}
