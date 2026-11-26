package com.example.data.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.MediaStore
import com.example.data.local.KronoDatabase
import com.example.data.local.SyncedFileEntity
import com.example.data.storage.KronoStorageRepository
import com.example.model.KronoFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

data class DriveAccountInfo(
  val email: String,
  val displayName: String,
)

data class DriveSyncResult(
  val uploadedCount: Int,
  val skippedCount: Int,
  val failedCount: Int,
  val folderId: String,
)

class GoogleDriveSyncRepository(
  private val context: Context,
  private val database: KronoDatabase,
) {
  companion object {
    private const val MULTIPART_THRESHOLD_BYTES = 4L * 1024L * 1024L
  }

  private val client =
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(180, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .build()

  fun hasNetworkConnection(): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }

  suspend fun fetchDriveAccountInfo(accessToken: String): Result<DriveAccountInfo> =
    withContext(Dispatchers.IO) {
      runCatching {
        require(hasNetworkConnection()) { "No internet connection. Connect to Wi-Fi or mobile data." }
        val request =
          Request.Builder()
            .url("https://www.googleapis.com/drive/v3/about?fields=user(emailAddress,displayName)")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
          val body = response.body?.string().orEmpty()
          check(response.isSuccessful) { "Drive auth error (${response.code}): ${extractApiError(body)}" }
          val user = JSONObject(body).optJSONObject("user")
          val email = user?.optString("emailAddress").orEmpty().ifBlank { "Google Drive Account" }
          val name = user?.optString("displayName").orEmpty().ifBlank { email }
          DriveAccountInfo(email = email, displayName = name)
        }
      }
    }

  suspend fun ensureKronoBackupFolder(accessToken: String): Result<String> =
    withContext(Dispatchers.IO) {
      runCatching {
        val query =
          "name='KronoFiles_Backup' and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val encoded = URLEncoder.encode(query, "UTF-8")
        val listReq =
          Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?q=$encoded&fields=files(id,name)&spaces=drive")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()
        client.newCall(listReq).execute().use { response ->
          val body = response.body?.string().orEmpty()
          check(response.isSuccessful) { "Failed to query Drive folder (${response.code}): ${extractApiError(body)}" }
          val files = JSONObject(body).optJSONArray("files")
          if (files != null && files.length() > 0) {
            return@runCatching files.getJSONObject(0).getString("id")
          }
        }

        val createMeta =
          JSONObject()
            .put("name", "KronoFiles_Backup")
            .put("mimeType", "application/vnd.google-apps.folder")
            .toString()
        val createReq =
          Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?fields=id")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(createMeta.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .build()
        client.newCall(createReq).execute().use { response ->
          val body = response.body?.string().orEmpty()
          check(response.isSuccessful) { "Failed to create Drive folder (${response.code}): ${extractApiError(body)}" }
          JSONObject(body).getString("id")
        }
      }
    }

  suspend fun syncIncremental(
    accessToken: String,
    candidateFiles: List<KronoFileItem>,
    onProgress: (processed: Int, total: Int, currentName: String) -> Unit,
  ): Result<DriveSyncResult> =
    withContext(Dispatchers.IO) {
      runCatching {
        require(hasNetworkConnection()) { "No internet connection available for Google Drive Sync." }
        val folderId = ensureKronoBackupFolder(accessToken).getOrThrow()
        val existingLedger = database.syncedFileDao().getAllSynced().associateBy { it.filePath }
        val syncableFiles =
          candidateFiles.filter { item ->
            !item.isDirectory && item.sizeBytes > 0L && isItemReadable(item)
          }

        var uploaded = 0
        var skipped = 0
        var failed = 0
        val total = syncableFiles.size

        syncableFiles.forEachIndexed { index, item ->
          onProgress(index + 1, total, item.name)
          val ledgerKey = item.contentUri ?: item.absolutePath
          val ledgerEntry = existingLedger[ledgerKey] ?: existingLedger[item.absolutePath]

          // Fast skip when size and lastModified timestamp match the previous synced snapshot
          if (ledgerEntry != null &&
            ledgerEntry.sizeBytes == item.sizeBytes &&
            ledgerEntry.lastModifiedMs == item.lastModifiedMs
          ) {
            skipped++
            return@forEachIndexed
          }

          val sha256 = computeSha256(item)
          if (ledgerEntry != null &&
            ledgerEntry.sizeBytes == item.sizeBytes &&
            ledgerEntry.sha256Hash == sha256
          ) {
            database.syncedFileDao().upsertSynced(
              ledgerEntry.copy(
                lastModifiedMs = item.lastModifiedMs,
                syncedAtMs = System.currentTimeMillis(),
              )
            )
            skipped++
            return@forEachIndexed
          }

          val mimeType = KronoStorageRepository.guessMimeType(item.extension)
          val uploadResult =
            if (ledgerEntry != null) {
              uploadOrUpdateFile(
                accessToken = accessToken,
                item = item,
                mimeType = mimeType,
                parentFolderId = folderId,
                existingDriveId = ledgerEntry.driveFileId,
              )
            } else {
              uploadNewFile(
                accessToken = accessToken,
                item = item,
                mimeType = mimeType,
                parentFolderId = folderId,
              )
            }

          uploadResult
            .onSuccess { driveId ->
              database.syncedFileDao().upsertSynced(
                SyncedFileEntity(
                  filePath = ledgerKey,
                  sizeBytes = item.sizeBytes,
                  lastModifiedMs = item.lastModifiedMs,
                  sha256Hash = sha256,
                  driveFileId = driveId,
                  syncedAtMs = System.currentTimeMillis(),
                )
              )
              uploaded++
            }
            .onFailure {
              failed++
            }
        }

        DriveSyncResult(
          uploadedCount = uploaded,
          skippedCount = skipped,
          failedCount = failed,
          folderId = folderId,
        )
      }
    }

  private fun isItemReadable(item: KronoFileItem): Boolean {
    val file = File(item.absolutePath)
    if (file.exists() && file.canRead() && file.length() > 0L) return true
    if (!item.contentUri.isNullOrBlank()) {
      return runCatching {
        context.contentResolver.openInputStream(Uri.parse(item.contentUri))?.use { true } ?: false
      }.getOrDefault(false)
    }
    return false
  }

  private fun openItemStream(item: KronoFileItem): Pair<InputStream, Long> {
    val file = File(item.absolutePath)
    if (file.exists() && file.canRead()) {
      return FileInputStream(file) to file.length()
    }
    val uriStr = item.contentUri ?: error("File unreadable: ${item.name}")
    val uri = Uri.parse(uriStr)
    val len = item.sizeBytes.takeIf { it > 0L } ?: runCatching {
      context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
    }.getOrDefault(-1L)
    val stream = context.contentResolver.openInputStream(uri) ?: error("Unable to open stream for ${item.name}")
    return stream to len
  }

  private fun uploadOrUpdateFile(
    accessToken: String,
    item: KronoFileItem,
    mimeType: String,
    parentFolderId: String,
    existingDriveId: String,
  ): Result<String> {
    val patchResult =
      runCatching {
        val metadata =
          JSONObject()
            .put("name", item.name)
            .put("description", "KronoFiles incremental sync (${item.relativePath})")
            .toString()
        val length = File(item.absolutePath).takeIf { it.exists() }?.length() ?: item.sizeBytes
        if (length >= MULTIPART_THRESHOLD_BYTES || length <= 0L) {
          return@runCatching uploadResumable(
            accessToken = accessToken,
            item = item,
            metadataJson = metadata,
            mimeType = mimeType,
            initUrl = "https://www.googleapis.com/upload/drive/v3/files/$existingDriveId?uploadType=resumable&fields=id",
            usePatchForInit = true,
          )
        }
        val boundary = "krono_${UUID.randomUUID()}"
        val body = StreamingDriveMultipartBody(boundary, metadata, length, mimeType) { openItemStream(item).first }
        val req =
          Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files/$existingDriveId?uploadType=multipart&fields=id")
            .addHeader("Authorization", "Bearer $accessToken")
            .patch(body)
            .build()
        client.newCall(req).execute().use { response ->
          val respStr = response.body?.string().orEmpty()
          if (response.code == 404) {
            throw FileNotFoundRemoteException()
          }
          check(response.isSuccessful) { "Drive update error (${response.code}): ${extractApiError(respStr)}" }
          JSONObject(respStr).getString("id")
        }
      }
    if (patchResult.exceptionOrNull() is FileNotFoundRemoteException) {
      return uploadNewFile(accessToken, item, mimeType, parentFolderId)
    }
    return patchResult
  }

  private fun uploadNewFile(
    accessToken: String,
    item: KronoFileItem,
    mimeType: String,
    parentFolderId: String,
  ): Result<String> =
    runCatching {
      val metadata =
        JSONObject()
          .put("name", item.name)
          .put("description", "KronoFiles incremental sync (${item.relativePath})")
          .put("parents", JSONArray().put(parentFolderId))
          .toString()
      val length = File(item.absolutePath).takeIf { it.exists() }?.length() ?: item.sizeBytes
      if (length >= MULTIPART_THRESHOLD_BYTES || length <= 0L) {
        return@runCatching uploadResumable(
          accessToken = accessToken,
          item = item,
          metadataJson = metadata,
          mimeType = mimeType,
          initUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&fields=id",
          usePatchForInit = false,
        )
      }
      val boundary = "krono_${UUID.randomUUID()}"
      val body = StreamingDriveMultipartBody(boundary, metadata, length, mimeType) { openItemStream(item).first }
      val req =
        Request.Builder()
          .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id")
          .addHeader("Authorization", "Bearer $accessToken")
          .post(body)
          .build()
      client.newCall(req).execute().use { response ->
        val respStr = response.body?.string().orEmpty()
        check(response.isSuccessful) { "Drive upload error (${response.code}): ${extractApiError(respStr)}" }
        JSONObject(respStr).getString("id")
      }
    }

  private fun uploadResumable(
    accessToken: String,
    item: KronoFileItem,
    metadataJson: String,
    mimeType: String,
    initUrl: String,
    usePatchForInit: Boolean,
  ): String {
    val (streamForLen, resolvedLen) = openItemStream(item)
    streamForLen.close()
    val metaBody = metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
    val initReqBuilder =
      Request.Builder()
        .url(initUrl)
        .addHeader("Authorization", "Bearer $accessToken")
        .addHeader("X-Upload-Content-Type", mimeType)
    if (resolvedLen > 0L) {
      initReqBuilder.addHeader("X-Upload-Content-Length", resolvedLen.toString())
    }
    val initReq = if (usePatchForInit) initReqBuilder.patch(metaBody).build() else initReqBuilder.post(metaBody).build()

    val sessionUri =
      client.newCall(initReq).execute().use { initResp ->
        val respStr = initResp.body?.string().orEmpty()
        if (initResp.code == 404 && usePatchForInit) {
          throw FileNotFoundRemoteException()
        }
        check(initResp.isSuccessful) { "Drive resumable init error (${initResp.code}): ${extractApiError(respStr)}" }
        initResp.header("Location") ?: error("Drive resumable session URI missing")
      }

    val putBody =
      object : RequestBody() {
        override fun contentType(): MediaType = mimeType.toMediaType()
        override fun contentLength(): Long = resolvedLen
        override fun writeTo(sink: BufferedSink) {
          openItemStream(item).first.use { fis ->
            val buf = ByteArray(64 * 1024)
            var read = fis.read(buf)
            while (read != -1) {
              sink.write(buf, 0, read)
              read = fis.read(buf)
            }
          }
        }
      }
    val uploadReq =
      Request.Builder()
        .url(sessionUri)
        .put(putBody)
        .build()
    client.newCall(uploadReq).execute().use { uploadResp ->
      val respStr = uploadResp.body?.string().orEmpty()
      check(uploadResp.isSuccessful) { "Drive resumable upload error (${uploadResp.code}): ${extractApiError(respStr)}" }
      return JSONObject(respStr).getString("id")
    }
  }

  private fun computeSha256(item: KronoFileItem): String {
    val digest = MessageDigest.getInstance("SHA-256")
    openItemStream(item).first.use { fis ->
      val buffer = ByteArray(16 * 1024)
      var read = fis.read(buffer)
      while (read != -1) {
        digest.update(buffer, 0, read)
        read = fis.read(buffer)
      }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  private fun extractApiError(body: String): String {
    return runCatching {
      JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
    }.getOrDefault("").ifBlank { body.take(140).ifBlank { "Request failed" } }
  }

  private class FileNotFoundRemoteException : RuntimeException()

  private class StreamingDriveMultipartBody(
    private val boundary: String,
    metadataJson: String,
    private val contentBytesLength: Long,
    mimeType: String,
    private val openStream: () -> InputStream,
  ) : RequestBody() {
    private val headerBytes =
      buildString {
        append("--").append(boundary).append("\r\n")
        append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
        append(metadataJson).append("\r\n")
        append("--").append(boundary).append("\r\n")
        append("Content-Type: ").append(mimeType).append("\r\n\r\n")
      }.toByteArray(Charsets.UTF_8)

    private val footerBytes = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)

    override fun contentType(): MediaType = "multipart/related; boundary=$boundary".toMediaType()

    override fun contentLength(): Long = headerBytes.size.toLong() + contentBytesLength + footerBytes.size.toLong()

    override fun writeTo(sink: BufferedSink) {
      sink.write(headerBytes)
      openStream().use { fis ->
        val buf = ByteArray(32 * 1024)
        var read = fis.read(buf)
        while (read != -1) {
          sink.write(buf, 0, read)
          read = fis.read(buf)
        }
      }
      sink.write(footerBytes)
    }
  }
}
