package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.LectureEntity
import com.example.data.network.KoyebApiClient
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MintGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@Composable
fun ImageViewerScreen(
    lecture: LectureEntity,
    courseTitle: String,
    onBack: () -> Unit,
    onDownload: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val apiClient = KoyebApiClient.instance
    val scope = rememberCoroutineScope()

    val isOffline = lecture.isDownloaded &&
            !lecture.localFilePath.isNullOrBlank() &&
            File(lecture.localFilePath).exists()

    var activeImageFile by remember { mutableStateOf<File?>(if (isOffline) File(lecture.localFilePath!!) else null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(!isOffline) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Zoom & pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    fun loadOfflineBitmap(file: File) {
        try {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            if (bmp != null) {
                bitmap = bmp
                isLoading = false
                errorMessage = null
            } else {
                isLoading = false
                errorMessage = "Failed to decode image file"
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = e.localizedMessage ?: "Image error"
        }
    }

    var useWebView by remember { mutableStateOf(false) }

    fun fetchAndDecodeOnlineImage(forceRefreshToken: Boolean = false) {
        isLoading = true
        errorMessage = null
        scope.launch(Dispatchers.IO) {
            try {
                var token = lecture.streamToken.ifBlank {
                    lecture.streamUrl.removePrefix(apiClient.baseUrl).removePrefix("/watch/").trim()
                }

                if (forceRefreshToken || token.isBlank()) {
                    val freshRes = apiClient.refreshLectureToken(lecture.courseId, lecture.title)
                    if (freshRes.isSuccess && !freshRes.getOrNull().isNullOrBlank()) {
                        token = freshRes.getOrNull()!!
                    }
                }

                // 1. Resolve nonce-protected direct stream URL from watch page
                val resolvedUrl = apiClient.resolveDirectStreamUrl(token).getOrNull() ?: "${apiClient.baseUrl}/watch-stream/$token"
                val cookieHeader = apiClient.getCookieHeader()

                val reqBuilder = Request.Builder()
                    .url(resolvedUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                    .header("Referer", "${apiClient.baseUrl}/watch/$token")
                    .header("Range", "bytes=0-")

                if (cookieHeader.isNotBlank()) {
                    reqBuilder.header("Cookie", cookieHeader)
                }

                val response = apiClient.client.newCall(reqBuilder.build()).execute()
                response.use { resp ->
                    if (resp.code == 403 && !forceRefreshToken) {
                        // Expired token retry
                        fetchAndDecodeOnlineImage(forceRefreshToken = true)
                        return@launch
                    }

                    if (!resp.isSuccessful) {
                        throw Exception("Image server error HTTP ${resp.code}: ${resp.message}. Switch to Web Viewer below.")
                    }

                    val bytes = resp.body?.bytes() ?: throw Exception("Empty image bytes from server")
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp == null) {
                        // Might be HTML error or unsupported format, switch to web view
                        withContext(Dispatchers.Main) {
                            useWebView = true
                            isLoading = false
                        }
                        return@launch
                    }

                    val cacheFile = File(context.cacheDir, "img_${lecture.id.hashCode()}.jpg")
                    FileOutputStream(cacheFile).use { it.write(bytes) }

                    withContext(Dispatchers.Main) {
                        activeImageFile = cacheFile
                        bitmap = bmp
                        isLoading = false
                        errorMessage = null
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = e.localizedMessage ?: "Failed to load image from server"
                }
            }
        }
    }

    LaunchedEffect(lecture) {
        if (isOffline) {
            loadOfflineBitmap(File(lecture.localFilePath!!))
        } else {
            fetchAndDecodeOnlineImage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("image_viewer_screen")
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_image")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lecture.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = if (isOffline) "$courseTitle • Offline Photo" else "$courseTitle • Telegram Image",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOffline) MintGreen else CyanGlow
                )
            }

            // Open in external gallery
            IconButton(
                onClick = {
                    try {
                        val file = activeImageFile
                        if (file != null && file.exists()) {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "image/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "View Image with"))
                        }
                    } catch (e: Exception) {}
                },
                modifier = Modifier.testTag("btn_open_external_image")
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = "Open in Gallery", tint = CyanGlow)
            }

            // Download offline button
            if (!isOffline) {
                IconButton(onClick = onDownload, modifier = Modifier.testTag("btn_download_image")) {
                    Icon(Icons.Default.Download, contentDescription = "Download Image", tint = MintGreen)
                }
            }
        }

        // Image Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyanGlow, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Loading Image...", color = TextSecondary, fontSize = 13.sp)
                    }
                }
                errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(text = errorMessage!!, color = DangerRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { fetchAndDecodeOnlineImage() },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Loading Image")
                        }
                    }
                }
                bitmap != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    if (scale == 1f) {
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = lecture.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
