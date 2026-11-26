package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.model.FileKind
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
  fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.1f GB", gb)
  }

  fun formatGbShort(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 10.0) String.format(Locale.US, "%.0f GB", gb)
    else String.format(Locale.US, "%.1f GB", gb)
  }

  fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
  }

  fun formatRelativeTime(timestampMs: Long): String {
    val diff = System.currentTimeMillis() - timestampMs
    if (diff < 60_000L) return "Just now"
    val mins = diff / 60_000L
    if (mins < 60L) return "${mins}m ago"
    val hours = mins / 60L
    if (hours < 24L) return "${hours}h ago"
    val days = hours / 24L
    if (days < 7L) return "${days}d ago"
    return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(timestampMs))
  }

  fun formatDateShort(timestampMs: Long): String =
    SimpleDateFormat("MMM dd • HH:mm", Locale.US).format(Date(timestampMs))

  fun iconForKind(kind: FileKind): ImageVector =
    when (kind) {
      FileKind.FOLDER -> Icons.Rounded.Folder
      FileKind.IMAGE -> Icons.Rounded.Image
      FileKind.VIDEO -> Icons.Rounded.VideoFile
      FileKind.AUDIO -> Icons.Rounded.AudioFile
      FileKind.PDF -> Icons.Rounded.PictureAsPdf
      FileKind.DOCX -> Icons.Rounded.Description
      FileKind.XLSX -> Icons.Rounded.TableChart
      FileKind.CODE -> Icons.Rounded.Code
      FileKind.TEXT -> Icons.Rounded.Description
      FileKind.APK -> Icons.Rounded.Android
      FileKind.ARCHIVE -> Icons.Rounded.FolderZip
      FileKind.OTHER -> Icons.Rounded.InsertDriveFile
    }

  fun accentForKind(kind: FileKind): Color =
    when (kind) {
      FileKind.FOLDER -> NeonCyan
      FileKind.IMAGE -> NeonCyan
      FileKind.VIDEO -> NeonViolet
      FileKind.AUDIO -> WarmAmber
      FileKind.PDF -> NeonRed
      FileKind.DOCX -> NeonCyan
      FileKind.XLSX -> NeonGreen
      FileKind.CODE -> NeonViolet
      FileKind.TEXT -> TextSecondary
      FileKind.APK -> NeonGreen
      FileKind.ARCHIVE -> WarmAmber
      FileKind.OTHER -> TextSecondary
    }
}
