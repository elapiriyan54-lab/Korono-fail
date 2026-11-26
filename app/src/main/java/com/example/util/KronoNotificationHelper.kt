package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object KronoNotificationHelper {
  const val CHANNEL_ALERTS = "krono_heads_up_alerts"
  const val CHANNEL_MEDIA = "krono_media_playback"

  fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val alertsChannel =
        NotificationChannel(
            CHANNEL_ALERTS,
            "Storage & Playback Alerts",
            NotificationManager.IMPORTANCE_HIGH,
          )
          .apply {
            description = "Heads-up alerts for low storage, duplicate files, and 30-min playback inactivity"
            enableVibration(true)
          }
      val mediaChannel =
        NotificationChannel(
            CHANNEL_MEDIA,
            "Audio Playback",
            NotificationManager.IMPORTANCE_LOW,
          )
          .apply {
            description = "Background Vinyl Audio Player controls"
          }
      manager.createNotificationChannel(alertsChannel)
      manager.createNotificationChannel(mediaChannel)
    }
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun postHeadsUpAlert(
    context: Context,
    notificationId: Int,
    title: String,
    message: String,
  ) {
    ensureChannels(context)
    if (!hasNotificationPermission(context)) return

    val launchIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        notificationId,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ALERTS)
        .setSmallIcon(R.drawable.ic_notification_krono)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    runCatching {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
  }
}
