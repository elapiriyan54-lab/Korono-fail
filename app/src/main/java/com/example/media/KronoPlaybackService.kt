package com.example.media

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R
import com.example.util.KronoNotificationHelper

class KronoPlaybackService : MediaSessionService() {
  private var mediaSession: MediaSession? = null

  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()
    KronoNotificationHelper.ensureChannels(this)
    val notificationProvider =
      DefaultMediaNotificationProvider.Builder(this)
        .setChannelId(KronoNotificationHelper.CHANNEL_MEDIA)
        .build()
        .apply { setSmallIcon(R.drawable.ic_notification_krono) }
    setMediaNotificationProvider(notificationProvider)

    val player =
      ExoPlayer.Builder(this)
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build(),
          /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    val sessionActivity =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    mediaSession =
      MediaSession.Builder(this, player)
        .setSessionActivity(sessionActivity)
        .build()
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

  override fun onTaskRemoved(rootIntent: Intent?) {
    val player = mediaSession?.player
    if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
      stopSelf()
    }
  }

  override fun onDestroy() {
    mediaSession?.run {
      player.release()
      release()
      mediaSession = null
    }
    super.onDestroy()
  }
}
