package com.example.viewmodel

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.media.KronoPlaybackService
import com.example.model.AudioTrackItem
import com.example.model.RepeatModeState
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class AudioPlayerUiState(
  val queue: List<AudioTrackItem> = emptyList(),
  val currentIndex: Int = 0,
  val isPlaying: Boolean = false,
  val positionMs: Long = 0L,
  val durationMs: Long = 1L,
  val shuffleEnabled: Boolean = false,
  val repeatMode: RepeatModeState = RepeatModeState.OFF,
  val isScrubbingVinyl: Boolean = false,
  val vinylRotationDegrees: Float = 0f,
  val inactivityAlertVisible: Boolean = false,
  val inactivityCountdownSec: Int = 30,
  val sleepTimerMinutes: Int? = null,
) {
  val currentTrack: AudioTrackItem?
    get() = queue.getOrNull(currentIndex)
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
  private var controllerFuture: ListenableFuture<MediaController>? = null
  private var mediaController: MediaController? = null
  private var pendingQueueStart: Pair<List<AudioTrackItem>, Int>? = null

  private val _audioState = MutableStateFlow(AudioPlayerUiState())
  val audioState: StateFlow<AudioPlayerUiState> = _audioState.asStateFlow()

  private val _inactivityTimeoutEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
  val inactivityTimeoutEvents: SharedFlow<String> = _inactivityTimeoutEvents.asSharedFlow()

  private var progressJob: Job? = null
  private var inactivityJob: Job? = null
  private var lastInteractionMs: Long = System.currentTimeMillis()
  private var wasPlayingBeforeScrub = false
  private var lastScrubSeekElapsedMs: Long = 0L

  companion object {
    const val INACTIVITY_THRESHOLD_MS = 30L * 60L * 1000L // 30 minutes
  }

  private val playerListener =
    object : Player.Listener {
      override fun onIsPlayingChanged(isPlaying: Boolean) {
        _audioState.update { it.copy(isPlaying = isPlaying) }
      }

      override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val player = mediaController ?: return
        val idx = player.currentMediaItemIndex
        if (idx in _audioState.value.queue.indices) {
          _audioState.update {
            it.copy(
              currentIndex = idx,
              durationMs = player.duration.takeIf { d -> d > 0 } ?: it.queue[idx].durationMs,
            )
          }
        }
      }

      override fun onPlaybackStateChanged(playbackState: Int) {
        val player = mediaController ?: return
        if (playbackState == Player.STATE_READY) {
          val dur = player.duration.takeIf { it > 0 } ?: _audioState.value.currentTrack?.durationMs ?: 1L
          _audioState.update { it.copy(durationMs = dur) }
        }
      }
    }

  init {
    connectMediaController()
    startProgressTicker()
    startInactivityMonitor()
  }

  private fun connectMediaController() {
    val app = getApplication<Application>()
    val sessionToken = SessionToken(app, ComponentName(app, KronoPlaybackService::class.java))
    val future = MediaController.Builder(app, sessionToken).buildAsync()
    controllerFuture = future
    future.addListener(
      {
        runCatching {
          val controller = future.get()
          mediaController = controller
          controller.addListener(playerListener)
          pendingQueueStart?.let { (tracks, startIndex) ->
            pendingQueueStart = null
            applyQueueToPlayer(controller, tracks, startIndex)
          }
        }
      },
      ContextCompat.getMainExecutor(app),
    )
  }

  fun startQueue(tracks: List<AudioTrackItem>, startIndex: Int) {
    if (tracks.isEmpty()) return
    val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
    markUserInteraction()

    _audioState.update {
      it.copy(
        queue = tracks,
        currentIndex = safeIndex,
        durationMs = tracks[safeIndex].durationMs.coerceAtLeast(1L),
        positionMs = 0L,
        isPlaying = true,
      )
    }

    val controller = mediaController
    if (controller != null) {
      applyQueueToPlayer(controller, tracks, safeIndex)
    } else {
      pendingQueueStart = tracks to safeIndex
    }
  }

  private fun applyQueueToPlayer(player: Player, tracks: List<AudioTrackItem>, safeIndex: Int) {
    player.stop()
    player.clearMediaItems()
    val mediaItems =
      tracks.map { track ->
        val metadata =
          MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .build()
        val uri = track.contentUri?.let { Uri.parse(it) } ?: Uri.fromFile(File(track.path))
        MediaItem.Builder()
          .setUri(uri)
          .setMediaId(track.path)
          .setMediaMetadata(metadata)
          .build()
      }
    player.setMediaItems(mediaItems, safeIndex, 0L)
    player.prepare()
    player.setPlaybackSpeed(1.0f)
    player.play()
  }

  fun selectTrackInQueue(index: Int) {
    if (index !in _audioState.value.queue.indices) return
    markUserInteraction()
    mediaController?.let { player ->
      player.seekTo(index, 0L)
      player.setPlaybackSpeed(1.0f)
      player.play()
    }
    _audioState.update {
      it.copy(
        currentIndex = index,
        positionMs = 0L,
        durationMs = it.queue[index].durationMs.coerceAtLeast(1L),
      )
    }
  }

  fun togglePlayPause() {
    markUserInteraction()
    val player = mediaController ?: return
    if (player.isPlaying) {
      player.pause()
    } else {
      player.setPlaybackSpeed(1.0f)
      player.play()
    }
  }

  fun playNext() {
    markUserInteraction()
    val player = mediaController ?: return
    if (player.hasNextMediaItem()) {
      player.seekToNextMediaItem()
    } else if (_audioState.value.queue.isNotEmpty()) {
      player.seekTo(0, 0L)
    }
    player.play()
  }

  fun playPrevious() {
    markUserInteraction()
    val player = mediaController ?: return
    if (player.currentPosition > 3000L) {
      player.seekTo(0L)
    } else if (player.hasPreviousMediaItem()) {
      player.seekToPreviousMediaItem()
    } else if (_audioState.value.queue.isNotEmpty()) {
      player.seekTo(_audioState.value.queue.lastIndex, 0L)
    }
    player.play()
  }

  fun toggleShuffle() {
    markUserInteraction()
    val next = !_audioState.value.shuffleEnabled
    mediaController?.shuffleModeEnabled = next
    _audioState.update { it.copy(shuffleEnabled = next) }
  }

  fun cycleRepeatMode() {
    markUserInteraction()
    val next =
      when (_audioState.value.repeatMode) {
        RepeatModeState.OFF -> RepeatModeState.ALL
        RepeatModeState.ALL -> RepeatModeState.ONE
        RepeatModeState.ONE -> RepeatModeState.OFF
      }
    mediaController?.repeatMode =
      when (next) {
        RepeatModeState.OFF -> Player.REPEAT_MODE_OFF
        RepeatModeState.ALL -> Player.REPEAT_MODE_ALL
        RepeatModeState.ONE -> Player.REPEAT_MODE_ONE
      }
    _audioState.update { it.copy(repeatMode = next) }
  }

  fun seekTo(positionMs: Long) {
    markUserInteraction()
    val clamped = positionMs.coerceIn(0L, _audioState.value.durationMs.coerceAtLeast(1L))
    mediaController?.seekTo(clamped)
    _audioState.update { it.copy(positionMs = clamped) }
  }

  fun onVinylScrubStart() {
    markUserInteraction()
    val player = mediaController
    wasPlayingBeforeScrub = player?.isPlaying == true || _audioState.value.isPlaying
    if (player?.isPlaying == true) {
      player.pause()
    }
    _audioState.update { it.copy(isScrubbingVinyl = true, isPlaying = false) }
  }

  fun onVinylScrubDelta(angleDeltaDegrees: Float) {
    markUserInteraction()
    val dur = _audioState.value.durationMs.coerceAtLeast(1L)
    val scrubWindowMs = minOf(12_000L, (dur / 2L).coerceAtLeast(4_000L))
    val deltaMs = ((angleDeltaDegrees / 360f) * scrubWindowMs).toLong()
    val nextPos = (_audioState.value.positionMs + deltaMs).coerceIn(0L, dur)
    val now = SystemClock.elapsedRealtime()
    if (now - lastScrubSeekElapsedMs >= 80L) {
      lastScrubSeekElapsedMs = now
      mediaController?.seekTo(nextPos)
    }
    _audioState.update {
      it.copy(
        positionMs = nextPos,
        vinylRotationDegrees = (it.vinylRotationDegrees + angleDeltaDegrees) % 360f,
      )
    }
  }

  fun onVinylScrubEnd() {
    val finalPos = _audioState.value.positionMs
    mediaController?.seekTo(finalPos)
    _audioState.update { it.copy(isScrubbingVinyl = false) }
    if (wasPlayingBeforeScrub) {
      mediaController?.let { player ->
        player.setPlaybackSpeed(1.0f)
        player.play()
      }
    }
  }

  fun markUserInteraction() {
    lastInteractionMs = System.currentTimeMillis()
    if (_audioState.value.inactivityAlertVisible) {
      _audioState.update { it.copy(inactivityAlertVisible = false, inactivityCountdownSec = 30) }
    }
  }

  fun triggerInactivityPromptNow() {
    _audioState.update { it.copy(inactivityAlertVisible = true, inactivityCountdownSec = 30) }
  }

  fun stopPlayback() {
    mediaController?.pause()
    _audioState.update { it.copy(isPlaying = false, inactivityAlertVisible = false, inactivityCountdownSec = 30) }
  }

  private fun startProgressTicker() {
    progressJob?.cancel()
    progressJob =
      viewModelScope.launch {
        while (true) {
          delay(32L)
          val player = mediaController
          if (player != null && player.isPlaying && !_audioState.value.isScrubbingVinyl) {
            val pos = player.currentPosition.coerceAtLeast(0L)
            val dur = player.duration.takeIf { it > 0 } ?: _audioState.value.durationMs
            _audioState.update {
              it.copy(
                positionMs = pos,
                durationMs = dur.coerceAtLeast(1L),
                vinylRotationDegrees = (it.vinylRotationDegrees + 0.55f) % 360f,
              )
            }
          }
        }
      }
  }

  private fun startInactivityMonitor() {
    inactivityJob?.cancel()
    inactivityJob =
      viewModelScope.launch {
        while (true) {
          delay(1000L)
          val state = _audioState.value
          if (state.isPlaying && !state.inactivityAlertVisible) {
            if (System.currentTimeMillis() - lastInteractionMs >= INACTIVITY_THRESHOLD_MS) {
              _audioState.update { it.copy(inactivityAlertVisible = true, inactivityCountdownSec = 30) }
            }
          } else if (state.inactivityAlertVisible) {
            val nextSec = state.inactivityCountdownSec - 1
            if (nextSec <= 0) {
              val trackTitle = state.currentTrack?.title ?: "Audio Playback"
              mediaController?.pause()
              _audioState.update {
                it.copy(
                  inactivityAlertVisible = false,
                  isPlaying = false,
                  inactivityCountdownSec = 30,
                )
              }
              _inactivityTimeoutEvents.tryEmit(trackTitle)
            } else {
              _audioState.update { it.copy(inactivityCountdownSec = nextSec) }
            }
          }
        }
      }
  }

  override fun onCleared() {
    super.onCleared()
    progressJob?.cancel()
    inactivityJob?.cancel()
    mediaController?.removeListener(playerListener)
    controllerFuture?.let { MediaController.releaseFuture(it) }
  }
}
