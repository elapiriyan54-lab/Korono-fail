package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackEntity
import com.example.model.AudioTrackItem
import com.example.ui.components.GlassBubbleButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassSegmentedToggle
import com.example.ui.components.LiquidGlassActionButton
import com.example.ui.components.LiquidGlassCheckboxOrb
import com.example.ui.components.LiquidGlassFrostedButton
import com.example.ui.components.LiquidGlassIconOrb
import com.example.ui.theme.DeepSapphire
import com.example.ui.theme.FrostedPearl
import com.example.ui.theme.IceCyan
import com.example.ui.theme.MoltenAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRed
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmGold
import com.example.util.FormatUtils
import kotlin.math.roundToInt

@Composable
fun AudioHubScreen(
  allTracks: List<AudioTrackItem>,
  playlists: List<PlaylistEntity>,
  allPlaylistTracks: List<PlaylistTrackEntity>,
  onBack: () -> Unit,
  onPlayTrack: (AudioTrackItem, Long) -> Unit,
  onCreatePlaylist: (String, List<AudioTrackItem>) -> Unit,
  onAddTrackToPlaylist: (Long, AudioTrackItem) -> Unit,
  onReorderPlaylistTracks: (List<PlaylistTrackEntity>, Int, Int) -> Unit,
  onCommitPlaylistOrder: (List<PlaylistTrackEntity>) -> Unit = {},
  onRemoveTrackFromPlaylist: (Long) -> Unit,
  onDeletePlaylist: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0=All Tracks, 1=Playlists
  var activePlaylistId by remember { mutableStateOf<Long?>(null) }
  var showCreatePlaylistDialog by remember { mutableStateOf(false) }
  var addToPlaylistTarget by remember { mutableStateOf<AudioTrackItem?>(null) }

  val activePlaylist = playlists.find { it.id == activePlaylistId }
  val activePlaylistTracks =
    remember(allPlaylistTracks, activePlaylistId) {
      allPlaylistTracks.filter { it.playlistId == activePlaylistId }.sortedBy { it.sortOrder }
    }

  val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .statusBarsPadding()
        .widthIn(max = 960.dp)
        .padding(top = 10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Top Header
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      GlassBubbleButton(
        icon = Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = "Back",
        onClick = {
          if (activePlaylistId != null) activePlaylistId = null else onBack()
        },
        accentStart = IceCyan,
        accentEnd = DeepSapphire,
        modifier = Modifier.testTag("audio_hub_back"),
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = activePlaylist?.name ?: "MP3 & Audio",
          style = MaterialTheme.typography.headlineSmall,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val trackCountText =
          if (activePlaylist != null) {
            val n = activePlaylistTracks.size
            if (n == 1) "1 track" else "$n tracks"
          } else {
            val t = allTracks.size
            val p = playlists.size
            val tStr = if (t == 1) "1 track" else "$t tracks"
            val pStr = if (p == 1) "1 playlist" else "$p playlists"
            "$tStr • $pStr"
          }
        Text(
          text =
            if (activePlaylist != null) "$trackCountText • Drag handle or arrows to reorder"
            else trackCountText,
          style = MaterialTheme.typography.bodySmall,
          color = WarmGold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      GlassChip(
        label = "+ New Playlist",
        selected = true,
        onClick = { showCreatePlaylistDialog = true },
        accentColor = MoltenAmber,
        modifier = Modifier.testTag("audio_new_playlist"),
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (activePlaylist == null) {
      GlassSegmentedToggle(
        options = listOf("All Tracks (${allTracks.size})", "Playlists (${playlists.size})"),
        selectedIndex = selectedTab,
        onSelect = { selectedTab = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("audio_hub_tabs"),
      )

      Spacer(modifier = Modifier.height(12.dp))

      if (selectedTab == 0) {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = navBottom + 36.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          itemsIndexed(allTracks, key = { _, t -> t.path }) { _, track ->
            GlassCard(
              modifier = Modifier.fillMaxWidth(),
              cornerRadius = 18.dp,
              borderAccentStart = NeonCyan.copy(alpha = 0.45f),
              borderAccentEnd = WarmAmber.copy(alpha = 0.4f),
              onClick = { onPlayTrack(track, -1L) },
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                MiniVinylBadge()
                Column(modifier = Modifier.weight(1f)) {
                  Text(track.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Text("${track.artist} • ${track.album}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(FormatUtils.formatDuration(track.durationMs), style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                if (playlists.isNotEmpty()) {
                  GlassBubbleButton(
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    onClick = { addToPlaylistTarget = track },
                    size = 48.dp,
                    iconSize = 20.dp,
                    tint = WarmGold,
                    accentStart = MoltenAmber,
                    accentEnd = WarmGold,
                  )
                }
              }
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = navBottom + 36.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          itemsIndexed(playlists, key = { _, p -> p.id }) { _, playlist ->
            val count = allPlaylistTracks.count { it.playlistId == playlist.id }
            GlassCard(
              modifier = Modifier.fillMaxWidth(),
              cornerRadius = 20.dp,
              borderAccentStart = WarmGold.copy(alpha = 0.65f),
              borderAccentEnd = IceCyan.copy(alpha = 0.45f),
              onClick = { activePlaylistId = playlist.id },
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                LiquidGlassIconOrb(
                  icon = Icons.Rounded.QueueMusic,
                  contentDescription = null,
                  accent = WarmGold,
                  secondaryAccent = MoltenAmber,
                  boxSize = 48.dp,
                  iconSize = 24.dp,
                  cornerRadius = 15.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(playlist.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                  Text(
                    if (count == 1) "1 track • Custom playback order" else "$count tracks • Custom playback order",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                  )
                }
                GlassBubbleButton(
                  icon = Icons.Rounded.Delete,
                  contentDescription = "Delete playlist",
                  onClick = { onDeletePlaylist(playlist.id) },
                  size = 48.dp,
                  iconSize = 20.dp,
                  tint = NeonRed,
                  accentStart = NeonRed,
                  accentEnd = MoltenAmber,
                )
              }
            }
          }
        }
      }
    } else {
      // Active Playlist View with Local-First Drag-and-Drop Reordering + Up/Down controls
      var localOrder by remember(activePlaylistTracks) { mutableStateOf(activePlaylistTracks) }
      var draggedIndex by remember { mutableStateOf<Int?>(null) }
      var dragOffsetY by remember { mutableFloatStateOf(0f) }
      val rowHeightPx = with(LocalDensity.current) { 78.dp.toPx() }
      val latestOrder by rememberUpdatedState(localOrder)

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = navBottom + 36.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        itemsIndexed(localOrder, key = { _, t -> t.id }) { index, pTrack ->
          val isDragging = draggedIndex == index
          val audioTrack =
            allTracks.find { it.path == pTrack.filePath }
              ?: AudioTrackItem(
                path = pTrack.filePath,
                title = pTrack.title,
                artist = pTrack.artist,
                album = pTrack.album,
                durationMs = pTrack.durationMs,
                sizeBytes = 0L,
                waveformPeaks = List(44) { 0.45f },
              )

          GlassCard(
            modifier =
              Modifier.fillMaxWidth()
                .zIndex(if (isDragging) 1f else 0f)
                .offset { IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0) },
            cornerRadius = 18.dp,
            borderAccentStart = if (isDragging) WarmAmber else NeonCyan.copy(alpha = 0.45f),
            borderAccentEnd = if (isDragging) NeonCyan else WarmAmber.copy(alpha = 0.35f),
            onClick = { onPlayTrack(audioTrack, activePlaylist.id) },
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              // Drag Handle (local state updated smoothly; Room persisted once on onDragEnd)
              Box(
                modifier =
                  Modifier.size(48.dp)
                    .pointerInput(activePlaylistId, pTrack.id) {
                      detectDragGesturesAfterLongPress(
                        onDragStart = {
                          draggedIndex = latestOrder.indexOfFirst { it.id == pTrack.id }.takeIf { it >= 0 }
                          dragOffsetY = 0f
                        },
                        onDragEnd = {
                          onCommitPlaylistOrder(latestOrder)
                          draggedIndex = null
                          dragOffsetY = 0f
                        },
                        onDragCancel = {
                          localOrder = activePlaylistTracks
                          draggedIndex = null
                          dragOffsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                          change.consume()
                          dragOffsetY += dragAmount.y
                          val cur = draggedIndex ?: return@detectDragGesturesAfterLongPress
                          val shift = (dragOffsetY / rowHeightPx).toInt()
                          val target = (cur + shift).coerceIn(0, latestOrder.lastIndex)
                          if (target != cur) {
                            val mutable = latestOrder.toMutableList()
                            val moved = mutable.removeAt(cur)
                            mutable.add(target, moved)
                            localOrder = mutable
                            draggedIndex = target
                            dragOffsetY -= (target - cur) * rowHeightPx
                          }
                        },
                      )
                    },
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.DragHandle,
                  contentDescription = "Drag to reorder track ${index + 1}",
                  tint = if (isDragging) WarmAmber else TextSecondary,
                  modifier = Modifier.size(24.dp),
                )
              }

              Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = WarmAmber,
              )

              MiniVinylBadge(size = 38.dp)

              Column(modifier = Modifier.weight(1f)) {
                Text(pTrack.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${pTrack.artist} • ${FormatUtils.formatDuration(pTrack.durationMs)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }

              GlassBubbleButton(
                icon = Icons.Rounded.ArrowUpward,
                contentDescription = "Move Up",
                onClick = { if (index > 0) onReorderPlaylistTracks(localOrder, index, (index - 1).coerceAtLeast(0)) },
                size = 48.dp,
                iconSize = 18.dp,
                tint = if (index > 0) IceCyan else TextMuted,
                accentStart = if (index > 0) IceCyan else TextMuted.copy(alpha = 0.4f),
                accentEnd = DeepSapphire,
              )
              GlassBubbleButton(
                icon = Icons.Rounded.ArrowDownward,
                contentDescription = "Move Down",
                onClick = { if (index < localOrder.lastIndex) onReorderPlaylistTracks(localOrder, index, (index + 1).coerceAtMost(localOrder.lastIndex)) },
                size = 48.dp,
                iconSize = 18.dp,
                tint = if (index < localOrder.lastIndex) IceCyan else TextMuted,
                accentStart = if (index < localOrder.lastIndex) IceCyan else TextMuted.copy(alpha = 0.4f),
                accentEnd = DeepSapphire,
              )
              GlassBubbleButton(
                icon = Icons.Rounded.Delete,
                contentDescription = "Remove from playlist",
                onClick = { onRemoveTrackFromPlaylist(pTrack.id) },
                size = 48.dp,
                iconSize = 18.dp,
                tint = NeonRed,
                accentStart = NeonRed,
                accentEnd = MoltenAmber,
              )
            }
          }
        }
      }
    }
  }

  if (showCreatePlaylistDialog) {
    CreatePlaylistDialog(
      allTracks = allTracks,
      onCreate = { name, selected ->
        showCreatePlaylistDialog = false
        onCreatePlaylist(name, selected)
      },
      onDismiss = { showCreatePlaylistDialog = false },
    )
  }

  addToPlaylistTarget?.let { track ->
    Dialog(onDismissRequest = { addToPlaylistTarget = null }) {
      GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, fillAlphaTop = 0.22f, fillAlphaBottom = 0.1f) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Add '${track.title}' to Playlist", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
          playlists.forEach { p ->
            GlassCard(
              modifier = Modifier.fillMaxWidth(),
              cornerRadius = 15.dp,
              borderAccentStart = WarmGold.copy(alpha = 0.55f),
              borderAccentEnd = IceCyan.copy(alpha = 0.4f),
              fillAlphaTop = 0.16f,
              fillAlphaBottom = 0.08f,
              onClick = {
                addToPlaylistTarget = null
                onAddTrackToPlaylist(p.id, track)
              },
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(p.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Icon(Icons.Rounded.Add, contentDescription = null, tint = WarmGold)
              }
            }
          }
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            LiquidGlassFrostedButton(onClick = { addToPlaylistTarget = null }) {
              Text("Cancel", color = TextPrimary)
            }
          }
        }
      }
    }
  }
}

@Composable
fun MiniVinylBadge(size: androidx.compose.ui.unit.Dp = 44.dp) {
  Box(
    modifier = Modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(this.size.width / 2f, this.size.height / 2f)
      val outerR = this.size.minDimension * 0.44f
      // Floating shadow underneath vinyl orb
      drawCircle(
        color = Color.Black.copy(alpha = 0.65f),
        radius = outerR * 1.05f,
        center = Offset(center.x, center.y + 2.5.dp.toPx()),
      )
      drawCircle(
        brush =
          Brush.linearGradient(
            listOf(IceCyan.copy(alpha = 0.48f), WarmGold.copy(alpha = 0.48f))
          ),
        radius = outerR,
        center = center,
        style = Stroke(width = 4.dp.toPx()),
      )
      drawCircle(
        brush = Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.72f), IceCyan, WarmGold)),
        radius = outerR,
        center = center,
        style = Stroke(width = 1.8.dp.toPx()),
      )
      drawCircle(color = Color(0xFF102430), radius = outerR * 0.78f, center = center)
      drawCircle(color = Color(0xFF42331E), radius = outerR * 0.34f, center = center)
      drawCircle(color = WarmGold, radius = outerR * 0.14f, center = center)
      // Specular glass crescent glint across top-left
      drawArc(
        color = FrostedPearl.copy(alpha = 0.48f),
        startAngle = 200f,
        sweepAngle = 80f,
        useCenter = false,
        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
        topLeft = Offset(center.x - outerR * 0.88f, center.y - outerR * 0.88f),
        size = Size(outerR * 1.76f, outerR * 1.76f),
      )
    }
  }
}

@Composable
private fun CreatePlaylistDialog(
  allTracks: List<AudioTrackItem>,
  onCreate: (String, List<AudioTrackItem>) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var selectedPaths by remember { mutableStateOf(allTracks.take(2).map { it.path }.toSet()) }

  Dialog(onDismissRequest = onDismiss) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, fillAlphaTop = 0.22f, fillAlphaBottom = 0.1f) {
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("New Custom Playlist", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Playlist Name") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = WarmAmber,
              unfocusedBorderColor = IceCyan.copy(alpha = 0.45f),
              focusedContainerColor = WarmGold.copy(alpha = 0.10f),
              unfocusedContainerColor = FrostedPearl.copy(alpha = 0.06f),
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedLabelColor = WarmGold,
              unfocusedLabelColor = TextSecondary,
              cursorColor = WarmGold,
            ),
          modifier = Modifier.fillMaxWidth(),
        )
        Text("Select Tracks", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          itemsIndexed(allTracks, key = { _, t -> t.path }) { _, track ->
            val checked = track.path in selectedPaths
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable {
                    selectedPaths = if (checked) selectedPaths - track.path else selectedPaths + track.path
                  }
                  .padding(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              LiquidGlassCheckboxOrb(
                checked = checked,
                onCheckedChange = {
                  selectedPaths = if (checked) selectedPaths - track.path else selectedPaths + track.path
                },
                accent = WarmGold,
              )
              Text(track.title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          LiquidGlassFrostedButton(onClick = onDismiss) {
            Text("Cancel", color = TextPrimary)
          }
          LiquidGlassActionButton(
            onClick = {
              val chosen = allTracks.filter { it.path in selectedPaths }
              onCreate(name.ifBlank { "My Playlist" }, chosen)
            },
            accentStart = MoltenAmber,
            accentEnd = WarmGold,
          ) {
            Text("Create", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
