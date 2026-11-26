package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.GoogleDriveSyncRepository
import com.example.data.local.FavoriteTrackEntity
import com.example.data.local.KronoDatabase
import com.example.data.local.KronoPreferences
import com.example.data.local.NotificationEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.VaultItemEntity
import com.example.data.storage.KronoFileSeeder
import com.example.data.storage.KronoStorageRepository
import com.example.model.AudioTrackItem
import com.example.model.BiometricVisualState
import com.example.model.CleanupSizeFilter
import com.example.model.DuplicateGroup
import com.example.model.FileCategory
import com.example.model.FilesViewMode
import com.example.model.KronoFileItem
import com.example.model.NotificationCategory
import com.example.model.NotificationSeverity
import com.example.model.SearchDateFilter
import com.example.model.SearchScope
import com.example.model.SearchSizeFilter
import com.example.model.SearchTypeFilter
import com.example.model.SortBy
import com.example.model.StorageStats
import com.example.util.FormatUtils
import com.example.util.KronoCrypto
import com.example.util.KronoNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ExplorerUiState(
  val currentFolderRel: String = "",
  val viewMode: FilesViewMode = FilesViewMode.FOLDER,
  val sortBy: SortBy = SortBy.DATE,
  val ascending: Boolean = false,
  val activeCategoryFilter: FileCategory? = null,
  val selectMode: Boolean = false,
  val selectedPaths: Set<String> = emptySet(),
  val items: List<KronoFileItem> = emptyList(),
  val allFilesForChronos: List<KronoFileItem> = emptyList(),
  val allFolders: List<KronoFileItem> = emptyList(),
)

data class SearchUiState(
  val query: String = "",
  val typeFilter: SearchTypeFilter = SearchTypeFilter.ALL,
  val sizeFilter: SearchSizeFilter = SearchSizeFilter.ANY,
  val dateFilter: SearchDateFilter = SearchDateFilter.ANY,
  val scope: SearchScope = SearchScope.ENTIRE_STORAGE,
  val scopeFolderRel: String = "",
)

data class CleanupUiState(
  val isScanning: Boolean = false,
  val duplicateGroups: List<DuplicateGroup> = emptyList(),
  val largeFiles: List<KronoFileItem> = emptyList(),
  val sizeFilter: CleanupSizeFilter = CleanupSizeFilter.ALL,
  val selectedForDeletion: Set<String> = emptySet(),
  val keptOverridePaths: Map<String, String> = emptyMap(),
)

data class VaultUiState(
  val isUnlocked: Boolean = false,
  val hasPinConfigured: Boolean = false,
  val pinSetupFirstEntry: String? = null,
  val biometricState: BiometricVisualState = BiometricVisualState.IDLE_SCANNING,
  val statusMessage: String = "Create a 4-digit PIN to secure Safe Vault",
  val pinInput: String = "",
  val failedAttempts: Int = 0,
  val exitRequestedOnAuthFail: Boolean = false,
)

data class StoragePermissionUiState(
  val hasAllFilesAccess: Boolean = false,
  val hasMediaAccess: Boolean = false,
  val hasMediaFallbackAccess: Boolean = false,
  val hasNotificationAccess: Boolean = false,
  val isUsingDeviceStorage: Boolean = false,
)

data class DriveSyncUiState(
  val isConnected: Boolean = false,
  val accountEmail: String? = null,
  val isSyncing: Boolean = false,
  val syncedCount: Int = 0,
  val lastSyncMs: Long = 0L,
  val progressText: String? = null,
)

class KronoViewModel(application: Application) : AndroidViewModel(application) {
  private val database = KronoDatabase.getInstance(application)
  val prefs = KronoPreferences(application)
  val repository = KronoStorageRepository(application, database)
  val driveSyncRepository = GoogleDriveSyncRepository(application, database)

  private val _storageStats = MutableStateFlow(StorageStats())
  val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

  private val _allFiles = MutableStateFlow<List<KronoFileItem>>(emptyList())
  val allFiles: StateFlow<List<KronoFileItem>> = _allFiles.asStateFlow()

  private val _explorer = MutableStateFlow(ExplorerUiState())
  val explorer: StateFlow<ExplorerUiState> = _explorer.asStateFlow()

  private val _search = MutableStateFlow(SearchUiState())
  val search: StateFlow<SearchUiState> = _search.asStateFlow()

  val searchResults: StateFlow<List<KronoFileItem>> =
    combine(_allFiles, _search) { files, s ->
      repository.searchFilesDeterministic(
        allFiles = files,
        query = s.query,
        typeFilter = s.typeFilter,
        sizeFilter = s.sizeFilter,
        dateFilter = s.dateFilter,
        scopeFolderRelativePath = if (s.scope == SearchScope.CURRENT_FOLDER) s.scopeFolderRel else null,
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _cleanup = MutableStateFlow(CleanupUiState())
  val cleanup: StateFlow<CleanupUiState> = _cleanup.asStateFlow()

  private val _vault = MutableStateFlow(VaultUiState())
  val vault: StateFlow<VaultUiState> = _vault.asStateFlow()

  private val _audioTracks = MutableStateFlow<List<AudioTrackItem>>(emptyList())
  val audioTracks: StateFlow<List<AudioTrackItem>> = _audioTracks.asStateFlow()

  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  private val _permissionState = MutableStateFlow(StoragePermissionUiState())
  val permissionState: StateFlow<StoragePermissionUiState> = _permissionState.asStateFlow()

  private var postedLowStorageAlertThisSession = false
  private var postedDuplicateAlertThisSession = false

  private var cleanedSandboxDemoRows = false

  val userName: StateFlow<String> =
    prefs.userName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

  val exitOnAuthFail: StateFlow<Boolean> =
    prefs.exitOnAuthFail.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  val notifications: StateFlow<List<NotificationEntity>> =
    database.notificationDao().observeNotifications()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val unreadNotificationCount: StateFlow<Int> =
    database.notificationDao().observeUnreadCount()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val recentSearches: StateFlow<List<SearchHistoryEntity>> =
    database.searchHistoryDao().observeRecentSearches()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val vaultItems: StateFlow<List<VaultItemEntity>> =
    database.vaultDao().observeVaultItems()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val playlists: StateFlow<List<PlaylistEntity>> =
    database.playlistDao().observePlaylists()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allPlaylistTracks: StateFlow<List<PlaylistTrackEntity>> =
    database.playlistDao().observeAllPlaylistTracks()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val favoriteTrackPaths: StateFlow<Set<String>> =
    database.favoriteTrackDao().observeFavoritePaths()
      .combine(MutableStateFlow(Unit)) { list, _ -> list.toSet() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

  private val _driveSyncProgress = MutableStateFlow<Pair<Boolean, String?>>(false to null)

  val driveSyncState: StateFlow<DriveSyncUiState> =
    combine(
      prefs.driveConnected,
      prefs.driveAccountEmail,
      prefs.lastDriveSyncMs,
      database.syncedFileDao().observeSyncedCount(),
      _driveSyncProgress,
    ) { connected, email, lastSync, syncedCount, progress ->
      DriveSyncUiState(
        isConnected = connected,
        accountEmail = email,
        isSyncing = progress.first,
        syncedCount = syncedCount,
        lastSyncMs = lastSync,
        progressText = progress.second,
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DriveSyncUiState())

  init {
    viewModelScope.launch {
      KronoNotificationHelper.ensureChannels(getApplication())
      refreshPermissionStatus()
      if (!repository.isUsingDeviceStorage) {
        KronoFileSeeder.ensureSeeded(getApplication(), repository.sandboxRootDir, database, prefs)
      } else {
        prefs.setSeededVersion(KronoFileSeeder.SEED_VERSION)
      }
      val initialConfigured = prefs.hasPinConfigured.first()
      _vault.update {
        it.copy(
          hasPinConfigured = initialConfigured,
          statusMessage =
            if (initialConfigured) "Touch side fingerprint sensor or enter PIN"
            else "Create a 4-digit PIN to secure Safe Vault",
        )
      }
      refreshAll(runCleanupAfter = true)
    }
    viewModelScope.launch {
      prefs.hasPinConfigured.collect { configured ->
        _vault.update { state ->
          val nextMsg =
            when {
              !configured && state.pinSetupFirstEntry == null && state.biometricState == BiometricVisualState.IDLE_SCANNING ->
                "Create a 4-digit PIN to secure Safe Vault"
              configured && state.statusMessage == "Create a 4-digit PIN to secure Safe Vault" ->
                "Touch side fingerprint sensor or enter PIN"
              else -> state.statusMessage
            }
          state.copy(hasPinConfigured = configured, statusMessage = nextMsg)
        }
      }
    }
  }

  fun refreshPermissionStatus() {
    _permissionState.value =
      StoragePermissionUiState(
        hasAllFilesAccess = repository.hasAllFilesAccess(),
        hasMediaAccess = repository.hasMediaReadPermissions(),
        hasMediaFallbackAccess = repository.hasMediaFallbackAccess,
        hasNotificationAccess = KronoNotificationHelper.hasNotificationPermission(getApplication()),
        isUsingDeviceStorage = repository.isUsingDeviceStorage,
      )
  }

  fun refreshStorageRootAndScan() {
    refreshPermissionStatus()
    refreshAll(runCleanupAfter = true)
  }

  fun refreshAll(runCleanupAfter: Boolean = false) {
    viewModelScope.launch {
      refreshPermissionStatus()
      if (repository.isUsingDeviceStorage && !cleanedSandboxDemoRows) {
        cleanedSandboxDemoRows = true
        KronoFileSeeder.removeSeededDemoFiles(repository.sandboxRootDir)
        val sandboxPrefix = repository.sandboxRootDir.absolutePath
        val playlistsNow = database.playlistDao().observePlaylists().first()
        val tracksNow = database.playlistDao().observeAllPlaylistTracks().first()
        val golden = playlistsNow.find { it.name == "Golden Hits Vol.1" }
        if (golden != null) {
          val goldenTracks = tracksNow.filter { it.playlistId == golden.id }
          if (goldenTracks.isNotEmpty() && goldenTracks.all { it.filePath.startsWith(sandboxPrefix) }) {
            database.playlistDao().clearTracksForPlaylist(golden.id)
            database.playlistDao().deletePlaylist(golden.id)
          }
        }
        database.notificationDao().observeNotifications().first()
          .filter {
            it.title == "2 Duplicate File Groups Detected" || it.title == "Chronos Index Synchronized"
          }
          .forEach { database.notificationDao().dismissNotification(it.id) }
      }
      val recursive = repository.listAllFilesRecursive()
      val stats = repository.computeStorageStats(preScannedFiles = recursive)
      _storageStats.value = stats
      _allFiles.value = recursive
      _audioTracks.value = repository.loadAllAudioTracks(preScannedFiles = recursive)
      refreshExplorerItems(preScannedFiles = recursive)
      if (runCleanupAfter) {
        runCleanupScan(preScannedFiles = recursive)
      }

      val freeFraction =
        if (stats.totalBytes > 0L) stats.freeBytes.toFloat() / stats.totalBytes.toFloat()
        else 1f
      if (freeFraction < 0.15f && !postedLowStorageAlertThisSession) {
        postedLowStorageAlertThisSession = true
        val msg =
          "Only ${FormatUtils.formatGbShort(stats.freeBytes)} (${(freeFraction * 100).toInt()}%) free space left on device storage."
        database.notificationDao().insertNotification(
          NotificationEntity(
            title = "Low Storage Warning",
            message = msg,
            category = NotificationCategory.STORAGE_DUPLICATES,
            severity = NotificationSeverity.WARNING,
            actionLabel = "Smart Cleanup",
            actionRoute = "cleanup_large",
          )
        )
        KronoNotificationHelper.postHeadsUpAlert(
          context = getApplication(),
          notificationId = 1001,
          title = "KronoFiles: Low Storage Warning",
          message = msg,
        )
      }
    }
  }

  fun openExplorer(
    folderRel: String = "",
    mode: FilesViewMode = FilesViewMode.FOLDER,
    categoryFilter: FileCategory? = null,
  ) {
    _explorer.update {
      it.copy(
        currentFolderRel = folderRel,
        viewMode = mode,
        activeCategoryFilter = categoryFilter,
        selectMode = false,
        selectedPaths = emptySet(),
      )
    }
    refreshExplorerItems()
  }

  fun navigateToFolder(folderRel: String) {
    _explorer.update {
      it.copy(
        currentFolderRel = folderRel,
        activeCategoryFilter = null,
        selectMode = false,
        selectedPaths = emptySet(),
      )
    }
    refreshExplorerItems()
  }

  fun setExplorerViewMode(mode: FilesViewMode) {
    _explorer.update { it.copy(viewMode = mode) }
    refreshExplorerItems()
  }

  fun setExplorerSort(sortBy: SortBy) {
    _explorer.update {
      val same = it.sortBy == sortBy
      it.copy(sortBy = sortBy, ascending = if (same) !it.ascending else (sortBy == SortBy.NAME))
    }
    refreshExplorerItems()
  }

  fun toggleSelectMode(enabled: Boolean? = null) {
    _explorer.update {
      val next = enabled ?: !it.selectMode
      it.copy(selectMode = next, selectedPaths = if (!next) emptySet() else it.selectedPaths)
    }
  }

  fun toggleFileSelected(path: String) {
    _explorer.update {
      val next = if (path in it.selectedPaths) it.selectedPaths - path else it.selectedPaths + path
      it.copy(selectedPaths = next, selectMode = next.isNotEmpty() || it.selectMode)
    }
  }

  private fun refreshExplorerItems(preScannedFiles: List<KronoFileItem>? = null) {
    viewModelScope.launch {
      val state = _explorer.value
      val rawItems =
        if (state.activeCategoryFilter != null) {
          repository.getFilesForCategory(state.activeCategoryFilter)
        } else {
          repository.listDirectory(state.currentFolderRel)
        }
      val sorted = sortItems(rawItems, state.sortBy, state.ascending)
      val allFilesForChronos =
        if (state.activeCategoryFilter != null) {
          rawItems
        } else {
          preScannedFiles ?: _allFiles.value.takeIf { it.isNotEmpty() } ?: repository.listAllFilesRecursive()
        }
      val folders = repository.listAllFoldersRecursive()
      _explorer.update {
        it.copy(
          items = sorted,
          allFilesForChronos = allFilesForChronos,
          allFolders = folders,
        )
      }
    }
  }

  private fun sortItems(items: List<KronoFileItem>, sortBy: SortBy, ascending: Boolean): List<KronoFileItem> {
    val comparator =
      when (sortBy) {
        SortBy.NAME -> compareBy<KronoFileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }
        SortBy.SIZE -> compareBy<KronoFileItem> { !it.isDirectory }.thenBy { it.sizeBytes }
        SortBy.DATE -> compareBy<KronoFileItem> { !it.isDirectory }.thenBy { it.lastModifiedMs }
        SortBy.TYPE -> compareBy<KronoFileItem> { !it.isDirectory }.thenBy { it.extension }
      }
    val base = items.sortedWith(comparator)
    return if (ascending) base
    else {
      val folders = base.filter { it.isDirectory }
      val files = base.filter { !it.isDirectory }.reversed()
      folders + files
    }
  }

  // File Actions
  fun createFolder(name: String, parentRel: String = _explorer.value.currentFolderRel) {
    viewModelScope.launch {
      repository.createFolder(parentRel, name)
        .onSuccess {
          postMessage("Folder '${it.name}' created")
          refreshAll()
        }
        .onFailure { postMessage(it.message ?: "Failed to create folder") }
    }
  }

  fun renameFile(item: KronoFileItem, newName: String) {
    viewModelScope.launch {
      repository.renameFile(item, newName)
        .onSuccess {
          postMessage("Renamed to ${it.name}")
          refreshAll()
        }
        .onFailure { postMessage(it.message ?: "Rename failed") }
    }
  }

  fun moveFile(item: KronoFileItem, targetFolderRel: String) {
    viewModelScope.launch {
      repository.moveFile(item, targetFolderRel)
        .onSuccess {
          postMessage("Moved ${it.name}")
          refreshAll()
        }
        .onFailure { postMessage(it.message ?: "Move failed") }
    }
  }

  fun copyFile(item: KronoFileItem, targetFolderRel: String) {
    viewModelScope.launch {
      repository.copyFile(item, targetFolderRel)
        .onSuccess {
          postMessage("Copied ${it.name}")
          refreshAll()
        }
        .onFailure { postMessage(it.message ?: "Copy failed") }
    }
  }

  fun deleteFiles(paths: List<String>) {
    viewModelScope.launch {
      val count = repository.deleteFiles(paths)
      _explorer.update { it.copy(selectMode = false, selectedPaths = emptySet()) }
      postMessage("Deleted $count item(s)")
      refreshAll(runCleanupAfter = true)
    }
  }

  fun compressItems(items: List<KronoFileItem>, zipName: String) {
    viewModelScope.launch {
      val folder = _explorer.value.currentFolderRel.ifBlank { "Archives" }
      repository.compressToZip(items, zipName, folder)
        .onSuccess {
          _explorer.update { state -> state.copy(selectMode = false, selectedPaths = emptySet()) }
          postMessage("Compressed to ${it.name}")
          refreshAll()
        }
        .onFailure { postMessage(it.message ?: "Compression failed") }
    }
  }

  fun extractArchive(item: KronoFileItem) {
    viewModelScope.launch {
      repository.extractArchive(item)
        .onSuccess {
          postMessage("Extracted into ${it.name}/")
          refreshAll()
        }
        .onFailure { postMessage(it.message ?: "Extract failed") }
    }
  }

  fun importDeviceUri(uri: Uri, targetFolder: String? = null) {
    viewModelScope.launch {
      repository.importUriToStorage(uri, targetFolder)
        .onSuccess {
          postMessage("Imported ${it.name}")
          refreshAll(runCleanupAfter = true)
        }
        .onFailure { postMessage(it.message ?: "Import failed") }
    }
  }

  // Search Actions (100% offline deterministic)
  fun updateSearchQuery(query: String) {
    _search.update { it.copy(query = query) }
  }

  fun commitSearchToHistory(query: String) {
    val clean = query.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      database.searchHistoryDao().upsertSearch(SearchHistoryEntity(clean))
    }
  }

  fun clearSearchHistory() {
    viewModelScope.launch { database.searchHistoryDao().clearHistory() }
  }

  fun setSearchTypeFilter(filter: SearchTypeFilter) {
    _search.update { it.copy(typeFilter = filter) }
  }

  fun setSearchSizeFilter(filter: SearchSizeFilter) {
    _search.update { it.copy(sizeFilter = filter) }
  }

  fun setSearchDateFilter(filter: SearchDateFilter) {
    _search.update { it.copy(dateFilter = filter) }
  }

  fun setSearchScope(scope: SearchScope, folderRel: String = _explorer.value.currentFolderRel) {
    _search.update { it.copy(scope = scope, scopeFolderRel = folderRel) }
  }

  // Safe Vault Actions
  fun lockFileToVault(item: KronoFileItem) {
    lockFilesToVault(listOf(item))
  }

  fun lockFilesToVault(items: List<KronoFileItem>) {
    val validFiles = items.filter { !it.isDirectory }
    if (validFiles.isEmpty()) return
    viewModelScope.launch {
      var lockedCount = 0
      var lastError: String? = null
      for (item in validFiles) {
        repository.lockFileToVault(item)
          .onSuccess { lockedCount++ }
          .onFailure { lastError = it.message }
      }
      _explorer.update { state -> state.copy(selectMode = false, selectedPaths = emptySet()) }
      if (lockedCount > 0) {
        postMessage(
          if (lockedCount == 1) "${validFiles.first().name} encrypted & locked in Safe Vault"
          else "$lockedCount file(s) encrypted & locked in Safe Vault"
        )
        refreshAll()
      } else {
        postMessage(lastError ?: "Unable to lock file(s)")
      }
    }
  }

  suspend fun decryptVaultItemForPreview(item: VaultItemEntity): File? {
    return repository.decryptVaultItemToTemp(item)
  }

  fun unlockVaultItem(item: VaultItemEntity) {
    viewModelScope.launch {
      repository.unlockVaultItemToStorage(item)
        .onSuccess {
          postMessage("Decrypted & restored ${it.name} to storage")
          refreshAll()
        }
        .onFailure { postMessage(it.message ?: "Restore failed") }
    }
  }

  fun deleteVaultItem(item: VaultItemEntity) {
    viewModelScope.launch {
      repository.deleteVaultItem(item)
      postMessage("Removed ${item.fileName} from Safe Vault")
    }
  }

  fun onPinDigitPressed(digit: Char) {
    val state = _vault.value
    val current = state.pinInput
    if (current.length >= 4) return
    val next = current + digit
    _vault.update { it.copy(pinInput = next, biometricState = BiometricVisualState.IDLE_SCANNING) }
    if (next.length == 4) {
      viewModelScope.launch {
        val expectedHash = prefs.vaultPinHash.first()
        if (expectedHash.isNullOrBlank()) {
          // Two-step first-time PIN setup
          val firstEntry = _vault.value.pinSetupFirstEntry
          if (firstEntry == null) {
            _vault.update {
              it.copy(
                pinSetupFirstEntry = next,
                pinInput = "",
                biometricState = BiometricVisualState.IDLE_SCANNING,
                statusMessage = "Confirm your 4-digit Vault PIN",
              )
            }
          } else if (firstEntry == next) {
            prefs.setVaultPin(next)
            _vault.update {
              it.copy(
                pinSetupFirstEntry = null,
                hasPinConfigured = true,
              )
            }
            onBiometricSucceeded("PIN Created — Vault Unlocked")
          } else {
            _vault.update {
              it.copy(
                pinSetupFirstEntry = null,
                pinInput = "",
                biometricState = BiometricVisualState.FAILURE,
                statusMessage = "PINs did not match — Create a 4-digit PIN",
              )
            }
          }
        } else {
          val pinSalt = prefs.vaultPinSalt.first()
          if (KronoCrypto.verifyPin(next, pinSalt, expectedHash)) {
            onBiometricSucceeded("PIN Verified — Vault Unlocked")
          } else {
            val attempts = _vault.value.failedAttempts + 1
            val shouldExit = exitOnAuthFail.value && attempts >= 3
            onBiometricFailed(
              msg = if (shouldExit) "Max failed PIN attempts — Closing app" else "Incorrect PIN ($attempts/3)",
              failedAttempts = attempts,
              requestExit = shouldExit,
            )
          }
        }
      }
    }
  }

  fun onPinBackspace() {
    _vault.update { it.copy(pinInput = it.pinInput.dropLast(1)) }
  }

  fun onBiometricSucceeded(msg: String = "Side sensor verified — Vault Unlocked") {
    _vault.update {
      it.copy(
        biometricState = BiometricVisualState.SUCCESS,
        statusMessage = msg,
        pinInput = "",
        pinSetupFirstEntry = null,
        isUnlocked = true,
        failedAttempts = 0,
        exitRequestedOnAuthFail = false,
      )
    }
  }

  fun onBiometricFailed(
    msg: String = "Fingerprint not recognized",
    failedAttempts: Int = _vault.value.failedAttempts,
    requestExit: Boolean = false,
  ) {
    _vault.update {
      it.copy(
        biometricState = BiometricVisualState.FAILURE,
        statusMessage = msg,
        pinInput = "",
        isUnlocked = false,
        failedAttempts = failedAttempts,
        exitRequestedOnAuthFail = requestExit,
      )
    }
  }

  fun consumeExitOnAuthFail() {
    _vault.update { it.copy(exitRequestedOnAuthFail = false) }
  }

  fun resetBiometricVisual(customMessage: String? = null) {
    _vault.update { state ->
      val fallbackMsg =
        when {
          customMessage != null -> customMessage
          !state.hasPinConfigured && state.pinSetupFirstEntry == null -> "Create a 4-digit PIN to secure Safe Vault"
          !state.hasPinConfigured && state.pinSetupFirstEntry != null -> "Confirm your 4-digit Vault PIN"
          else -> "Touch side fingerprint sensor or enter PIN"
        }
      state.copy(
        biometricState = BiometricVisualState.IDLE_SCANNING,
        statusMessage = fallbackMsg,
        pinInput = "",
        exitRequestedOnAuthFail = false,
      )
    }
  }

  fun cleanupVaultPreviewCache() {
    KronoCrypto.clearTempPreviewCache(getApplication())
  }

  fun lockVaultImmediately() {
    cleanupVaultPreviewCache()
    _vault.update { state ->
      state.copy(
        isUnlocked = false,
        biometricState = BiometricVisualState.IDLE_SCANNING,
        statusMessage =
          if (state.hasPinConfigured) "Vault locked securely"
          else "Create a 4-digit PIN to secure Safe Vault",
        pinInput = "",
        pinSetupFirstEntry = null,
        failedAttempts = 0,
        exitRequestedOnAuthFail = false,
      )
    }
  }

  fun updateVaultPin(newPin: String) {
    if (newPin.length == 4 && newPin.all { it.isDigit() }) {
      viewModelScope.launch {
        prefs.setVaultPin(newPin)
        postMessage("Safe Vault PIN updated")
      }
    } else {
      postMessage("Vault PIN must be exactly 4 digits")
    }
  }

  fun setExitOnAuthFail(enabled: Boolean) {
    viewModelScope.launch { prefs.setExitOnAuthFail(enabled) }
  }

  // Smart Cleanup Actions (Manual-Only Deletion)
  fun runCleanupScan(preScannedFiles: List<KronoFileItem>? = null) {
    viewModelScope.launch {
      _cleanup.update { it.copy(isScanning = true) }
      val snapshot = preScannedFiles ?: repository.listAllFilesRecursive()
      val dups = repository.findDuplicateGroups(snapshot)
      val large = repository.findLargeFiles(_cleanup.value.sizeFilter.minBytes, snapshot)
      _cleanup.update {
        it.copy(
          isScanning = false,
          duplicateGroups = dups,
          largeFiles = large,
        )
      }
      if (dups.isNotEmpty() && !postedDuplicateAlertThisSession) {
        postedDuplicateAlertThisSession = true
        val totalWaste = dups.sumOf { it.fileSize * (it.files.size - 1).coerceAtLeast(0) }
        KronoNotificationHelper.postHeadsUpAlert(
          context = getApplication(),
          notificationId = 1002,
          title = "KronoFiles: ${dups.size} Duplicate Group(s) Found",
          message = "Reclaim ${FormatUtils.formatBytes(totalWaste)} manually in Smart Cleanup.",
        )
      }
    }
  }

  fun setCleanupSizeFilter(filter: CleanupSizeFilter) {
    _cleanup.update { it.copy(sizeFilter = filter) }
    viewModelScope.launch {
      val cached = _allFiles.value.takeIf { it.isNotEmpty() }
      val large = repository.findLargeFiles(filter.minBytes, cached)
      _cleanup.update { it.copy(largeFiles = large) }
    }
  }

  fun toggleCleanupSelection(path: String) {
    _cleanup.update {
      val next =
        if (path in it.selectedForDeletion) it.selectedForDeletion - path
        else it.selectedForDeletion + path
      it.copy(selectedForDeletion = next)
    }
  }

  fun setKeepCopyForGroup(groupHash: String, keepPath: String) {
    _cleanup.update { state ->
      val nextOverrides = state.keptOverridePaths + (groupHash to keepPath)
      val nextSelection = state.selectedForDeletion - keepPath
      state.copy(keptOverridePaths = nextOverrides, selectedForDeletion = nextSelection)
    }
  }

  fun deselectAllCleanup() {
    _cleanup.update { it.copy(selectedForDeletion = emptySet()) }
  }

  fun deleteSelectedCleanupFiles() {
    val paths = _cleanup.value.selectedForDeletion.toList()
    if (paths.isEmpty()) return
    viewModelScope.launch {
      val deleted = repository.deleteFiles(paths)
      _cleanup.update { it.copy(selectedForDeletion = emptySet()) }
      postMessage("Freed storage by removing $deleted selected file(s)")
      refreshAll(runCleanupAfter = true)
    }
  }

  // Notifications Actions
  fun markAllNotificationsRead() {
    viewModelScope.launch { database.notificationDao().markAllRead() }
  }

  fun dismissNotification(id: Long) {
    viewModelScope.launch { database.notificationDao().dismissNotification(id) }
  }

  fun clearAllNotifications() {
    viewModelScope.launch { database.notificationDao().clearAll() }
  }

  fun logInactivityAlert(sourceName: String) {
    viewModelScope.launch {
      val msg = "Playback paused during $sourceName after 30 minutes of no user interaction."
      database.notificationDao().insertNotification(
        NotificationEntity(
          title = "30-Min Playback Inactivity Alert",
          message = msg,
          category = NotificationCategory.ACTIVITY,
          severity = NotificationSeverity.VIOLET_SYSTEM,
          actionLabel = "Dismiss",
          actionRoute = null,
        )
      )
      KronoNotificationHelper.postHeadsUpAlert(
        context = getApplication(),
        notificationId = 1003,
        title = "30-Min Playback Inactivity Alert",
        message = msg,
      )
    }
  }

  // Playlist & Audio Favorites Actions
  fun createPlaylist(name: String, initialTracks: List<AudioTrackItem>) {
    val clean = name.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      val playlistId = database.playlistDao().insertPlaylist(PlaylistEntity(name = clean))
      initialTracks.forEachIndexed { index, track ->
        database.playlistDao().insertTrack(
          PlaylistTrackEntity(
            playlistId = playlistId,
            filePath = track.path,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationMs = track.durationMs,
            sortOrder = index,
          )
        )
      }
      postMessage("Playlist '$clean' created")
    }
  }

  fun addTrackToPlaylist(playlistId: Long, track: AudioTrackItem) {
    viewModelScope.launch {
      val maxOrder = database.playlistDao().getMaxOrderForPlaylist(playlistId)
      database.playlistDao().insertTrack(
        PlaylistTrackEntity(
          playlistId = playlistId,
          filePath = track.path,
          title = track.title,
          artist = track.artist,
          album = track.album,
          durationMs = track.durationMs,
          sortOrder = maxOrder + 1,
        )
      )
      postMessage("Added '${track.title}' to playlist")
    }
  }

  fun movePlaylistTrack(tracks: List<PlaylistTrackEntity>, fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex || fromIndex !in tracks.indices || toIndex !in tracks.indices) return
    val mutable = tracks.toMutableList()
    val item = mutable.removeAt(fromIndex)
    mutable.add(toIndex, item)
    val reindexed = mutable.mapIndexed { idx, track -> track.copy(sortOrder = idx) }
    viewModelScope.launch {
      database.playlistDao().updateTracks(reindexed)
    }
  }

  fun reorderPlaylistTracksDirect(reordered: List<PlaylistTrackEntity>) {
    if (reordered.isEmpty()) return
    val reindexed = reordered.mapIndexed { idx, track -> track.copy(sortOrder = idx) }
    viewModelScope.launch {
      database.playlistDao().updateTracks(reindexed)
    }
  }

  fun removeTrackFromPlaylist(trackId: Long) {
    viewModelScope.launch { database.playlistDao().removeTrackFromPlaylist(trackId) }
  }

  fun deletePlaylist(playlistId: Long) {
    viewModelScope.launch {
      database.playlistDao().clearTracksForPlaylist(playlistId)
      database.playlistDao().deletePlaylist(playlistId)
      postMessage("Playlist deleted")
    }
  }

  fun toggleFavoriteTrack(path: String) {
    viewModelScope.launch {
      val dao = database.favoriteTrackDao()
      if (dao.isFavorite(path)) dao.removeFavorite(path)
      else dao.addFavorite(FavoriteTrackEntity(path))
    }
  }

  fun updateUserName(name: String) {
    viewModelScope.launch { prefs.setUserName(name) }
  }

  fun hasNetworkForDrive(): Boolean = driveSyncRepository.hasNetworkConnection()

  fun connectGoogleDriveWithToken(accessToken: String) {
    viewModelScope.launch {
      _driveSyncProgress.value = true to "Linking Google Drive..."
      driveSyncRepository.fetchDriveAccountInfo(accessToken)
        .onSuccess { info ->
          prefs.setDriveConnection(connected = true, email = info.email)
          _driveSyncProgress.value = false to null
          postMessage("Google Drive connected (${info.email})")
        }
        .onFailure { err ->
          _driveSyncProgress.value = false to null
          postMessage(err.message ?: "Unable to connect Google Drive")
        }
    }
  }

  fun syncNewFilesToDrive(accessToken: String) {
    if (_driveSyncProgress.value.first) return
    viewModelScope.launch {
      _driveSyncProgress.value = true to "Scanning for new & modified files..."
      val emailFallback = prefs.driveAccountEmail.first()
      if (emailFallback.isNullOrBlank()) {
        driveSyncRepository.fetchDriveAccountInfo(accessToken).onSuccess { info ->
          prefs.setDriveConnection(connected = true, email = info.email)
        }
      } else {
        prefs.setDriveConnection(connected = true, email = emailFallback)
      }

      val filesToInspect = repository.listAllFilesRecursive()
      driveSyncRepository.syncIncremental(
        accessToken = accessToken,
        candidateFiles = filesToInspect,
        onProgress = { processed, total, name ->
          _driveSyncProgress.value = true to "Syncing $processed/$total: $name"
        },
      )
        .onSuccess { result ->
          val now = System.currentTimeMillis()
          prefs.setLastDriveSyncMs(now)
          _driveSyncProgress.value = false to null
          val summary =
            buildString {
              append("Drive Sync complete: ${result.uploadedCount} uploaded")
              if (result.skippedCount > 0) append(", ${result.skippedCount} skipped")
              if (result.failedCount > 0) append(", ${result.failedCount} failed")
            }
          database.notificationDao().insertNotification(
            NotificationEntity(
              title = "Google Drive Incremental Sync",
              message = summary,
              category = NotificationCategory.ACTIVITY,
              severity = NotificationSeverity.CYAN_INFO,
              actionLabel = "Dismiss",
              actionRoute = null,
            )
          )
          postMessage(summary)
        }
        .onFailure { err ->
          _driveSyncProgress.value = false to null
          postMessage(err.message ?: "Google Drive sync failed")
        }
    }
  }

  fun disconnectGoogleDrive() {
    viewModelScope.launch {
      prefs.setDriveConnection(connected = false, email = null)
      prefs.setLastDriveSyncMs(0L)
      database.syncedFileDao().clearAll()
      _driveSyncProgress.value = false to null
      postMessage("Google Drive disconnected and sync ledger cleared")
    }
  }

  fun postMessage(msg: String) {
    _snackbarMessage.value = msg
  }

  fun consumeMessage() {
    _snackbarMessage.value = null
  }

  fun openFileAsFileObject(path: String): File = File(path)
}
