package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.example.data.local.KronoDatabase
import com.example.data.local.KronoPreferences
import com.example.data.local.NotificationEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackEntity
import com.example.model.NotificationCategory
import com.example.model.NotificationSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.PI
import kotlin.math.sin

object KronoFileSeeder {
  const val SEED_VERSION = "kronofiles_v1"

  private val seededRelativeFiles =
    listOf(
      "DCIM/Camera/Neon_Horizon_Capture.png",
      "DCIM/Camera/Obsidian_Skyline_Shot.png",
      "Pictures/Cyber_Vinyl_Cover.png",
      "Downloads/Neon_Horizon_Capture_Copy.png",
      "Music/Yesterday's Echo.wav",
      "Music/Chinna Chinna Aasai.wav",
      "Music/Roja Roja.wav",
      "Music/Kannukkul Nooru Nilava.wav",
      "Music/Puthu Vellai Mazhai.wav",
      "Movies/Krono_Neon_Visualizer.mp4",
      "Documents/KronoFiles_Architecture_Brief.pdf",
      "Documents/Field_Operations_Report.docx",
      "Documents/Storage_Audit_Sheet.xlsx",
      "Documents/glass_preview.html",
      "Documents/theme.css",
      "Documents/controller.js",
      "Documents/Changelog.md",
      "Archives/Project_Assets_Backup.zip",
      "Downloads/KronoFiles_Architecture_Brief_dup.pdf",
      "Downloads/Offline_Media_Pack_HD.zip",
      "Apps/KronoSync_Companion_v2.4.apk",
    )

  private val seededDirs =
    listOf("DCIM/Camera", "DCIM", "Pictures", "Music", "Movies", "Documents", "Archives", "Downloads", "Apps")

  fun removeSeededDemoFiles(rootDir: File) {
    if (!rootDir.exists()) return
    seededRelativeFiles.forEach { rel ->
      val f = File(rootDir, rel)
      if (f.exists() && f.isFile) {
        runCatching { f.delete() }
      }
    }
    seededDirs.forEach { rel ->
      val d = File(rootDir, rel)
      if (d.exists() && d.isDirectory && (d.listFiles()?.isEmpty() == true)) {
        runCatching { d.delete() }
      }
    }
  }

  suspend fun ensureSeeded(
    context: Context,
    rootDir: File,
    database: KronoDatabase,
    prefs: KronoPreferences,
  ) =
    withContext(Dispatchers.IO) {
      val currentSeed = prefs.seededVersion.first()
      if (currentSeed == SEED_VERSION) {
        return@withContext
      }

      rootDir.mkdirs()
      val dcimCamera = File(rootDir, "DCIM/Camera").apply { mkdirs() }
      val pictures = File(rootDir, "Pictures").apply { mkdirs() }
      val music = File(rootDir, "Music").apply { mkdirs() }
      val movies = File(rootDir, "Movies").apply { mkdirs() }
      val documents = File(rootDir, "Documents").apply { mkdirs() }
      val downloads = File(rootDir, "Downloads").apply { mkdirs() }
      val archives = File(rootDir, "Archives").apply { mkdirs() }
      val apps = File(rootDir, "Apps").apply { mkdirs() }

      val now = System.currentTimeMillis()
      val dayMs = 24L * 60L * 60L * 1000L

      // 1. Real PNG Artwork Photos (and intentional duplicate for Smart Cleanup!)
      val photo1 = File(dcimCamera, "Neon_Horizon_Capture.png")
      writeNeonArtPng(photo1, 0xFF00E5FF.toInt(), 0xFF8B5CF6.toInt(), "KRONO // 01")
      photo1.setLastModified(now - 2 * 60 * 60 * 1000L) // Today

      val photo2 = File(dcimCamera, "Obsidian_Skyline_Shot.png")
      writeNeonArtPng(photo2, 0xFFFF9E44.toInt(), 0xFF00E5FF.toInt(), "SKYLINE // 02")
      photo2.setLastModified(now - dayMs - 3600_000L) // Yesterday

      val photo3 = File(pictures, "Cyber_Vinyl_Cover.png")
      writeNeonArtPng(photo3, 0xFF8B5CF6.toInt(), 0xFFFF9E44.toInt(), "VINYL ART")
      photo3.setLastModified(now - 4 * dayMs) // This Week

      // Exact byte-for-byte duplicate in Downloads for Smart Cleanup demonstration
      val dupPhoto = File(downloads, "Neon_Horizon_Capture_Copy.png")
      photo1.copyTo(dupPhoto, overwrite = true)
      dupPhoto.setLastModified(now - 3 * 60 * 60 * 1000L)

      // 2. Real Playable Harmonic Audio Tracks (WAV PCM so Media3 ExoPlayer plays real sound!)
      val tracks =
        listOf(
          Triple("Yesterday's Echo.wav", 261.63, 18),
          Triple("Chinna Chinna Aasai.wav", 293.66, 16),
          Triple("Roja Roja.wav", 329.63, 20),
          Triple("Kannukkul Nooru Nilava.wav", 349.23, 15),
          Triple("Puthu Vellai Mazhai.wav", 392.00, 17),
        )
      val createdTracks = mutableListOf<File>()
      tracks.forEachIndexed { idx, (filename, baseFreq, secs) ->
        val f = File(music, filename)
        writeMelodicWav(f, baseFreq, secs)
        f.setLastModified(now - (idx * dayMs))
        createdTracks.add(f)
      }

      // 3. Real Offline Video Clip (.mp4 via MediaCodec + MediaMuxer)
      val videoFile = File(movies, "Krono_Neon_Visualizer.mp4")
      generateSampleMp4(videoFile)
      videoFile.setLastModified(now - 2 * dayMs)

      // 4. Real Documents, Code, Spreadsheets & PDFs
      val pdfFile = File(documents, "KronoFiles_Architecture_Brief.pdf")
      OfficeDocEngine.exportTextToPdf(
        title = "KronoFiles Offline Storage Architecture",
        content =
          """
          1. Overview
          KronoFiles is a 100% offline futuristic glassmorphism file manager.
          
          2. Core Modules
          - Chronos Timeline: Date-grouped media and file stream.
          - Deterministic Smart Search: Zero-AI instant rule filtering.
          - Biometric Safe Vault: Hardware-backed local isolation.
          - Smart Cleanup: Manual-only SHA-256 duplicate & large file control.
          - Vinyl Audio & Video Studio: Media3 playback with gesture HUD.
          - Offline Office Studio: Create, edit, export PDF, and print offline.
          """
            .trimIndent(),
        targetFile = pdfFile,
      )
      pdfFile.setLastModified(now - 45 * 60 * 1000L)

      val docxFile = File(documents, "Field_Operations_Report.docx")
      OfficeDocEngine.writeDocxText(
        docxFile,
        "KronoFiles Offline Field Report\n\nAll local file indexing, PDF exporting, and SHA-256 duplicate audits run strictly offline on-device.\n\nEdit any paragraph here and export directly to PDF or Print.",
      )
      docxFile.setLastModified(now - 10 * dayMs)

      val xlsxFile = File(documents, "Storage_Audit_Sheet.xlsx")
      OfficeDocEngine.writeXlsxGrid(
        xlsxFile,
        listOf(
          listOf("Module", "Status", "Size MB", "Offline"),
          listOf("Chronos Stream", "Active", "18.4", "Yes"),
          listOf("Vinyl Hub", "Ready", "24.2", "Yes"),
          listOf("Safe Vault", "Locked", "12.0", "Yes"),
          listOf("Office Studio", "Ready", "4.8", "Yes"),
        ),
      )
      xlsxFile.setLastModified(now - 12 * dayMs)

      val htmlFile = File(documents, "glass_preview.html")
      htmlFile.writeText(
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <title>KronoFiles Glass Card</title>
          <link rel="stylesheet" href="theme.css" />
        </head>
        <body>
          <section class="glass-panel">
            <h1>KronoFiles Studio</h1>
            <p>100% Offline HTML / CSS / JS Workspace</p>
          </section>
          <script src="controller.js"></script>
        </body>
        </html>
        """
          .trimIndent()
      )
      htmlFile.setLastModified(now - 3 * 60 * 60 * 1000L)

      val cssFile = File(documents, "theme.css")
      cssFile.writeText(
        """
        .glass-panel {
          background: rgba(11, 14, 23, 0.72);
          border: 1px solid rgba(0, 229, 255, 0.45);
          border-radius: 20px;
          backdrop-filter: blur(18px);
          color: #F4F7FF;
          padding: 24px;
        }
        """
          .trimIndent()
      )
      cssFile.setLastModified(now - 6 * dayMs)

      val jsFile = File(documents, "controller.js")
      jsFile.writeText(
        """
        // Deterministic local file sorter
        function sortByChronos(items) {
          return [...items].sort((a, b) => b.modifiedMs - a.modifiedMs);
        }
        window.sortByChronos = sortByChronos;
        """
          .trimIndent()
      )
      jsFile.setLastModified(now - 40 * dayMs) // Earlier

      val mdFile = File(documents, "Changelog.md")
      mdFile.writeText(
        """
        # KronoFiles Offline Release Notes
        - Added Chronos Timeline rail grouping
        - Added Retro-Futuristic Vinyl Player with touch scrubbing
        - Added Landscape Left/Right Volume & Brightness gestures
        - Added 100% Offline PDF / DOCX / XLSX / Code Studio
        """
          .trimIndent()
      )
      mdFile.setLastModified(now - 2 * dayMs)

      // 5. Real ZIP Archive & Duplicate Document in Downloads + Large sample bundle
      val zipFile = File(archives, "Project_Assets_Backup.zip")
      ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
        zos.putNextEntry(ZipEntry("readme.txt"))
        zos.write("KronoFiles Backup Bundle — Created Offline\n".toByteArray())
        zos.closeEntry()
        zos.putNextEntry(ZipEntry("glass_preview.html"))
        zos.write(htmlFile.readBytes())
        zos.closeEntry()
      }
      zipFile.setLastModified(now - 5 * dayMs)

      // Intentional duplicate PDF in Downloads
      val dupPdf = File(downloads, "KronoFiles_Architecture_Brief_dup.pdf")
      pdfFile.copyTo(dupPdf, overwrite = true)
      dupPdf.setLastModified(now - 50 * 60 * 1000L)

      // Large sample archive/media cache in Downloads (~6.4 MB) for Large Files cleanup demo
      val largeArchive = File(downloads, "Offline_Media_Pack_HD.zip")
      ZipOutputStream(FileOutputStream(largeArchive)).use { zos ->
        zos.setLevel(0) // Store uncompressed so size is cleanly ~6 MB for Large File cleanup list
        zos.putNextEntry(ZipEntry("ambient_pack.bin"))
        val block = ByteArray(64 * 1024) { (it % 251).toByte() }
        repeat(96) { zos.write(block) }
        zos.closeEntry()
      }
      largeArchive.setLastModified(now - 14 * dayMs)

      // 6. APK Archive Package entry (read-only metadata inspection, no install permission)
      val apkStub = File(apps, "KronoSync_Companion_v2.4.apk")
      ZipOutputStream(FileOutputStream(apkStub)).use { zos ->
        zos.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
        zos.write("Manifest-Version: 1.0\nPackage: com.kronofiles.companion\nVersion: 2.4.0\n".toByteArray())
        zos.closeEntry()
      }
      apkStub.setLastModified(now - 45 * dayMs)

      // Seed Default Playlists in Room
      val playlistDao = database.playlistDao()
      val existingPlaylists = playlistDao.observePlaylists().first()
      if (existingPlaylists.isEmpty() && createdTracks.isNotEmpty()) {
        val p1Id = playlistDao.insertPlaylist(PlaylistEntity(name = "Golden Hits Vol.1"))
        val metadataList =
          listOf(
            Triple("Yesterday's Echo", "Ilayaraja", "Golden Hits Vol.1"),
            Triple("Chinna Chinna Aasai", "A.R. Rahman", "Roja Classics"),
            Triple("Roja Roja", "Ilayaraja", "Golden Hits Vol.1"),
            Triple("Kannukkul Nooru Nilava", "Ilayaraja", "Golden Hits Vol.1"),
            Triple("Puthu Vellai Mazhai", "A.R. Rahman", "Roja Classics"),
          )
        createdTracks.forEachIndexed { idx, file ->
          val (title, artist, album) = metadataList.getOrElse(idx) { Triple(file.nameWithoutExtension, "Krono Synth", "Local Audio") }
          playlistDao.insertTrack(
            PlaylistTrackEntity(
              playlistId = p1Id,
              filePath = file.absolutePath,
              title = title,
              artist = artist,
              album = album,
              durationMs = (15 + idx) * 1000L,
              sortOrder = idx,
            )
          )
        }
      }

      // Seed Initial Notifications in Room
      val notifDao = database.notificationDao()
      if (notifDao.observeNotifications().first().isEmpty()) {
        notifDao.insertNotification(
          NotificationEntity(
            title = "2 Duplicate File Groups Detected",
            message = "Identical copies found across DCIM and Downloads. Review manually in Smart Cleanup.",
            category = NotificationCategory.STORAGE_DUPLICATES,
            severity = NotificationSeverity.WARNING,
            actionLabel = "Review Duplicates",
            actionRoute = "cleanup_duplicates",
            createdAt = now - 15 * 60 * 1000L,
          )
        )
        notifDao.insertNotification(
          NotificationEntity(
            title = "Chronos Index Synchronized",
            message = "All offline folders, media waveforms, and documents are indexed locally.",
            category = NotificationCategory.ACTIVITY,
            severity = NotificationSeverity.CYAN_INFO,
            actionLabel = "Open Cleanup",
            actionRoute = "cleanup",
            createdAt = now - 2 * 60 * 60 * 1000L,
          )
        )
      }

      prefs.setSeededVersion(SEED_VERSION)
    }

  private fun writeNeonArtPng(file: File, c1: Int, c2: Int, label: String) {
    val w = 480
    val h = 480
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val bgPaint =
      Paint().apply {
        shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), 0xFF07090F.toInt(), 0xFF11172B.toInt(), Shader.TileMode.CLAMP)
      }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

    val ringPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        shader = LinearGradient(60f, 60f, w - 60f, h - 60f, c1, c2, Shader.TileMode.CLAMP)
      }
    canvas.drawCircle(w / 2f, h / 2f, 130f, ringPaint)

    val innerPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.argb(140, 255, 255, 255)
      }
    canvas.drawCircle(w / 2f, h / 2f, 88f, innerPaint)

    val textPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 26f
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText(label, w / 2f, h / 2f + 10f, textPaint)

    FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 95, out) }
    bmp.recycle()
  }

  private fun writeMelodicWav(file: File, baseFreq: Double, durationSec: Int) {
    val sampleRate = 22050
    val numSamples = sampleRate * durationSec
    val pcmBytes = ByteArray(numSamples * 2)
    val notes = doubleArrayOf(1.0, 1.125, 1.25, 1.5, 1.333, 1.25, 1.125, 1.0)

    for (i in 0 until numSamples) {
      val t = i.toDouble() / sampleRate
      val beatIdx = ((t * 1.5).toInt()) % notes.size
      val freq = baseFreq * notes[beatIdx]
      val notePhase = (t * 1.5) - (t * 1.5).toInt()
      val env = (1.0 - notePhase * 0.65) * 0.35
      val sample =
        (sin(2.0 * PI * freq * t) * 0.65 +
          sin(2.0 * PI * freq * 1.5 * t) * 0.25 +
          sin(2.0 * PI * (freq * 0.5) * t) * 0.2) * env
      val pcmVal = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      pcmBytes[i * 2] = (pcmVal.toInt() and 0xFF).toByte()
      pcmBytes[i * 2 + 1] = ((pcmVal.toInt() shr 8) and 0xFF).toByte()
    }

    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
      put("RIFF".toByteArray())
      putInt(36 + pcmBytes.size)
      put("WAVE".toByteArray())
      put("fmt ".toByteArray())
      putInt(16)
      putShort(1) // PCM
      putShort(1) // Mono
      putInt(sampleRate)
      putInt(sampleRate * 2)
      putShort(2)
      putShort(16)
      put("data".toByteArray())
      putInt(pcmBytes.size)
    }.array()

    FileOutputStream(file).use { out ->
      out.write(header)
      out.write(pcmBytes)
    }
  }

  private fun generateSampleMp4(outputFile: File) {
    runCatching {
      val width = 320
      val height = 240
      val bitRate = 400_000
      val frameRate = 15
      val durationFrames = 60 // 4 seconds

      val format =
        MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
          setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
          setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
          setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
          setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

      val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
      encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      val inputSurface = encoder.createInputSurface()
      encoder.start()

      val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
      var trackIndex = -1
      var muxerStarted = false
      val bufferInfo = MediaCodec.BufferInfo()

      val paintCyan = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.CYAN; style = Paint.Style.STROKE; strokeWidth = 8f }
      val paintAmber = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 158, 68); style = Paint.Style.STROKE; strokeWidth = 6f }
      val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 20f; textAlign = Paint.Align.CENTER }

      for (frame in 0 until durationFrames) {
        val canvas = inputSurface.lockCanvas(null)
        canvas.drawColor(Color.rgb(7, 9, 15))
        val angle = frame * 6f
        val r = 45f + 15f * sin(frame * 0.25).toFloat()
        canvas.drawCircle(width / 2f, height / 2f, r, paintCyan)
        canvas.save()
        canvas.rotate(angle, width / 2f, height / 2f)
        canvas.drawRect(width / 2f - 55f, height / 2f - 55f, width / 2f + 55f, height / 2f + 55f, paintAmber)
        canvas.restore()
        canvas.drawText("KRONOFILES STUDIO", width / 2f, height - 24f, textPaint)
        inputSurface.unlockCanvasAndPost(canvas)

        trackIndex =
          drainEncoder(encoder, muxer, bufferInfo, false, trackIndex) { idx ->
            muxer.start()
            muxerStarted = true
            idx
          }
      }

      encoder.signalEndOfInputStream()
      trackIndex =
        drainEncoder(encoder, muxer, bufferInfo, true, trackIndex) { idx ->
          muxer.start()
          muxerStarted = true
          idx
        }

      encoder.stop()
      encoder.release()
      inputSurface.release()
      if (muxerStarted) {
        muxer.stop()
      }
      muxer.release()
    }.onFailure {
      // If hardware encoder is unavailable on a headless host, create a clean fallback byte file
      if (!outputFile.exists()) {
        outputFile.writeBytes(ByteArray(128 * 1024))
      }
    }
  }

  private fun drainEncoder(
    encoder: MediaCodec,
    muxer: MediaMuxer,
    bufferInfo: MediaCodec.BufferInfo,
    endOfStream: Boolean,
    currentTrackIndex: Int,
    onFormatReady: (Int) -> Int,
  ): Int {
    var trackIndex = currentTrackIndex
    while (true) {
      val status = encoder.dequeueOutputBuffer(bufferInfo, 5000)
      if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
        if (!endOfStream) break
      } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        if (trackIndex == -1) {
          trackIndex = onFormatReady(muxer.addTrack(encoder.outputFormat))
        }
      } else if (status >= 0) {
        val encodedData = encoder.getOutputBuffer(status)
        if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size > 0 && trackIndex >= 0) {
          encodedData.position(bufferInfo.offset)
          encodedData.limit(bufferInfo.offset + bufferInfo.size)
          muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
        }
        encoder.releaseOutputBuffer(status, false)
        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
      }
    }
    return trackIndex
  }
}
