package com.example.data.storage

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.local.KronoDatabase
import com.example.data.local.NotificationEntity
import com.example.data.local.VaultItemEntity
import com.example.model.AudioTrackItem
import com.example.model.CategorySummary
import com.example.model.ChronosBucket
import com.example.model.DuplicateGroup
import com.example.model.FileCategory
import com.example.model.FileKind
import com.example.model.KronoFileItem
import com.example.model.NotificationCategory
import com.example.model.NotificationSeverity
import com.example.model.SearchDateFilter
import com.example.model.SearchSizeFilter
import com.example.model.SearchTypeFilter
import com.example.model.StorageStats
import com.example.util.KronoCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.Calendar
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.abs

class KronoStorageRepository(
  private val context: Context,
  private val database: KronoDatabase,
) {
  val sandboxRootDir: File = File(context.filesDir, "KronoStorage").apply { mkdirs() }
  val vaultDir: File = File(context.filesDir, "KronoSafeVault").apply { mkdirs() }

  private fun notifyMediaScanner(vararg paths: String) {
    val clean = paths.filter { it.isNotBlank() && !it.startsWith("content://") }.distinct().toTypedArray()
    if (clean.isNotEmpty()) {
      runCatching { MediaScannerConnection.scanFile(context, clean, null, null) }
    }
  }

  private fun collectScannablePaths(target: File, limit: Int = 500): List<String> {
    if (!target.exists()) return listOf(target.absolutePath)
    if (!target.isDirectory) return listOf(target.absolutePath)
    val children =
      target.walkTopDown()
        .filter { !it.isDirectory }
        .take(limit)
        .map { it.absolutePath }
        .toList()
    return children.ifEmpty { listOf(target.absolutePath) }
  }

  fun hasAllFilesAccess(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      Environment.isExternalStorageManager()
    } else {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
        PackageManager.PERMISSION_GRANTED
    }
  }

  private fun hasVisualMediaAccess(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
  }

  fun hasMediaReadPermissions(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED ||
        hasVisualMediaAccess()
    } else {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
        PackageManager.PERMISSION_GRANTED
    }
  }

  val isUsingExternalStorage: Boolean
    get() {
      if (!hasAllFilesAccess()) return false
      val ext = Environment.getExternalStorageDirectory()
      return ext.exists() && ext.canRead()
    }

  val hasMediaFallbackAccess: Boolean
    get() = !isUsingExternalStorage && hasMediaReadPermissions()

  val isUsingDeviceStorage: Boolean
    get() = isUsingExternalStorage || hasMediaReadPermissions()

  val rootDir: File
    get() =
      if (isUsingExternalStorage) Environment.getExternalStorageDirectory()
      else sandboxRootDir

  private fun isScannableDir(dir: File): Boolean {
    val name = dir.name
    if (name.startsWith(".")) return false
    val path = dir.absolutePath.replace('\\', '/')
    if (path.contains("/Android/data") || path.contains("/Android/obb")) return false
    return dir.canRead()
  }

  private fun queryMediaStoreFiles(): List<KronoFileItem> {
    val results = mutableListOf<KronoFileItem>()
    val cr = context.contentResolver

    data class MediaSourceSpec(
      val collection: Uri,
      val kind: FileKind,
      val defaultFolder: String,
      val checkPermission: Boolean,
    )

    val specs =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val visualPerm = hasVisualMediaAccess()
        listOf(
          MediaSourceSpec(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            FileKind.IMAGE,
            "Pictures",
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ==
              PackageManager.PERMISSION_GRANTED || visualPerm,
          ),
          MediaSourceSpec(
            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            FileKind.VIDEO,
            "Movies",
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) ==
              PackageManager.PERMISSION_GRANTED || visualPerm,
          ),
          MediaSourceSpec(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            FileKind.AUDIO,
            "Music",
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) ==
              PackageManager.PERMISSION_GRANTED,
          ),
        )
      } else {
        val granted =
          ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        listOf(
          MediaSourceSpec(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, FileKind.IMAGE, "Pictures", granted),
          MediaSourceSpec(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, FileKind.VIDEO, "Movies", granted),
          MediaSourceSpec(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, FileKind.AUDIO, "Music", granted),
        )
      }

    val projection =
      arrayOf(
        android.provider.MediaStore.MediaColumns._ID,
        android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
        android.provider.MediaStore.MediaColumns.SIZE,
        android.provider.MediaStore.MediaColumns.DATE_MODIFIED,
        android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
        android.provider.MediaStore.MediaColumns.DATA,
      )

    for (spec in specs) {
      if (!spec.checkPermission) continue
      runCatching {
        cr.query(
          spec.collection,
          projection,
          null,
          null,
          "${android.provider.MediaStore.MediaColumns.DATE_MODIFIED} DESC",
        )?.use { cursor ->
          val idCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns._ID)
          val nameCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
          val sizeCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.SIZE)
          val dateCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATE_MODIFIED)
          val relCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.RELATIVE_PATH)
          val dataCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)

          while (cursor.moveToNext() && results.size < 4000) {
            val id = if (idCol >= 0) cursor.getLong(idCol) else continue
            val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "media_$id" else "media_$id"
            val size = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(0L) else 0L
            val modSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
            val relDir =
              (if (relCol >= 0) cursor.getString(relCol) else null)
                ?.trim('/')
                ?.takeIf { it.isNotBlank() }
                ?: spec.defaultFolder
            val rawPath = if (dataCol >= 0) cursor.getString(dataCol) else null
            val contentUri = android.content.ContentUris.withAppendedId(spec.collection, id).toString()
            val absPath = rawPath?.takeIf { it.isNotBlank() } ?: contentUri
            val relPath = "$relDir/$name"
            val ext = name.substringAfterLast('.', "").lowercase()
            results.add(
              KronoFileItem(
                id = contentUri,
                name = name,
                absolutePath = absPath,
                relativePath = relPath,
                parentRelativePath = relDir,
                isDirectory = false,
                sizeBytes = size,
                lastModifiedMs = if (modSec > 0L) modSec * 1000L else System.currentTimeMillis(),
                extension = ext,
                kind = if (ext.isNotEmpty()) classifyExtension(ext).takeIf { it != FileKind.OTHER } ?: spec.kind else spec.kind,
                childCount = 0,
                contentUri = contentUri,
              )
            )
          }
        }
      }
    }
    return results
  }

  private fun bfsScanFiles(root: File, limit: Int = 12000): List<KronoFileItem> {
    val results = mutableListOf<KronoFileItem>()
    val seen = HashSet<String>()
    val queue = ArrayDeque<File>()
    queue.add(root)
    val priorityOrder = setOf("dcim", "pictures", "music", "movies", "download", "downloads", "documents")

    while (queue.isNotEmpty() && results.size < limit) {
      val dir = queue.removeFirst()
      val children = dir.listFiles()?.toList().orEmpty()
      val (dirs, files) = children.partition { it.isDirectory }
      val sortedFiles = files.sortedByDescending { it.lastModified() }
      for (f in sortedFiles) {
        if (results.size >= limit) break
        if (!f.name.startsWith(".") && seen.add(f.absolutePath)) {
          results.add(toFileItem(f, root))
        }
      }
      val sortedDirs =
        if (dir == root) {
          dirs.sortedBy { d -> if (d.name.lowercase() in priorityOrder) 0 else 1 }
        } else dirs
      for (sub in sortedDirs) {
        if (isScannableDir(sub)) queue.add(sub)
      }
    }
    return results
  }

  suspend fun listDirectory(relativePath: String): List<KronoFileItem> =
    withContext(Dispatchers.IO) {
      if (hasMediaFallbackAccess) {
        val mediaItems = queryMediaStoreFiles()
        val clean = relativePath.trim('/')
        val sandboxDir = if (clean.isEmpty()) sandboxRootDir else File(sandboxRootDir, clean)
        val sandboxChildren =
          if (sandboxDir.exists() && sandboxDir.isDirectory && sandboxDir.canonicalPath.startsWith(sandboxRootDir.canonicalPath)) {
            sandboxDir.listFiles()?.toList().orEmpty()
              .filter { if (it.isDirectory) isScannableDir(it) else !it.name.startsWith(".") }
              .map { toFileItem(it, sandboxRootDir) }
          } else emptyList()

        if (clean.isEmpty()) {
          val groupedTop =
            mediaItems.groupBy { item ->
              item.parentRelativePath.substringBefore('/').ifBlank { "Media" }
            }
          val virtualTop =
            groupedTop.entries.map { (folderName, itemsInFolder) ->
              KronoFileItem(
                id = "virtual_$folderName",
                name = folderName,
                absolutePath = File(Environment.getExternalStorageDirectory(), folderName).absolutePath,
                relativePath = folderName,
                parentRelativePath = "",
                isDirectory = true,
                sizeBytes = 0L,
                lastModifiedMs = itemsInFolder.maxOfOrNull { it.lastModifiedMs } ?: System.currentTimeMillis(),
                extension = "",
                kind = FileKind.FOLDER,
                childCount = itemsInFolder.size,
              )
            }
          val mergedByRel = LinkedHashMap<String, KronoFileItem>()
          virtualTop.forEach { mergedByRel[it.relativePath.lowercase()] = it }
          sandboxChildren.forEach { item ->
            val key = item.relativePath.lowercase()
            val existing = mergedByRel[key]
            if (existing != null && existing.isDirectory && item.isDirectory) {
              mergedByRel[key] =
                existing.copy(
                  childCount = existing.childCount + item.childCount,
                  lastModifiedMs = maxOf(existing.lastModifiedMs, item.lastModifiedMs),
                )
            } else {
              mergedByRel[item.id] = item
            }
          }
          return@withContext mergedByRel.values.sortedWith(
            compareBy<KronoFileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }
          )
        } else {
          val matchingSubfolders =
            mediaItems
              .filter { it.parentRelativePath.startsWith("$clean/", ignoreCase = true) }
              .map { item ->
                val remainder = item.parentRelativePath.drop(clean.length + 1)
                remainder.substringBefore('/')
              }
              .filter { it.isNotBlank() }
              .distinctBy { it.lowercase() }
              .map { subName ->
                val subRel = "$clean/$subName"
                val count = mediaItems.count {
                  it.parentRelativePath.equals(subRel, ignoreCase = true) ||
                    it.parentRelativePath.startsWith("$subRel/", ignoreCase = true)
                }
                KronoFileItem(
                  id = "virtual_$subRel",
                  name = subName,
                  absolutePath = File(Environment.getExternalStorageDirectory(), subRel).absolutePath,
                  relativePath = subRel,
                  parentRelativePath = clean,
                  isDirectory = true,
                  sizeBytes = 0L,
                  lastModifiedMs = System.currentTimeMillis(),
                  extension = "",
                  kind = FileKind.FOLDER,
                  childCount = count,
                )
              }
          val directMedia =
            mediaItems.filter { item ->
              item.parentRelativePath.equals(clean, ignoreCase = true)
            }
          val merged = LinkedHashMap<String, KronoFileItem>()
          matchingSubfolders.forEach { merged["dir_${it.relativePath.lowercase()}"] = it }
          sandboxChildren.filter { it.isDirectory }.forEach { sub ->
            val k = "dir_${sub.relativePath.lowercase()}"
            val existing = merged[k]
            merged[k] =
              if (existing != null) existing.copy(childCount = existing.childCount + sub.childCount)
              else sub
          }
          (directMedia + sandboxChildren.filter { !it.isDirectory }).forEach { f ->
            merged[f.id] = f
          }
          return@withContext merged.values.toList()
        }
      }

      val dir = resolveRelativeDir(relativePath)
      val children =
        dir.listFiles()?.toList().orEmpty()
          .filter { if (it.isDirectory) isScannableDir(it) else !it.name.startsWith(".") }
      children.map { toFileItem(it) }
    }

  suspend fun listAllFilesRecursive(): List<KronoFileItem> =
    withContext(Dispatchers.IO) {
      if (isUsingExternalStorage) {
        return@withContext bfsScanFiles(rootDir, limit = 12000)
      }

      if (hasMediaFallbackAccess) {
        val media = queryMediaStoreFiles()
        val sandboxFiles = bfsScanFiles(sandboxRootDir, limit = 5000)
        return@withContext (media + sandboxFiles).distinctBy { it.id }
      }

      bfsScanFiles(sandboxRootDir, limit = 5000)
    }

  suspend fun listAllFoldersRecursive(): List<KronoFileItem> =
    withContext(Dispatchers.IO) {
      val activeRoot = rootDir
      val rootLabel =
        when {
          isUsingExternalStorage -> "Device Storage"
          hasMediaFallbackAccess -> "Device Media & Docs"
          else -> "App Sandbox"
        }
      val rootItem =
        KronoFileItem(
          id = "",
          name = rootLabel,
          absolutePath = activeRoot.absolutePath,
          relativePath = "",
          parentRelativePath = "",
          isDirectory = true,
          sizeBytes = 0L,
          lastModifiedMs = activeRoot.lastModified(),
          extension = "",
          kind = FileKind.FOLDER,
          childCount = activeRoot.listFiles()?.size ?: 0,
        )
      val priorityOrder = setOf("dcim", "pictures", "music", "movies", "download", "downloads", "documents", "archives")
      val localSubs =
        activeRoot
          .walkTopDown()
          .maxDepth(4)
          .onEnter { dir -> dir == activeRoot || isScannableDir(dir) }
          .filter { it != activeRoot && it.isDirectory }
          .map { toFileItem(it, activeRoot) }
          .toList()
          .sortedWith(
            compareBy<KronoFileItem> { item ->
              val top = item.relativePath.substringBefore('/').lowercase()
              if (top in priorityOrder) 0 else 1
            }.thenBy { it.relativePath.lowercase() }
          )
          .take(400)
      listOf(rootItem) + localSubs
    }

  private fun isDownloadsPath(relPath: String): Boolean {
    val lower = relPath.lowercase()
    return lower.startsWith("downloads/") || lower.startsWith("download/") || lower == "downloads" || lower == "download"
  }

  suspend fun getFilesForCategory(category: FileCategory): List<KronoFileItem> =
    withContext(Dispatchers.IO) {
      val all = listAllFilesRecursive()
      when (category) {
        FileCategory.PHOTOS -> all.filter { it.kind == FileKind.IMAGE }
        FileCategory.VIDEOS -> all.filter { it.kind == FileKind.VIDEO }
        FileCategory.AUDIO -> all.filter { it.kind == FileKind.AUDIO }
        FileCategory.DOCUMENTS ->
          all.filter {
            it.kind in setOf(FileKind.PDF, FileKind.DOCX, FileKind.XLSX, FileKind.CODE, FileKind.TEXT)
          }
        FileCategory.DOWNLOADS -> all.filter { isDownloadsPath(it.relativePath) }
        FileCategory.APKS -> all.filter { it.kind == FileKind.APK }
        FileCategory.ARCHIVES -> all.filter { it.kind == FileKind.ARCHIVE }
      }
    }

  suspend fun computeStorageStats(preScannedFiles: List<KronoFileItem>? = null): StorageStats =
    withContext(Dispatchers.IO) {
      val statPath =
        if (isUsingDeviceStorage) Environment.getExternalStorageDirectory().path
        else sandboxRootDir.path
      val stat = StatFs(statPath)
      val totalBytes = stat.totalBytes
      val freeBytes = stat.availableBytes
      val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

      val allFiles = preScannedFiles ?: listAllFilesRecursive()
      val kronoManaged = allFiles.sumOf { it.sizeBytes }

      val summaries =
        FileCategory.entries.associateWith { cat ->
          val items =
            when (cat) {
              FileCategory.PHOTOS -> allFiles.filter { it.kind == FileKind.IMAGE }
              FileCategory.VIDEOS -> allFiles.filter { it.kind == FileKind.VIDEO }
              FileCategory.AUDIO -> allFiles.filter { it.kind == FileKind.AUDIO }
              FileCategory.DOCUMENTS ->
                allFiles.filter {
                  it.kind in setOf(FileKind.PDF, FileKind.DOCX, FileKind.XLSX, FileKind.CODE, FileKind.TEXT)
                }
              FileCategory.DOWNLOADS -> allFiles.filter { isDownloadsPath(it.relativePath) }
              FileCategory.APKS -> allFiles.filter { it.kind == FileKind.APK }
              FileCategory.ARCHIVES -> allFiles.filter { it.kind == FileKind.ARCHIVE }
            }
          CategorySummary(cat, items.size, items.sumOf { it.sizeBytes })
        }

      StorageStats(
        totalBytes = totalBytes,
        usedBytes = usedBytes,
        freeBytes = freeBytes,
        kronoManagedBytes = kronoManaged,
        categorySummaries = summaries,
      )
    }

  suspend fun createFolder(parentRelativePath: String, folderName: String): Result<KronoFileItem> =
    withContext(Dispatchers.IO) {
      runCatching {
        val clean = sanitizeFileName(folderName)
        require(clean.isNotBlank()) { "Folder name cannot be empty" }
        val parent = resolveRelativeDir(parentRelativePath)
        val target = File(parent, clean)
        require(!target.exists()) { "A folder named '$clean' already exists" }
        target.mkdirs()
        notifyMediaScanner(target.absolutePath)
        toFileItem(target)
      }
    }

  suspend fun renameFile(file: KronoFileItem, newName: String): Result<KronoFileItem> =
    withContext(Dispatchers.IO) {
      runCatching {
        val clean = sanitizeFileName(newName)
        require(clean.isNotBlank()) { "Name cannot be empty" }
        val src = File(file.absolutePath)
        val dst = File(src.parentFile, clean)
        require(!dst.exists()) { "'$clean' already exists" }
        check(src.renameTo(dst)) { "Unable to rename file" }
        notifyMediaScanner(src.absolutePath, dst.absolutePath)
        toFileItem(dst)
      }
    }

  suspend fun moveFile(file: KronoFileItem, targetFolderRelativePath: String): Result<KronoFileItem> =
    withContext(Dispatchers.IO) {
      runCatching {
        val src = File(file.absolutePath)
        val oldScanPaths = collectScannablePaths(src)
        val destDir = resolveRelativeDir(targetFolderRelativePath)
        val dst = uniqueTarget(destDir, src.name)
        if (!src.renameTo(dst)) {
          src.copyRecursively(dst, overwrite = true)
          src.deleteRecursively()
        }
        val newScanPaths = collectScannablePaths(dst)
        notifyMediaScanner(*(oldScanPaths + newScanPaths).toTypedArray())
        toFileItem(dst)
      }
    }

  suspend fun copyFile(file: KronoFileItem, targetFolderRelativePath: String): Result<KronoFileItem> =
    withContext(Dispatchers.IO) {
      runCatching {
        val src = File(file.absolutePath)
        val destDir = resolveRelativeDir(targetFolderRelativePath)
        val dst = uniqueTarget(destDir, file.name)
        if (src.exists() && src.canRead()) {
          src.copyRecursively(dst, overwrite = true)
        } else {
          openItemInputStream(file)?.use { input ->
            FileOutputStream(dst).use { output -> input.copyTo(output) }
          } ?: error("Unable to read source file for copying")
        }
        notifyMediaScanner(*collectScannablePaths(dst).toTypedArray())
        toFileItem(dst)
      }
    }

  suspend fun deleteFiles(paths: List<String>): Int =
    withContext(Dispatchers.IO) {
      var deleted = 0
      val scanTargets = mutableListOf<String>()
      for (path in paths) {
        if (path.startsWith("content://")) {
          val rows = runCatching { context.contentResolver.delete(Uri.parse(path), null, null) }.getOrDefault(0)
          if (rows > 0) deleted++
        } else {
          val f = File(path)
          val preDeletePaths = if (f.exists()) collectScannablePaths(f) else emptyList()
          if (f.exists() && f.deleteRecursively()) {
            deleted++
            scanTargets.addAll(preDeletePaths)
          }
        }
      }
      if (scanTargets.isNotEmpty()) notifyMediaScanner(*scanTargets.toTypedArray())
      deleted
    }

  private fun openItemInputStream(item: KronoFileItem): InputStream? {
    val file = File(item.absolutePath)
    if (file.exists() && file.canRead() && !file.isDirectory) {
      return runCatching { FileInputStream(file) }.getOrNull()
    }
    if (!item.contentUri.isNullOrBlank()) {
      return runCatching { context.contentResolver.openInputStream(Uri.parse(item.contentUri)) }.getOrNull()
    }
    return null
  }

  suspend fun compressToZip(items: List<KronoFileItem>, zipName: String, targetFolderRel: String): Result<KronoFileItem> =
    withContext(Dispatchers.IO) {
      runCatching {
        val clean = sanitizeFileName(zipName).let { if (it.endsWith(".zip", true)) it else "$it.zip" }
        val destDir = resolveRelativeDir(targetFolderRel)
        val zipFile = uniqueTarget(destDir, clean)
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
          for (item in items) {
            val file = File(item.absolutePath)
            if (file.exists() && file.isDirectory) {
              file.walkTopDown().filter { !it.isDirectory }.forEach { child ->
                val rel = child.relativeTo(file.parentFile ?: rootDir).path
                zos.putNextEntry(ZipEntry(rel))
                FileInputStream(child).use { it.copyTo(zos) }
                zos.closeEntry()
              }
            } else {
              openItemInputStream(item)?.use { input ->
                zos.putNextEntry(ZipEntry(item.name))
                input.copyTo(zos)
                zos.closeEntry()
              }
            }
          }
        }
        notifyMediaScanner(zipFile.absolutePath)
        toFileItem(zipFile)
      }
    }

  suspend fun extractArchive(archiveFile: KronoFileItem): Result<KronoFileItem> =
    withContext(Dispatchers.IO) {
      runCatching {
        val src = File(archiveFile.absolutePath)
        val ext = archiveFile.extension.ifBlank { src.extension }.lowercase()
        require(ext == "zip" || ext == "rar") {
          if (ext in setOf("7z", "tar", "gz")) {
            "Formats .7z, .tar, and .gz require external extraction tools; offline extraction supports .zip and standard .rar."
          } else {
            "Supported offline formats: .zip and standard .rar"
          }
        }
        val parentDir =
          src.parentFile?.takeIf { it.exists() && it.canWrite() }
            ?: resolveRelativeDir("Archives")
        val baseName = archiveFile.name.substringBeforeLast('.', archiveFile.name)
        val outDir = uniqueTarget(parentDir, baseName).apply { mkdirs() }
        if (ext == "rar") {
          val tempRar =
            if (src.exists() && src.canRead()) null
            else File(context.cacheDir, "rar_extract_${System.currentTimeMillis()}.rar").also { tmp ->
              openItemInputStream(archiveFile)?.use { input ->
                FileOutputStream(tmp).use { output -> input.copyTo(output) }
              } ?: error("Unable to read RAR archive")
            }
          try {
            val ok = OfficeDocEngine.extractRar(tempRar ?: src, outDir)
            check(ok) { "Unable to extract RAR archive (solid/RAR5 or encrypted headers unsupported)." }
          } finally {
            tempRar?.delete()
          }
        } else {
          val stream = openItemInputStream(archiveFile) ?: error("Unable to open ZIP archive")
          ZipInputStream(stream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
              val outFile = File(outDir, entry.name)
              // Zip-slip protection
              require(outFile.canonicalPath.startsWith(outDir.canonicalPath)) { "Invalid archive path" }
              if (entry.isDirectory) {
                outFile.mkdirs()
              } else {
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
              }
              entry = zis.nextEntry
            }
          }
        }
        notifyMediaScanner(*collectScannablePaths(outDir).toTypedArray())
        toFileItem(outDir)
      }
    }

  suspend fun importUriToStorage(uri: Uri, targetSubfolder: String? = null): Result<KronoFileItem> =
    withContext(Dispatchers.IO) {
      runCatching {
        val name = queryDisplayName(uri) ?: "imported_${System.currentTimeMillis()}"
        val mime = context.contentResolver.getType(uri).orEmpty()
        val folderRel =
          targetSubfolder
            ?: when {
              mime.startsWith("image/") -> "Pictures"
              mime.startsWith("video/") -> "Movies"
              mime.startsWith("audio/") -> "Music"
              mime.contains("pdf") || mime.contains("text") || mime.contains("document") -> "Documents"
              else -> "Downloads"
            }
        val destDir = resolveRelativeDir(folderRel)
        val dst = uniqueTarget(destDir, sanitizeFileName(name))
        context.contentResolver.openInputStream(uri)?.use { input ->
          FileOutputStream(dst).use { output -> input.copyTo(output) }
        } ?: error("Unable to open selected file")

        database.notificationDao().insertNotification(
          NotificationEntity(
            title = "File Imported Offline",
            message = "${dst.name} added to $folderRel",
            category = NotificationCategory.ACTIVITY,
            severity = NotificationSeverity.CYAN_INFO,
            actionLabel = "Dismiss",
            actionRoute = null,
          )
        )
        notifyMediaScanner(dst.absolutePath)
        toFileItem(dst)
      }
    }

  suspend fun lockFileToVault(file: KronoFileItem): Result<VaultItemEntity> =
    withContext(Dispatchers.IO) {
      runCatching {
        require(!file.isDirectory) { "Select a valid file to lock" }
        val src = File(file.absolutePath)
        val vaultTarget = uniqueTarget(vaultDir, "${System.currentTimeMillis()}_${file.name}.kenc")
        var originalSize = file.sizeBytes

        if (src.exists() && src.canRead() && !src.isDirectory) {
          KronoCrypto.encryptFile(src, vaultTarget)
          originalSize = src.length()
          if (src.canWrite()) {
            src.delete()
            notifyMediaScanner(src.absolutePath)
          }
        } else {
          val stream = openItemInputStream(file) ?: error("Select a valid readable file to lock")
          stream.use { input -> KronoCrypto.encryptStream(input, vaultTarget) }
          if (!file.contentUri.isNullOrBlank()) {
            runCatching { context.contentResolver.delete(Uri.parse(file.contentUri), null, null) }
          }
        }

        val entity =
          VaultItemEntity(
            fileName = file.name,
            vaultFilePath = vaultTarget.absolutePath,
            originalRelativePath = file.relativePath,
            extension = file.extension,
            sizeBytes = originalSize,
          )
        val id = database.vaultDao().insertVaultItem(entity)
        entity.copy(id = id)
      }
    }

  suspend fun unlockVaultItemToStorage(item: VaultItemEntity): Result<KronoFileItem> =
    withContext(Dispatchers.IO) {
      runCatching {
        val src = File(item.vaultFilePath)
        val relParent = item.originalRelativePath.substringBeforeLast('/', "Downloads")
        val destDir = resolveRelativeDir(if (relParent == item.originalRelativePath) "Downloads" else relParent)
        val dst = uniqueTarget(destDir, item.fileName)
        if (src.exists()) {
          if (src.extension == "kenc") {
            KronoCrypto.decryptFile(src, dst)
            src.delete()
          } else if (!src.renameTo(dst)) {
            src.copyTo(dst, overwrite = true)
            src.delete()
          }
        }
        database.vaultDao().deleteVaultItem(item.id)
        notifyMediaScanner(dst.absolutePath)
        toFileItem(dst)
      }
    }

  suspend fun decryptVaultItemToTemp(item: VaultItemEntity): File? =
    withContext(Dispatchers.IO) {
      val src = File(item.vaultFilePath)
      if (!src.exists()) return@withContext null
      if (src.extension == "kenc") {
        runCatching {
          KronoCrypto.decryptToTempCacheFile(context, src, item.fileName)
        }.getOrNull()
      } else {
        src
      }
    }

  suspend fun deleteVaultItem(item: VaultItemEntity) =
    withContext(Dispatchers.IO) {
      File(item.vaultFilePath).delete()
      database.vaultDao().deleteVaultItem(item.id)
    }

  suspend fun findDuplicateGroups(preScannedFiles: List<KronoFileItem>? = null): List<DuplicateGroup> =
    withContext(Dispatchers.IO) {
      val allFiles =
        (preScannedFiles ?: listAllFilesRecursive()).filter { item ->
          item.sizeBytes > 0L && (File(item.absolutePath).canRead() || !item.contentUri.isNullOrBlank())
        }
      val bySize = allFiles.groupBy { it.sizeBytes }.filterValues { it.size > 1 }
      val groups = mutableListOf<DuplicateGroup>()
      for ((size, candidates) in bySize) {
        val byHash =
          candidates.mapNotNull { candidate ->
            sha256Item(candidate)?.let { hash -> hash to candidate }
          }.groupBy({ it.first }, { it.second })
        for ((hash, matches) in byHash) {
          if (matches.size > 1) {
            val sorted = matches.sortedBy { it.lastModifiedMs }
            groups.add(
              DuplicateGroup(
                hash = hash,
                fileSize = size,
                files = sorted,
                keepFilePath = sorted.first().absolutePath,
              )
            )
          }
        }
      }
      groups.sortedByDescending { it.fileSize * (it.files.size - 1) }
    }

  suspend fun findLargeFiles(
    minBytes: Long,
    preScannedFiles: List<KronoFileItem>? = null,
  ): List<KronoFileItem> =
    withContext(Dispatchers.IO) {
      (preScannedFiles ?: listAllFilesRecursive())
        .filter { it.sizeBytes >= minBytes }
        .sortedByDescending { it.sizeBytes }
    }

  suspend fun loadAllAudioTracks(preScannedFiles: List<KronoFileItem>? = null): List<AudioTrackItem> =
    withContext(Dispatchers.IO) {
      val allAudio = (preScannedFiles ?: listAllFilesRecursive()).filter { it.kind == FileKind.AUDIO }
      val presetMeta =
        mapOf(
          "Yesterday's Echo" to ("Ilayaraja" to "Golden Hits Vol.1"),
          "Chinna Chinna Aasai" to ("A.R. Rahman" to "Roja Classics"),
          "Roja Roja" to ("Ilayaraja" to "Golden Hits Vol.1"),
          "Kannukkul Nooru Nilava" to ("Ilayaraja" to "Golden Hits Vol.1"),
          "Puthu Vellai Mazhai" to ("A.R. Rahman" to "Roja Classics"),
        )
      val retriever = MediaMetadataRetriever()
      try {
        allAudio.map { item ->
          val file = File(item.absolutePath)
          val base = item.name.substringBeforeLast('.')
          var id3Title: String? = null
          var id3Artist: String? = null
          var id3Album: String? = null
          var id3DurationMs: Long? = null

          runCatching {
            if (item.contentUri != null) {
              retriever.setDataSource(context, Uri.parse(item.contentUri))
            } else {
              retriever.setDataSource(file.absolutePath)
            }
            id3Title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            id3Artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
            id3Album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }
            id3DurationMs =
              retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
          }

          val isSeededSandboxItem =
            !isUsingDeviceStorage && item.absolutePath.startsWith(sandboxRootDir.absolutePath)
          val fallbackMeta = if (isSeededSandboxItem) presetMeta[base] else null
          val title = id3Title ?: base
          val artist = id3Artist ?: fallbackMeta?.first ?: "Local Artist"
          val album = id3Album ?: fallbackMeta?.second ?: item.parentRelativePath.ifBlank { "Music" }
          val durationMs =
            id3DurationMs
              ?: if (file.exists() && file.canRead()) estimateWavOrAudioDurationMs(file)
              else ((item.sizeBytes / 16_000L).coerceAtLeast(15L)) * 1000L
          val peaks = extractWaveformPeaks(file, item.contentUri, 44)
          AudioTrackItem(
            path = item.absolutePath,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            sizeBytes = item.sizeBytes,
            waveformPeaks = peaks,
            contentUri = item.contentUri,
          )
        }
      } finally {
        runCatching { retriever.release() }
      }
    }

  fun searchFilesDeterministic(
    allFiles: List<KronoFileItem>,
    query: String,
    typeFilter: SearchTypeFilter,
    sizeFilter: SearchSizeFilter,
    dateFilter: SearchDateFilter,
    scopeFolderRelativePath: String?,
  ): List<KronoFileItem> {
    val q = query.trim().lowercase()
    val now = System.currentTimeMillis()
    val startOfToday = startOfTodayMs()

    return allFiles.filter { item ->
      if (scopeFolderRelativePath != null && scopeFolderRelativePath.isNotBlank()) {
        if (!item.relativePath.startsWith(scopeFolderRelativePath)) return@filter false
      }
      if (q.isNotEmpty() && !item.name.lowercase().contains(q) && !item.relativePath.lowercase().contains(q)) {
        return@filter false
      }
      val typeMatches =
        when (typeFilter) {
          SearchTypeFilter.ALL -> true
          SearchTypeFilter.IMAGES -> item.kind == FileKind.IMAGE
          SearchTypeFilter.VIDEOS -> item.kind == FileKind.VIDEO
          SearchTypeFilter.AUDIO -> item.kind == FileKind.AUDIO
          SearchTypeFilter.PDF -> item.kind == FileKind.PDF
          SearchTypeFilter.DOCS -> item.kind == FileKind.DOCX || item.kind == FileKind.TEXT
          SearchTypeFilter.SHEETS -> item.kind == FileKind.XLSX
          SearchTypeFilter.CODE -> item.kind == FileKind.CODE
          SearchTypeFilter.APKS -> item.kind == FileKind.APK
          SearchTypeFilter.ARCHIVES -> item.kind == FileKind.ARCHIVE
        }
      if (!typeMatches) return@filter false

      val sizeMatches =
        item.sizeBytes >= sizeFilter.minBytes &&
          (sizeFilter.maxBytes == null || item.sizeBytes <= sizeFilter.maxBytes)
      if (!sizeMatches) return@filter false

      val dateMatches =
        when (dateFilter) {
          SearchDateFilter.ANY -> true
          SearchDateFilter.TODAY -> item.lastModifiedMs >= startOfToday
          SearchDateFilter.LAST_7_DAYS -> item.lastModifiedMs >= now - 7L * 24L * 3600L * 1000L
          SearchDateFilter.LAST_30_DAYS -> item.lastModifiedMs >= now - 30L * 24L * 3600L * 1000L
        }
      dateMatches
    }
  }

  fun groupIntoChronosBuckets(files: List<KronoFileItem>): List<Pair<ChronosBucket, List<KronoFileItem>>> {
    val todayStart = startOfTodayMs()
    val dayMs = 24L * 60L * 60L * 1000L
    val yesterdayStart = todayStart - dayMs
    val weekStart = todayStart - 7L * dayMs
    val monthStart = todayStart - 30L * dayMs

    val grouped =
      files.sortedByDescending { it.lastModifiedMs }.groupBy { item ->
        when {
          item.lastModifiedMs >= todayStart -> ChronosBucket.TODAY
          item.lastModifiedMs >= yesterdayStart -> ChronosBucket.YESTERDAY
          item.lastModifiedMs >= weekStart -> ChronosBucket.THIS_WEEK
          item.lastModifiedMs >= monthStart -> ChronosBucket.THIS_MONTH
          else -> ChronosBucket.EARLIER
        }
      }
    return ChronosBucket.entries.mapNotNull { bucket ->
      grouped[bucket]?.takeIf { it.isNotEmpty() }?.let { bucket to it }
    }
  }

  fun buildShareIntent(file: File): Intent? {
    if (!file.exists()) return null
    val uri =
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val mime = guessMimeType(file.extension)
    return Intent(Intent.ACTION_SEND).apply {
      type = mime
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }

  fun buildShareIntentForItem(item: KronoFileItem): Intent? {
    val f = File(item.absolutePath)
    if (f.exists() && f.canRead()) {
      return buildShareIntent(f)
    }
    val uriStr = item.contentUri ?: return null
    val uri = Uri.parse(uriStr)
    val mime = guessMimeType(item.extension)
    return Intent(Intent.ACTION_SEND).apply {
      type = mime
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }

  fun toFileItem(file: File, baseRoot: File = rootDir): KronoFileItem {
    val rel =
      when {
        file == baseRoot -> ""
        file.absolutePath.startsWith(baseRoot.absolutePath) ->
          file.relativeTo(baseRoot).path.replace(File.separatorChar, '/')
        file.absolutePath.startsWith(sandboxRootDir.absolutePath) ->
          file.relativeTo(sandboxRootDir).path.replace(File.separatorChar, '/')
        else -> file.name
      }
    val parentRel = rel.substringBeforeLast('/', "")
    val ext = if (file.isDirectory) "" else file.extension.lowercase()
    return KronoFileItem(
      id = file.absolutePath,
      name = file.name,
      absolutePath = file.absolutePath,
      relativePath = rel,
      parentRelativePath = parentRel,
      isDirectory = file.isDirectory,
      sizeBytes = if (file.isDirectory) 0L else file.length(),
      lastModifiedMs = file.lastModified(),
      extension = ext,
      kind = if (file.isDirectory) FileKind.FOLDER else classifyExtension(ext),
      childCount = if (file.isDirectory) (file.listFiles()?.size ?: 0) else 0,
    )
  }

  fun resolveRelativeDir(relativePath: String): File {
    val activeRoot = rootDir
    val clean = relativePath.trim('/')
    val dir = if (clean.isEmpty()) activeRoot else File(activeRoot, clean)
    if (!dir.canonicalPath.startsWith(activeRoot.canonicalPath)) return activeRoot
    if (!dir.exists()) dir.mkdirs()
    return dir
  }

  private fun uniqueTarget(dir: File, desiredName: String): File {
    val base = File(dir, desiredName)
    if (!base.exists()) return base
    val dotIdx = desiredName.lastIndexOf('.')
    val namePart = if (dotIdx > 0) desiredName.substring(0, dotIdx) else desiredName
    val extPart = if (dotIdx > 0) desiredName.substring(dotIdx) else ""
    var count = 1
    while (true) {
      val candidate = File(dir, "${namePart}_$count$extPart")
      if (!candidate.exists()) return candidate
      count++
    }
  }

  private fun sanitizeFileName(raw: String): String =
    raw.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")

  private fun queryDisplayName(uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) {
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0) return cursor.getString(idx)
      }
    }
    return uri.lastPathSegment?.substringAfterLast('/')
  }

  private fun sha256Item(item: KronoFileItem): String? {
    return runCatching {
      val digest = MessageDigest.getInstance("SHA-256")
      openItemInputStream(item)?.use { fis ->
        val buffer = ByteArray(16 * 1024)
        var read = fis.read(buffer)
        while (read != -1) {
          digest.update(buffer, 0, read)
          read = fis.read(buffer)
        }
      } ?: return null
      digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()
  }

  private fun estimateWavOrAudioDurationMs(file: File): Long {
    if (file.extension.lowercase() == "wav" && file.length() > 44) {
      runCatching {
        FileInputStream(file).use { fis ->
          val header = ByteArray(44)
          if (fis.read(header) == 44) {
            val byteRate = ByteBuffer.wrap(header, 28, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (byteRate > 0) {
              return ((file.length() - 44) * 1000L) / byteRate
            }
          }
        }
      }
    }
    return ((file.length() / 16_000L).coerceAtLeast(15L)) * 1000L
  }

  private fun extractWaveformPeaks(file: File, contentUri: String?, bars: Int): List<Float> {
    return runCatching {
      val bytes: ByteArray? =
        if (file.exists() && file.canRead() && file.length() >= 128) {
          val size = file.length().toInt().coerceAtMost(512 * 1024)
          ByteArray(size).also { buf -> FileInputStream(file).use { it.read(buf) } }
        } else if (contentUri != null) {
          context.contentResolver.openInputStream(Uri.parse(contentUri))?.use { input ->
            val maxRead = 256 * 1024
            val buf = ByteArray(maxRead)
            val n = input.read(buf)
            if (n >= 128) buf.copyOf(n) else null
          }
        } else {
          null
        }
      if (bytes == null || bytes.size < 128) return@runCatching List(bars) { 0.35f }
      val start = if (bytes.size > 64) 44 else 0
      val step = ((bytes.size - start) / bars).coerceAtLeast(1)
      (0 until bars).map { b ->
        var maxSample = 0
        val offset = start + b * step
        val end = (offset + step).coerceAtMost(bytes.size)
        for (i in offset until end step 4) {
          maxSample = maxOf(maxSample, abs(bytes[i].toInt()))
        }
        (maxSample / 128f).coerceIn(0.14f, 1.0f)
      }
    }.getOrElse { List(bars) { 0.4f } }
  }

  private fun startOfTodayMs(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
  }

  companion object {
    fun classifyExtension(ext: String): FileKind =
      when (ext.lowercase()) {
        "png", "jpg", "jpeg", "webp", "gif", "bmp", "heic" -> FileKind.IMAGE
        "mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "ts", "m2ts", "m4v", "wmv", "ogv", "vob" -> FileKind.VIDEO
        "mp3", "wav", "aac", "flac", "ogg", "m4a" -> FileKind.AUDIO
        "pdf" -> FileKind.PDF
        "docx", "doc" -> FileKind.DOCX
        "xlsx", "xls", "csv" -> FileKind.XLSX
        "html", "htm", "css", "js", "json", "xml", "kt", "py" -> FileKind.CODE
        "txt", "md", "log" -> FileKind.TEXT
        "apk" -> FileKind.APK
        "zip", "rar", "7z", "tar", "gz" -> FileKind.ARCHIVE
        else -> FileKind.OTHER
      }

    fun guessMimeType(ext: String): String =
      when (ext.lowercase()) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "ts", "m2ts", "m4v", "wmv", "ogv", "vob" -> "video/*"
        "mp3", "wav", "aac", "flac", "ogg", "m4a" -> "audio/*"
        "pdf" -> "application/pdf"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "zip" -> "application/zip"
        "rar" -> "application/vnd.rar"
        "apk" -> "application/vnd.android.package-archive"
        "html", "htm" -> "text/html"
        else -> "text/plain"
      }
  }
}
