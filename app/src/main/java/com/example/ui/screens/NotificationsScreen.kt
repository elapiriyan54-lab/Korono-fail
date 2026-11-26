package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.NotificationEntity
import com.example.model.NotificationCategory
import com.example.model.NotificationSeverity
import com.example.ui.components.GlassBubbleButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.LiquidGlassActionButton
import com.example.ui.components.LiquidGlassFrostedButton
import com.example.ui.components.LiquidGlassIconOrb
import com.example.ui.theme.DeepSapphire
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.IceCyan
import com.example.ui.theme.LiquidEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmGold
import com.example.util.FormatUtils

@Composable
fun NotificationsScreen(
  notifications: List<NotificationEntity>,
  onBack: () -> Unit,
  onMarkAllRead: () -> Unit,
  onClearAll: () -> Unit,
  onDismissItem: (Long) -> Unit,
  onActionClick: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0=All, 1=Storage & Duplicates, 2=Activity

  LaunchedEffect(Unit) { onMarkAllRead() }

  val filtered =
    remember(notifications, selectedTab) {
      when (selectedTab) {
        1 -> notifications.filter { it.category == NotificationCategory.STORAGE_DUPLICATES }
        2 -> notifications.filter { it.category == NotificationCategory.ACTIVITY }
        else -> notifications
      }
    }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .statusBarsPadding()
        .widthIn(max = 900.dp)
        .padding(top = 10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      GlassBubbleButton(
        icon = Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = "Back to Home",
        onClick = onBack,
        modifier = Modifier.testTag("notifications_back"),
      )
      Text(
        text = "Notifications",
        style = MaterialTheme.typography.headlineSmall,
        color = TextPrimary,
        modifier = Modifier.weight(1f),
      )
      if (notifications.isNotEmpty()) {
        GlassChip(
          label = "Clear All",
          selected = false,
          leadingIcon = Icons.Rounded.DeleteSweep,
          onClick = onClearAll,
          accentColor = WarmAmber,
          modifier = Modifier.testTag("notifications_clear_all"),
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item {
        GlassChip(label = "All", selected = selectedTab == 0, onClick = { selectedTab = 0 }, accentColor = NeonCyan)
      }
      item {
        GlassChip(label = "Storage & Duplicates", selected = selectedTab == 1, onClick = { selectedTab = 1 }, accentColor = WarmAmber)
      }
      item {
        GlassChip(label = "Activity", selected = selectedTab == 2, onClick = { selectedTab = 2 }, accentColor = NeonViolet)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (filtered.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        GlassCard(
          modifier = Modifier.fillMaxWidth(),
          cornerRadius = 24.dp,
          borderAccentStart = NeonGreen.copy(alpha = 0.55f),
          borderAccentEnd = NeonCyan.copy(alpha = 0.45f),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = null,
              tint = NeonGreen,
              modifier = Modifier.size(44.dp),
            )
            Text(
              text = "All clear — no active alerts",
              style = MaterialTheme.typography.titleMedium,
              color = TextPrimary,
              textAlign = TextAlign.Center,
            )
            Text(
              text = "Storage notices, duplicate scans, and playback inactivity alerts appear here.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    } else {
      val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = navBottom + 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(filtered, key = { it.id }) { item ->
          val accent =
            when (item.severity) {
              NotificationSeverity.WARNING -> WarmAmber
              NotificationSeverity.CYAN_INFO -> NeonCyan
              NotificationSeverity.VIOLET_SYSTEM -> NeonViolet
            }
          val icon =
            when (item.severity) {
              NotificationSeverity.WARNING -> Icons.Rounded.WarningAmber
              NotificationSeverity.CYAN_INFO -> Icons.Rounded.Sync
              NotificationSeverity.VIOLET_SYSTEM -> Icons.Rounded.Timer
            }
          GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            borderAccentStart = accent.copy(alpha = 0.72f),
            borderAccentEnd = accent.copy(alpha = 0.2f),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                LiquidGlassIconOrb(
                  icon = icon,
                  contentDescription = null,
                  accent = accent,
                  secondaryAccent = if (accent == WarmAmber) WarmGold else ElectricBlue,
                  boxSize = 44.dp,
                  iconSize = 22.dp,
                  cornerRadius = 14.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(item.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                  Text(
                    FormatUtils.formatRelativeTime(item.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                  )
                }
              }

              Text(item.message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                LiquidGlassFrostedButton(onClick = { onDismissItem(item.id) }) {
                  Text("Dismiss", color = TextPrimary)
                }
                val route = item.actionRoute
                val label = item.actionLabel
                if (route != null && label != null && label != "Dismiss") {
                  LiquidGlassActionButton(
                    onClick = { onActionClick(route) },
                    accentStart = LiquidEmerald,
                    accentEnd = if (accent == WarmAmber) WarmGold else IceCyan,
                  ) {
                    Text(label, fontWeight = FontWeight.SemiBold)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
