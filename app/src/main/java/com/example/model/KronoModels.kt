package com.example.model

enum class FileCategory(val label: String) {
  PHOTOS("Photos"),
  VIDEOS("Videos"),
  AUDIO("Audio / MP3"),
  DOCUMENTS("Documents"),
  DOWNLOADS("Downloads"),
  APKS("APKs"),
  ARCHIVES("ZIP / RAR"),
}

enum class FileKind {
  FOLDER,
  IMAGE,
  VIDEO,
  AUDIO,
  PDF,
  DOCX,
  XLSX,
  CODE,
  TEXT,
  APK,
  ARCHIVE,
  OTHER,
}

data class KronoFileItem(
  val id: String,
  val name: String,
  val absolutePath: String,
  val relativePath: String,
  val parentRelativePath: String,
  val isDirectory: Boolean,
  val sizeBytes: Long,
  val lastModifiedMs: Long,
  val extension: String,
  val kind: FileKind,
  val childCount: Int = 0,
  val contentUri: String? = null,
)

enum class SortBy(val label: String) {
  NAME("Name"),
  SIZE("Size"),
  DATE("Date"),
  TYPE("Type"),
}

enum class FilesViewMode {
  FOLDER,
  CHRONOS,
}

enum class ChronosBucket(val label: String) {
  TODAY("Today"),
  YESTERDAY("Yesterday"),
  THIS_WEEK("This Week"),
  THIS_MONTH("This Month"),
  EARLIER("Earlier"),
}

data class StorageStats(
  val totalBytes: Long = 128L * 1024 * 1024 * 1024,
  val usedBytes: Long = 64L * 1024 * 1024 * 1024,
  val freeBytes: Long = 64L * 1024 * 1024 * 1024,
  val kronoManagedBytes: Long = 0L,
  val categorySummaries: Map<FileCategory, CategorySummary> = emptyMap(),
) {
  val usedFraction: Float
    get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class CategorySummary(
  val category: FileCategory,
  val itemCount: Int,
  val totalBytes: Long,
)

data class DuplicateGroup(
  val hash: String,
  val fileSize: Long,
  val files: List<KronoFileItem>,
  val keepFilePath: String,
)

enum class SearchTypeFilter(val label: String) {
  ALL("All"),
  IMAGES("Images"),
  VIDEOS("Videos"),
  AUDIO("Audio"),
  PDF("PDF"),
  DOCS("Docs"),
  SHEETS("Sheets"),
  CODE("Code"),
  APKS("APKs"),
  ARCHIVES("ZIP/RAR"),
}

enum class SearchSizeFilter(val label: String, val minBytes: Long, val maxBytes: Long?) {
  ANY("Any Size", 0L, null),
  SMALL("< 1 MB", 0L, 1024L * 1024L),
  OVER_1MB("> 1 MB", 1024L * 1024L, null),
  OVER_100MB("> 100 MB", 100L * 1024L * 1024L, null),
  OVER_500MB("> 500 MB", 500L * 1024L * 1024L, null),
}

enum class SearchDateFilter(val label: String) {
  ANY("Any Time"),
  TODAY("Today"),
  LAST_7_DAYS("Last 7 Days"),
  LAST_30_DAYS("Last 30 Days"),
}

enum class SearchScope(val label: String) {
  ENTIRE_STORAGE("Entire Storage"),
  CURRENT_FOLDER("Current Folder"),
}

enum class CleanupSizeFilter(val label: String, val minBytes: Long) {
  ALL("All Large", 100L * 1024L),
  OVER_1MB("> 1 MB", 1024L * 1024L),
  OVER_10MB("> 10 MB", 10L * 1024L * 1024L),
  OVER_100MB("> 100 MB", 100L * 1024L * 1024L),
  OVER_500MB("> 500 MB", 500L * 1024L * 1024L),
  OVER_1GB("> 1 GB", 1024L * 1024L * 1024L),
}

data class AudioTrackItem(
  val path: String,
  val title: String,
  val artist: String,
  val album: String,
  val durationMs: Long,
  val sizeBytes: Long,
  val waveformPeaks: List<Float>,
  val contentUri: String? = null,
)

enum class BiometricVisualState {
  IDLE_SCANNING,
  SUCCESS,
  FAILURE,
}

enum class NotificationCategory {
  STORAGE_DUPLICATES,
  ACTIVITY,
}

enum class NotificationSeverity {
  WARNING,
  CYAN_INFO,
  VIOLET_SYSTEM,
}

enum class RepeatModeState {
  OFF,
  ALL,
  ONE,
}
