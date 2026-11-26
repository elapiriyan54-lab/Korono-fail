package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Xml
import com.github.junrar.Archive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object OfficeDocEngine {

  private fun openReadPfd(context: Context, filePath: String, contentUri: String?): ParcelFileDescriptor? {
    val file = File(filePath)
    if (file.exists() && file.canRead()) {
      return runCatching { ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) }.getOrNull()
    }
    if (!contentUri.isNullOrBlank()) {
      return runCatching { context.contentResolver.openFileDescriptor(Uri.parse(contentUri), "r") }.getOrNull()
    }
    return null
  }

  private fun openReadStream(context: Context, filePath: String, contentUri: String?): InputStream? {
    val file = File(filePath)
    if (file.exists() && file.canRead()) {
      return runCatching { FileInputStream(file) }.getOrNull()
    }
    if (!contentUri.isNullOrBlank()) {
      return runCatching { context.contentResolver.openInputStream(Uri.parse(contentUri)) }.getOrNull()
    }
    return null
  }

  suspend fun getPdfPageCount(context: Context, filePath: String, contentUri: String? = null): Int =
    withContext(Dispatchers.IO) {
      runCatching {
        openReadPfd(context, filePath, contentUri)?.use { pfd ->
          PdfRenderer(pfd).use { renderer -> renderer.pageCount }
        } ?: 0
      }.getOrDefault(0)
    }

  suspend fun renderPdfPage(
    context: Context,
    filePath: String,
    contentUri: String? = null,
    pageIndex: Int,
  ): Bitmap? =
    withContext(Dispatchers.IO) {
      runCatching {
        openReadPfd(context, filePath, contentUri)?.use { pfd ->
          PdfRenderer(pfd).use { renderer ->
            if (pageIndex !in 0 until renderer.pageCount) return@withContext null
            renderer.openPage(pageIndex).use { page ->
              val scale = if (page.width * page.height > 1_200_000) 1 else 2
              val bmp =
                Bitmap.createBitmap(
                  (page.width * scale).coerceAtLeast(1),
                  (page.height * scale).coerceAtLeast(1),
                  Bitmap.Config.ARGB_8888,
                )
              bmp.eraseColor(Color.WHITE)
              page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
              bmp
            }
          }
        }
      }.getOrNull()
    }

  suspend fun readTextContent(context: Context, filePath: String, contentUri: String? = null): String =
    withContext(Dispatchers.IO) {
      runCatching {
        openReadStream(context, filePath, contentUri)?.bufferedReader()?.use { it.readText() }.orEmpty()
      }.getOrDefault("")
    }

  suspend fun renderPdfPages(file: File): List<Bitmap> =
    withContext(Dispatchers.IO) {
      val bitmaps = mutableListOf<Bitmap>()
      runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
          PdfRenderer(pfd).use { renderer ->
            for (i in 0 until renderer.pageCount) {
              renderer.openPage(i).use { page ->
                val scale = if (page.width * page.height > 1_200_000) 1 else 2
                val bmp =
                  Bitmap.createBitmap(
                    page.width * scale,
                    page.height * scale,
                    Bitmap.Config.ARGB_8888,
                  )
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bmp)
              }
            }
          }
        }
      }
      bitmaps
    }

  suspend fun exportTextToPdf(title: String, content: String, targetFile: File): Boolean =
    withContext(Dispatchers.IO) {
      writePdfSync(title, content, targetFile)
    }

  suspend fun readDocxText(context: Context, filePath: String, contentUri: String? = null): String =
    withContext(Dispatchers.IO) {
      runCatching {
          val stream = openReadStream(context, filePath, contentUri) ?: return@withContext ""
          ZipInputStream(stream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
              if (entry.name == "word/document.xml") {
                val parser = Xml.newPullParser()
                parser.setInput(zis, "UTF-8")
                val paragraphs = mutableListOf<String>()
                var currentParagraph = StringBuilder()
                var inText = false
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                  when (eventType) {
                    XmlPullParser.START_TAG -> {
                      if (parser.name == "p" || parser.name == "w:p") {
                        currentParagraph = StringBuilder()
                      } else if (parser.name == "t" || parser.name == "w:t") {
                        inText = true
                      }
                    }
                    XmlPullParser.TEXT -> {
                      if (inText) {
                        currentParagraph.append(parser.text)
                      }
                    }
                    XmlPullParser.END_TAG -> {
                      if (parser.name == "t" || parser.name == "w:t") {
                        inText = false
                      } else if (parser.name == "p" || parser.name == "w:p") {
                        paragraphs.add(currentParagraph.toString())
                      }
                    }
                  }
                  eventType = parser.next()
                }
                return@withContext paragraphs.joinToString("\n")
              }
              entry = zis.nextEntry
            }
          }
          ""
        }
        .getOrElse { "" }
    }

  suspend fun writeDocxText(file: File, content: String): Boolean =
    withContext(Dispatchers.IO) {
      runCatching {
          file.parentFile?.mkdirs()
          val paragraphsXml =
            content.replace("\r\n", "\n").split("\n").joinToString("") { line ->
              val escaped = escapeXml(line)
              "<w:p><w:r><w:t xml:space=\"preserve\">$escaped</w:t></w:r></w:p>"
            }

          val documentXml =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    $paragraphsXml
    <w:sectPr/>
  </w:body>
</w:document>"""

          val contentTypesXml =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

          val relsXml =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

          ZipOutputStream(FileOutputStream(file)).use { zos ->
            putZipEntry(zos, "[Content_Types].xml", contentTypesXml)
            putZipEntry(zos, "_rels/.rels", relsXml)
            putZipEntry(zos, "word/document.xml", documentXml)
          }
          true
        }
        .getOrDefault(false)
    }

  suspend fun readXlsxGrid(context: Context, filePath: String, contentUri: String? = null): List<List<String>> =
    withContext(Dispatchers.IO) {
      runCatching {
          val sharedStrings = mutableListOf<String>()
          var sheetXmlBytes: ByteArray? = null

          val stream = openReadStream(context, filePath, contentUri) ?: return@withContext defaultEmptyGrid()
          ZipInputStream(stream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
              when (entry.name) {
                "xl/sharedStrings.xml" -> {
                  val bytes = zis.readBytes()
                  sharedStrings.addAll(parseSharedStrings(bytes))
                }
                "xl/worksheets/sheet1.xml" -> {
                  sheetXmlBytes = zis.readBytes()
                }
              }
              entry = zis.nextEntry
            }
          }

          val rows = mutableListOf<MutableList<String>>()
          val bytes = sheetXmlBytes ?: return@withContext defaultEmptyGrid()
          val parser = Xml.newPullParser()
          parser.setInput(bytes.inputStream(), "UTF-8")

          var currentRow = mutableListOf<String>()
          var cellType: String? = null
          var cellRef = ""
          var inValue = false
          var inInlineText = false
          var valueBuilder = StringBuilder()

          var eventType = parser.eventType
          while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
              XmlPullParser.START_TAG -> {
                when (parser.name) {
                  "row" -> currentRow = mutableListOf()
                  "c" -> {
                    cellType = parser.getAttributeValue(null, "t")
                    cellRef = parser.getAttributeValue(null, "r") ?: ""
                    valueBuilder = StringBuilder()
                  }
                  "v" -> inValue = true
                  "t" -> inInlineText = true
                }
              }
              XmlPullParser.TEXT -> {
                if (inValue || inInlineText) valueBuilder.append(parser.text)
              }
              XmlPullParser.END_TAG -> {
                when (parser.name) {
                  "v" -> inValue = false
                  "t" -> inInlineText = false
                  "c" -> {
                    val raw = valueBuilder.toString()
                    val resolved =
                      if (cellType == "s") {
                        raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: raw
                      } else {
                        raw
                      }
                    val colIndex = columnIndexFromRef(cellRef)
                    while (currentRow.size <= colIndex) currentRow.add("")
                    if (colIndex >= 0) currentRow[colIndex] = resolved
                  }
                  "row" -> rows.add(currentRow)
                }
              }
            }
            eventType = parser.next()
          }

          val maxCols = maxOf(4, rows.maxOfOrNull { it.size } ?: 4)
          val normalized =
            rows.map { row ->
              val padded = row.toMutableList()
              while (padded.size < maxCols) padded.add("")
              padded.toList()
            }.toMutableList()
          while (normalized.size < 8) {
            normalized.add(List(maxCols) { "" })
          }
          normalized.toList()
        }
        .getOrElse { defaultEmptyGrid() }
    }

  suspend fun writeXlsxGrid(file: File, grid: List<List<String>>): Boolean =
    withContext(Dispatchers.IO) {
      runCatching {
          file.parentFile?.mkdirs()
          val sharedStrings = mutableListOf<String>()
          val stringIndexMap = mutableMapOf<String, Int>()

          val rowsXml = StringBuilder()
          grid.forEachIndexed { rIdx, row ->
            val rowNum = rIdx + 1
            rowsXml.append("<row r=\"$rowNum\">")
            row.forEachIndexed { cIdx, value ->
              val ref = "${columnLetter(cIdx)}$rowNum"
              val numeric = value.toDoubleOrNull()
              if (numeric != null && value.isNotBlank()) {
                rowsXml.append("<c r=\"$ref\"><v>$value</v></c>")
              } else if (value.isNotEmpty()) {
                val idx =
                  stringIndexMap.getOrPut(value) {
                    sharedStrings.add(value)
                    sharedStrings.lastIndex
                  }
                rowsXml.append("<c r=\"$ref\" t=\"s\"><v>$idx</v></c>")
              }
            }
            rowsXml.append("</row>")
          }

          val sharedStringsXml =
            buildString {
              append(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${sharedStrings.size}" uniqueCount="${sharedStrings.size}">"""
              )
              for (str in sharedStrings) {
                append("<si><t xml:space=\"preserve\">${escapeXml(str)}</t></si>")
              }
              append("</sst>")
            }

          val sheetXml =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
    $rowsXml
  </sheetData>
</worksheet>"""

          val workbookXml =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

          val workbookRelsXml =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""

          val rootRelsXml =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

          val contentTypesXml =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>"""

          ZipOutputStream(FileOutputStream(file)).use { zos ->
            putZipEntry(zos, "[Content_Types].xml", contentTypesXml)
            putZipEntry(zos, "_rels/.rels", rootRelsXml)
            putZipEntry(zos, "xl/workbook.xml", workbookXml)
            putZipEntry(zos, "xl/_rels/workbook.xml.rels", workbookRelsXml)
            putZipEntry(zos, "xl/sharedStrings.xml", sharedStringsXml)
            putZipEntry(zos, "xl/worksheets/sheet1.xml", sheetXml)
          }
          true
        }
        .getOrDefault(false)
    }

  fun printFileOffline(
    context: Context,
    jobName: String,
    file: File,
    contentUri: String? = null,
    textFallback: String? = null,
  ) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val adapter =
      object : PrintDocumentAdapter() {
        override fun onLayout(
          oldAttributes: PrintAttributes?,
          newAttributes: PrintAttributes,
          cancellationSignal: CancellationSignal?,
          callback: LayoutResultCallback,
          extras: Bundle?,
        ) {
          if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
          }
          val info =
            PrintDocumentInfo.Builder(jobName)
              .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
              .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
              .build()
          callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
          pages: Array<out PageRange>?,
          destination: ParcelFileDescriptor,
          cancellationSignal: CancellationSignal?,
          callback: WriteResultCallback,
        ) {
          val tempPdf = File(context.cacheDir, "print_temp_${System.currentTimeMillis()}.pdf")
          try {
            val isPdf = jobName.endsWith(".pdf", ignoreCase = true) || file.extension.equals("pdf", ignoreCase = true)
            val stream = if (isPdf) openReadStream(context, file.absolutePath, contentUri) else null
            if (stream != null) {
              stream.use { input ->
                FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
              }
            } else {
              val text = textFallback ?: if (file.exists()) file.readText() else ""
              val ok = writePdfSync(jobName, text, tempPdf)
              if (ok && tempPdf.exists()) {
                FileInputStream(tempPdf).use { input ->
                  FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
                }
              }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
          } catch (e: Exception) {
            callback.onWriteFailed(e.message)
          } finally {
            runCatching { tempPdf.delete() }
          }
        }
      }
    printManager.print(jobName, adapter, null)
  }

  private fun writePdfSync(title: String, content: String, targetFile: File): Boolean {
    return runCatching {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 44f
        val titlePaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1020")
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          }
        val bodyPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 11.5f
            typeface = Typeface.MONOSPACE
          }
        val footerPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 9f
          }

        val rawLines = content.replace("\r\n", "\n").split("\n")
        val wrappedLines = mutableListOf<String>()
        val maxLineWidth = pageWidth - margin * 2
        for (line in rawLines) {
          if (line.isEmpty()) {
            wrappedLines.add("")
          } else {
            var start = 0
            while (start < line.length) {
              val count = bodyPaint.breakText(line, start, line.length, true, maxLineWidth, null)
              val end = (start + maxOf(1, count)).coerceAtMost(line.length)
              wrappedLines.add(line.substring(start, end))
              start = end
            }
          }
        }
        if (wrappedLines.isEmpty()) wrappedLines.add("")

        val lineHeight = 16f
        val firstPageLines = ((pageHeight - margin * 2 - 64f) / lineHeight).toInt().coerceAtLeast(1)
        val otherPageLines = ((pageHeight - margin * 2 - 36f) / lineHeight).toInt().coerceAtLeast(1)
        var pageIndex = 0
        var lineIndex = 0

        while (lineIndex < wrappedLines.size) {
          pageIndex++
          val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
          val page = pdf.startPage(pageInfo)
          val canvas: Canvas = page.canvas
          canvas.drawColor(Color.WHITE)

          var y = margin + 12f
          val limit = if (pageIndex == 1) firstPageLines else otherPageLines
          if (pageIndex == 1) {
            canvas.drawText(title, margin, y, titlePaint)
            y += 28f
          }

          var drawn = 0
          while (lineIndex < wrappedLines.size && drawn < limit) {
            canvas.drawText(wrappedLines[lineIndex], margin, y, bodyPaint)
            y += lineHeight
            lineIndex++
            drawn++
          }

          canvas.drawText(
            "KronoFiles Offline Studio • Page $pageIndex",
            margin,
            pageHeight - 24f,
            footerPaint,
          )
          pdf.finishPage(page)
        }

        targetFile.parentFile?.mkdirs()
        FileOutputStream(targetFile).use { fos -> pdf.writeTo(fos) }
        pdf.close()
        true
      }
      .getOrDefault(false)
  }

  fun listRarEntries(file: File): List<String> {
    return runCatching {
      Archive(file).use { archive ->
        archive.fileHeaders.map { header -> header.fileName.replace('\\', '/') }
      }
    }.getOrDefault(emptyList())
  }

  fun extractRar(file: File, outDir: File): Boolean {
    return runCatching {
      outDir.mkdirs()
      val canonicalRoot = outDir.canonicalPath
      Archive(file).use { archive ->
        var header = archive.nextFileHeader()
        while (header != null) {
          val normalized = header.fileName.replace('\\', '/').trimStart('/')
          if (normalized.isNotEmpty()) {
            val outFile = File(outDir, normalized)
            require(outFile.canonicalPath.startsWith(canonicalRoot)) { "Invalid RAR entry path" }
            if (header.isDirectory) {
              outFile.mkdirs()
            } else {
              outFile.parentFile?.mkdirs()
              FileOutputStream(outFile).use { fos ->
                archive.extractFile(header, fos)
              }
            }
          }
          header = archive.nextFileHeader()
        }
      }
      true
    }.getOrDefault(false)
  }

  fun markdownToStyledHtml(markdown: String, title: String): String {
    val lines = markdown.replace("\r\n", "\n").split("\n")
    val body = StringBuilder()
    var inList = false
    var inCode = false
    for (raw in lines) {
      val trimmed = raw.trim()
      if (trimmed.startsWith("```")) {
        if (inList) {
          body.append("</ul>")
          inList = false
        }
        if (inCode) {
          body.append("</pre>")
          inCode = false
        } else {
          body.append("<pre>")
          inCode = true
        }
        continue
      }
      if (inCode) {
        body.append(escapeHtml(raw)).append("\n")
        continue
      }
      when {
        trimmed.startsWith("### ") -> {
          if (inList) { body.append("</ul>"); inList = false }
          body.append("<h3>").append(formatInlineMd(trimmed.drop(4))).append("</h3>")
        }
        trimmed.startsWith("## ") -> {
          if (inList) { body.append("</ul>"); inList = false }
          body.append("<h2>").append(formatInlineMd(trimmed.drop(3))).append("</h2>")
        }
        trimmed.startsWith("# ") -> {
          if (inList) { body.append("</ul>"); inList = false }
          body.append("<h1>").append(formatInlineMd(trimmed.drop(2))).append("</h1>")
        }
        trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
          if (!inList) { body.append("<ul>"); inList = true }
          body.append("<li>").append(formatInlineMd(trimmed.drop(2))).append("</li>")
        }
        trimmed.isEmpty() -> {
          if (inList) { body.append("</ul>"); inList = false }
          body.append("<br/>")
        }
        else -> {
          if (inList) { body.append("</ul>"); inList = false }
          body.append("<p>").append(formatInlineMd(raw)).append("</p>")
        }
      }
    }
    if (inList) body.append("</ul>")
    if (inCode) body.append("</pre>")

    return """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>${escapeHtml(title)}</title>
<style>
  body { background: #09222C; color: #F8FBFF; font-family: sans-serif; padding: 18px; line-height: 1.6; }
  h1, h2, h3 { color: #38BDF8; border-bottom: 1px solid rgba(56,189,248,0.30); padding-bottom: 6px; }
  code { background: rgba(56,189,248,0.18); color: #FBBF24; padding: 2px 6px; border-radius: 6px; font-family: monospace; }
  pre { background: #0F313E; border: 1px solid rgba(251,191,36,0.42); padding: 12px; border-radius: 12px; overflow-x: auto; color: #A5F3FC; }
  li { margin: 6px 0; }
  strong { color: #FFA552; }
</style>
</head>
<body>$body</body>
</html>"""
  }

  private fun formatInlineMd(text: String): String {
    var s = escapeHtml(text)
    s = Regex("\\*\\*(.+?)\\*\\*").replace(s) { "<strong>${it.groupValues[1]}</strong>" }
    s = Regex("`(.+?)`").replace(s) { "<code>${it.groupValues[1]}</code>" }
    return s
  }

  private fun escapeHtml(s: String): String =
    s.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")

  private fun parseSharedStrings(bytes: ByteArray): List<String> {
    val result = mutableListOf<String>()
    val parser = Xml.newPullParser()
    parser.setInput(bytes.inputStream(), "UTF-8")
    var inSi = false
    var inT = false
    var sb = StringBuilder()
    var eventType = parser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT) {
      when (eventType) {
        XmlPullParser.START_TAG -> {
          if (parser.name == "si") {
            inSi = true
            sb = StringBuilder()
          } else if (parser.name == "t" && inSi) {
            inT = true
          }
        }
        XmlPullParser.TEXT -> if (inT) sb.append(parser.text)
        XmlPullParser.END_TAG -> {
          if (parser.name == "t") inT = false
          else if (parser.name == "si") {
            result.add(sb.toString())
            inSi = false
          }
        }
      }
      eventType = parser.next()
    }
    return result
  }

  private fun columnIndexFromRef(ref: String): Int {
    val letters = ref.takeWhile { it.isLetter() }.uppercase()
    if (letters.isEmpty()) return 0
    var col = 0
    for (ch in letters) {
      col = col * 26 + (ch - 'A' + 1)
    }
    return (col - 1).coerceAtLeast(0)
  }

  private fun columnLetter(index: Int): String {
    var n = index + 1
    val sb = StringBuilder()
    while (n > 0) {
      val rem = (n - 1) % 26
      sb.append(('A'.code + rem).toChar())
      n = (n - 1) / 26
    }
    return sb.reverse().toString()
  }

  private fun defaultEmptyGrid(): List<List<String>> =
    listOf(
      listOf("Column A", "Column B", "Column C", "Column D"),
      listOf("", "", "", ""),
      listOf("", "", "", ""),
      listOf("", "", "", ""),
      listOf("", "", "", ""),
      listOf("", "", "", ""),
    )

  private fun putZipEntry(zos: ZipOutputStream, name: String, content: String) {
    zos.putNextEntry(ZipEntry(name))
    zos.write(content.toByteArray(Charsets.UTF_8))
    zos.closeEntry()
  }

  private fun escapeXml(s: String): String =
    s.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")
}
