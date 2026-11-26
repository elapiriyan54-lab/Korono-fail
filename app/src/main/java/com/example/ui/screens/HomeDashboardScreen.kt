package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material3.CircularProgressIndicator
import com.example.R
import com.example.model.FileCategory
import com.example.model.StorageStats
import com.example.ui.components.GlassBubbleButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassTextInputDialog
import com.example.ui.components.LiquidGlassIconOrb
import com.example.ui.theme.DeepSapphire
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FrostedPearl
import com.example.ui.theme.IceCyan
import com.example.ui.theme.LiquidEmerald
import com.example.ui.theme.MoltenAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.PrismaticViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmGold
import com.example.util.FormatUtils
import com.example.viewmodel.DriveSyncUiState
import com.example.viewmodel.StoragePermissionUiState

private data class CategoryTileSpec(
  val category: FileCategory,
  val icon: ImageVector,
  val accent: Color,
  val subtitle: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeDashboardScreen(
  userName: String,
  storageStats: StorageStats,
  permissionState: StoragePermissionUiState,
  driveSyncState: DriveSyncUiState,
  unreadNotifications: Int,
  onOpenVault: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenChronos: () -> Unit,
  onOpenCleanup: () -> Unit,
  onOpenCategory: (FileCategory) -> Unit,
  onImportMedia: () -> Unit,
  onImportDocument: () -> Unit,
  onCreateFolder: (String) -> Unit,
  onCreateNewDocument: (String) -> Unit,
  onUpdateUserName: (String) -> Unit,
  onRequestAllFilesAccess: () -> Unit,
  onRequestMediaAndAlertsPermissions: () -> Unit,
  onConnectGoogleDrive: () -> Unit,
  onSyncNewFilesToDrive: () -> Unit,
  onDisconnectGoogleDrive: () -> Unit,
  hasBottomBar: Boolean = true,
  modifier: Modifier = Modifier,
) {
  var showFabSheet by remember { mutableStateOf(false) }
  var showNewFolderDialog by remember { mutableStateOf(false) }
  var showNameDialog by remember { mutableStateOf(false) }

  val categories =
    remember {
      listOf(
        CategoryTileSpec(FileCategory.PHOTOS, Icons.Rounded.PhotoLibrary, NeonCyan, "Gallery & Shots"),
        CategoryTileSpec(FileCategory.VIDEOS, Icons.Rounded.VideoLibrary, NeonViolet, "HD Clips"),
        CategoryTileSpec(FileCategory.AUDIO, Icons.Rounded.MusicNote, WarmAmber, "MP3 & Playlists"),
        CategoryTileSpec(FileCategory.DOCUMENTS, Icons.Rounded.Description, NeonCyan, "Doc / Code / PDF"),
        CategoryTileSpec(FileCategory.DOWNLOADS, Icons.Rounded.Download, NeonGreen, "Offline Saved"),
        CategoryTileSpec(FileCategory.APKS, Icons.Rounded.Android, NeonGreen, "App Packages"),
        CategoryTileSpec(FileCategory.ARCHIVES, Icons.Rounded.FolderZip, WarmAmber, "ZIP / RAR Vault"),
      )
    }

  val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  Box(modifier = modifier.fillMaxSize()) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val gridColumns =
        when {
          maxWidth >= 840.dp -> 4
          maxWidth >= 600.dp -> 3
          else -> 2
        }
      val rows = categories.chunked(gridColumns)

      LazyColumn(
        modifier =
          Modifier.fillMaxSize()
            .statusBarsPadding()
            .widthIn(max = 980.dp)
            .align(Alignment.TopCenter),
        contentPadding =
          PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = navBottom + if (hasBottomBar) 196.dp else 96.dp,
          ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        // 1. Top Header Bar
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier =
                Modifier.weight(1f)
                  .clip(RoundedCornerShape(20.dp))
                  .clickable { showNameDialog = true }
                  .padding(end = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(50.dp)
                    .drawBehind {
                      drawCircle(
                        brush =
                          Brush.radialGradient(
                            colors =
                              listOf(
                                LiquidEmerald.copy(alpha = 0.34f),
                                IceCyan.copy(alpha = 0.22f),
                                Color.Transparent,
                              )
                          ),
                        radius = size.maxDimension * 0.72f,
                      )
                    }
                    .clip(RoundedCornerShape(15.dp))
                    .border(
                      width = 1.4.dp,
                      brush =
                        Brush.linearGradient(
                          listOf(FrostedPearl.copy(alpha = 0.85f), LiquidEmerald, IceCyan)
                        ),
                      shape = RoundedCornerShape(15.dp),
                    ),
                contentAlignment = Alignment.Center,
              ) {
                Image(
                  painter = painterResource(id = R.drawable.kronofiles_logo),
                  contentDescription = "KronoFiles Logo",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              }
              Column {
                Text(
                  text = if (userName.isBlank()) "KronoFiles Studio" else "Welcome back!",
                  style = MaterialTheme.typography.bodySmall,
                  color = IceCyan,
                )
                Text(
                  text = if (userName.isBlank()) "Welcome back!" else userName,
                  style = MaterialTheme.typography.headlineSmall,
                  color = TextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              GlassBubbleButton(
                icon = Icons.Rounded.Lock,
                contentDescription = "Open Safe Vault",
                onClick = onOpenVault,
                tint = FrostedPearl,
                accentStart = IceCyan,
                accentEnd = DeepSapphire,
                modifier = Modifier.testTag("home_vault_button"),
              )
              GlassBubbleButton(
                icon = Icons.Rounded.Notifications,
                contentDescription = "Open Notifications",
                onClick = onOpenNotifications,
                tint = FrostedPearl,
                accentStart = PrismaticViolet,
                accentEnd = WarmGold,
                badgeCount = unreadNotifications,
                modifier = Modifier.testTag("home_notifications_button"),
              )
            }
          }
        }

        // 2. Hero Storage Dashboard Card
        item {
          StorageHeroCard(
            stats = storageStats,
            permissionState = permissionState,
            driveSyncState = driveSyncState,
            onOpenChronos = onOpenChronos,
            onOpenCleanup = onOpenCleanup,
            onImportMedia = onImportMedia,
            onConnectGoogleDrive = onConnectGoogleDrive,
            onSyncNewFilesToDrive = onSyncNewFilesToDrive,
            onDisconnectGoogleDrive = onDisconnectGoogleDrive,
          )
        }

        // 2b. Real-Device Storage & Alert Permission Prompt Card (only if any key permission is missing)
        if (!permissionState.hasAllFilesAccess || !permissionState.hasMediaAccess || !permissionState.hasNotificationAccess) {
          item {
            PermissionOnboardingGlassCard(
              permissionState = permissionState,
              onRequestAllFilesAccess = onRequestAllFilesAccess,
              onRequestMediaAndAlertsPermissions = onRequestMediaAndAlertsPermissions,
            )
          }
        }

        // 3. Category Grid Header
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Storage Categories",
              style = MaterialTheme.typography.titleLarge,
              color = TextPrimary,
            )
            Text(
              text = "${FormatUtils.formatBytes(storageStats.kronoManagedBytes)} indexed",
              style = MaterialTheme.typography.labelMedium,
              color = NeonCyan,
            )
          }
        }

        // Responsive Category Grid Rows
        items(rows.size) { rowIdx ->
          val rowItems = rows[rowIdx]
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            rowItems.forEach { spec ->
              val summary = storageStats.categorySummaries[spec.category]
              CategoryGlassTile(
                spec = spec,
                count = summary?.itemCount ?: 0,
                sizeBytes = summary?.totalBytes ?: 0L,
                onClick = { onOpenCategory(spec.category) },
                modifier = Modifier.weight(1f),
              )
            }
            repeat(gridColumns - rowItems.size) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }

    // 4. 3D Liquid Glass Focal '+' FAB above floating navigation dock
    GlassBubbleButton(
      icon = Icons.Rounded.Add,
      contentDescription = "Create or Import File",
      onClick = { showFabSheet = true },
      size = 64.dp,
      iconSize = 32.dp,
      tint = FrostedPearl,
      accentStart = WarmGold,
      accentEnd = IceCyan,
      isActive = true,
      modifier =
        Modifier.align(Alignment.BottomEnd)
          .navigationBarsPadding()
          .padding(end = 20.dp, bottom = if (hasBottomBar) 106.dp else 20.dp)
          .testTag("home_fab_add"),
    )
  }

  if (showFabSheet) {
    ModalBottomSheet(
      onDismissRequest = { showFabSheet = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = Color.Transparent,
      dragHandle = null,
      contentColor = TextPrimary,
    ) {
      GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        cornerRadius = 28.dp,
        borderAccentStart = IceCyan,
        borderAccentEnd = MoltenAmber,
        fillAlphaTop = 0.24f,
        fillAlphaBottom = 0.14f,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(
            modifier =
              Modifier.align(Alignment.CenterHorizontally)
                .width(42.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(FrostedPearl.copy(alpha = 0.42f)),
          )
          Text(
            text = "Quick Offline Create & Import",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
          )
          FabActionRow(
            icon = Icons.Rounded.CreateNewFolder,
            title = "New Folder",
            subtitle =
              when {
                permissionState.hasAllFilesAccess -> "Create a folder inside Device Storage"
                permissionState.hasMediaFallbackAccess -> "Create a folder inside KronoStorage (App Docs/Files)"
                else -> "Create a folder inside App Sandbox"
              },
            accent = IceCyan,
            onClick = {
              showFabSheet = false
              showNewFolderDialog = true
            },
            tag = "fab_new_folder",
          )
          FabActionRow(
            icon = Icons.Rounded.PictureAsPdf,
            title = "Create PDF",
            subtitle = "Author text offline and compile to PDF",
            accent = NeonRed,
            onClick = {
              showFabSheet = false
              onCreateNewDocument("pdf")
            },
            tag = "fab_create_pdf",
          )
          FabActionRow(
            icon = Icons.Rounded.Code,
            title = "Create Doc / Code File",
            subtitle = "Choose format below (.txt, .md, .html, .css, .js, .docx, .xlsx)",
            accent = NeonViolet,
            onClick = {
              showFabSheet = false
              onCreateNewDocument("txt")
            },
            tag = "fab_create_doc",
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val formats = listOf("txt", "md", "html", "css", "js", "docx", "xlsx")
            items(formats.size) { idx ->
              val ext = formats[idx]
              GlassChip(
                label = ".$ext",
                selected = false,
                onClick = {
                  showFabSheet = false
                  onCreateNewDocument(ext)
                },
                accentColor = NeonCyan,
              )
            }
          }
          FabActionRow(
            icon = Icons.Rounded.UploadFile,
            title = "Import from Device",
            subtitle = "Pick Photos/Videos (Photo Picker) or Docs offline",
            accent = WarmAmber,
            onClick = {
              showFabSheet = false
              onImportDocument()
            },
            tag = "fab_import_device",
          )
          Spacer(modifier = Modifier.height(10.dp))
        }
      }
    }
  }

  if (showNewFolderDialog) {
    GlassTextInputDialog(
      title = "Create New Folder",
      label = "Folder Name",
      initialValue = "New Folder",
      confirmLabel = "Create",
      onConfirm = { name ->
        showNewFolderDialog = false
        onCreateFolder(name)
      },
      onDismiss = { showNewFolderDialog = false },
    )
  }

  if (showNameDialog) {
    GlassTextInputDialog(
      title = "Profile Greeting",
      label = "Display Name",
      initialValue = userName,
      confirmLabel = "Save",
      onConfirm = { name ->
        showNameDialog = false
        onUpdateUserName(name)
      },
      onDismiss = { showNameDialog = false },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PermissionOnboardingGlassCard(
  permissionState: StoragePermissionUiState,
  onRequestAllFilesAccess: () -> Unit,
  onRequestMediaAndAlertsPermissions: () -> Unit,
) {
  GlassCard(
    modifier = Modifier.fillMaxWidth().testTag("home_permission_card"),
    cornerRadius = 22.dp,
    borderAccentStart = WarmAmber.copy(alpha = 0.68f),
    borderAccentEnd = NeonCyan.copy(alpha = 0.58f),
    fillAlphaTop = 0.16f,
    fillAlphaBottom = 0.08f,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        LiquidGlassIconOrb(
          icon = Icons.Rounded.AdminPanelSettings,
          contentDescription = "Storage Permissions",
          accent = WarmGold,
          secondaryAccent = MoltenAmber,
          boxSize = 42.dp,
          iconSize = 22.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text =
              when {
                permissionState.hasAllFilesAccess && permissionState.hasMediaAccess -> "Enable Heads-Up Notifications"
                permissionState.hasAllFilesAccess -> "Enable Media & Heads-Up Alerts"
                else -> "Unlock Real Device Storage"
              },
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
          )
          Text(
            text =
              when {
                permissionState.hasAllFilesAccess && permissionState.hasMediaAccess ->
                  "Allow notification permission for low-storage, duplicate, and 30-min playback alerts."
                permissionState.hasAllFilesAccess ->
                  "Allow notifications & media library metadata for real heads-up alerts."
                permissionState.hasMediaFallbackAccess ->
                  "Media library active — grant All Files Access for folders, docs & cleanup on /storage/emulated/0/."
                else ->
                  "Grant All Files Access to manage /storage/emulated/0/ (currently showing App Sandbox)."
              },
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
          )
        }
      }
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (!permissionState.hasAllFilesAccess) {
          GlassChip(
            label = "Grant All Files Access",
            selected = true,
            leadingIcon = Icons.Rounded.AdminPanelSettings,
            onClick = onRequestAllFilesAccess,
            accentColor = NeonCyan,
            modifier = Modifier.testTag("grant_all_files_chip"),
          )
        }
        if (!permissionState.hasMediaAccess || !permissionState.hasNotificationAccess) {
          GlassChip(
            label =
              if (permissionState.hasMediaAccess && !permissionState.hasNotificationAccess) "Allow Notifications"
              else "Allow Media & Alerts",
            selected = permissionState.hasAllFilesAccess,
            leadingIcon = if (!permissionState.hasNotificationAccess) Icons.Rounded.NotificationsActive else Icons.Rounded.PermMedia,
            onClick = onRequestMediaAndAlertsPermissions,
            accentColor = WarmAmber,
            modifier = Modifier.testTag("grant_media_alerts_chip"),
          )
        }
      }
    }
  }
}

@Composable
private fun StorageHeroCard(
  stats: StorageStats,
  permissionState: StoragePermissionUiState,
  driveSyncState: DriveSyncUiState,
  onOpenChronos: () -> Unit,
  onOpenCleanup: () -> Unit,
  onImportMedia: () -> Unit,
  onConnectGoogleDrive: () -> Unit,
  onSyncNewFilesToDrive: () -> Unit,
  onDisconnectGoogleDrive: () -> Unit,
) {
  val animatedSweep by animateFloatAsState(
    targetValue = (stats.usedFraction * 360f).coerceIn(18f, 350f),
    animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
    label = "ring_sweep",
  )
  val usedPercent = (stats.usedFraction * 100).toInt()

  GlassCard(
    modifier = Modifier.fillMaxWidth(),
    cornerRadius = 26.dp,
    borderAccentStart = NeonCyan.copy(alpha = 0.6f),
    borderAccentEnd = WarmAmber.copy(alpha = 0.45f),
    fillAlphaTop = 0.16f,
    fillAlphaBottom = 0.06f,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        // Circular Ring Chart
        Box(
          modifier = Modifier.size(126.dp),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            drawArc(
              color = FrostedPearl.copy(alpha = 0.12f),
              startAngle = 0f,
              sweepAngle = 360f,
              useCenter = false,
              topLeft = topLeft,
              size = arcSize,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
              brush =
                Brush.sweepGradient(
                  listOf(IceCyan, DeepSapphire, PrismaticViolet, WarmGold, MoltenAmber, IceCyan)
                ),
              startAngle = -90f,
              sweepAngle = animatedSweep,
              useCenter = false,
              topLeft = topLeft,
              size = arcSize,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "$usedPercent%",
              style = MaterialTheme.typography.headlineSmall,
              color = TextPrimary,
            )
            Text(
              text = "USED",
              style = MaterialTheme.typography.labelSmall,
              color = NeonCyan,
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                when {
                  permissionState.hasAllFilesAccess -> "Device Storage"
                  permissionState.hasMediaFallbackAccess -> "Device Media"
                  else -> "App Sandbox"
                },
              style = MaterialTheme.typography.titleLarge,
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
          }
          StorageMetricLine("Total", FormatUtils.formatGbShort(stats.totalBytes), TextSecondary)
          StorageMetricLine("Used", FormatUtils.formatGbShort(stats.usedBytes), NeonViolet)
          StorageMetricLine("Free", FormatUtils.formatGbShort(stats.freeBytes), NeonCyan)
        }
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
          GlassChip(
            label = "Chronos Timeline",
            selected = true,
            leadingIcon = Icons.Rounded.Timeline,
            onClick = onOpenChronos,
            accentColor = NeonCyan,
            modifier = Modifier.testTag("hero_chronos_pill"),
          )
        }
        item {
          GlassChip(
            label = "Smart Cleanup",
            selected = false,
            leadingIcon = Icons.Rounded.CleaningServices,
            onClick = onOpenCleanup,
            accentColor = WarmAmber,
            modifier = Modifier.testTag("hero_cleanup_pill"),
          )
        }
        item {
          GlassChip(
            label = "Import Media",
            selected = false,
            leadingIcon = Icons.Rounded.PhotoLibrary,
            onClick = onImportMedia,
            accentColor = NeonViolet,
            modifier = Modifier.testTag("hero_import_media_pill"),
          )
        }
      }

      GoogleDriveSyncStrip(
        state = driveSyncState,
        onConnect = onConnectGoogleDrive,
        onSyncNow = onSyncNewFilesToDrive,
        onDisconnect = onDisconnectGoogleDrive,
      )
    }
  }
}

@Composable
private fun GoogleDriveSyncStrip(
  state: DriveSyncUiState,
  onConnect: () -> Unit,
  onSyncNow: () -> Unit,
  onDisconnect: () -> Unit,
) {
  val accent = if (state.isConnected) NeonGreen else NeonCyan
  GlassCard(
    modifier = Modifier.fillMaxWidth(),
    cornerRadius = 18.dp,
    borderAccentStart = accent.copy(alpha = 0.72f),
    borderAccentEnd = WarmAmber.copy(alpha = 0.55f),
    fillAlphaTop = 0.20f,
    fillAlphaBottom = 0.10f,
    elevationShadowAlpha = 0.35f,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier =
            Modifier.size(42.dp)
              .drawBehind {
                drawCircle(
                  brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.36f), Color.Transparent)),
                  radius = size.minDimension * 0.76f,
                )
              }
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  listOf(
                    FrostedPearl.copy(alpha = 0.28f),
                    accent.copy(alpha = 0.32f),
                    Color(0xFF0A2B36).copy(alpha = 0.56f),
                  ),
                  center = Offset(12f, 10f),
                )
              )
              .border(
                1.25.dp,
                Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.75f), accent)),
                CircleShape,
              ),
          contentAlignment = Alignment.Center,
        ) {
          if (state.isSyncing) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = IceCyan,
              strokeWidth = 2.dp,
            )
          } else {
            Icon(
              imageVector = if (state.isConnected) Icons.Rounded.CloudDone else Icons.Rounded.CloudUpload,
              contentDescription = "Google Drive Sync",
              tint = accent,
              modifier = Modifier.size(20.dp),
            )
          }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text =
              if (state.isConnected) state.accountEmail ?: "Google Drive Connected"
              else "Google Drive Backup",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text =
              when {
                state.isSyncing -> state.progressText ?: "Uploading new & modified files..."
                state.isConnected && state.lastSyncMs > 0L ->
                  "${state.syncedCount} synced • ${FormatUtils.formatRelativeTime(state.lastSyncMs)}"
                state.isConnected ->
                  "${state.syncedCount} synced • Ready for incremental sync"
                else -> "On-demand incremental sync for new files"
              },
            style = MaterialTheme.typography.labelSmall,
            color = if (state.isSyncing) IceCyan else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        if (!state.isConnected) {
          GlassChip(
            label = "Connect",
            selected = true,
            leadingIcon = Icons.Rounded.CloudUpload,
            onClick = onConnect,
            accentColor = IceCyan,
            modifier = Modifier.testTag("hero_drive_connect_button"),
          )
        }
      }

      if (state.isConnected) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          GlassChip(
            label = if (state.isSyncing) "Syncing..." else "Sync New Files",
            selected = true,
            leadingIcon = Icons.Rounded.CloudSync,
            onClick = { if (!state.isSyncing) onSyncNow() },
            accentColor = IceCyan,
            modifier = Modifier.testTag("hero_drive_sync_button"),
          )
          if (!state.isSyncing) {
            GlassBubbleButton(
              icon = Icons.Rounded.CloudOff,
              contentDescription = "Disconnect Google Drive",
              onClick = onDisconnect,
              size = 48.dp,
              iconSize = 20.dp,
              tint = WarmGold,
              accentStart = MoltenAmber,
              accentEnd = WarmGold,
              modifier = Modifier.testTag("hero_drive_disconnect_button"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StorageMetricLine(label: String, value: String, valueColor: Color) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    Text(
      text = value,
      style = MaterialTheme.typography.labelLarge,
      color = valueColor,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun CategoryGlassTile(
  spec: CategoryTileSpec,
  count: Int,
  sizeBytes: Long,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  GlassCard(
    modifier = modifier.testTag("category_${spec.category.name.lowercase()}"),
    cornerRadius = 20.dp,
    borderAccentStart = NeonCyan.copy(alpha = 0.65f),
    borderAccentEnd = WarmAmber.copy(alpha = 0.52f),
    onClick = onClick,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LiquidGlassIconOrb(
          icon = spec.icon,
          contentDescription = spec.category.label,
          accent = spec.accent,
          secondaryAccent = if (spec.accent == WarmAmber) WarmGold else ElectricBlue,
          boxSize = 44.dp,
          iconSize = 22.dp,
          cornerRadius = 14.dp,
        )
        Text(
          text = "$count",
          style = MaterialTheme.typography.labelMedium,
          color = spec.accent,
          fontWeight = FontWeight.SemiBold,
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = spec.category.label,
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = if (sizeBytes > 0) FormatUtils.formatBytes(sizeBytes) else spec.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun FabActionRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  accent: Color,
  onClick: () -> Unit,
  tag: String,
) {
  GlassCard(
    modifier = Modifier.fillMaxWidth().testTag(tag),
    cornerRadius = 18.dp,
    borderAccentStart = accent.copy(alpha = 0.72f),
    borderAccentEnd = FrostedPearl.copy(alpha = 0.25f),
    onClick = onClick,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      LiquidGlassIconOrb(
        icon = icon,
        contentDescription = title,
        accent = accent,
        secondaryAccent = if (accent == WarmAmber) WarmGold else ElectricBlue,
        boxSize = 44.dp,
        iconSize = 22.dp,
        cornerRadius = 14.dp,
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
      }
    }
  }
}
