package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.storage.KronoStorageRepository
import com.example.model.AudioTrackItem
import com.example.model.FileCategory
import com.example.model.FileKind
import com.example.model.FilesViewMode
import com.example.model.KronoFileItem
import kotlinx.coroutines.launch
import com.example.ui.components.ApkInspectorDialog
import com.example.ui.components.BottomNavTab
import com.example.ui.components.FloatingGlassBottomBar
import com.example.ui.components.GlassNavigationRail
import com.example.ui.components.ImagePreviewDialog
import com.example.ui.components.KronoObsidianBackdrop
import com.example.ui.navigation.KronoRoute
import com.example.ui.screens.AllFilesChronosScreen
import com.example.ui.screens.AudioHubScreen
import com.example.ui.screens.DocStudioScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.SafeVaultScreen
import com.example.ui.screens.SmartCleanupScreen
import com.example.ui.screens.SmartSearchScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.screens.VinylAudioPlayerScreen
import com.example.ui.theme.KronoFilesTheme
import com.example.ui.theme.TextPrimary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.activity.result.IntentSenderRequest
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidGlassActionButton
import com.example.ui.components.LiquidGlassFrostedButton
import com.example.ui.theme.DeepSapphire
import com.example.ui.theme.IceCyan
import com.example.ui.theme.LiquidEmerald
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmGold
import com.example.viewmodel.DocStudioViewModel
import com.example.viewmodel.KronoViewModel
import com.example.viewmodel.PlayerViewModel
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import java.io.File

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      KronoFilesTheme {
        KronoFilesApp()
      }
    }
  }
}

@Composable
fun KronoFilesApp(
  kronoVm: KronoViewModel = viewModel(),
  playerVm: PlayerViewModel = viewModel(),
  studioVm: DocStudioViewModel = viewModel(),
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()
  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }

  val storageStats by kronoVm.storageStats.collectAsStateWithLifecycle()
  val permissionState by kronoVm.permissionState.collectAsStateWithLifecycle()
  val driveSyncState by kronoVm.driveSyncState.collectAsStateWithLifecycle()
  val userName by kronoVm.userName.collectAsStateWithLifecycle()
  val unreadCount by kronoVm.unreadNotificationCount.collectAsStateWithLifecycle()
  val explorerState by kronoVm.explorer.collectAsStateWithLifecycle()
  val searchState by kronoVm.search.collectAsStateWithLifecycle()
  val searchResults by kronoVm.searchResults.collectAsStateWithLifecycle()
  val recentSearches by kronoVm.recentSearches.collectAsStateWithLifecycle()
  val vaultState by kronoVm.vault.collectAsStateWithLifecycle()
  val vaultItems by kronoVm.vaultItems.collectAsStateWithLifecycle()
  val exitOnAuthFail by kronoVm.exitOnAuthFail.collectAsStateWithLifecycle()
  val notifications by kronoVm.notifications.collectAsStateWithLifecycle()
  val cleanupState by kronoVm.cleanup.collectAsStateWithLifecycle()
  val audioTracks by kronoVm.audioTracks.collectAsStateWithLifecycle()
  val playlists by kronoVm.playlists.collectAsStateWithLifecycle()
  val allPlaylistTracks by kronoVm.allPlaylistTracks.collectAsStateWithLifecycle()
  val favoritePaths by kronoVm.favoriteTrackPaths.collectAsStateWithLifecycle()
  val allFiles by kronoVm.allFiles.collectAsStateWithLifecycle()
  val snackbarMsg by kronoVm.snackbarMessage.collectAsStateWithLifecycle()

  val audioPlayerState by playerVm.audioState.collectAsStateWithLifecycle()
  val studioState by studioVm.state.collectAsStateWithLifecycle()

  var previewImageTarget by remember { mutableStateOf<KronoFileItem?>(null) }
  var previewApkFile by remember { mutableStateOf<File?>(null) }
  var isVaultPreviewOpen by remember { mutableStateOf(false) }
  var pendingDriveAction by remember { mutableStateOf<String?>(null) }

  val latestPermissionState by rememberUpdatedState(permissionState)
  val latestVaultUnlocked by rememberUpdatedState(vaultState.isUnlocked)

  // Google Drive OAuth2 Authorization Launcher (https://www.googleapis.com/auth/drive.file)
  val driveAuthLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
      val action = pendingDriveAction
      pendingDriveAction = null
      val activity = context as? Activity
      if (result.resultCode == Activity.RESULT_OK && activity != null) {
        runCatching {
          Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(result.data)
        }
          .onSuccess { authResult ->
            val token = authResult.accessToken
            if (token.isNullOrBlank()) {
              kronoVm.postMessage("Google Drive authorization returned no access token")
            } else if (action == "SYNC") {
              kronoVm.syncNewFilesToDrive(token)
            } else {
              kronoVm.connectGoogleDriveWithToken(token)
            }
          }
          .onFailure { err ->
            kronoVm.postMessage(err.localizedMessage ?: "Google Drive sign-in failed")
          }
      } else {
        kronoVm.postMessage("Google Drive authorization canceled")
      }
    }

  fun launchGoogleDriveAuth(action: String) {
    if (!kronoVm.hasNetworkForDrive()) {
      kronoVm.postMessage("No internet connection. Connect to Wi-Fi or mobile data first.")
      return
    }
    val activity = context as? Activity
    if (activity == null) {
      kronoVm.postMessage("Unable to resolve host activity for Google Sign-In")
      return
    }
    pendingDriveAction = action
    val authRequest =
      AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/drive.file")))
        .build()
    Identity.getAuthorizationClient(activity)
      .authorize(authRequest)
      .addOnSuccessListener { authResult ->
        if (authResult.hasResolution()) {
          val pendingIntent = authResult.pendingIntent
          if (pendingIntent != null) {
            runCatching {
              driveAuthLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            }.onFailure { err ->
              pendingDriveAction = null
              kronoVm.postMessage("Could not launch Google consent screen: ${err.localizedMessage}")
            }
          } else {
            pendingDriveAction = null
            kronoVm.postMessage("Google authorization resolution missing")
          }
        } else {
          val token = authResult.accessToken
          pendingDriveAction = null
          if (token.isNullOrBlank()) {
            kronoVm.postMessage("Sign in to a Google account on this device to link Google Drive")
          } else if (action == "SYNC") {
            kronoVm.syncNewFilesToDrive(token)
          } else {
            kronoVm.connectGoogleDriveWithToken(token)
          }
        }
      }
      .addOnFailureListener { err ->
        pendingDriveAction = null
        kronoVm.postMessage("Google Drive unavailable: ${err.localizedMessage ?: "Sign-in failed"}")
      }
  }

  // Runtime Media & Notifications Permission Launcher
  val runtimePermissionsLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      kronoVm.refreshStorageRootAndScan()
    }

  // All Files Access (Android 11+ Settings Launcher)
  val allFilesAccessLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      kronoVm.refreshStorageRootAndScan()
    }

  fun requestAllFilesAccess() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val intent =
        runCatching {
          Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
          }
        }.getOrElse {
          Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }
      allFilesAccessLauncher.launch(intent)
    } else {
      runtimePermissionsLauncher.launch(
        arrayOf(
          Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
      )
    }
  }

  fun requestMediaAndAlertPermissions() {
    val perms =
      buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          add(Manifest.permission.POST_NOTIFICATIONS)
          add(Manifest.permission.READ_MEDIA_IMAGES)
          add(Manifest.permission.READ_MEDIA_VIDEO)
          add(Manifest.permission.READ_MEDIA_AUDIO)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
          }
        } else {
          add(Manifest.permission.READ_EXTERNAL_STORAGE)
          if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
          }
        }
      }.toTypedArray()
    runtimePermissionsLauncher.launch(perms)
  }

  DisposableEffect(lifecycleOwner) {
    val observer =
      LifecycleEventObserver { _, event ->
        when (event) {
          Lifecycle.Event.ON_RESUME -> {
            val prevPerm = latestPermissionState
            kronoVm.refreshPermissionStatus()
            val curPerm = kronoVm.permissionState.value
            if (curPerm != prevPerm) {
              kronoVm.refreshStorageRootAndScan()
            }
          }
          Lifecycle.Event.ON_STOP -> {
            isVaultPreviewOpen = false
            kronoVm.cleanupVaultPreviewCache()
            if (latestVaultUnlocked) {
              kronoVm.lockVaultImmediately()
            }
          }
          else -> Unit
        }
      }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  // Zero-permission Photo Picker & Document Picker
  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) kronoVm.importDeviceUri(uri)
    }

  val openDocumentLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) kronoVm.importDeviceUri(uri)
    }

  LaunchedEffect(Unit) {
    playerVm.inactivityTimeoutEvents.collect { title ->
      kronoVm.logInactivityAlert(title)
    }
  }

  LaunchedEffect(snackbarMsg) {
    snackbarMsg?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      kronoVm.consumeMessage()
    }
  }

  LaunchedEffect(studioState.statusMessage) {
    studioState.statusMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      studioVm.consumeStatus()
    }
  }

  fun shareFile(file: File) {
    val intent = kronoVm.repository.buildShareIntent(file)
    if (intent != null) {
      context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
    } else {
      kronoVm.postMessage("Unable to share file")
    }
  }

  fun shareItem(item: KronoFileItem) {
    val intent = kronoVm.repository.buildShareIntentForItem(item)
    if (intent != null) {
      context.startActivity(Intent.createChooser(intent, "Share ${item.name}"))
    } else {
      kronoVm.postMessage("Unable to share file")
    }
  }

  fun openAnyFile(filePath: String, kind: FileKind, contentUri: String? = null, displayName: String? = null, sizeBytes: Long? = null) {
    val file = File(filePath)
    when (kind) {
      FileKind.IMAGE -> {
        previewImageTarget =
          KronoFileItem(
            id = contentUri ?: filePath,
            name = displayName ?: file.name,
            absolutePath = filePath,
            relativePath = file.name,
            parentRelativePath = "",
            isDirectory = false,
            sizeBytes = sizeBytes ?: file.length(),
            lastModifiedMs = file.lastModified(),
            extension = file.extension.lowercase(),
            kind = kind,
            contentUri = contentUri,
          )
      }
      FileKind.APK -> previewApkFile = file
      FileKind.VIDEO -> navController.navigate(KronoRoute.VideoPlayer(videoPath = contentUri ?: filePath))
      FileKind.AUDIO -> {
        val tracks = kronoVm.audioTracks.value
        val idx =
          tracks.indexOfFirst {
            it.path == filePath || (contentUri != null && it.contentUri == contentUri)
          }.coerceAtLeast(0)
        val queue =
          if (tracks.isEmpty()) {
            listOf(
              AudioTrackItem(
                path = filePath,
                title = (displayName ?: file.name).substringBeforeLast('.'),
                artist = "Local Audio",
                album = "KronoFiles",
                durationMs = 20_000L,
                sizeBytes = sizeBytes ?: file.length(),
                waveformPeaks = List(44) { 0.45f },
                contentUri = contentUri,
              )
            )
          } else tracks
        playerVm.startQueue(queue, idx)
        navController.navigate(KronoRoute.VinylPlayer(trackPath = filePath, playlistId = -1L))
      }
      FileKind.PDF, FileKind.DOCX, FileKind.XLSX, FileKind.CODE, FileKind.TEXT -> {
        val ext = (displayName ?: file.name).substringAfterLast('.', file.extension).lowercase()
        studioVm.openOrCreate(filePath, ext, "", contentUri = contentUri, displayName = displayName)
        navController.navigate(KronoRoute.DocStudio(filePath = filePath, newExtension = ext))
      }
      FileKind.ARCHIVE -> {
        val archiveItem =
          if (file.exists()) kronoVm.repository.toFileItem(file)
          else
            KronoFileItem(
              id = contentUri ?: filePath,
              name = displayName ?: file.name,
              absolutePath = filePath,
              relativePath = displayName ?: file.name,
              parentRelativePath = "",
              isDirectory = false,
              sizeBytes = sizeBytes ?: 0L,
              lastModifiedMs = System.currentTimeMillis(),
              extension = (displayName ?: file.name).substringAfterLast('.', "").lowercase(),
              kind = FileKind.ARCHIVE,
              contentUri = contentUri,
            )
        if (file.exists() || contentUri != null) {
          kronoVm.extractArchive(archiveItem)
        } else {
          kronoVm.postMessage("Please import or grant All Files access to extract this archive")
        }
      }
      else -> {
        val ext = (displayName ?: file.name).substringAfterLast('.', file.extension).ifBlank { "txt" }.lowercase()
        studioVm.openOrCreate(filePath, ext, "", contentUri = contentUri, displayName = displayName)
        navController.navigate(KronoRoute.DocStudio(filePath = filePath, newExtension = ext))
      }
    }
  }

  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route.orEmpty()

  val activeBottomTab: BottomNavTab? =
    when {
      currentRoute.contains("Home") -> BottomNavTab.HOME
      currentRoute.contains("AllFiles") -> BottomNavTab.ALL_FILES
      currentRoute.contains("SmartSearch") -> BottomNavTab.SEARCH
      currentRoute.contains("SafeVault") -> BottomNavTab.VAULT
      else -> null
    }

  LaunchedEffect(activeBottomTab, currentRoute) {
    val inVaultPreviewRoute =
      isVaultPreviewOpen &&
        (currentRoute.contains("VinylPlayer") ||
          currentRoute.contains("VideoPlayer") ||
          currentRoute.contains("DocStudio"))
    if (activeBottomTab != BottomNavTab.VAULT && !inVaultPreviewRoute && latestVaultUnlocked) {
      isVaultPreviewOpen = false
      kronoVm.lockVaultImmediately()
    }
  }

  fun navigateBottomTab(tab: BottomNavTab) {
    when (tab) {
      BottomNavTab.HOME -> {
        navController.navigate(KronoRoute.Home) {
          popUpTo(KronoRoute.Home) { inclusive = true }
          launchSingleTop = true
        }
      }
      BottomNavTab.ALL_FILES -> {
        kronoVm.openExplorer(folderRel = "", mode = FilesViewMode.FOLDER, categoryFilter = null)
        navController.navigate(KronoRoute.AllFiles()) {
          popUpTo(KronoRoute.Home)
          launchSingleTop = true
        }
      }
      BottomNavTab.SEARCH -> {
        navController.navigate(KronoRoute.SmartSearch(explorerState.currentFolderRel)) {
          popUpTo(KronoRoute.Home)
          launchSingleTop = true
        }
      }
      BottomNavTab.VAULT -> {
        navController.navigate(KronoRoute.SafeVault) {
          popUpTo(KronoRoute.Home)
          launchSingleTop = true
        }
      }
    }
  }

  KronoObsidianBackdrop {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val showRail = maxWidth >= 840.dp && activeBottomTab != null

      Row(modifier = Modifier.fillMaxSize()) {
        if (showRail && activeBottomTab != null) {
          GlassNavigationRail(
            selectedTab = activeBottomTab,
            onSelectTab = ::navigateBottomTab,
          )
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          NavHost(
            navController = navController,
            startDestination = KronoRoute.Home,
            modifier = Modifier.fillMaxSize(),
          ) {
            composable<KronoRoute.Home> {
              HomeDashboardScreen(
                userName = userName,
                storageStats = storageStats,
                permissionState = permissionState,
                driveSyncState = driveSyncState,
                unreadNotifications = unreadCount,
                hasBottomBar = !showRail,
                onOpenVault = { navigateBottomTab(BottomNavTab.VAULT) },
                onOpenNotifications = { navController.navigate(KronoRoute.Notifications) },
                onOpenChronos = {
                  kronoVm.openExplorer(folderRel = "", mode = FilesViewMode.CHRONOS, categoryFilter = null)
                  navController.navigate(KronoRoute.AllFiles(initialMode = "CHRONOS"))
                },
                onOpenCleanup = {
                  kronoVm.runCleanupScan()
                  navController.navigate(KronoRoute.SmartCleanup())
                },
                onOpenCategory = { category ->
                  when (category) {
                    FileCategory.AUDIO -> navController.navigate(KronoRoute.AudioHub)
                    else -> {
                      kronoVm.openExplorer(folderRel = "", mode = FilesViewMode.FOLDER, categoryFilter = category)
                      navController.navigate(
                        KronoRoute.AllFiles(categoryFilter = category.name)
                      )
                    }
                  }
                },
                onImportMedia = {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                  )
                },
                onImportDocument = {
                  openDocumentLauncher.launch(arrayOf("*/*"))
                },
                onCreateFolder = { name -> kronoVm.createFolder(name, "") },
                onCreateNewDocument = { ext ->
                  studioVm.openOrCreate("", ext, "Documents")
                  navController.navigate(KronoRoute.DocStudio(filePath = "", newExtension = ext))
                },
                onUpdateUserName = { kronoVm.updateUserName(it) },
                onRequestAllFilesAccess = ::requestAllFilesAccess,
                onRequestMediaAndAlertsPermissions = ::requestMediaAndAlertPermissions,
                onConnectGoogleDrive = { launchGoogleDriveAuth("CONNECT") },
                onSyncNewFilesToDrive = { launchGoogleDriveAuth("SYNC") },
                onDisconnectGoogleDrive = { kronoVm.disconnectGoogleDrive() },
              )
            }

            composable<KronoRoute.AllFiles> { entry ->
              val route = entry.toRoute<KronoRoute.AllFiles>()
              val chronosGrouped =
                remember(explorerState.allFilesForChronos) {
                  kronoVm.repository.groupIntoChronosBuckets(explorerState.allFilesForChronos)
                }
              AllFilesChronosScreen(
                state = explorerState,
                chronosGrouped = chronosGrouped,
                isUsingDeviceStorage = permissionState.isUsingDeviceStorage,
                hasMediaFallbackAccess = permissionState.hasMediaFallbackAccess,
                hasBottomBar = !showRail,
                onNavigateBack =
                  if (route.categoryFilter != null || route.initialMode == "CHRONOS") {
                    { navController.popBackStack() }
                  } else null,
                onNavigateFolder = { kronoVm.navigateToFolder(it) },
                onChangeViewMode = { kronoVm.setExplorerViewMode(it) },
                onSortSelect = { kronoVm.setExplorerSort(it) },
                onToggleSelectMode = { kronoVm.toggleSelectMode() },
                onToggleItemSelect = { kronoVm.toggleFileSelected(it) },
                onOpenFile = { item -> openAnyFile(item.absolutePath, item.kind, item.contentUri, item.name, item.sizeBytes) },
                onRenameFile = { item, newName -> kronoVm.renameFile(item, newName) },
                onMoveFile = { item, target -> kronoVm.moveFile(item, target) },
                onCopyFile = { item, target -> kronoVm.copyFile(item, target) },
                onCompressItems = { items, zipName -> kronoVm.compressItems(items, zipName) },
                onExtractArchive = { item -> kronoVm.extractArchive(item) },
                onLockToVault = { item -> kronoVm.lockFileToVault(item) },
                onLockMultipleToVault = { items -> kronoVm.lockFilesToVault(items) },
                onShareFile = { item -> shareItem(item) },
                onDeletePaths = { paths -> kronoVm.deleteFiles(paths) },
              )
            }

            composable<KronoRoute.SmartSearch> {
              SmartSearchScreen(
                searchState = searchState,
                results = searchResults,
                recentSearches = recentSearches,
                currentFolderRel = explorerState.currentFolderRel,
                hasBottomBar = !showRail,
                onQueryChange = { kronoVm.updateSearchQuery(it) },
                onCommitQuery = { kronoVm.commitSearchToHistory(it) },
                onClearHistory = { kronoVm.clearSearchHistory() },
                onSelectType = { kronoVm.setSearchTypeFilter(it) },
                onSelectSize = { kronoVm.setSearchSizeFilter(it) },
                onSelectDate = { kronoVm.setSearchDateFilter(it) },
                onSelectScope = { kronoVm.setSearchScope(it, explorerState.currentFolderRel) },
                onOpenFile = { item -> openAnyFile(item.absolutePath, item.kind, item.contentUri, item.name, item.sizeBytes) },
              )
            }

            composable<KronoRoute.SafeVault> {
              SafeVaultScreen(
                vaultState = vaultState,
                vaultItems = vaultItems,
                availableStorageFiles = allFiles,
                exitOnAuthFail = exitOnAuthFail,
                hasBottomBar = !showRail,
                onPinDigit = { kronoVm.onPinDigitPressed(it) },
                onPinBackspace = { kronoVm.onPinBackspace() },
                onBiometricSuccess = { kronoVm.onBiometricSucceeded() },
                onBiometricFailure = { msg, reqExit -> kronoVm.onBiometricFailed(msg, requestExit = reqExit) },
                onResetBiometricVisual = { kronoVm.resetBiometricVisual() },
                onExitOnFailToggle = { kronoVm.setExitOnAuthFail(it) },
                onLockVaultNow = { kronoVm.lockVaultImmediately() },
                onLockFileIntoVault = { kronoVm.lockFileToVault(it) },
                onUnlockVaultItem = { kronoVm.unlockVaultItem(it) },
                onDeleteVaultItem = { kronoVm.deleteVaultItem(it) },
                onOpenVaultItem = { item ->
                  coroutineScope.launch {
                    val decrypted = kronoVm.decryptVaultItemForPreview(item)
                    if (decrypted != null) {
                      isVaultPreviewOpen = true
                      openAnyFile(
                        decrypted.absolutePath,
                        KronoStorageRepository.classifyExtension(item.extension),
                        displayName = item.fileName,
                        sizeBytes = item.sizeBytes,
                      )
                    } else {
                      kronoVm.postMessage("Unable to decrypt vault item")
                    }
                  }
                },
                onUpdatePin = { kronoVm.updateVaultPin(it) },
                onConsumeExitOnAuthFail = { kronoVm.consumeExitOnAuthFail() },
                onCloseVaultFlowOnAuthFail = { (context as? Activity)?.finishAffinity() },
              )
            }

            composable<KronoRoute.Notifications> {
              NotificationsScreen(
                notifications = notifications,
                onBack = { navController.popBackStack() },
                onMarkAllRead = { kronoVm.markAllNotificationsRead() },
                onClearAll = { kronoVm.clearAllNotifications() },
                onDismissItem = { kronoVm.dismissNotification(it) },
                onActionClick = { route ->
                  kronoVm.runCleanupScan()
                  val tab = if (route.contains("duplicates")) "DUPLICATES" else "LARGE"
                  navController.navigate(KronoRoute.SmartCleanup(initialTab = tab))
                },
              )
            }

            composable<KronoRoute.SmartCleanup> { entry ->
              val route = entry.toRoute<KronoRoute.SmartCleanup>()
              SmartCleanupScreen(
                state = cleanupState,
                initialTabIndex = if (route.initialTab == "LARGE") 1 else 0,
                onBack = { navController.popBackStack() },
                onRescan = { kronoVm.runCleanupScan() },
                onSelectSizeFilter = { kronoVm.setCleanupSizeFilter(it) },
                onToggleSelectPath = { kronoVm.toggleCleanupSelection(it) },
                onKeepCopy = { hash, path -> kronoVm.setKeepCopyForGroup(hash, path) },
                onDeselectAll = { kronoVm.deselectAllCleanup() },
                onConfirmDeleteSelected = { kronoVm.deleteSelectedCleanupFiles() },
              )
            }

            composable<KronoRoute.AudioHub> {
              AudioHubScreen(
                allTracks = audioTracks,
                playlists = playlists,
                allPlaylistTracks = allPlaylistTracks,
                onBack = { navController.popBackStack() },
                onPlayTrack = { track, playlistId ->
                  val queue =
                    if (playlistId > 0L) {
                      allPlaylistTracks
                        .filter { it.playlistId == playlistId }
                        .sortedBy { it.sortOrder }
                        .map { pt ->
                          audioTracks.find { it.path == pt.filePath }
                            ?: AudioTrackItem(
                              path = pt.filePath,
                              title = pt.title,
                              artist = pt.artist,
                              album = pt.album,
                              durationMs = pt.durationMs,
                              sizeBytes = 0L,
                              waveformPeaks = List(44) { 0.45f },
                            )
                        }
                    } else audioTracks
                  val startIndex = queue.indexOfFirst { it.path == track.path }.coerceAtLeast(0)
                  playerVm.startQueue(queue, startIndex)
                  navController.navigate(KronoRoute.VinylPlayer(trackPath = track.path, playlistId = playlistId))
                },
                onCreatePlaylist = { name, tracks -> kronoVm.createPlaylist(name, tracks) },
                onAddTrackToPlaylist = { pid, track -> kronoVm.addTrackToPlaylist(pid, track) },
                onReorderPlaylistTracks = { list, from, to -> kronoVm.movePlaylistTrack(list, from, to) },
                onCommitPlaylistOrder = { reordered -> kronoVm.reorderPlaylistTracksDirect(reordered) },
                onRemoveTrackFromPlaylist = { id -> kronoVm.removeTrackFromPlaylist(id) },
                onDeletePlaylist = { id -> kronoVm.deletePlaylist(id) },
              )
            }

            composable<KronoRoute.VinylPlayer> {
              VinylAudioPlayerScreen(
                state = audioPlayerState,
                favoritePaths = favoritePaths,
                onBack = {
                  isVaultPreviewOpen = false
                  kronoVm.cleanupVaultPreviewCache()
                  navController.popBackStack()
                },
                onShareTrack = { track ->
                  val f = File(track.path)
                  if (f.exists() && f.canRead()) {
                    shareFile(f)
                  } else if (track.contentUri != null) {
                    val intent =
                      Intent(Intent.ACTION_SEND).apply {
                        type = "audio/*"
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(track.contentUri))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                      }
                    context.startActivity(Intent.createChooser(intent, "Share ${track.title}"))
                  } else {
                    kronoVm.postMessage("Unable to share audio track")
                  }
                },
                onToggleFavorite = { path -> kronoVm.toggleFavoriteTrack(path) },
                onTogglePlayPause = { playerVm.togglePlayPause() },
                onPrevious = { playerVm.playPrevious() },
                onNext = { playerVm.playNext() },
                onToggleShuffle = { playerVm.toggleShuffle() },
                onCycleRepeat = { playerVm.cycleRepeatMode() },
                onSeekTo = { pos -> playerVm.seekTo(pos) },
                onVinylScrubStart = { playerVm.onVinylScrubStart() },
                onVinylScrubDelta = { delta -> playerVm.onVinylScrubDelta(delta) },
                onVinylScrubEnd = { playerVm.onVinylScrubEnd() },
                onSelectQueueTrack = { idx -> playerVm.selectTrackInQueue(idx) },
                onTriggerInactivityTest = { playerVm.triggerInactivityPromptNow() },
                onStayAwake = { playerVm.markUserInteraction() },
                onStopForInactivity = { title ->
                  playerVm.stopPlayback()
                  kronoVm.logInactivityAlert(title)
                },
              )
            }

            composable<KronoRoute.VideoPlayer> { entry ->
              val route = entry.toRoute<KronoRoute.VideoPlayer>()
              VideoPlayerScreen(
                videoPath = route.videoPath,
                onClose = {
                  isVaultPreviewOpen = false
                  kronoVm.cleanupVaultPreviewCache()
                  navController.popBackStack()
                },
                onLogInactivityAlert = { name -> kronoVm.logInactivityAlert(name) },
              )
            }

            composable<KronoRoute.DocStudio> {
              DocStudioScreen(
                state = studioState,
                onBack = {
                  isVaultPreviewOpen = false
                  kronoVm.cleanupVaultPreviewCache()
                  navController.popBackStack()
                },
                onRenderPdfPage = { pageIdx -> studioVm.loadPdfPage(pageIdx) },
                onRename = { studioVm.updateFileName(it) },
                onTextChange = { studioVm.onTextChange(it) },
                onCellChange = { r, c, v -> studioVm.updateSheetCell(r, c, v) },
                onAddSheetRow = { studioVm.addSheetRow() },
                onUndo = { studioVm.undo() },
                onRedo = { studioVm.redo() },
                onFormat = { studioVm.formatTextSnippet() },
                onTogglePreview = { studioVm.togglePreviewMode() },
                onSave = { studioVm.saveCurrentFile { kronoVm.refreshAll() } },
                onExportPdf = { studioVm.exportAsPdf { kronoVm.refreshAll() } },
                onPrint = { studioVm.printDocument(context) },
              )
            }
          }

          // Floating Glass Bottom Navigation Bar on Compact / Medium widths
          if (!showRail && activeBottomTab != null) {
            FloatingGlassBottomBar(
              selectedTab = activeBottomTab,
              onSelectTab = ::navigateBottomTab,
              modifier = Modifier.align(Alignment.BottomCenter),
            )
          }

          SnackbarHost(
            hostState = snackbarHostState,
            modifier =
              Modifier.align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp),
          ) { data ->
            GlassCard(
              modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
              cornerRadius = 18.dp,
              borderAccentStart = LiquidEmerald,
              borderAccentEnd = IceCyan,
              fillAlphaTop = 0.28f,
              fillAlphaBottom = 0.16f,
            ) {
              Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
              )
            }
          }
        }
      }
    }
  }

  previewImageTarget?.let { target ->
    val file = File(target.absolutePath)
    ImagePreviewDialog(
      file = file,
      title = target.name,
      contentUri = target.contentUri,
      sizeBytes = target.sizeBytes,
      onShare = { shareItem(target) },
      onDismiss = {
        previewImageTarget = null
        kronoVm.cleanupVaultPreviewCache()
      },
    )
  }

  previewApkFile?.let { file ->
    ApkInspectorDialog(
      file = file,
      onShare = { shareFile(file) },
      onDismiss = {
        previewApkFile = null
        kronoVm.cleanupVaultPreviewCache()
      },
    )
  }

  if (audioPlayerState.inactivityAlertVisible && !currentRoute.contains("VinylPlayer")) {
    val activeTitle = audioPlayerState.currentTrack?.title ?: "Audio Playback"
    Dialog(onDismissRequest = { playerVm.markUserInteraction() }) {
      GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        borderAccentStart = IceCyan,
        borderAccentEnd = WarmAmber,
        fillAlphaTop = 0.30f,
        fillAlphaBottom = 0.16f,
      ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("30-Min Inactivity Check", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
          Text(
            "No interaction detected for 30 minutes during audio playback ($activeTitle). Auto-stopping in ${audioPlayerState.inactivityCountdownSec}s.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            LiquidGlassFrostedButton(
              onClick = {
                playerVm.stopPlayback()
                kronoVm.logInactivityAlert(activeTitle)
              },
              contentColor = WarmGold,
            ) {
              Text("Stop Now", color = WarmGold)
            }
            LiquidGlassActionButton(
              onClick = { playerVm.markUserInteraction() },
              accentStart = LiquidEmerald,
              accentEnd = IceCyan,
            ) {
              Text("Keep Playing", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
