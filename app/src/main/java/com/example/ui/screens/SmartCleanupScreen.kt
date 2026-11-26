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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CleanupSizeFilter
import com.example.ui.components.FileThumbnailOrIcon
import com.example.ui.components.GlassBubbleButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassConfirmDialog
import com.example.ui.components.GlassSegmentedToggle
import com.example.ui.components.LiquidGlassActionButton
import com.example.ui.components.LiquidGlassBadgePill
import com.example.ui.components.LiquidGlassCheckboxOrb
import com.example.ui.components.LiquidGlassFrostedButton
import com.example.ui.components.LiquidGlassIconOrb
import com.example.ui.theme.FrostedPearl
import com.example.ui.theme.IceCyan
import com.example.ui.theme.LiquidEmerald
import com.example.ui.theme.MoltenAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmGold
import com.example.util.FormatUtils
import com.example.viewmodel.CleanupUiState

@Composable
fun SmartCleanupScreen(
  state: CleanupUiState,
  initialTabIndex: Int = 0,
  onBack: () -> Unit,
  onRescan: () -> Unit,
  onSelectSizeFilter: (CleanupSizeFilter) -> Unit,
  onToggleSelectPath: (String) -> Unit,
  onKeepCopy: (String, String) -> Unit,
  onDeselectAll: () -> Unit,
  onConfirmDeleteSelected: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTab by remember(initialTabIndex) { mutableIntStateOf(initialTabIndex.coerceIn(0, 1)) }
  var showConfirmDelete by remember { mutableStateOf(false) }

  val duplicateReclaimable =
    remember(state.duplicateGroups) {
      state.duplicateGroups.sumOf { it.fileSize * (it.files.size - 1) }
    }
  val largeFilesTotal = remember(state.largeFiles) { state.largeFiles.sumOf { it.sizeBytes } }

  val allKnownFiles =
    remember(state.duplicateGroups, state.largeFiles) {
      (state.duplicateGroups.flatMap { it.files } + state.largeFiles).associateBy { it.absolutePath }
    }
  val selectedBytes =
    remember(state.selectedForDeletion, allKnownFiles) {
      state.selectedForDeletion.sumOf { path -> allKnownFiles[path]?.sizeBytes ?: 0L }
    }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .statusBarsPadding()
          .widthIn(max = 960.dp)
          .align(Alignment.TopCenter)
          .padding(top = 10.dp),
    ) {
      // 1. Top Header
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        GlassBubbleButton(
          icon = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Back",
          onClick = onBack,
          modifier = Modifier.testTag("cleanup_back"),
        )
        Text(
          text = "Smart Cleanup",
          style = MaterialTheme.typography.headlineSmall,
          color = TextPrimary,
          modifier = Modifier.weight(1f),
        )
        GlassBubbleButton(
          icon = Icons.Rounded.Refresh,
          contentDescription = "Rescan Storage",
          onClick = onRescan,
          tint = NeonCyan,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Summary Banner (Strict Manual-Only Rule)
      GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        cornerRadius = 22.dp,
        borderAccentStart = WarmAmber.copy(alpha = 0.65f),
        borderAccentEnd = NeonCyan.copy(alpha = 0.5f),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          LiquidGlassIconOrb(
            icon = Icons.Rounded.CleaningServices,
            contentDescription = null,
            accent = WarmGold,
            secondaryAccent = MoltenAmber,
            boxSize = 48.dp,
            iconSize = 24.dp,
            cornerRadius = 15.dp,
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "${FormatUtils.formatBytes(duplicateReclaimable + largeFilesTotal)} Scan Summary",
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
            )
            Text(
              text = "${state.duplicateGroups.size} duplicate group(s) • ${state.largeFiles.size} large file(s)",
              style = MaterialTheme.typography.bodySmall,
              color = NeonCyan,
            )
            Text(
              text = "No automatic deletion — files are removed only after your manual selection.",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 2. Section Tab Switcher
      GlassSegmentedToggle(
        options = listOf("Duplicate Files (${state.duplicateGroups.size})", "Large Files (${state.largeFiles.size})"),
        selectedIndex = selectedTab,
        onSelect = { selectedTab = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("cleanup_tabs"),
      )

      Spacer(modifier = Modifier.height(10.dp))

      if (selectedTab == 1) {
        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(CleanupSizeFilter.entries) { filter ->
            GlassChip(
              label = filter.label,
              selected = state.sizeFilter == filter,
              onClick = { onSelectSizeFilter(filter) },
              accentColor = WarmAmber,
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      if (state.isScanning) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = NeonCyan)
        }
      } else if (selectedTab == 0) {
        if (state.duplicateGroups.isEmpty()) {
          EmptyCleanupState("No byte-identical duplicates found")
        } else {
          val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = navBottom + 132.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(state.duplicateGroups, key = { it.hash }) { group ->
              val keepPath = state.keptOverridePaths[group.hash] ?: group.keepFilePath
              GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                borderAccentStart = WarmAmber.copy(alpha = 0.5f),
                borderAccentEnd = NeonViolet.copy(alpha = 0.35f),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "${group.files.size} Identical Copies • ${FormatUtils.formatBytes(group.fileSize)} each",
                      style = MaterialTheme.typography.titleSmall,
                      color = WarmAmber,
                    )
                    Text(
                      text = "SHA-256",
                      style = MaterialTheme.typography.labelSmall,
                      color = TextMuted,
                    )
                  }

                  group.files.forEach { file ->
                    val isKeep = file.absolutePath == keepPath
                    val isChecked = file.absolutePath in state.selectedForDeletion
                    GlassCard(
                      modifier = Modifier.fillMaxWidth(),
                      cornerRadius = 15.dp,
                      borderAccentStart = if (isKeep) LiquidEmerald.copy(alpha = 0.72f) else IceCyan.copy(alpha = 0.42f),
                      borderAccentEnd = if (isKeep) IceCyan.copy(alpha = 0.45f) else FrostedPearl.copy(alpha = 0.24f),
                      fillAlphaTop = 0.14f,
                      fillAlphaBottom = 0.06f,
                      elevationShadowAlpha = 0.28f,
                      onClick = { if (!isKeep) onToggleSelectPath(file.absolutePath) },
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                      ) {
                        FileThumbnailOrIcon(item = file, boxSize = 40.dp)
                        Column(modifier = Modifier.weight(1f)) {
                          Text(file.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                          Text(file.relativePath, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (isKeep) {
                          LiquidGlassBadgePill(
                            text = "Keep",
                            accent = LiquidEmerald,
                            secondaryAccent = IceCyan,
                            leadingIcon = Icons.Rounded.CheckCircle,
                          )
                        } else {
                          LiquidGlassFrostedButton(
                            onClick = { onKeepCopy(group.hash, file.absolutePath) },
                            contentColor = IceCyan,
                          ) {
                            Text("Keep", color = TextPrimary, style = MaterialTheme.typography.labelSmall)
                          }
                          LiquidGlassCheckboxOrb(
                            checked = isChecked,
                            onCheckedChange = { onToggleSelectPath(file.absolutePath) },
                            accent = IceCyan,
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      } else {
        if (state.largeFiles.isEmpty()) {
          EmptyCleanupState("No files match this size threshold")
        } else {
          val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = navBottom + 132.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(state.largeFiles, key = { it.id }) { file ->
              val checked = file.absolutePath in state.selectedForDeletion
              GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                borderAccentStart = if (checked) NeonCyan else IceCyan.copy(alpha = 0.48f),
                borderAccentEnd = FrostedPearl.copy(alpha = 0.35f),
                onClick = { onToggleSelectPath(file.absolutePath) },
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  FileThumbnailOrIcon(item = file)
                  Column(modifier = Modifier.weight(1f)) {
                    Text(file.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                      "${FormatUtils.formatBytes(file.sizeBytes)} • ${file.relativePath}",
                      style = MaterialTheme.typography.bodySmall,
                      color = WarmAmber,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  LiquidGlassCheckboxOrb(
                    checked = checked,
                    onCheckedChange = { onToggleSelectPath(file.absolutePath) },
                    accent = IceCyan,
                  )
                }
              }
            }
          }
        }
      }
    }

    // 4. Bottom Floating Glass Action Bar
    GlassCard(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .widthIn(max = 640.dp)
          .fillMaxWidth(),
      cornerRadius = 24.dp,
      borderAccentStart = if (state.selectedForDeletion.isNotEmpty()) NeonRed else NeonCyan.copy(alpha = 0.4f),
      borderAccentEnd = WarmAmber.copy(alpha = 0.5f),
      fillAlphaTop = 0.24f,
      fillAlphaBottom = 0.12f,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${state.selectedForDeletion.size} selected",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
          )
          Text(
            text = "${FormatUtils.formatBytes(selectedBytes)} to free",
            style = MaterialTheme.typography.labelMedium,
            color = NeonCyan,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          if (state.selectedForDeletion.isNotEmpty()) {
            LiquidGlassFrostedButton(onClick = onDeselectAll) {
              Text("Deselect All", color = TextPrimary)
            }
          }
          LiquidGlassActionButton(
            onClick = { showConfirmDelete = true },
            enabled = state.selectedForDeletion.isNotEmpty(),
            accentStart = NeonRed,
            accentEnd = MoltenAmber,
            modifier = Modifier.testTag("cleanup_delete_selected"),
          ) {
            Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("Delete Selected", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }

  if (showConfirmDelete) {
    GlassConfirmDialog(
      title = "Confirm Manual Deletion",
      message = "Permanently delete ${state.selectedForDeletion.size} selected file(s) and free ${FormatUtils.formatBytes(selectedBytes)}?",
      confirmLabel = "Delete Files",
      isDestructive = true,
      onConfirm = {
        showConfirmDelete = false
        onConfirmDeleteSelected()
      },
      onDismiss = { showConfirmDelete = false },
    )
  }
}

@Composable
private fun EmptyCleanupState(message: String) {
  Box(modifier = Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(40.dp))
        Text(message, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
      }
    }
  }
}
