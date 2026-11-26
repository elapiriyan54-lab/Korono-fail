package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FileKind
import com.example.model.FilesViewMode
import com.example.model.KronoFileItem
import com.example.model.SortBy
import com.example.ui.components.FileThumbnailOrIcon
import com.example.ui.components.FolderPickerDialog
import com.example.ui.components.GlassBubbleButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassConfirmDialog
import com.example.ui.components.GlassSegmentedToggle
import com.example.ui.components.GlassTextInputDialog
import com.example.ui.theme.DeepSapphire
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FrostedPearl
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.IceCyan
import com.example.ui.theme.MoltenAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PrismaticViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmGold
import com.example.util.FormatUtils
import com.example.viewmodel.ExplorerUiState

@Composable
fun AllFilesChronosScreen(
  state: ExplorerUiState,
  chronosGrouped: List<Pair<com.example.model.ChronosBucket, List<KronoFileItem>>>,
  isUsingDeviceStorage: Boolean,
  hasMediaFallbackAccess: Boolean = false,
  onNavigateBack: (() -> Unit)?,
  onNavigateFolder: (String) -> Unit,
  onChangeViewMode: (FilesViewMode) -> Unit,
  onSortSelect: (SortBy) -> Unit,
  onToggleSelectMode: () -> Unit,
  onToggleItemSelect: (String) -> Unit,
  onOpenFile: (KronoFileItem) -> Unit,
  onRenameFile: (KronoFileItem, String) -> Unit,
  onMoveFile: (KronoFileItem, String) -> Unit,
  onCopyFile: (KronoFileItem, String) -> Unit,
  onCompressItems: (List<KronoFileItem>, String) -> Unit,
  onExtractArchive: (KronoFileItem) -> Unit,
  onLockToVault: (KronoFileItem) -> Unit,
  onLockMultipleToVault: (List<KronoFileItem>) -> Unit,
  onShareFile: (KronoFileItem) -> Unit,
  onDeletePaths: (List<String>) -> Unit,
  hasBottomBar: Boolean = true,
  modifier: Modifier = Modifier,
) {
  var showSortMenu by remember { mutableStateOf(false) }
  var showArchiveActionDialog by remember { mutableStateOf(false) }
  var renameTarget by remember { mutableStateOf<KronoFileItem?>(null) }
  var moveTarget by remember { mutableStateOf<KronoFileItem?>(null) }
  var copyTarget by remember { mutableStateOf<KronoFileItem?>(null) }
  var deleteTargets by remember { mutableStateOf<List<KronoFileItem>?>(null) }

  val title = state.activeCategoryFilter?.label ?: "All Files"
  val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .statusBarsPadding()
          .widthIn(max = 980.dp)
          .align(Alignment.TopCenter),
    ) {
      // 1. Top Header & Actions
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (onNavigateBack != null || state.currentFolderRel.isNotBlank()) {
          GlassBubbleButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            onClick = {
              if (state.currentFolderRel.isNotBlank()) {
                val parent = state.currentFolderRel.substringBeforeLast('/', "")
                onNavigateFolder(if (parent == state.currentFolderRel) "" else parent)
              } else {
                onNavigateBack?.invoke()
              }
            },
            accentStart = IceCyan,
            accentEnd = DeepSapphire,
            modifier = Modifier.testTag("files_back_button"),
          )
        }
        Text(
          text = title,
          style = MaterialTheme.typography.headlineSmall,
          color = TextPrimary,
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Box {
          GlassBubbleButton(
            icon = Icons.AutoMirrored.Rounded.Sort,
            contentDescription = "Sort files",
            onClick = { showSortMenu = true },
            tint = IceCyan,
            accentStart = IceCyan,
            accentEnd = DeepSapphire,
            modifier = Modifier.testTag("files_sort_button"),
          )
          DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = GlassSurfaceDark,
            modifier =
              Modifier.border(
                1.4.dp,
                Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.72f), IceCyan.copy(alpha = 0.65f), WarmGold.copy(alpha = 0.5f))),
                RoundedCornerShape(18.dp),
              ),
          ) {
            SortBy.entries.forEach { sort ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = "${sort.label} ${if (state.sortBy == sort) (if (state.ascending) "↑" else "↓") else ""}",
                    color = if (state.sortBy == sort) IceCyan else TextPrimary,
                  )
                },
                onClick = {
                  showSortMenu = false
                  onSortSelect(sort)
                },
              )
            }
          }
        }

        GlassBubbleButton(
          icon = Icons.Rounded.Checklist,
          contentDescription = "Select mode",
          onClick = onToggleSelectMode,
          isActive = state.selectMode,
          tint = if (state.selectMode) IceCyan else FrostedPearl,
          accentStart = PrismaticViolet,
          accentEnd = ElectricBlue,
          modifier = Modifier.testTag("files_select_mode_button"),
        )

        GlassBubbleButton(
          icon = Icons.Rounded.FolderZip,
          contentDescription = "Compress to ZIP",
          onClick = { showArchiveActionDialog = true },
          tint = WarmGold,
          accentStart = MoltenAmber,
          accentEnd = WarmGold,
          modifier = Modifier.testTag("files_zip_action_button"),
        )
      }

      // View Switcher (Folder / List vs Chronos Timeline)
      GlassSegmentedToggle(
        options = listOf("Folder / List", "Chronos Timeline"),
        selectedIndex = if (state.viewMode == FilesViewMode.FOLDER) 0 else 1,
        onSelect = { idx ->
          onChangeViewMode(if (idx == 0) FilesViewMode.FOLDER else FilesViewMode.CHRONOS)
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("files_view_toggle"),
      )

      Spacer(modifier = Modifier.height(10.dp))

      // 2. Breadcrumb / Path Bar (Folder Mode)
      if (state.viewMode == FilesViewMode.FOLDER && state.activeCategoryFilter == null) {
        val segments =
          remember(state.currentFolderRel) {
            if (state.currentFolderRel.isBlank()) emptyList()
            else state.currentFolderRel.split('/').filter { it.isNotBlank() }
          }
        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          item {
            GlassChip(
              label =
                when {
                  hasMediaFallbackAccess -> "Device Media"
                  isUsingDeviceStorage -> "Device Storage"
                  else -> "App Sandbox"
                },
              selected = segments.isEmpty(),
              onClick = { onNavigateFolder("") },
              accentColor = NeonCyan,
            )
          }
          items(segments.size) { idx ->
            val pathSoFar = segments.take(idx + 1).joinToString("/")
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              GlassChip(
                label = segments[idx],
                selected = idx == segments.lastIndex,
                onClick = { onNavigateFolder(pathSoFar) },
                accentColor = NeonViolet,
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      // 3. Main Content: Folder List OR Chronos Timeline Stream
      if (state.viewMode == FilesViewMode.FOLDER) {
        if (state.items.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center,
          ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
              Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text("Folder is Empty", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                  "Use + on Home to create folders, PDFs, or import device media.",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary,
                )
              }
            }
          }
        } else {
          val extraBatchClearance = if (state.selectMode && state.selectedPaths.isNotEmpty()) 84.dp else 0.dp
          val baseBottom = if (hasBottomBar) 136.dp else 28.dp
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = navBottom + baseBottom + extraBatchClearance),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(state.items, key = { it.id }) { item ->
              FileGlassRow(
                item = item,
                selectMode = state.selectMode,
                isSelected = item.absolutePath in state.selectedPaths,
                onClick = {
                  if (state.selectMode) onToggleItemSelect(item.absolutePath)
                  else if (item.isDirectory) onNavigateFolder(item.relativePath)
                  else onOpenFile(item)
                },
                onRename = { renameTarget = item },
                onMove = { moveTarget = item },
                onCopy = { copyTarget = item },
                onCompress = { onCompressItems(listOf(item), "${item.name}_Archive.zip") },
                onExtract = { onExtractArchive(item) },
                onLockToVault = { onLockToVault(item) },
                onShare = { onShareFile(item) },
                onDelete = { deleteTargets = listOf(item) },
              )
            }
          }
        }
      } else {
        val extraBatchClearance = if (state.selectMode && state.selectedPaths.isNotEmpty()) 84.dp else 0.dp
        val baseBottom = if (hasBottomBar) 136.dp else 28.dp
        // Chronos Timeline View
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = navBottom + baseBottom + extraBatchClearance),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          chronosGrouped.forEach { (bucket, files) ->
            item(key = "header_${bucket.name}") {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 4.dp),
              ) {
                Box(
                  modifier =
                    Modifier.size(12.dp)
                      .clip(CircleShape)
                      .background(Brush.linearGradient(listOf(NeonCyan, NeonViolet)))
                )
                Text(
                  text = bucket.label,
                  style = MaterialTheme.typography.titleMedium,
                  color = NeonCyan,
                )
                Text(
                  text = if (files.size == 1) "1 item" else "${files.size} items",
                  style = MaterialTheme.typography.labelMedium,
                  color = TextMuted,
                )
              }
            }

            items(files, key = { "chronos_${it.id}" }) { item ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                // Glowing timeline rail
                Box(
                  modifier = Modifier.width(12.dp).height(76.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Box(
                    modifier =
                      Modifier.width(2.dp)
                        .fillMaxHeight()
                        .background(
                          Brush.verticalGradient(
                            listOf(NeonCyan.copy(alpha = 0.85f), WarmAmber.copy(alpha = 0.65f))
                          )
                        )
                  )
                  Box(
                    modifier =
                      Modifier.size(7.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                  )
                }

                FileGlassRow(
                  item = item,
                  selectMode = state.selectMode,
                  isSelected = item.absolutePath in state.selectedPaths,
                  onClick = {
                    if (state.selectMode) onToggleItemSelect(item.absolutePath)
                    else onOpenFile(item)
                  },
                  onRename = { renameTarget = item },
                  onMove = { moveTarget = item },
                  onCopy = { copyTarget = item },
                  onCompress = { onCompressItems(listOf(item), "${item.name}_Archive.zip") },
                  onExtract = { onExtractArchive(item) },
                  onLockToVault = { onLockToVault(item) },
                  onShare = { onShareFile(item) },
                  onDelete = { deleteTargets = listOf(item) },
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }
    }

    // Batch Action Bar in Select Mode
    if (state.selectMode && state.selectedPaths.isNotEmpty()) {
      val selectedItems =
        (state.items + state.allFilesForChronos)
          .distinctBy { it.absolutePath }
          .filter { it.absolutePath in state.selectedPaths }
      GlassCard(
        modifier =
          Modifier.align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = if (hasBottomBar) 108.dp else 20.dp)
            .widthIn(max = 560.dp)
            .fillMaxWidth(),
        cornerRadius = 22.dp,
        borderAccentStart = IceCyan,
        borderAccentEnd = WarmGold,
        fillAlphaTop = 0.28f,
        fillAlphaBottom = 0.16f,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${selectedItems.size} selected",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassBubbleButton(
              icon = Icons.Rounded.FolderZip,
              contentDescription = "Compress Selected",
              onClick = { onCompressItems(selectedItems, "Krono_Selection_${System.currentTimeMillis() % 1000}.zip") },
              tint = WarmAmber,
              size = 48.dp,
            )
            GlassBubbleButton(
              icon = Icons.Rounded.Lock,
              contentDescription = "Lock Selected to Vault",
              onClick = {
                val filesToLock = selectedItems.filter { !it.isDirectory }
                if (filesToLock.isNotEmpty()) {
                  onLockMultipleToVault(filesToLock)
                }
              },
              tint = NeonViolet,
              size = 48.dp,
            )
            GlassBubbleButton(
              icon = Icons.Rounded.Delete,
              contentDescription = "Delete Selected",
              onClick = { deleteTargets = selectedItems },
              tint = NeonRed,
              size = 48.dp,
            )
          }
        }
      }
    }
  }

  renameTarget?.let { target ->
    GlassTextInputDialog(
      title = "Rename File",
      label = "New Name",
      initialValue = target.name,
      confirmLabel = "Rename",
      onConfirm = { newName ->
        renameTarget = null
        onRenameFile(target, newName)
      },
      onDismiss = { renameTarget = null },
    )
  }

  moveTarget?.let { target ->
    FolderPickerDialog(
      title = "Move '${target.name}' To",
      folders = state.allFolders,
      isUsingDeviceStorage = isUsingDeviceStorage,
      hasMediaFallbackAccess = hasMediaFallbackAccess,
      onSelectFolder = { folderRel ->
        moveTarget = null
        onMoveFile(target, folderRel)
      },
      onDismiss = { moveTarget = null },
    )
  }

  copyTarget?.let { target ->
    FolderPickerDialog(
      title = "Copy '${target.name}' To",
      folders = state.allFolders,
      isUsingDeviceStorage = isUsingDeviceStorage,
      hasMediaFallbackAccess = hasMediaFallbackAccess,
      onSelectFolder = { folderRel ->
        copyTarget = null
        onCopyFile(target, folderRel)
      },
      onDismiss = { copyTarget = null },
    )
  }

  if (showArchiveActionDialog) {
    val selected =
      (state.items + state.allFilesForChronos)
        .distinctBy { it.absolutePath }
        .filter { it.absolutePath in state.selectedPaths }
        .ifEmpty { state.items.filter { !it.isDirectory }.take(5) }
    GlassTextInputDialog(
      title = "Compress ${selected.size} Item(s) to ZIP",
      label = "Archive Filename",
      initialValue = "Krono_Backup.zip",
      confirmLabel = "Compress",
      onConfirm = { zipName ->
        showArchiveActionDialog = false
        onCompressItems(selected, zipName)
      },
      onDismiss = { showArchiveActionDialog = false },
    )
  }

  deleteTargets?.let { targets ->
    GlassConfirmDialog(
      title = "Delete ${targets.size} Item(s)?",
      message = "This permanently removes ${targets.joinToString { it.name }} from offline storage.",
      confirmLabel = "Delete",
      isDestructive = true,
      onConfirm = {
        deleteTargets = null
        onDeletePaths(targets.map { it.absolutePath })
      },
      onDismiss = { deleteTargets = null },
    )
  }
}

@Composable
fun FileGlassRow(
  item: KronoFileItem,
  selectMode: Boolean,
  isSelected: Boolean,
  onClick: () -> Unit,
  onRename: () -> Unit,
  onMove: () -> Unit,
  onCopy: () -> Unit,
  onCompress: () -> Unit,
  onExtract: () -> Unit,
  onLockToVault: () -> Unit,
  onShare: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showMenu by remember { mutableStateOf(false) }

  GlassCard(
    modifier = modifier.fillMaxWidth(),
    cornerRadius = 18.dp,
    borderAccentStart =
      if (isSelected) NeonCyan else NeonCyan.copy(alpha = 0.52f),
    borderAccentEnd =
      if (isSelected) WarmAmber else WarmAmber.copy(alpha = 0.42f),
    onClick = onClick,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (selectMode) {
        Icon(
          imageVector = if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
          contentDescription = "Select",
          tint = if (isSelected) NeonCyan else TextSecondary,
          modifier = Modifier.size(24.dp),
        )
      }

      FileThumbnailOrIcon(item = item)

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = item.name,
          style = MaterialTheme.typography.titleSmall,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val meta =
          if (item.isDirectory) {
            val itemLabel = if (item.childCount == 1) "1 item" else "${item.childCount} items"
            "$itemLabel • ${FormatUtils.formatDateShort(item.lastModifiedMs)}"
          } else {
            "${FormatUtils.formatBytes(item.sizeBytes)} • ${FormatUtils.formatDateShort(item.lastModifiedMs)}"
          }
        Text(
          text = meta,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Box {
        GlassBubbleButton(
          icon = Icons.Rounded.MoreVert,
          contentDescription = "File actions for ${item.name}",
          onClick = { showMenu = true },
          size = 48.dp,
          iconSize = 20.dp,
          tint = TextSecondary,
          accentStart = FrostedPearl.copy(alpha = 0.5f),
          accentEnd = IceCyan.copy(alpha = 0.35f),
        )
        val isVirtualFolder = item.id.startsWith("virtual_")
        val canDirectModify = !isVirtualFolder && (item.contentUri == null || java.io.File(item.absolutePath).canWrite())
        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          shape = RoundedCornerShape(18.dp),
          containerColor = GlassSurfaceDark,
          modifier =
            Modifier.border(
              1.4.dp,
              Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.72f), IceCyan.copy(alpha = 0.65f), WarmGold.copy(alpha = 0.5f))),
              RoundedCornerShape(18.dp),
            ),
        ) {
          if (isVirtualFolder) {
            DropdownMenuItem(text = { Text("Open Folder", color = IceCyan) }, onClick = { showMenu = false; onClick() })
          } else {
            if (canDirectModify) {
              DropdownMenuItem(text = { Text("Rename", color = TextPrimary) }, onClick = { showMenu = false; onRename() })
              DropdownMenuItem(text = { Text("Move", color = TextPrimary) }, onClick = { showMenu = false; onMove() })
            }
            DropdownMenuItem(text = { Text("Copy", color = TextPrimary) }, onClick = { showMenu = false; onCopy() })
            DropdownMenuItem(text = { Text("Compress to ZIP", color = WarmAmber) }, onClick = { showMenu = false; onCompress() })
            if (item.kind == FileKind.ARCHIVE) {
              DropdownMenuItem(text = { Text("Extract Archive", color = NeonCyan) }, onClick = { showMenu = false; onExtract() })
            }
            if (!item.isDirectory) {
              DropdownMenuItem(text = { Text("Lock to Safe Vault", color = NeonViolet) }, onClick = { showMenu = false; onLockToVault() })
              DropdownMenuItem(text = { Text("Share", color = TextPrimary) }, onClick = { showMenu = false; onShare() })
            }
            DropdownMenuItem(
              text = { Text("Delete", color = NeonRed, fontWeight = FontWeight.SemiBold) },
              onClick = { showMenu = false; onDelete() },
            )
          }
        }
      }
    }
  }
}
