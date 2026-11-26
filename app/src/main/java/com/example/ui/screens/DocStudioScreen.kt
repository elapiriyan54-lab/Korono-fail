package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.storage.OfficeDocEngine
import com.example.ui.components.GlassBubbleButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassTextInputDialog
import com.example.ui.theme.DeepSapphire
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FrostedPearl
import com.example.ui.theme.IceCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiquidEmerald
import com.example.ui.theme.MoltenAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PrismaticViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmGold
import com.example.viewmodel.DocStudioUiState
import com.example.viewmodel.StudioMode

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DocStudioScreen(
  state: DocStudioUiState,
  onBack: () -> Unit,
  onRenderPdfPage: suspend (Int) -> Bitmap? = { null },
  onRename: (String) -> Unit,
  onTextChange: (String) -> Unit,
  onCellChange: (Int, Int, String) -> Unit,
  onAddSheetRow: () -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onFormat: () -> Unit,
  onTogglePreview: () -> Unit,
  onSave: () -> Unit,
  onExportPdf: () -> Unit,
  onPrint: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var showRenameDialog by remember { mutableStateOf(false) }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .widthIn(max = 1040.dp)
        .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // 1. Frosted Glass Top Toolbar
    GlassCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 22.dp,
      borderAccentStart = NeonCyan.copy(alpha = 0.55f),
      borderAccentEnd = NeonViolet.copy(alpha = 0.5f),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        GlassBubbleButton(
          icon = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Back",
          onClick = onBack,
          accentStart = IceCyan,
          accentEnd = DeepSapphire,
          modifier = Modifier.testTag("studio_back"),
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = state.fileName,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "100% Offline Studio • ${state.extension.uppercase()}",
            style = MaterialTheme.typography.labelSmall,
            color = IceCyan,
          )
        }
        if (state.mode != StudioMode.PDF_VIEW) {
          GlassBubbleButton(
            icon = Icons.Rounded.Edit,
            contentDescription = "Edit Filename",
            onClick = { showRenameDialog = true },
            size = 48.dp,
            iconSize = 20.dp,
            tint = FrostedPearl,
            accentStart = PrismaticViolet,
            accentEnd = ElectricBlue,
          )
          GlassBubbleButton(
            icon = Icons.Rounded.Save,
            contentDescription = "Save Offline",
            onClick = onSave,
            tint = NeonGreen,
            accentStart = NeonGreen,
            accentEnd = IceCyan,
            isActive = true,
            size = 48.dp,
            iconSize = 21.dp,
            modifier = Modifier.testTag("studio_save"),
          )
        }
      }
    }

    // 2. Main Document / Code / Spreadsheet / PDF Surface
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when {
        state.isLoading -> {
          CircularProgressIndicator(color = NeonCyan, modifier = Modifier.align(Alignment.Center))
        }
        state.mode == StudioMode.PDF_VIEW -> {
          if (state.pdfPageCount <= 0) {
            GlassCard(modifier = Modifier.fillMaxSize()) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Unable to render PDF pages", color = TextSecondary)
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              items(state.pdfPageCount, key = { it }) { idx ->
                val pageBitmap by produceState<Bitmap?>(
                  initialValue = null,
                  state.filePath,
                  state.contentUri,
                  state.pdfPageCount,
                  idx,
                ) {
                  value = onRenderPdfPage(idx)
                }
                GlassCard(
                  modifier = Modifier.fillMaxWidth(),
                  cornerRadius = 18.dp,
                  borderAccentStart = NeonCyan.copy(alpha = 0.55f),
                ) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Page ${idx + 1} / ${state.pdfPageCount}", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                    val bmp = pageBitmap
                    if (bmp != null) {
                      Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "PDF Page ${idx + 1}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                      )
                    } else {
                      Box(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        contentAlignment = Alignment.Center,
                      ) {
                        CircularProgressIndicator(color = NeonCyan)
                      }
                    }
                  }
                }
              }
            }
          }
        }
        state.mode == StudioMode.XLSX -> {
          GlassCard(modifier = Modifier.fillMaxSize(), cornerRadius = 20.dp) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("Offline XLSX Spreadsheet Grid", style = MaterialTheme.typography.labelLarge, color = NeonGreen)
                GlassChip(
                  label = "+ Row",
                  selected = false,
                  leadingIcon = Icons.Rounded.Add,
                  onClick = onAddSheetRow,
                  accentColor = NeonGreen,
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              Column(
                modifier =
                  Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                state.sheetGrid.forEachIndexed { rIdx, row ->
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEachIndexed { cIdx, cellValue ->
                      val cellShape = RoundedCornerShape(12.dp)
                      Box(
                        modifier =
                          Modifier.width(126.dp)
                            .clip(cellShape)
                            .background(
                              if (rIdx == 0) {
                                Brush.linearGradient(
                                  listOf(IceCyan.copy(alpha = 0.24f), LiquidEmerald.copy(alpha = 0.14f))
                                )
                              } else {
                                Brush.linearGradient(
                                  listOf(FrostedPearl.copy(alpha = 0.14f), Color(0x1807161B))
                                )
                              }
                            )
                            .border(
                              width = 1.15.dp,
                              brush =
                                Brush.linearGradient(
                                  if (rIdx == 0) listOf(FrostedPearl.copy(alpha = 0.65f), IceCyan.copy(alpha = 0.72f))
                                  else listOf(FrostedPearl.copy(alpha = 0.36f), IceCyan.copy(alpha = 0.25f))
                                ),
                              shape = cellShape,
                            )
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                      ) {
                        BasicTextField(
                          value = cellValue,
                          onValueChange = { onCellChange(rIdx, cIdx, it) },
                          singleLine = true,
                          textStyle =
                            TextStyle(
                              fontFamily = JetBrainsMonoFamily,
                              fontSize = 13.sp,
                              color = if (rIdx == 0) IceCyan else TextPrimary,
                            ),
                          cursorBrush = SolidColor(IceCyan),
                          modifier = Modifier.fillMaxWidth(),
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
        state.mode == StudioMode.TEXT_CODE &&
          state.showCodePreview &&
          state.extension.lowercase() in setOf("html", "htm", "md") -> {
          val renderedHtml =
            remember(state.textContent, state.extension, state.fileName) {
              if (state.extension.lowercase() == "md") {
                OfficeDocEngine.markdownToStyledHtml(state.textContent, state.fileName)
              } else {
                state.textContent
              }
            }
          val baseUrl =
            remember(state.filePath) {
              java.io.File(state.filePath).parentFile?.toURI()?.toString()
            }
          GlassCard(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 20.dp,
            borderAccentStart = NeonCyan.copy(alpha = 0.6f),
            borderAccentEnd = WarmAmber.copy(alpha = 0.5f),
          ) {
            AndroidView(
              factory = { ctx ->
                WebView(ctx).apply {
                  setBackgroundColor(android.graphics.Color.parseColor("#09222C"))
                  settings.allowFileAccess = true
                  settings.allowContentAccess = false
                }
              },
              update = { webView ->
                webView.settings.javaScriptEnabled = state.extension.lowercase() in setOf("html", "htm")
                webView.loadDataWithBaseURL(baseUrl, renderedHtml, "text/html", "UTF-8", null)
              },
              modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).testTag("studio_webview_preview"),
            )
          }
        }
        else -> {
          // Text / Code / DOCX Editor with soft-wrap-aware line numbers
          val lineStartOffsets =
            remember(state.textContent) {
              buildList {
                add(0)
                state.textContent.forEachIndexed { idx, ch ->
                  if (ch == '\n') add(idx + 1)
                }
              }
            }
          var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
          val textMeasurer = rememberTextMeasurer()
          val density = LocalDensity.current
          val gutterStyle = TextStyle(fontFamily = JetBrainsMonoFamily, fontSize = 13.sp, lineHeight = 20.sp, color = TextMuted)
          val gutterLayouts =
            remember(lineStartOffsets.size, gutterStyle, textMeasurer) {
              List(lineStartOffsets.size) { idx ->
                textMeasurer.measure("${idx + 1}", style = gutterStyle)
              }
            }
          val gutterWidth = ((lineStartOffsets.size.toString().length * 9) + 12).dp.coerceAtLeast(28.dp)
          val contentHeightDp =
            with(density) {
              (textLayoutResult?.size?.height?.toDp() ?: (lineStartOffsets.size * 20).dp).coerceAtLeast(240.dp)
            }

          GlassCard(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 20.dp,
            borderAccentStart = NeonViolet.copy(alpha = 0.5f),
            borderAccentEnd = NeonCyan.copy(alpha = 0.45f),
          ) {
            Row(
              modifier =
                Modifier.fillMaxSize()
                  .verticalScroll(rememberScrollState())
                  .padding(14.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Canvas(modifier = Modifier.width(gutterWidth).height(contentHeightDp)) {
                val layout = textLayoutResult
                lineStartOffsets.forEachIndexed { idx, offset ->
                  val yPx =
                    if (layout != null && state.textContent.isNotEmpty()) {
                      val safeOffset = offset.coerceIn(0, state.textContent.length)
                      val visualLine = layout.getLineForOffset(safeOffset).coerceIn(0, layout.lineCount - 1)
                      layout.getLineTop(visualLine)
                    } else {
                      idx * 20.sp.toPx()
                    }
                  val measured = gutterLayouts[idx]
                  drawText(
                    textLayoutResult = measured,
                    topLeft = Offset((size.width - measured.size.width).coerceAtLeast(0f), yPx),
                  )
                }
              }
              Box(
                modifier =
                  Modifier.width(1.5.dp)
                    .height(contentHeightDp)
                    .background(
                      Brush.verticalGradient(
                        listOf(
                          IceCyan.copy(alpha = 0.55f),
                          LiquidEmerald.copy(alpha = 0.45f),
                          WarmAmber.copy(alpha = 0.35f),
                        )
                      )
                    )
              )
              BasicTextField(
                value = state.textContent,
                onValueChange = onTextChange,
                onTextLayout = { textLayoutResult = it },
                textStyle =
                  TextStyle(
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = TextPrimary,
                  ),
                cursorBrush = SolidColor(NeonCyan),
                modifier = Modifier.weight(1f).testTag("studio_text_editor"),
              )
            }
          }
        }
      }
    }

    // 3. Frosted Glass Bottom Studio Toolbar
    GlassCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 22.dp,
      borderAccentStart = WarmAmber.copy(alpha = 0.45f),
      borderAccentEnd = NeonCyan.copy(alpha = 0.55f),
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (state.mode == StudioMode.TEXT_CODE || state.mode == StudioMode.DOCX) {
          GlassChip(
            label = "Undo",
            selected = state.undoStack.isNotEmpty(),
            leadingIcon = Icons.AutoMirrored.Rounded.Undo,
            onClick = onUndo,
            accentColor = NeonCyan,
          )
          GlassChip(
            label = "Redo",
            selected = state.redoStack.isNotEmpty(),
            leadingIcon = Icons.AutoMirrored.Rounded.Redo,
            onClick = onRedo,
            accentColor = NeonViolet,
          )
          GlassChip(
            label = "Format",
            selected = false,
            leadingIcon = Icons.Rounded.Code,
            onClick = onFormat,
            accentColor = NeonCyan,
          )
          if (state.mode == StudioMode.TEXT_CODE && state.extension.lowercase() in setOf("html", "htm", "md")) {
            GlassChip(
              label = if (state.showCodePreview) "Editor" else "Preview",
              selected = state.showCodePreview,
              leadingIcon = Icons.Rounded.Visibility,
              onClick = onTogglePreview,
              accentColor = WarmAmber,
              modifier = Modifier.testTag("studio_preview_toggle"),
            )
          }
        }
        if (state.mode != StudioMode.PDF_VIEW) {
          GlassChip(
            label = "Export PDF",
            selected = true,
            leadingIcon = Icons.Rounded.PictureAsPdf,
            onClick = onExportPdf,
            accentColor = MoltenAmber,
            modifier = Modifier.testTag("studio_export_pdf"),
          )
        }
        GlassChip(
          label = "Print",
          selected = false,
          leadingIcon = Icons.Rounded.Print,
          onClick = onPrint,
          accentColor = WarmGold,
          modifier = Modifier.testTag("studio_print"),
        )
      }
    }
  }

  if (showRenameDialog) {
    GlassTextInputDialog(
      title = "Rename File",
      label = "Filename",
      initialValue = state.fileName,
      confirmLabel = "Apply",
      onConfirm = { newName ->
        showRenameDialog = false
        onRename(newName)
      },
      onDismiss = { showRenameDialog = false },
    )
  }
}
