package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.AudioTrackItem
import com.example.model.RepeatModeState
import com.example.ui.components.GlassBubbleButton
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidGlassActionButton
import com.example.ui.components.LiquidGlassBadgePill
import com.example.ui.components.LiquidGlassFrostedButton
import com.example.ui.theme.DeepSapphire
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FrostedPearl
import com.example.ui.theme.GlassBorderAmber
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.IceCyan
import com.example.ui.theme.LiquidEmerald
import com.example.ui.theme.MoltenAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanSoft
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.PrismaticViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberDeep
import com.example.ui.theme.WarmGold
import com.example.util.FormatUtils
import com.example.viewmodel.AudioPlayerUiState
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp

@Composable
fun VinylAudioPlayerScreen(
  state: AudioPlayerUiState,
  favoritePaths: Set<String>,
  onBack: () -> Unit,
  onShareTrack: (AudioTrackItem) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onTogglePlayPause: () -> Unit,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onToggleShuffle: () -> Unit,
  onCycleRepeat: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onVinylScrubStart: () -> Unit,
  onVinylScrubDelta: (Float) -> Unit,
  onVinylScrubEnd: () -> Unit,
  onSelectQueueTrack: (Int) -> Unit,
  onTriggerInactivityTest: () -> Unit,
  onStayAwake: () -> Unit,
  onStopForInactivity: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val track = state.currentTrack ?: return
  val isFavorite = track.path in favoritePaths
  var showMoreMenu by remember { mutableStateOf(false) }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding(),
  ) {
    val isWideLandscape = maxWidth >= 680.dp && maxWidth > maxHeight

    if (isWideLandscape) {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(
          modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          PlayerTopBubbleBar(
            isFavorite = isFavorite,
            showMoreMenu = showMoreMenu,
            onDismissMenu = { showMoreMenu = false },
            onBack = onBack,
            onShare = { onShareTrack(track) },
            onFavorite = { onToggleFavorite(track.path) },
            onOpenMore = { showMoreMenu = true },
            onTriggerInactivityTest = onTriggerInactivityTest,
          )
          VinylDiscGlassFrame(
            rotationDegrees = state.vinylRotationDegrees,
            isScrubbing = state.isScrubbingVinyl,
            onScrubStart = onVinylScrubStart,
            onScrubDelta = onVinylScrubDelta,
            onScrubEnd = onVinylScrubEnd,
            modifier = Modifier.widthIn(max = 276.dp).aspectRatio(1f),
          )
          MetadataGlassCard(track = track, modifier = Modifier.fillMaxWidth())
          WaveformSeekbarCapsule(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            peaks = track.waveformPeaks,
            onSeekTo = onSeekTo,
          )
          PlayerBottomControlsRow(
            isPlaying = state.isPlaying,
            shuffleEnabled = state.shuffleEnabled,
            repeatMode = state.repeatMode,
            onToggleShuffle = onToggleShuffle,
            onPrevious = onPrevious,
            onTogglePlayPause = onTogglePlayPause,
            onNext = onNext,
            onCycleRepeat = onCycleRepeat,
          )
        }

        // Right Panel: Up Next / Song List
        GlassCard(
          modifier = Modifier.weight(1f).fillMaxHeight(),
          cornerRadius = 24.dp,
          borderAccentStart = GlassBorderCyan,
          borderAccentEnd = GlassBorderAmber,
          fillAlphaTop = 0.24f,
          fillAlphaBottom = 0.14f,
        ) {
          Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = NeonCyanSoft, modifier = Modifier.size(22.dp))
              Text("Up Next / Song List", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(14.dp))
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              itemsIndexed(state.queue, key = { idx, item -> "${item.path}_$idx" }) { idx, item ->
                val active = idx == state.currentIndex
                UpNextGlassRow(
                  track = item,
                  isNowPlaying = active,
                  elapsedMs = if (active) state.positionMs else item.durationMs,
                  onClick = { onSelectQueueTrack(idx) },
                )
              }
            }
          }
        }
      }
    } else {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        PlayerTopBubbleBar(
          isFavorite = isFavorite,
          showMoreMenu = showMoreMenu,
          onDismissMenu = { showMoreMenu = false },
          onBack = onBack,
          onShare = { onShareTrack(track) },
          onFavorite = { onToggleFavorite(track.path) },
          onOpenMore = { showMoreMenu = true },
          onTriggerInactivityTest = onTriggerInactivityTest,
        )

        VinylDiscGlassFrame(
          rotationDegrees = state.vinylRotationDegrees,
          isScrubbing = state.isScrubbingVinyl,
          onScrubStart = onVinylScrubStart,
          onScrubDelta = onVinylScrubDelta,
          onScrubEnd = onVinylScrubEnd,
          modifier = Modifier.widthIn(max = 330.dp).fillMaxWidth(0.92f).aspectRatio(1f),
        )

        MetadataGlassCard(track = track, modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth())

        WaveformSeekbarCapsule(
          positionMs = state.positionMs,
          durationMs = state.durationMs,
          peaks = track.waveformPeaks,
          onSeekTo = onSeekTo,
          modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
        )

        PlayerBottomControlsRow(
          isPlaying = state.isPlaying,
          shuffleEnabled = state.shuffleEnabled,
          repeatMode = state.repeatMode,
          onToggleShuffle = onToggleShuffle,
          onPrevious = onPrevious,
          onTogglePlayPause = onTogglePlayPause,
          onNext = onNext,
          onCycleRepeat = onCycleRepeat,
          modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
        )
      }
    }
  }

  if (state.inactivityAlertVisible) {
    Dialog(onDismissRequest = onStayAwake) {
      GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        borderAccentStart = NeonCyan,
        borderAccentEnd = WarmAmber,
        fillAlphaTop = 0.30f,
        fillAlphaBottom = 0.16f,
      ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("30-Min Inactivity Check", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
          Text(
            "No interaction detected for 30 minutes during audio playback. Auto-stopping in ${state.inactivityCountdownSec}s.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            LiquidGlassFrostedButton(
              onClick = { onStopForInactivity(track.title) },
              contentColor = WarmGold,
            ) {
              Text("Stop Now", color = WarmGold)
            }
            LiquidGlassActionButton(
              onClick = onStayAwake,
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

@Composable
private fun PlayerTopBubbleBar(
  isFavorite: Boolean,
  showMoreMenu: Boolean,
  onDismissMenu: () -> Unit,
  onBack: () -> Unit,
  onShare: () -> Unit,
  onFavorite: () -> Unit,
  onOpenMore: () -> Unit,
  onTriggerInactivityTest: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    GlassBubbleButton(
      icon = Icons.AutoMirrored.Rounded.ArrowBack,
      contentDescription = "Back",
      onClick = onBack,
      accentStart = IceCyan,
      accentEnd = DeepSapphire,
      modifier = Modifier.testTag("vinyl_back"),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      GlassBubbleButton(
        icon = Icons.Rounded.Share,
        contentDescription = "Share track",
        onClick = onShare,
        accentStart = IceCyan,
        accentEnd = ElectricBlue,
      )
      GlassBubbleButton(
        icon = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
        contentDescription = "Favorite track",
        onClick = onFavorite,
        tint = if (isFavorite) WarmGold else TextPrimary,
        accentStart = WarmGold,
        accentEnd = MoltenAmber,
        isActive = isFavorite,
      )
      Box {
        GlassBubbleButton(
          icon = Icons.Rounded.MoreVert,
          contentDescription = "More player options",
          onClick = onOpenMore,
          accentStart = PrismaticViolet,
          accentEnd = ElectricBlue,
        )
        DropdownMenu(
          expanded = showMoreMenu,
          onDismissRequest = onDismissMenu,
          shape = RoundedCornerShape(18.dp),
          containerColor = GlassSurfaceDark,
          modifier =
            Modifier.border(
              1.4.dp,
              Brush.linearGradient(listOf(FrostedPearl.copy(alpha = 0.72f), IceCyan.copy(alpha = 0.65f), WarmGold.copy(alpha = 0.5f))),
              RoundedCornerShape(18.dp),
            ),
        ) {
          DropdownMenuItem(
            text = { Text("Inactivity Guard: 30 min Active", color = NeonCyan) },
            onClick = { onDismissMenu() },
          )
          DropdownMenuItem(
            text = { Text("Preview 30-Min Inactivity Prompt", color = WarmAmber) },
            onClick = {
              onDismissMenu()
              onTriggerInactivityTest()
            },
          )
        }
      }
    }
  }
}

@Composable
private fun VinylDiscGlassFrame(
  rotationDegrees: Float,
  isScrubbing: Boolean,
  onScrubStart: () -> Unit,
  onScrubDelta: (Float) -> Unit,
  onScrubEnd: () -> Unit,
  modifier: Modifier = Modifier,
) {
  GlassCard(
    modifier = modifier.testTag("vinyl_disc_frame"),
    cornerRadius = 24.dp,
    borderAccentStart = GlassBorderCyan,
    borderAccentEnd = GlassBorderAmber,
    fillAlphaTop = 0.26f,
    fillAlphaBottom = 0.14f,
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.linearGradient(
              colors = listOf(Color(0x2E428B82), Color(0x16123236), Color(0x28523823)),
              start = Offset.Zero,
              end = Offset(800f, 800f),
            )
          )
          .padding(10.dp)
          .pointerInput(Unit) {
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false)
              val cx = size.width / 2f
              val cy = size.height / 2f
              var prevAngle =
                Math.toDegrees(atan2((down.position.y - cy).toDouble(), (down.position.x - cx).toDouble())).toFloat()
              onScrubStart()
              val pointerId = down.id
              try {
                while (true) {
                  val event = awaitPointerEvent()
                  val change =
                    event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull()
                  if (change == null || !change.pressed) {
                    break
                  }
                  if (change.position != change.previousPosition) {
                    change.consume()
                    val curAngle =
                      Math.toDegrees(
                        atan2((change.position.y - cy).toDouble(), (change.position.x - cx).toDouble())
                      ).toFloat()
                    var delta = curAngle - prevAngle
                    if (delta > 180f) delta -= 360f
                    if (delta < -180f) delta += 360f
                    prevAngle = curAngle
                    onScrubDelta(delta)
                  }
                }
              } finally {
                onScrubEnd()
              }
            }
          },
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRingRadius = size.minDimension * 0.43f

        // Multi-layered Soft Dual-Tone Neon Halo (Cyan top-left, Warm Amber bottom-right)
        val dualRingBrush =
          Brush.linearGradient(
            colors = listOf(NeonCyanSoft, NeonCyan, WarmAmber, WarmAmberDeep),
            start = Offset(size.width * 0.1f, size.height * 0.1f),
            end = Offset(size.width * 0.9f, size.height * 0.9f),
          )
        drawCircle(
          brush =
            Brush.linearGradient(
              colors = listOf(NeonCyan.copy(alpha = 0.22f), WarmAmberDeep.copy(alpha = 0.22f)),
              start = Offset.Zero,
              end = Offset(size.width, size.height),
            ),
          radius = outerRingRadius,
          center = center,
          style = Stroke(width = 30.dp.toPx()),
        )
        drawCircle(
          brush =
            Brush.linearGradient(
              colors = listOf(NeonCyan.copy(alpha = 0.42f), WarmAmber.copy(alpha = 0.42f)),
              start = Offset.Zero,
              end = Offset(size.width, size.height),
            ),
          radius = outerRingRadius,
          center = center,
          style = Stroke(width = 16.dp.toPx()),
        )
        drawCircle(
          brush = dualRingBrush,
          radius = outerRingRadius,
          center = center,
          style = Stroke(width = 5.dp.toPx()),
        )
        drawCircle(
          color = Color.White.copy(alpha = 0.65f),
          radius = outerRingRadius,
          center = center,
          style = Stroke(width = 1.6.dp.toPx()),
        )

        // Rotating Realistic Vinyl Record
        rotate(degrees = rotationDegrees, pivot = center) {
          val vinylRadius = outerRingRadius * 0.77f
          drawCircle(
            brush =
              Brush.radialGradient(
                colors = listOf(Color(0xFF1A1C22), Color(0xFF07080B), Color(0xFF14161D)),
                center = center,
                radius = vinylRadius,
              ),
            radius = vinylRadius,
            center = center,
          )
          // Anisotropic light-cone specular sheen (10-12 o'clock & 4-6 o'clock)
          drawArc(
            brush =
              Brush.radialGradient(
                colors = listOf(Color(0x88E8D8BE), Color(0x228894A0), Color.Transparent),
                center = center,
                radius = vinylRadius,
              ),
            startAngle = 195f,
            sweepAngle = 55f,
            useCenter = true,
            topLeft = Offset(center.x - vinylRadius, center.y - vinylRadius),
            size = Size(vinylRadius * 2f, vinylRadius * 2f),
          )
          drawArc(
            brush =
              Brush.radialGradient(
                colors = listOf(Color(0x88E8D8BE), Color(0x228894A0), Color.Transparent),
                center = center,
                radius = vinylRadius,
              ),
            startAngle = 15f,
            sweepAngle = 55f,
            useCenter = true,
            topLeft = Offset(center.x - vinylRadius, center.y - vinylRadius),
            size = Size(vinylRadius * 2f, vinylRadius * 2f),
          )
          // Fine concentric vinyl grooves
          for (i in 1..14) {
            drawCircle(
              color = Color.White.copy(alpha = if (i % 3 == 0) 0.14f else 0.06f),
              radius = vinylRadius * (0.38f + i * 0.042f),
              center = center,
              style = Stroke(width = 0.9.dp.toPx()),
            )
          }
          // Warm Bronze / Gold Center Label
          val labelRadius = vinylRadius * 0.34f
          drawCircle(
            brush =
              Brush.radialGradient(
                listOf(Color(0xFF3E301F), Color(0xFF1F170D), Color(0xFF110D08)),
                center = center,
                radius = labelRadius,
              ),
            radius = labelRadius,
            center = center,
          )
          drawCircle(
            color = WarmAmber.copy(alpha = 0.6f),
            radius = labelRadius,
            center = center,
            style = Stroke(width = 1.2.dp.toPx()),
          )
          // Musical notation & equalizer ticks around center spindle
          for (b in -2..2) {
            val xOff = b * 4.dp.toPx() - labelRadius * 0.56f
            val h = if (b % 2 == 0) 7.dp.toPx() else 11.dp.toPx()
            drawLine(
              color = Color(0xFFE6C288),
              start = Offset(center.x + xOff, center.y - h / 2f),
              end = Offset(center.x + xOff, center.y + h / 2f),
              strokeWidth = 1.6.dp.toPx(),
              cap = StrokeCap.Round,
            )
          }
          // Eighth-note accents
          drawCircle(color = Color(0xFFE6C288), radius = 2.6.dp.toPx(), center = Offset(center.x - 4.dp.toPx(), center.y - labelRadius * 0.48f))
          drawLine(
            color = Color(0xFFE6C288),
            start = Offset(center.x - 2.dp.toPx(), center.y - labelRadius * 0.48f),
            end = Offset(center.x - 2.dp.toPx(), center.y - labelRadius * 0.72f),
            strokeWidth = 1.6.dp.toPx(),
          )
          drawCircle(color = Color(0xFFE6C288), radius = 2.6.dp.toPx(), center = Offset(center.x + 12.dp.toPx(), center.y + labelRadius * 0.48f))
          drawLine(
            color = Color(0xFFE6C288),
            start = Offset(center.x + 14.dp.toPx(), center.y + labelRadius * 0.48f),
            end = Offset(center.x + 14.dp.toPx(), center.y + labelRadius * 0.24f),
            strokeWidth = 1.6.dp.toPx(),
          )
          // Metallic Center Spindle Ring
          drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFDCE4EE), Color(0xFF566473)), center = center, radius = labelRadius * 0.22f),
            radius = labelRadius * 0.22f,
            center = center,
          )
          drawCircle(color = ObsidianDeep, radius = labelRadius * 0.1f, center = center)
        }
      }

      if (isScrubbing) {
        LiquidGlassBadgePill(
          text = "SCRUBBING",
          accent = MoltenAmber,
          secondaryAccent = WarmGold,
          modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
        )
      }
    }
  }
}

@Composable
private fun MetadataGlassCard(track: AudioTrackItem, modifier: Modifier = Modifier) {
  GlassCard(
    modifier = modifier,
    cornerRadius = 18.dp,
    borderAccentStart = GlassBorderCyan,
    borderAccentEnd = GlassBorderAmber,
    fillAlphaTop = 0.24f,
    fillAlphaBottom = 0.14f,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Text(
        text = track.title,
        style = MaterialTheme.typography.headlineSmall,
        color = TextPrimary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "Artist: ${track.artist}",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "Album: ${track.album}",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun WaveformSeekbarCapsule(
  positionMs: Long,
  durationMs: Long,
  peaks: List<Float>,
  onSeekTo: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val progress = (positionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)

  GlassCard(
    modifier = modifier,
    cornerRadius = 50.dp,
    borderAccentStart = GlassBorderCyan,
    borderAccentEnd = GlassBorderAmber,
    fillAlphaTop = 0.24f,
    fillAlphaBottom = 0.13f,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = FormatUtils.formatDuration(positionMs),
        style = MaterialTheme.typography.labelMedium,
        color = TextPrimary,
      )

      Canvas(
        modifier =
          Modifier.weight(1f)
            .height(28.dp)
            .pointerInput(durationMs) {
              detectTapGestures { offset ->
                val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                onSeekTo((frac * durationMs).toLong())
              }
            }
            .pointerInput(durationMs) {
              detectDragGestures { change, _ ->
                change.consume()
                val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                onSeekTo((frac * durationMs).toLong())
              }
            },
      ) {
        val barCount = 46
        val slotWidth = size.width / barCount
        val barWidth = (slotWidth * 0.48f).coerceAtLeast(1.8.dp.toPx())
        val midY = size.height / 2f

        // Smooth horizontal baseline matching reference capsule
        drawLine(
          color = Color.White.copy(alpha = 0.24f),
          start = Offset(0f, midY),
          end = Offset(size.width, midY),
          strokeWidth = 1.5.dp.toPx(),
          cap = StrokeCap.Round,
        )
        drawLine(
          brush = Brush.horizontalGradient(listOf(NeonCyanSoft, NeonCyan)),
          start = Offset(0f, midY),
          end = Offset(size.width * progress, midY),
          strokeWidth = 2.dp.toPx(),
          cap = StrokeCap.Round,
        )

        // Centered glowing waveform envelope
        for (i in 0 until barCount) {
          val frac = i.toFloat() / (barCount - 1).toFloat()
          val distFromCenter = abs(frac - 0.5f) * 2.8f
          val envelope = exp(-distFromCenter * distFromCenter * 1.4f)
          val rawPeak = peaks.getOrElse((frac * peaks.lastIndex).toInt()) { 0.5f }
          val amp = (0.14f + rawPeak * 0.86f * envelope).coerceIn(0.08f, 1.0f)
          val barH = (size.height * amp).coerceAtLeast(3.dp.toPx())
          val played = frac <= progress
          val color =
            if (played) NeonCyanSoft.copy(alpha = 0.95f)
            else Color(0xFF7CA4C4).copy(alpha = 0.55f)
          drawRoundRect(
            color = color,
            topLeft = Offset(i * slotWidth + (slotWidth - barWidth) / 2f, midY - barH / 2f),
            size = Size(barWidth, barH),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
          )
        }

        // Luminous 3D Liquid Glass Playhead Droplet Orb
        val thumbX = (size.width * progress).coerceIn(5.dp.toPx(), size.width - 5.dp.toPx())
        drawCircle(
          brush = Brush.radialGradient(listOf(IceCyan.copy(alpha = 0.55f), Color.Transparent), center = Offset(thumbX, midY), radius = 11.dp.toPx()),
          radius = 11.dp.toPx(),
          center = Offset(thumbX, midY),
        )
        drawCircle(
          brush = Brush.radialGradient(listOf(FrostedPearl, IceCyan, DeepSapphire), center = Offset(thumbX - 1.5.dp.toPx(), midY - 1.5.dp.toPx()), radius = 6.dp.toPx()),
          radius = 4.8.dp.toPx(),
          center = Offset(thumbX, midY),
        )
      }

      Text(
        text = FormatUtils.formatDuration(durationMs),
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
      )
    }
  }
}

@Composable
private fun PlayerBottomControlsRow(
  isPlaying: Boolean,
  shuffleEnabled: Boolean,
  repeatMode: RepeatModeState,
  onToggleShuffle: () -> Unit,
  onPrevious: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onNext: () -> Unit,
  onCycleRepeat: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    GlassBubbleButton(
      icon = Icons.Rounded.Shuffle,
      contentDescription = "Shuffle",
      onClick = onToggleShuffle,
      size = 50.dp,
      tint = if (shuffleEnabled) IceCyan else TextPrimary,
      accentStart = IceCyan,
      accentEnd = DeepSapphire,
      isActive = shuffleEnabled,
    )
    GlassBubbleButton(
      icon = Icons.Rounded.SkipPrevious,
      contentDescription = "Previous",
      onClick = onPrevious,
      size = 56.dp,
      iconSize = 26.dp,
      accentStart = IceCyan,
      accentEnd = ElectricBlue,
    )
    GlassBubbleButton(
      icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
      contentDescription = if (isPlaying) "Pause" else "Play",
      onClick = onTogglePlayPause,
      size = 78.dp,
      iconSize = 36.dp,
      tint = if (isPlaying) WarmGold else FrostedPearl,
      accentStart = if (isPlaying) MoltenAmber else IceCyan,
      accentEnd = if (isPlaying) IceCyan else WarmGold,
      isActive = true,
      modifier = Modifier.testTag("vinyl_play_pause"),
    )
    GlassBubbleButton(
      icon = Icons.Rounded.SkipNext,
      contentDescription = "Next",
      onClick = onNext,
      size = 56.dp,
      iconSize = 26.dp,
      accentStart = IceCyan,
      accentEnd = ElectricBlue,
    )
    GlassBubbleButton(
      icon = if (repeatMode == RepeatModeState.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
      contentDescription = "Repeat",
      onClick = onCycleRepeat,
      size = 50.dp,
      tint = if (repeatMode != RepeatModeState.OFF) WarmGold else TextPrimary,
      accentStart = MoltenAmber,
      accentEnd = WarmGold,
      isActive = repeatMode != RepeatModeState.OFF,
    )
  }
}

@Composable
private fun UpNextGlassRow(
  track: AudioTrackItem,
  isNowPlaying: Boolean,
  elapsedMs: Long,
  onClick: () -> Unit,
) {
  GlassCard(
    modifier = Modifier.fillMaxWidth(),
    cornerRadius = 16.dp,
    borderAccentStart = if (isNowPlaying) IceCyan else Color.White.copy(alpha = 0.35f),
    borderAccentEnd = if (isNowPlaying) WarmGold else WarmAmber.copy(alpha = 0.25f),
    fillAlphaTop = if (isNowPlaying) 0.34f else 0.20f,
    fillAlphaBottom = if (isNowPlaying) 0.20f else 0.12f,
    onClick = onClick,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      MiniVinylBadge(size = 44.dp)
      Column(modifier = Modifier.weight(1f)) {
        Text(track.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.artist, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (isNowPlaying) {
          LiquidGlassBadgePill(
            text = "NOW PLAYING",
            accent = IceCyan,
            secondaryAccent = WarmGold,
            leadingIcon = Icons.Rounded.AutoAwesome,
          )
        }
        Text(
          text = FormatUtils.formatDuration(elapsedMs),
          style = MaterialTheme.typography.labelMedium,
          color = if (isNowPlaying) TextPrimary else TextSecondary,
        )
      }
    }
  }
}
