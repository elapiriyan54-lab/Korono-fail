package com.example.ui.components

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.model.KronoFileItem
import com.example.ui.theme.DeepSapphire
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.IceCyan
import com.example.ui.theme.MoltenAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmGold
import com.example.util.FormatUtils
import java.io.File
import java.util.zip.ZipInputStream

@Composable
fun GlassTextInputDialog(
  title: String,
  label: String,
  initialValue: String,
  confirmLabel: String,
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var value by remember { mutableStateOf(initialValue) }
  Dialog(onDismissRequest = onDismiss) {
    GlassCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 24.dp,
      borderAccentStart = NeonCyan.copy(alpha = 0.75f),
      borderAccentEnd = NeonViolet.copy(alpha = 0.65f),
      fillAlphaTop = 0.34f,
      fillAlphaBottom = 0.20f,
    ) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        OutlinedTextField(
          value = value,
          onValueChange = { value = it },
          label = { Text(label) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = NeonCyan,
              unfocusedBorderColor = TextSecondary.copy(alpha = 0.55f),
              focusedContainerColor = IceCyan.copy(alpha = 0.10f),
              unfocusedContainerColor = com.example.ui.theme.FrostedPearl.copy(alpha = 0.06f),
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedLabelColor = NeonCyan,
              unfocusedLabelColor = TextSecondary,
              cursorColor = IceCyan,
            ),
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          LiquidGlassFrostedButton(onClick = onDismiss) { Text("Cancel") }
          Spacer(modifier = Modifier.size(10.dp))
          LiquidGlassActionButton(
            onClick = { onConfirm(value) },
            accentStart = IceCyan,
            accentEnd = DeepSapphire,
          ) {
            Text(confirmLabel, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun GlassConfirmDialog(
  title: String,
  message: String,
  confirmLabel: String,
  isDestructive: Boolean = false,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  val accent = if (isDestructive) NeonRed else NeonCyan
  Dialog(onDismissRequest = onDismiss) {
    GlassCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 24.dp,
      borderAccentStart = accent.copy(alpha = 0.8f),
      borderAccentEnd = NeonViolet.copy(alpha = 0.55f),
      fillAlphaTop = 0.34f,
      fillAlphaBottom = 0.20f,
    ) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          LiquidGlassFrostedButton(onClick = onDismiss) { Text("Cancel") }
          Spacer(modifier = Modifier.size(10.dp))
          LiquidGlassActionButton(
            onClick = onConfirm,
            accentStart = accent,
            accentEnd = if (isDestructive) MoltenAmber else DeepSapphire,
          ) {
            Text(confirmLabel, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun FolderPickerDialog(
  title: String,
  folders: List<KronoFileItem>,
  isUsingDeviceStorage: Boolean = false,
  hasMediaFallbackAccess: Boolean = false,
  onSelectFolder: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    GlassCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 24.dp,
      borderAccentStart = NeonCyan.copy(alpha = 0.72f),
      borderAccentEnd = NeonViolet.copy(alpha = 0.6f),
      fillAlphaTop = 0.34f,
      fillAlphaBottom = 0.20f,
    ) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(folders, key = { it.absolutePath + it.relativePath }) { folder ->
            GlassCard(
              modifier = Modifier.fillMaxWidth(),
              cornerRadius = 16.dp,
              borderAccentStart = IceCyan.copy(alpha = 0.62f),
              borderAccentEnd = ElectricBlue.copy(alpha = 0.52f),
              fillAlphaTop = 0.24f,
              fillAlphaBottom = 0.14f,
              onClick = { onSelectFolder(folder.relativePath) },
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                LiquidGlassIconOrb(
                  icon = Icons.Rounded.Folder,
                  contentDescription = null,
                  accent = IceCyan,
                  secondaryAccent = DeepSapphire,
                  boxSize = 38.dp,
                  iconSize = 20.dp,
                  cornerRadius = 12.dp,
                )
                Column {
                  Text(
                    text =
                      if (folder.relativePath.isBlank()) {
                        when {
                          hasMediaFallbackAccess -> "Device Media & Docs (Root)"
                          isUsingDeviceStorage -> "Device Storage (Root)"
                          else -> "App Sandbox (Root)"
                        }
                      } else folder.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                  )
                  if (folder.relativePath.isNotBlank()) {
                    Text(
                      text = folder.relativePath,
                      style = MaterialTheme.typography.labelSmall,
                      color = TextSecondary,
                    )
                  }
                }
              }
            }
          }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          LiquidGlassFrostedButton(onClick = onDismiss) { Text("Close") }
        }
      }
    }
  }
}

@Composable
fun ImagePreviewDialog(
  file: File,
  title: String,
  contentUri: String? = null,
  sizeBytes: Long = file.length(),
  onShare: () -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    GlassCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 24.dp,
      borderAccentStart = NeonCyan.copy(alpha = 0.75f),
      borderAccentEnd = WarmAmber.copy(alpha = 0.65f),
      fillAlphaTop = 0.34f,
      fillAlphaBottom = 0.20f,
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
          )
          GlassBubbleButton(
            icon = Icons.Rounded.Close,
            contentDescription = "Close preview",
            onClick = onDismiss,
            size = 48.dp,
          )
        }
        AsyncImage(
          model = contentUri?.let { Uri.parse(it) } ?: file,
          contentDescription = title,
          contentScale = ContentScale.Fit,
          modifier =
            Modifier.fillMaxWidth()
              .height(280.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(com.example.ui.theme.ObsidianSurface.copy(alpha = 0.52f))
              .border(1.dp, IceCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = FormatUtils.formatBytes(sizeBytes),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
          )
          LiquidGlassActionButton(
            onClick = onShare,
            accentStart = IceCyan,
            accentEnd = DeepSapphire,
          ) {
            Text("Share", fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}

@Composable
fun ApkInspectorDialog(
  file: File,
  onShare: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val infoText =
    remember(file.absolutePath) {
      val pm = context.packageManager
      val pkgInfo = runCatching { pm.getPackageArchiveInfo(file.absolutePath, 0) }.getOrNull()
      if (pkgInfo != null) {
        "Package: ${pkgInfo.packageName}\nVersion: ${pkgInfo.versionName ?: "1.0"}\nSize: ${FormatUtils.formatBytes(file.length())}"
      } else {
        // Read manifest summary from archive entry if available
        val manifest =
          runCatching {
            ZipInputStream(file.inputStream()).use { zis ->
              var entry = zis.nextEntry
              while (entry != null) {
                if (entry.name.contains("MANIFEST.MF", ignoreCase = true)) {
                  return@runCatching zis.readBytes().decodeToString()
                }
                entry = zis.nextEntry
              }
              null
            }
          }.getOrNull()
        manifest ?: "APK Package File\nSize: ${FormatUtils.formatBytes(file.length())}\nRead-only offline inspection."
      }
    }
  Dialog(onDismissRequest = onDismiss) {
    GlassCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 24.dp,
      borderAccentStart = NeonGreen.copy(alpha = 0.75f),
      borderAccentEnd = NeonCyan.copy(alpha = 0.60f),
      fillAlphaTop = 0.34f,
      fillAlphaBottom = 0.20f,
    ) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          LiquidGlassIconOrb(
            icon = Icons.Rounded.Android,
            contentDescription = null,
            accent = NeonGreen,
            secondaryAccent = IceCyan,
            boxSize = 42.dp,
            iconSize = 22.dp,
            cornerRadius = 13.dp,
          )
          Text(file.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        Text(text = infoText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
          text = "Offline APK inspector — verifies package metadata before installation.",
          style = MaterialTheme.typography.labelSmall,
          color = WarmAmber,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          LiquidGlassFrostedButton(onClick = onShare) { Text("Share") }
          LiquidGlassActionButton(
            onClick = {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent =
                  Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                  )
                context.startActivity(settingsIntent)
              } else {
                runCatching {
                  val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                  val installIntent =
                    Intent(Intent.ACTION_VIEW).apply {
                      setDataAndType(uri, "application/vnd.android.package-archive")
                      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                  context.startActivity(installIntent)
                }
              }
            },
            accentStart = NeonGreen,
            accentEnd = IceCyan,
          ) {
            Text("Install", fontWeight = FontWeight.Bold)
          }
          LiquidGlassFrostedButton(onClick = onDismiss) { Text("Close") }
        }
      }
    }
  }
}
