package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.storage.OfficeDocEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class StudioMode {
  TEXT_CODE,
  DOCX,
  XLSX,
  PDF_VIEW,
}

data class DocStudioUiState(
  val fileName: String = "Untitled.txt",
  val filePath: String = "",
  val contentUri: String? = null,
  val extension: String = "txt",
  val mode: StudioMode = StudioMode.TEXT_CODE,
  val textContent: String = "",
  val undoStack: List<String> = emptyList(),
  val redoStack: List<String> = emptyList(),
  val sheetGrid: List<List<String>> = emptyList(),
  val pdfPageCount: Int = 0,
  val showCodePreview: Boolean = false,
  val isLoading: Boolean = false,
  val statusMessage: String? = null,
)

class DocStudioViewModel(application: Application) : AndroidViewModel(application) {
  private val _state = MutableStateFlow(DocStudioUiState())
  val state: StateFlow<DocStudioUiState> = _state.asStateFlow()

  private val pdfPageCache = mutableMapOf<Int, Bitmap>()

  private fun notifyMediaScanner(path: String) {
    if (path.isNotBlank() && !path.startsWith("content://")) {
      runCatching {
        MediaScannerConnection.scanFile(getApplication(), arrayOf(path), null, null)
      }
    }
  }

  private fun resolveActiveRoot(): File {
    val app = getApplication<Application>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      if (Environment.isExternalStorageManager()) {
        val ext = Environment.getExternalStorageDirectory()
        if (ext.exists() && ext.canWrite()) return ext
      }
    } else {
      val hasWrite =
        androidx.core.content.ContextCompat.checkSelfPermission(
          app,
          android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
      if (hasWrite) {
        val ext = Environment.getExternalStorageDirectory()
        if (ext.exists() && ext.canWrite()) return ext
      }
    }
    return File(app.filesDir, "KronoStorage").apply { mkdirs() }
  }

  private fun activeDocsDir(): File = File(resolveActiveRoot(), "Documents").apply { mkdirs() }

  suspend fun loadPdfPage(pageIndex: Int): Bitmap? {
    pdfPageCache[pageIndex]?.let { cached ->
      if (!cached.isRecycled) return cached
    }
    val s = _state.value
    val bmp = OfficeDocEngine.renderPdfPage(getApplication(), s.filePath, s.contentUri, pageIndex)
    if (bmp != null) {
      if (pdfPageCache.size >= 8) {
        val evictKey = pdfPageCache.keys.firstOrNull { it != pageIndex }
        if (evictKey != null) pdfPageCache.remove(evictKey)
      }
      pdfPageCache[pageIndex] = bmp
    }
    return bmp
  }

  fun openOrCreate(
    filePath: String,
    newExtension: String,
    targetFolder: String,
    contentUri: String? = null,
    displayName: String? = null,
  ) {
    viewModelScope.launch {
      pdfPageCache.clear()
      _state.update {
        it.copy(
          isLoading = true,
          pdfPageCount = 0,
          showCodePreview = false,
          contentUri = contentUri,
        )
      }
      val file = File(filePath)
      val hasLocalFile = filePath.isNotBlank() && file.exists()
      val hasUri = !contentUri.isNullOrBlank()
      if (hasLocalFile || hasUri) {
        val resolvedName =
          displayName?.takeIf { it.isNotBlank() }
            ?: file.name.ifBlank { "Document.${newExtension.ifBlank { "txt" }}" }
        val ext =
          resolvedName.substringAfterLast('.', newExtension.ifBlank { "txt" }).lowercase()
        when (ext) {
          "pdf" -> {
            val pageCount = OfficeDocEngine.getPdfPageCount(getApplication(), filePath, contentUri)
            _state.update {
              it.copy(
                fileName = resolvedName,
                filePath = filePath,
                contentUri = contentUri,
                extension = ext,
                mode = StudioMode.PDF_VIEW,
                pdfPageCount = pageCount,
                isLoading = false,
              )
            }
          }
          "docx" -> {
            val text = OfficeDocEngine.readDocxText(getApplication(), filePath, contentUri)
            _state.update {
              it.copy(
                fileName = resolvedName,
                filePath = filePath,
                contentUri = contentUri,
                extension = ext,
                mode = StudioMode.DOCX,
                textContent = text,
                undoStack = emptyList(),
                redoStack = emptyList(),
                isLoading = false,
              )
            }
          }
          "xlsx" -> {
            val grid = OfficeDocEngine.readXlsxGrid(getApplication(), filePath, contentUri)
            _state.update {
              it.copy(
                fileName = resolvedName,
                filePath = filePath,
                contentUri = contentUri,
                extension = ext,
                mode = StudioMode.XLSX,
                sheetGrid = grid,
                isLoading = false,
              )
            }
          }
          else -> {
            val text = OfficeDocEngine.readTextContent(getApplication(), filePath, contentUri)
            _state.update {
              it.copy(
                fileName = resolvedName,
                filePath = filePath,
                contentUri = contentUri,
                extension = ext,
                mode = StudioMode.TEXT_CODE,
                textContent = text,
                undoStack = emptyList(),
                redoStack = emptyList(),
                isLoading = false,
              )
            }
          }
        }
        return@launch
      }

      // Create new offline document
      val baseRoot = resolveActiveRoot()
      val folderDir =
        if (targetFolder.isBlank()) activeDocsDir()
        else File(baseRoot, targetFolder).apply { mkdirs() }
      val ext = newExtension.lowercase().ifBlank { "txt" }
      val stamp = System.currentTimeMillis() % 10000
      val name = "Krono_Note_$stamp.$ext"
      val newFile = File(folderDir, name)
      val starterText =
        when (ext) {
          "html" -> "<!DOCTYPE html>\n<html>\n<head><title>KronoPage</title></head>\n<body>\n  <h1>Hello KronoFiles</h1>\n</body>\n</html>"
          "css" -> ".glass {\n  background: rgba(11, 14, 23, 0.7);\n  border: 1px solid #00E5FF;\n}"
          "js" -> "// Offline JavaScript\nconsole.log('KronoFiles Studio Ready');"
          "md" -> "# New Offline Note\n\n- Created inside KronoFiles Studio"
          else -> "New offline document created in KronoFiles Studio."
        }
      val mode =
        when (ext) {
          "docx" -> StudioMode.DOCX
          "xlsx" -> StudioMode.XLSX
          "pdf" -> StudioMode.TEXT_CODE // Write text then save/export directly to PDF!
          else -> StudioMode.TEXT_CODE
        }
      val defaultGrid =
        listOf(
          listOf("Column A", "Column B", "Column C", "Total"),
          listOf("", "", "", ""),
          listOf("", "", "", ""),
          listOf("", "", "", ""),
          listOf("", "", "", ""),
        )
      _state.update {
        it.copy(
          fileName = name,
          filePath = newFile.absolutePath,
          contentUri = null,
          extension = ext,
          mode = mode,
          textContent = starterText,
          sheetGrid = if (mode == StudioMode.XLSX) defaultGrid else emptyList(),
          pdfPageCount = 0,
          undoStack = emptyList(),
          redoStack = emptyList(),
          isLoading = false,
        )
      }
    }
  }

  fun updateFileName(newName: String) {
    val clean = newName.trim()
    if (clean.isEmpty()) return
    val current = File(_state.value.filePath)
    val parent = current.parentFile ?: activeDocsDir()
    val updated = File(parent, clean)
    val updatedExt = clean.substringAfterLast('.', _state.value.extension).lowercase()
    _state.update { it.copy(fileName = clean, filePath = updated.absolutePath, extension = updatedExt) }
  }

  fun onTextChange(newText: String) {
    val current = _state.value.textContent
    if (current == newText) return
    _state.update {
      it.copy(
        textContent = newText,
        undoStack = (it.undoStack + current).takeLast(30),
        redoStack = emptyList(),
      )
    }
  }

  fun undo() {
    val stack = _state.value.undoStack
    if (stack.isEmpty()) return
    val prev = stack.last()
    _state.update {
      it.copy(
        textContent = prev,
        undoStack = stack.dropLast(1),
        redoStack = it.redoStack + it.textContent,
      )
    }
  }

  fun redo() {
    val stack = _state.value.redoStack
    if (stack.isEmpty()) return
    val next = stack.last()
    _state.update {
      it.copy(
        textContent = next,
        redoStack = stack.dropLast(1),
        undoStack = it.undoStack + it.textContent,
      )
    }
  }

  fun updateSheetCell(row: Int, col: Int, value: String) {
    val grid = _state.value.sheetGrid.map { it.toMutableList() }.toMutableList()
    if (row in grid.indices && col in grid[row].indices) {
      grid[row][col] = value
      _state.update { it.copy(sheetGrid = grid.map { r -> r.toList() }) }
    }
  }

  fun addSheetRow() {
    val cols = _state.value.sheetGrid.firstOrNull()?.size ?: 4
    _state.update { it.copy(sheetGrid = it.sheetGrid + listOf(List(cols) { "" })) }
  }

  fun togglePreviewMode() {
    _state.update { it.copy(showCodePreview = !it.showCodePreview) }
  }

  fun formatTextSnippet() {
    val current = _state.value.textContent
    val formatted =
      current.lines().joinToString("\n") { it.trimEnd() }.trim() + "\n"
    onTextChange(formatted)
    _state.update { it.copy(statusMessage = "Whitespace & line endings formatted") }
  }

  fun saveCurrentFile(onSaved: () -> Unit = {}) {
    viewModelScope.launch {
      val s = _state.value
      val rawFile =
        if (s.filePath.isNotBlank() && !s.filePath.startsWith("content://")) File(s.filePath)
        else File(activeDocsDir(), s.fileName)
      val targetFile =
        if (rawFile.parentFile?.canWrite() == true || !rawFile.exists() && rawFile.parentFile?.mkdirs() == true) rawFile
        else File(activeDocsDir(), s.fileName)

      val ok =
        when (s.mode) {
          StudioMode.DOCX -> OfficeDocEngine.writeDocxText(targetFile, s.textContent)
          StudioMode.XLSX -> OfficeDocEngine.writeXlsxGrid(targetFile, s.sheetGrid)
          StudioMode.PDF_VIEW -> true
          StudioMode.TEXT_CODE -> {
            if (s.extension == "pdf") {
              val saved = OfficeDocEngine.exportTextToPdf(s.fileName, s.textContent, targetFile)
              if (saved) {
                pdfPageCache.clear()
                val pageCount = OfficeDocEngine.getPdfPageCount(getApplication(), targetFile.absolutePath, null)
                _state.update {
                  it.copy(
                    filePath = targetFile.absolutePath,
                    pdfPageCount = pageCount,
                    mode = StudioMode.PDF_VIEW,
                  )
                }
              }
              saved
            } else {
              kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    targetFile.parentFile?.mkdirs()
                    targetFile.writeText(s.textContent)
                    true
                  }
                  .getOrDefault(false)
              }
            }
          }
        }
      if (ok && s.mode != StudioMode.PDF_VIEW) {
        notifyMediaScanner(targetFile.absolutePath)
      }
      _state.update {
        it.copy(statusMessage = if (ok) "Saved ${targetFile.name} offline" else "Save failed")
      }
      if (ok) onSaved()
    }
  }

  fun exportAsPdf(onExported: (File) -> Unit = {}) {
    viewModelScope.launch {
      val s = _state.value
      val currentFile = File(s.filePath.ifBlank { s.fileName })
      val targetParent =
        currentFile.parentFile?.takeIf { it.exists() && it.canWrite() } ?: activeDocsDir()
      val pdfTarget =
        File(
          targetParent,
          "${currentFile.nameWithoutExtension.ifBlank { "Document" }}_Export.pdf",
        )
      val exportText =
        if (s.mode == StudioMode.XLSX) {
          s.sheetGrid.joinToString("\n") { row -> row.joinToString("  |  ") }
        } else {
          s.textContent
        }
      val ok = OfficeDocEngine.exportTextToPdf(s.fileName, exportText, pdfTarget)
      if (ok) {
        notifyMediaScanner(pdfTarget.absolutePath)
      }
      _state.update {
        it.copy(
          statusMessage =
            if (ok) "Exported PDF: ${pdfTarget.name}" else "PDF export failed",
        )
      }
      if (ok) onExported(pdfTarget)
    }
  }

  fun printDocument(context: Context) {
    val s = _state.value
    val file = File(s.filePath)
    val fallback =
      if (s.mode == StudioMode.XLSX) {
        s.sheetGrid.joinToString("\n") { it.joinToString(" | ") }
      } else {
        s.textContent
      }
    OfficeDocEngine.printFileOffline(
      context = context,
      jobName = s.fileName,
      file = file,
      contentUri = s.contentUri,
      textFallback = fallback,
    )
  }

  fun consumeStatus() {
    _state.update { it.copy(statusMessage = null) }
  }

  override fun onCleared() {
    super.onCleared()
    pdfPageCache.clear()
  }
}
