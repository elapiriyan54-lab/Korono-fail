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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.data.local.SearchHistoryEntity
import com.example.model.KronoFileItem
import com.example.model.SearchDateFilter
import com.example.model.SearchScope
import com.example.model.SearchSizeFilter
import com.example.model.SearchTypeFilter
import com.example.ui.components.FileThumbnailOrIcon
import com.example.ui.components.GlassBubbleButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.LiquidGlassFrostedButton
import com.example.ui.components.LiquidGlassIconOrb
import com.example.ui.theme.DeepSapphire
import com.example.ui.theme.IceCyan
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PrismaticViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import com.example.util.FormatUtils
import com.example.viewmodel.SearchUiState

@Composable
fun SmartSearchScreen(
  searchState: SearchUiState,
  results: List<KronoFileItem>,
  recentSearches: List<SearchHistoryEntity>,
  currentFolderRel: String,
  onQueryChange: (String) -> Unit,
  onCommitQuery: (String) -> Unit,
  onClearHistory: () -> Unit,
  onSelectType: (SearchTypeFilter) -> Unit,
  onSelectSize: (SearchSizeFilter) -> Unit,
  onSelectDate: (SearchDateFilter) -> Unit,
  onSelectScope: (SearchScope) -> Unit,
  onOpenFile: (KronoFileItem) -> Unit,
  hasBottomBar: Boolean = true,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxSize()
        .statusBarsPadding()
        .widthIn(max = 980.dp)
        .padding(top = 10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Title & Deterministic Offline Badge
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Smart Search",
        style = MaterialTheme.typography.headlineSmall,
        color = TextPrimary,
      )
      Text(
        text = "100% OFFLINE INDEX",
        style = MaterialTheme.typography.labelSmall,
        color = NeonCyan,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 1. Prominent Frosted Glass Search Input Bar
    GlassCard(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      cornerRadius = 20.dp,
      borderAccentStart = NeonCyan.copy(alpha = 0.78f),
      borderAccentEnd = NeonViolet.copy(alpha = 0.65f),
      fillAlphaTop = 0.18f,
      fillAlphaBottom = 0.08f,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LiquidGlassIconOrb(
          icon = Icons.Rounded.Search,
          contentDescription = "Search",
          accent = IceCyan,
          secondaryAccent = DeepSapphire,
          boxSize = 38.dp,
          iconSize = 20.dp,
          cornerRadius = 12.dp,
        )
        Box(
          modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          if (searchState.query.isEmpty()) {
            Text(
              text = "Search filenames, extensions (.pdf, .js, .wav)...",
              style = MaterialTheme.typography.bodyMedium,
              color = TextMuted,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          BasicTextField(
            value = searchState.query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            cursorBrush = SolidColor(IceCyan),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onCommitQuery(searchState.query) }),
            modifier = Modifier.fillMaxWidth().testTag("smart_search_input"),
          )
        }
        if (searchState.query.isNotEmpty()) {
          GlassBubbleButton(
            icon = Icons.Rounded.Close,
            contentDescription = "Clear search",
            onClick = { onQueryChange("") },
            size = 48.dp,
            iconSize = 18.dp,
            tint = TextPrimary,
            accentStart = PrismaticViolet,
            accentEnd = IceCyan,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. Smart Filter Chips (Type / Size / Date / Scope)
    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(SearchTypeFilter.entries) { filter ->
        GlassChip(
          label = filter.label,
          selected = searchState.typeFilter == filter,
          onClick = { onSelectType(filter) },
          accentColor = NeonCyan,
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(SearchSizeFilter.entries) { sizeFilter ->
        GlassChip(
          label = sizeFilter.label,
          selected = searchState.sizeFilter == sizeFilter,
          onClick = { onSelectSize(sizeFilter) },
          accentColor = WarmAmber,
        )
      }
      items(SearchDateFilter.entries) { dateFilter ->
        GlassChip(
          label = dateFilter.label,
          selected = searchState.dateFilter == dateFilter,
          onClick = { onSelectDate(dateFilter) },
          accentColor = NeonViolet,
        )
      }
      item {
        val inFolderScope = searchState.scope == SearchScope.CURRENT_FOLDER
        val folderLabel =
          if (currentFolderRel.isBlank()) "Scope: Root"
          else "Folder: ${currentFolderRel.substringAfterLast('/')}"
        GlassChip(
          label = if (inFolderScope) folderLabel else "Entire Storage",
          selected = inFolderScope,
          onClick = {
            onSelectScope(
              if (inFolderScope) SearchScope.ENTIRE_STORAGE else SearchScope.CURRENT_FOLDER
            )
          },
          accentColor = NeonCyan,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3. Recent Searches + 4. Search Results List
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = navBottom + if (hasBottomBar) 140.dp else 28.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (recentSearches.isNotEmpty() && searchState.query.isBlank()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Recent Searches", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
              LiquidGlassFrostedButton(
                onClick = onClearHistory,
                contentColor = WarmAmber,
              ) {
                Text("Clear History", color = WarmAmber)
              }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(recentSearches, key = { it.query }) { entry ->
                GlassChip(
                  label = entry.query,
                  selected = false,
                  leadingIcon = Icons.Rounded.History,
                  onClick = { onQueryChange(entry.query) },
                  accentColor = NeonCyan,
                )
              }
            }
          }
        }
      }

      item {
        Text(
          text = "${results.size} matching file(s)",
          style = MaterialTheme.typography.labelMedium,
          color = TextMuted,
        )
      }

      if (results.isEmpty()) {
        item {
          GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(28.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.SearchOff,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(36.dp),
              )
              Text("No exact matches found", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
              Text(
                "Adjust filters or try another filename, extension, or folder keyword.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
              )
            }
          }
        }
      } else {
        items(results, key = { it.id }) { item ->
          SearchResultCard(
            item = item,
            query = searchState.query,
            onClick = {
              if (searchState.query.isNotBlank()) onCommitQuery(searchState.query)
              onOpenFile(item)
            },
          )
        }
      }
    }
  }
}

@Composable
private fun SearchResultCard(
  item: KronoFileItem,
  query: String,
  onClick: () -> Unit,
) {
  val highlightedName =
    remember(item.name, query) {
      val q = query.trim()
      if (q.isEmpty()) {
        buildAnnotatedString { append(item.name) }
      } else {
        val idx = item.name.indexOf(q, ignoreCase = true)
        if (idx < 0) {
          buildAnnotatedString { append(item.name) }
        } else {
          buildAnnotatedString {
            append(item.name.substring(0, idx))
            withStyle(SpanStyle(color = NeonCyan, fontWeight = FontWeight.Bold)) {
              append(item.name.substring(idx, idx + q.length))
            }
            append(item.name.substring(idx + q.length))
          }
        }
      }
    }

  GlassCard(
    modifier = Modifier.fillMaxWidth(),
    cornerRadius = 18.dp,
    onClick = onClick,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      FileThumbnailOrIcon(item = item)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = highlightedName,
          style = MaterialTheme.typography.titleSmall,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = item.relativePath,
          style = MaterialTheme.typography.labelSmall,
          color = NeonViolet.copy(alpha = 0.9f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${FormatUtils.formatBytes(item.sizeBytes)} • ${FormatUtils.formatDateShort(item.lastModifiedMs)}",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
        )
      }
    }
  }
}
