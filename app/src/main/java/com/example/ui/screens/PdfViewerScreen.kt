package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
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
fun PdfViewerScreen(
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

    var activePdfFile by remember { mutableStateOf<File?>(if (isOffline) File(lecture.localFilePath!!) else null) }
    var isLoading by remember { mutableStateOf(!isOffline) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var pdfRendererRef by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfdRef by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    fun renderPage(renderer: PdfRenderer, index: Int) {
        if (index < 0 || index >= renderer.pageCount) return
        try {
            val page = renderer.openPage(index)
            // Render at 2x resolution for crisp text
            val targetWidth = (page.width * 2).coerceAtMost(2400)
            val targetHeight = (page.height * 2).coerceAtMost(3200)
            val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(android.graphics.Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            currentBitmap = bmp
            currentPageIndex = index
        } catch (e: Exception) {
            errorMessage = "Error rendering PDF page: ${e.message}"
        }
    }

    fun openRendererForFile(file: File) {
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            pfdRef = pfd
            pdfRendererRef = renderer
            totalPages = renderer.pageCount
            currentPageIndex = 0
            renderPage(renderer, 0)
            isLoading = false
            errorMessage = null
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Cannot open PDF: ${e.message}"
        }
    }

    var useWebView by remember { mutableStateOf(false) }

    fun fetchAndOpenOnlinePdf(forceRefreshToken: Boolean = false) {
        isLoading = true
        errorMessage = null
        scope.launch(Dispatchers.IO) {
            try {
                var token = lecture.streamToken.ifBlank {
                    lecture.streamUrl.removePrefix(apiClient.baseUrl).removePrefix("/watch/").removePrefix("/pdf-view/").trim()
                }

                if (forceRefreshToken || token.isBlank()) {
                    val freshRes = apiClient.refreshLectureToken(lecture.courseId, lecture.title)
                    if (freshRes.isSuccess && !freshRes.getOrNull().isNullOrBlank()) {
                        token = freshRes.getOrNull()!!
                    }
                }

                val targetUrl = apiClient.buildPdfDownloadUrl(token)
                val cookieHeader = apiClient.getCookieHeader()

                val reqBuilder = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                    .header("Referer", "${apiClient.baseUrl}/watch/$token")

                if (cookieHeader.isNotBlank()) {
                    reqBuilder.header("Cookie", cookieHeader)
                }

                val response = apiClient.client.newCall(reqBuilder.build()).execute()
                response.use { resp ->
                    if (resp.code == 403 && !forceRefreshToken) {
                        // Token might have expired (2-hr TTL). Retry once with fresh token!
                        fetchAndOpenOnlinePdf(forceRefreshToken = true)
                        return@launch
                    }

                    if (!resp.isSuccessful) {
                        throw Exception("PDF server error HTTP ${resp.code}: ${resp.message}. Tap 'Web Viewer' or 'Retry'.")
                    }

                    val contentType = resp.header("Content-Type", "") ?: ""
                    if (contentType.contains("text/html")) {
                        throw Exception("Server returned HTML page instead of PDF bytes (Session may need refresh).")
                    }

                    val body = resp.body ?: throw Exception("Empty PDF response from server")
                    val cacheFile = File(context.cacheDir, "pdf_${lecture.id.hashCode()}.pdf")
                    val fos = FileOutputStream(cacheFile)
                    body.byteStream().use { input ->
                        fos.use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (cacheFile.length() < 200) {
                        throw Exception("Received empty or corrupt PDF (${cacheFile.length()} bytes)")
                    }

                    withContext(Dispatchers.Main) {
                        activePdfFile = cacheFile
                        openRendererForFile(cacheFile)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = e.localizedMessage ?: "Failed to load PDF"
                }
            }
        }
    }

    LaunchedEffect(lecture) {
        if (isOffline) {
            openRendererForFile(File(lecture.localFilePath!!))
        } else {
            fetchAndOpenOnlinePdf()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRendererRef?.close()
                pfdRef?.close()
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("pdf_viewer_screen")
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_pdf")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lecture.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = if (isOffline) "$courseTitle • Offline PDF" else "$courseTitle • Online PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOffline) MintGreen else CyanGlow
                )
            }

            // External viewer button
            IconButton(
                onClick = {
                    try {
                        val file = activePdfFile
                        if (file != null && file.exists()) {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Open PDF with"))
                        }
                    } catch (e: Exception) {
                        // Fallback
                    }
                },
                modifier = Modifier.testTag("btn_open_external_pdf")
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = "External App", tint = CyanGlow)
            }

            // Download offline button
            if (!isOffline) {
                IconButton(onClick = onDownload, modifier = Modifier.testTag("btn_download_pdf")) {
                    Icon(Icons.Default.Download, contentDescription = "Download Offline", tint = MintGreen)
                }
            }
        }

        // PDF Body Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyanGlow, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Loading PDF pages...", color = TextSecondary, fontSize = 13.sp)
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
                            onClick = { fetchAndOpenOnlinePdf() },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Loading PDF")
                        }
                    }
                }
                currentBitmap != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 4f)
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
                            bitmap = currentBitmap!!.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPageIndex + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        // Bottom Controls Bar (Pagination and Zoom)
        if (totalPages > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev button
                IconButton(
                    onClick = {
                        if (currentPageIndex > 0) {
                            pdfRendererRef?.let { renderPage(it, currentPageIndex - 1) }
                        }
                    },
                    enabled = currentPageIndex > 0,
                    modifier = Modifier.testTag("btn_prev_page")
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous Page",
                        tint = if (currentPageIndex > 0) TextPrimary else TextSecondary.copy(alpha = 0.3f)
                    )
                }

                // Page indicator
                Text(
                    text = "Page ${currentPageIndex + 1} of $totalPages",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                // Next button
                IconButton(
                    onClick = {
                        if (currentPageIndex < totalPages - 1) {
                            pdfRendererRef?.let { renderPage(it, currentPageIndex + 1) }
                        }
                    },
                    enabled = currentPageIndex < totalPages - 1,
                    modifier = Modifier.testTag("btn_next_page")
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next Page",
                        tint = if (currentPageIndex < totalPages - 1) TextPrimary else TextSecondary.copy(alpha = 0.3f)
                    )
                }

                // Zoom controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { scale = (scale - 0.5f).coerceAtLeast(1f) }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { scale = (scale + 0.5f).coerceAtMost(4f) }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = CyanGlow, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
