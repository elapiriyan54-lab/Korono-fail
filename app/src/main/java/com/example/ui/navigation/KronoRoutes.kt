package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface KronoRoute {
  @Serializable data object Home : KronoRoute

  @Serializable
  data class AllFiles(
    val initialFolder: String = "",
    val initialMode: String = "FOLDER", // FOLDER or CHRONOS
    val categoryFilter: String? = null,
  ) : KronoRoute

  @Serializable
  data class SmartSearch(val scopeFolder: String = "") : KronoRoute

  @Serializable data object SafeVault : KronoRoute

  @Serializable data object Notifications : KronoRoute

  @Serializable
  data class SmartCleanup(val initialTab: String = "DUPLICATES") : KronoRoute

  @Serializable data object AudioHub : KronoRoute

  @Serializable
  data class VinylPlayer(
    val trackPath: String,
    val playlistId: Long = -1L,
  ) : KronoRoute

  @Serializable
  data class VideoPlayer(val videoPath: String) : KronoRoute

  @Serializable
  data class DocStudio(
    val filePath: String = "",
    val newExtension: String = "txt",
    val targetFolder: String = "Documents",
  ) : KronoRoute
}
