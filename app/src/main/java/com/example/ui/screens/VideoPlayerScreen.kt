package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.components.GlassBubbleButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.LiquidGlassActionButton
import com.example.ui.components.LiquidGlassFrostedButton
import com.example.ui.components.LiquidGlassIconOrb
import com.example.ui.components.LiquidGlassScrubBar
import com.example.ui.theme.DeepSapphire
import com.example.ui.theme.FrostedPearl
import com.example.ui.theme.IceCyan
import com.example.ui.theme.LiquidEmerald
import com.example.ui.theme.MoltenAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmGold
import com.example.util.FormatUtils
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
  videoPath: String,
  onClose: () -> Unit,
  onLogInactivityAlert: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val videoFile = remember(videoPath) { File(videoPath) }
  val videoUri =
    remember(videoPath) {
      if (videoPath.startsWith("content://")) Uri.parse(videoPath) else Uri.fromFile(videoFile)
    }
  val displayTitle =
    remember(videoPath) {
      if (videoPath.startsWith("content://")) {
        runCatching {
          context.contentResolver.query(videoUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
              val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
              if (idx >= 0) c.getString(idx) else null
            } else null
          }
        }.getOrNull() ?: "Device Video"
      } else {
        videoFile.name
      }
    }
  val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

  val exoPlayer = remember(videoPath) {
    val renderersFactory =
      DefaultRenderersFactory(context)
        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        .setEnableDecoderFallback(true)
    val extractorsFactory =
      DefaultExtractorsFactory()
        .setConstantBitrateSeekingEnabled(true)
        .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES)
    ExoPlayer.Builder(context, renderersFactory)
      .setMediaSourceFactory(DefaultMediaSourceFactory(context, extractorsFactory))
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(C.USAGE_MEDIA)
          .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
          .build(),
        /* handleAudioFocus = */ true,
      )
      .setHandleAudioBecomingNoisy(true)
      .build()
      .apply {
        setMediaItem(MediaItem.fromUri(videoUri))
        prepare()
        playWhenReady = true
      }
  }

  var isPlaying by remember { mutableStateOf(true) }
  var playbackError by remember(videoPath) { mutableStateOf<String?>(null) }
  var positionMs by remember { mutableLongStateOf(0L) }
  var durationMs by remember { mutableLongStateOf(1L) }
  var controlsVisible by remember { mutableStateOf(true) }
  var controlsLocked by remember { mutableStateOf(false) }
  var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
  var isLandscapeForced by remember { mutableStateOf(false) }
  var showTrackDialog by remember { mutableStateOf(false) }

  // Gesture Overlay States
  val maxVol = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
  var volumeFrac by remember {
    mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol)
  }
  var brightnessFrac by remember {
    val cur = activity?.window?.attributes?.screenBrightness ?: -1f
    val initial =
      if (cur in 0f..1f) cur
      else {
        val sys =
          runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 165)
          }.getOrDefault(165)
        (sys.coerceIn(13, 255) / 255f)
      }
    mutableFloatStateOf(initial)
  }
  var activeGestureHud by remember { mutableStateOf<String?>(null) } // "VOLUME" or "BRIGHTNESS"
  var gestureTick by remember { mutableIntStateOf(0) }
  var isDraggingGesture by remember { mutableStateOf(false) }

  // 30-Min No-Interaction Guard
  var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
  var showInactivityDialog by remember { mutableStateOf(false) }
  var inactivityCountdown by remember { mutableIntStateOf(30) }

  fun recordInteraction() {
    lastInteractionMs = System.currentTimeMillis()
    if (showInactivityDialog) {
      showInactivityDialog = false
      inactivityCountdown = 30
    }
    activity?.window?.attributes =
      activity?.window?.attributes?.apply {
        if (screenBrightness in 0f..0.08f) {
          screenBrightness = brightnessFrac
        }
      }
  }

  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || isLandscapeForced

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(exoPlayer, lifecycleOwner) {
    val listener =
      object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
          isPlaying = playing
          if (playing) playbackError = null
        }

        override fun onPlayerError(error: PlaybackException) {
          playbackError =
            error.localizedMessage?.takeIf { it.isNotBlank() }
              ?: "Codec or container unsupported (${error.errorCodeName})."
        }
      }
    val lifecycleObserver =
      LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_PAUSE && exoPlayer.isPlaying) {
          exoPlayer.pause()
        }
      }
    exoPlayer.addListener(listener)
    lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
      exoPlayer.removeListener(listener)
      exoPlayer.release()
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
      activity?.window?.attributes =
        activity?.window?.attributes?.apply {
          screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }
  }

  // 250ms smooth playback timeline poller
  LaunchedEffect(exoPlayer) {
    while (true) {
      delay(250L)
      positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
      durationMs = exoPlayer.duration.takeIf { it > 0 } ?: 1L
    }
  }

  // Dedicated 1000ms 30-minute inactivity & 30s countdown guard
  LaunchedEffect(exoPlayer) {
    while (true) {
      delay(1000L)
      if (isPlaying && !showInactivityDialog) {
        if (System.currentTimeMillis() - lastInteractionMs >= 30L * 60L * 1000L) {
          showInactivityDialog = true
          inactivityCountdown = 30
        }
      } else if (showInactivityDialog) {
        inactivityCountdown -= 1
        if (inactivityCountdown <= 0) {
          exoPlayer.pause()
          showInactivityDialog = false
          controlsLocked = true
          // Dim screen for inactivity sleep
          activity?.window?.attributes =
            activity?.window?.attributes?.apply { screenBrightness = 0.05f }
          onLogInactivityAlert(displayTitle)
        }
      }
    }
  }

  LaunchedEffect(activeGestureHud, gestureTick, isDraggingGesture) {
    if (activeGestureHud != null && !isDraggingGesture) {
      delay(1200L)
      activeGestureHud = null
    }
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black),
  ) {
    // Media3 PlayerView
    AndroidView(
      factory = { ctx ->
        PlayerView(ctx).apply {
          player = exoPlayer
          useController = false
          layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
      },
      update = { view ->
        view.resizeMode = resizeMode
        view.keepScreenOn = isPlaying
      },
      modifier = Modifier.fillMaxSize(),
    )

    // Gesture Layer (Tap to toggle HUD, Left-half Vertical Drag = Volume, Right-half Vertical Drag = Brightness)
    Row(
      modifier =
        Modifier.fillMaxSize()
          .pointerInput(controlsLocked) {
            detectTapGestures(
              onTap = {
                recordInteraction()
                controlsVisible = !controlsVisible
              }
            )
          },
    ) {
      // Left Half: Volume Drag (Landscape Only)
      Box(
        modifier =
          Modifier.weight(1f)
            .fillMaxHeight()
            .pointerInput(controlsLocked, isLandscape) {
              if (!controlsLocked && isLandscape) {
                detectVerticalDragGestures(
                  onDragStart = {
                    isDraggingGesture = true
                    recordInteraction()
                  },
                  onDragEnd = { isDraggingGesture = false },
                  onDragCancel = { isDraggingGesture = false },
                ) { change, dragAmount ->
                  change.consume()
                  recordInteraction()
                  val delta = -dragAmount / (size.height.toFloat().coerceAtLeast(300f))
                  volumeFrac = (volumeFrac + delta).coerceIn(0f, 1f)
                  val targetVol = (volumeFrac * maxVol).roundToInt().coerceIn(0, maxVol)
                  audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                  activeGestureHud = "VOLUME"
                  gestureTick++
                }
              }
            }
      )
      // Right Half: Brightness Drag (Landscape Only)
      Box(
        modifier =
          Modifier.weight(1f)
            .fillMaxHeight()
            .pointerInput(controlsLocked, isLandscape) {
              if (!controlsLocked && isLandscape) {
                detectVerticalDragGestures(
                  onDragStart = {
                    isDraggingGesture = true
                    recordInteraction()
                  },
                  onDragEnd = { isDraggingGesture = false },
                  onDragCancel = { isDraggingGesture = false },
                ) { change, dragAmount ->
                  change.consume()
                  recordInteraction()
                  val delta = -dragAmount / (size.height.toFloat().coerceAtLeast(300f))
                  brightnessFrac = (brightnessFrac + delta).coerceIn(0.05f, 1f)
                  activity?.window?.attributes =
                    activity?.window?.attributes?.apply { screenBrightness = brightnessFrac }
                  activeGestureHud = "BRIGHTNESS"
                  gestureTick++
                }
              }
            }
      )
    }

    // Vertical Glass Volume / Brightness HUD Overlay
    AnimatedVisibility(
      visible = activeGestureHud != null,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.Center),
    ) {
      val isVol = activeGestureHud == "VOLUME"
      val frac = if (isVol) volumeFrac else brightnessFrac
      val accent = if (isVol) NeonCyan else WarmAmber
      GlassCard(
        cornerRadius = 24.dp,
        borderAccentStart = accent,
        borderAccentEnd = NeonViolet,
        fillAlphaTop = 0.24f,
        fillAlphaBottom = 0.12f,
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = if (isVol) Icons.Rounded.VolumeUp else Icons.Rounded.BrightnessHigh,
            contentDescription = activeGestureHud,
            tint = accent,
            modifier = Modifier.size(28.dp),
          )
          Box(
            modifier =
              Modifier.width(14.dp)
                .height(124.dp)
                .clip(RoundedCornerShape(50))
                .background(FrostedPearl.copy(alpha = 0.14f))
                .border(1.dp, FrostedPearl.copy(alpha = 0.42f), RoundedCornerShape(50)),
            contentAlignment = Alignment.BottomCenter,
          ) {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .fillMaxHeight(frac)
                  .clip(RoundedCornerShape(50))
                  .background(Brush.verticalGradient(listOf(FrostedPearl.copy(alpha = 0.72f), accent, NeonViolet)))
            )
          }
          Text(
            text = "${(frac * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
          )
        }
      }
    }

    // Glassmorphism Controls Overlay
    AnimatedVisibility(
      visible = controlsVisible,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
      ) {
        // Top Glass Control Bar
        GlassCard(
          modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
          cornerRadius = 20.dp,
          fillAlphaTop = 0.2f,
          fillAlphaBottom = 0.1f,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            GlassBubbleButton(
              icon = Icons.Rounded.Close,
              contentDescription = "Close Video",
              onClick = onClose,
              modifier = Modifier.testTag("video_close"),
            )
            Text(
              text = displayTitle,
              style = MaterialTheme.typography.titleSmall,
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            if (!controlsLocked) {
              GlassBubbleButton(
                icon = Icons.Rounded.Subtitles,
                contentDescription = "Subtitle and Audio Tracks",
                onClick = { showTrackDialog = true },
                tint = NeonCyan,
              )
              GlassBubbleButton(
                icon = Icons.Rounded.AspectRatio,
                contentDescription = "Aspect Ratio",
                onClick = {
                  resizeMode =
                    when (resizeMode) {
                      AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                      AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                      else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                tint = WarmAmber,
              )
              GlassBubbleButton(
                icon = Icons.Rounded.ScreenRotation,
                contentDescription = "Toggle Orientation",
                onClick = {
                  isLandscapeForced = !isLandscapeForced
                  activity?.requestedOrientation =
                    if (isLandscapeForced) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                },
                tint = NeonViolet,
              )
            }
            GlassBubbleButton(
              icon = if (controlsLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
              contentDescription = "Lock Controls",
              onClick = {
                recordInteraction()
                controlsLocked = !controlsLocked
              },
              tint = if (controlsLocked) WarmAmber else TextPrimary,
              isActive = controlsLocked,
              modifier = Modifier.testTag("video_lock_toggle"),
            )
          }
        }

        // Center Play/Pause 3D Liquid Glass Orb or Codec Fallback Card
        if (playbackError != null) {
          GlassCard(
            modifier =
              Modifier.align(Alignment.Center)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            cornerRadius = 24.dp,
            borderAccentStart = NeonRed,
            borderAccentEnd = WarmGold,
            fillAlphaTop = 0.28f,
            fillAlphaBottom = 0.14f,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              LiquidGlassIconOrb(
                icon = Icons.Rounded.ErrorOutline,
                contentDescription = "Playback error",
                accent = NeonRed,
                secondaryAccent = MoltenAmber,
                boxSize = 54.dp,
                iconSize = 28.dp,
              )
              Text(
                text = "Unsupported Codec or Container",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
              )
              Text(
                text = playbackError ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                LiquidGlassFrostedButton(
                  onClick = {
                    playbackError = null
                    exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                  },
                  contentColor = IceCyan,
                ) {
                  Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Retry")
                }
                LiquidGlassActionButton(
                  onClick = {
                    val shareUri =
                      if (videoPath.startsWith("content://")) {
                        videoUri
                      } else {
                        runCatching {
                          FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            videoFile,
                          )
                        }.getOrDefault(videoUri)
                      }
                    val intent =
                      Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(shareUri, "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                      }
                    runCatching {
                      context.startActivity(Intent.createChooser(intent, "Open video with"))
                    }
                  },
                  accentStart = IceCyan,
                  accentEnd = LiquidEmerald,
                ) {
                  Icon(
                    Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("External Player", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        } else if (!controlsLocked) {
          GlassBubbleButton(
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause Video" else "Play Video",
            onClick = {
              recordInteraction()
              if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            },
            size = 78.dp,
            iconSize = 36.dp,
            tint = if (isPlaying) WarmGold else FrostedPearl,
            accentStart = if (isPlaying) MoltenAmber else IceCyan,
            accentEnd = if (isPlaying) IceCyan else WarmGold,
            isActive = true,
            modifier = Modifier.align(Alignment.Center).testTag("video_play_pause"),
          )

          // Bottom Glass Seekbar & Gesture Hint
          GlassCard(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            cornerRadius = 22.dp,
            fillAlphaTop = 0.2f,
            fillAlphaBottom = 0.1f,
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(FormatUtils.formatDuration(positionMs), style = MaterialTheme.typography.labelMedium, color = NeonCyan)
                Text(
                  text = if (isLandscape) "Swipe Left: Volume • Right: Brightness" else "Landscape: Swipe Volume & Brightness",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextSecondary,
                  textAlign = TextAlign.Center,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Text(FormatUtils.formatDuration(durationMs), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
              }
              Spacer(modifier = Modifier.height(6.dp))
              val progressFrac =
                if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                else 0f
              LiquidGlassScrubBar(
                valueFraction = progressFrac,
                onSeekFraction = { frac ->
                  recordInteraction()
                  if (durationMs > 0L) {
                    exoPlayer.seekTo((frac * durationMs.toFloat()).toLong())
                  }
                },
                accentStart = IceCyan,
                accentEnd = WarmGold,
              )
            }
          }
        }
      }
    }
  }

  if (showTrackDialog) {
    val tracks = exoPlayer.currentTracks
    val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
    val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
    Dialog(onDismissRequest = { showTrackDialog = false }) {
      GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, fillAlphaTop = 0.24f, fillAlphaBottom = 0.12f) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Audio & Subtitle Tracks", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
          Text("Audio Streams: ${audioGroups.size} detected", style = MaterialTheme.typography.bodyMedium, color = NeonCyan)
          Text("Subtitle Tracks: ${textGroups.size} detected", style = MaterialTheme.typography.bodyMedium, color = WarmAmber)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassChip(
              label = "Preview 30m Alert",
              selected = false,
              onClick = {
                showTrackDialog = false
                showInactivityDialog = true
                inactivityCountdown = 30
              },
              accentColor = NeonViolet,
            )
          }
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            LiquidGlassFrostedButton(onClick = { showTrackDialog = false }) {
              Text("Close", color = TextPrimary)
            }
          }
        }
      }
    }
  }

  if (showInactivityDialog) {
    Dialog(onDismissRequest = { recordInteraction() }) {
      GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        borderAccentStart = NeonViolet,
        borderAccentEnd = WarmAmber,
        fillAlphaTop = 0.24f,
        fillAlphaBottom = 0.12f,
      ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Are you still watching?", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
          Text(
            "30-minute playback inactivity threshold reached. Auto-pausing & dimming screen in ${inactivityCountdown}s.",
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
                showInactivityDialog = false
                exoPlayer.pause()
                onLogInactivityAlert(displayTitle)
              },
              contentColor = WarmGold,
            ) {
              Text("Pause", color = WarmGold)
            }
            LiquidGlassActionButton(
              onClick = {
                controlsLocked = false
                recordInteraction()
              },
              accentStart = LiquidEmerald,
              accentEnd = IceCyan,
            ) {
              Text("Continue", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
