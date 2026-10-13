package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.CourseDao
import com.example.data.local.DownloadTaskEntity
import com.example.data.network.KoyebApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class ActiveDownloadProgress(
    val lectureId: String,
    val progress: Float,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val status: String,
    val errorMessage: String? = null
)

class DownloadManager(
    private val context: Context,
    private val courseDao: CourseDao,
    private val apiClient: KoyebApiClient = KoyebApiClient.instance
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = mutableMapOf<String, Job>()

    private val _downloadProgressMap = MutableStateFlow<Map<String, ActiveDownloadProgress>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, ActiveDownloadProgress>> = _downloadProgressMap.asStateFlow()

    private val downloadDir: File by lazy {
        val dir = File(context.filesDir, "downloads")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    fun startDownload(
        lectureId: String,
        courseId: String,
        title: String,
        streamUrl: String,
        fileType: String = "VIDEO"
    ) {
        if (activeJobs[lectureId]?.isActive == true) return

        val job = scope.launch {
            val extension = when {
                fileType.equals("PDF", ignoreCase = true) -> "pdf"
                fileType.equals("IMAGE", ignoreCase = true) || fileType.equals("PHOTO", ignoreCase = true) -> "jpg"
                fileType.equals("AUDIO", ignoreCase = true) -> "mp3"
                else -> "mp4"
            }
            val targetFile = File(downloadDir, "${lectureId}.$extension")

            val initialTask = DownloadTaskEntity(
                id = lectureId,
                lectureId = lectureId,
                courseId = courseId,
                title = title,
                progressPercent = 0.05f,
                status = "DOWNLOADING",
                updatedAt = System.currentTimeMillis()
            )
            courseDao.insertDownloadTask(initialTask)
            updateProgressState(lectureId, 0.05f, 0, 0, "DOWNLOADING", null)

            try {
                var token = streamUrl.removePrefix(apiClient.baseUrl).removePrefix("/watch/").removePrefix("/pdf-view/").trim()

                // Resolve authenticated download URL
                suspend fun resolveUrl(tok: String): String = when {
                    fileType.equals("PDF", ignoreCase = true) -> apiClient.buildPdfDownloadUrl(tok)
                    fileType.equals("IMAGE", ignoreCase = true) || fileType.equals("PHOTO", ignoreCase = true) -> {
                        apiClient.resolveDirectStreamUrl(tok).getOrNull() ?: "${apiClient.baseUrl}/watch-stream/$tok"
                    }
                    else -> {
                        val resolved = apiClient.resolveDirectStreamUrl(tok)
                        if (resolved.isSuccess && resolved.getOrNull()?.isNotBlank() == true) {
                            resolved.getOrNull()!!
                        } else {
                            "${apiClient.baseUrl}/watch-stream/$tok"
                        }
                    }
                }

                var downloadUrl = resolveUrl(token)
                val cookieHeader = apiClient.getCookieHeader()

                fun buildReq(url: String, tok: String): Request {
                    val builder = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                        .header("Referer", "${apiClient.baseUrl}/watch/$tok")
                        .header("Range", "bytes=0-")
                    if (cookieHeader.isNotBlank()) {
                        builder.header("Cookie", cookieHeader)
                    }
                    return builder.build()
                }

                var response = apiClient.client.newCall(buildReq(downloadUrl, token)).execute()

                // If 403, stream link expired — attempt refresh token once
                if (response.code == 403) {
                    response.close()
                    val freshRes = apiClient.refreshLectureToken(courseId, title)
                    if (freshRes.isSuccess && !freshRes.getOrNull().isNullOrBlank()) {
                        token = freshRes.getOrNull()!!
                        downloadUrl = resolveUrl(token)
                        response = apiClient.client.newCall(buildReq(downloadUrl, token)).execute()
                    }
                }

                response.use { resp ->
                    if (!resp.isSuccessful) {
                        throw Exception("Download failed: Server returned HTTP ${resp.code} (${resp.message})")
                    }

                    val contentType = resp.header("Content-Type", "") ?: ""
                    if (contentType.contains("text/html") && !fileType.equals("HTML", ignoreCase = true)) {
                        throw Exception("Server required session authorization or returned error page instead of media bytes.")
                    }

                    val body = resp.body ?: throw Exception("Empty response body from media stream")
                    val rawLength = body.contentLength()
                    val contentRange = resp.header("Content-Range", "") ?: ""
                    val totalFromRange = if (contentRange.contains("/")) {
                        contentRange.substringAfter("/").toLongOrNull() ?: 0L
                    } else 0L
                    val contentLength = if (rawLength > 0) rawLength else if (totalFromRange > 0) totalFromRange else 0L
                    val inputStream: InputStream = body.byteStream()
                    val outputStream = FileOutputStream(targetFile)

                    val buffer = ByteArray(32768)
                    var bytesRead: Long = 0
                    var read: Int
                    var lastEmittedTime = 0L

                    inputStream.use { input ->
                        outputStream.use { output ->
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                bytesRead += read

                                val now = System.currentTimeMillis()
                                if (now - lastEmittedTime > 300) {
                                    lastEmittedTime = now
                                    val progress = if (contentLength > 0) {
                                        (bytesRead.toFloat() / contentLength).coerceIn(0.05f, 0.99f)
                                    } else {
                                        0.5f
                                    }
                                    updateProgressState(lectureId, progress, bytesRead, contentLength, "DOWNLOADING", null)
                                    courseDao.insertDownloadTask(
                                        DownloadTaskEntity(
                                            id = lectureId,
                                            lectureId = lectureId,
                                            courseId = courseId,
                                            title = title,
                                            progressPercent = progress,
                                            bytesDownloaded = bytesRead,
                                            totalBytes = contentLength,
                                            status = "DOWNLOADING",
                                            updatedAt = now
                                        )
                                    )
                                }
                            }
                        }
                    }

                    val finalLength = targetFile.length()
                    if (finalLength < 200) {
                        targetFile.delete()
                        throw Exception("Downloaded file is invalid or too small (${finalLength} bytes)")
                    }

                    val completedTask = DownloadTaskEntity(
                        id = lectureId,
                        lectureId = lectureId,
                        courseId = courseId,
                        title = title,
                        progressPercent = 1.0f,
                        bytesDownloaded = finalLength,
                        totalBytes = finalLength,
                        status = "COMPLETED",
                        localPath = targetFile.absolutePath,
                        updatedAt = System.currentTimeMillis()
                    )
                    courseDao.insertDownloadTask(completedTask)
                    courseDao.updateLectureDownloadStatus(lectureId, isDownloaded = true, localPath = targetFile.absolutePath)
                    updateProgressState(lectureId, 1.0f, finalLength, finalLength, "COMPLETED", null)
                }
            } catch (e: CancellationException) {
                updateProgressState(lectureId, 0f, 0, 0, "PAUSED", null)
                courseDao.insertDownloadTask(
                    DownloadTaskEntity(
                        id = lectureId,
                        lectureId = lectureId,
                        courseId = courseId,
                        title = title,
                        progressPercent = 0f,
                        status = "PAUSED",
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Download failed"
                Log.e("DownloadManager", "Download failed for $lectureId: $errorMsg")
                if (targetFile.exists()) targetFile.delete()

                updateProgressState(lectureId, 0f, 0, 0, "FAILED", errorMsg)
                courseDao.insertDownloadTask(
                    DownloadTaskEntity(
                        id = lectureId,
                        lectureId = lectureId,
                        courseId = courseId,
                        title = title,
                        progressPercent = 0f,
                        status = "FAILED",
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } finally {
                activeJobs.remove(lectureId)
            }
        }

        activeJobs[lectureId] = job
    }

    fun pauseDownload(lectureId: String) {
        activeJobs[lectureId]?.cancel()
        activeJobs.remove(lectureId)
    }

    fun deleteDownloadedFile(lectureId: String, courseId: String) {
        scope.launch {
            try {
                val extensions = listOf("mp4", "pdf", "jpg", "mp3", "bin")
                for (ext in extensions) {
                    val f = File(downloadDir, "$lectureId.$ext")
                    if (f.exists()) f.delete()
                }

                courseDao.deleteDownloadTask(lectureId)
                courseDao.updateLectureDownloadStatus(lectureId, isDownloaded = false, localPath = null)

                val current = _downloadProgressMap.value.toMutableMap()
                current.remove(lectureId)
                _downloadProgressMap.value = current
            } catch (e: Exception) {
                Log.e("DownloadManager", "Error deleting download: ${e.message}")
            }
        }
    }

    fun getStorageMetrics(): Pair<Float, Float> {
        val downloadSize = downloadDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        val appGb = downloadSize.toFloat() / (1024 * 1024 * 1024)
        val freeGb = downloadDir.freeSpace.toFloat() / (1024 * 1024 * 1024)
        return Pair(appGb, freeGb)
    }

    private fun updateProgressState(
        lectureId: String,
        progress: Float,
        downloaded: Long,
        total: Long,
        status: String,
        error: String?
    ) {
        val current = _downloadProgressMap.value.toMutableMap()
        current[lectureId] = ActiveDownloadProgress(
            lectureId = lectureId,
            progress = progress,
            bytesDownloaded = downloaded,
            totalBytes = total,
            status = status,
            errorMessage = error
        )
        _downloadProgressMap.value = current
    }
}
