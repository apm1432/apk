package com.example.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.LectureEntity
import com.example.data.network.KoyebApiClient
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MintGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerScreen(
    lecture: LectureEntity,
    courseTitle: String,
    onBack: () -> Unit,
    onSaveProgress: (Long, Long) -> Unit
) {
    BackHandler { onBack() }
    val apiClient = KoyebApiClient.instance

    val isOfflineAvailable = remember(lecture) {
        lecture.isDownloaded &&
                !lecture.localFilePath.isNullOrBlank() &&
                File(lecture.localFilePath).exists() &&
                File(lecture.localFilePath).length() > 500
    }

    // Inbuilt Native Player is the primary default engine for both online and offline playback
    var useWebEngine by remember { mutableStateOf(false) }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf((lecture.durationSeconds * 1000L).coerceAtLeast(1000L)) }
    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    var currentToken by remember {
        mutableStateOf(
            lecture.streamToken.ifBlank {
                lecture.streamUrl.removePrefix(apiClient.baseUrl).removePrefix("/watch/").trim()
            }
        )
    }
    var isRefreshingToken by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var resolvedStreamUrl by remember { mutableStateOf<String?>(null) }

    fun playInVideoView(vv: VideoView, url: String) {
        if (isOfflineAvailable && !lecture.localFilePath.isNullOrBlank()) {
            vv.setVideoPath(lecture.localFilePath)
            vv.start()
            isPlaying = true
            isBuffering = false
            return
        }

        val headers = mutableMapOf<String, String>()
        val cookie = apiClient.getCookieHeader()
        if (cookie.isNotBlank()) {
            headers["Cookie"] = cookie
        }
        headers["User-Agent"] = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
        headers["Referer"] = "${apiClient.baseUrl}/watch/$currentToken"
        headers["Range"] = "bytes=0-"

        try {
            vv.setVideoURI(Uri.parse(url), headers)
        } catch (_: Exception) {
            vv.setVideoPath(url)
        }
        vv.start()
        isPlaying = true
    }

    // Resolve direct stream URL with nonce and tokens for inbuilt player
    LaunchedEffect(currentToken, isOfflineAvailable, useWebEngine) {
        if (useWebEngine) return@LaunchedEffect

        if (isOfflineAvailable && !lecture.localFilePath.isNullOrBlank()) {
            resolvedStreamUrl = lecture.localFilePath
            videoViewRef?.let { playInVideoView(it, lecture.localFilePath!!) }
            isBuffering = false
            playbackError = null
        } else {
            isBuffering = true
            playbackError = null
            val res = apiClient.resolveDirectStreamUrl(currentToken)
            if (res.isSuccess && !res.getOrNull().isNullOrBlank()) {
                val direct = res.getOrNull()!!
                resolvedStreamUrl = direct
                videoViewRef?.let { playInVideoView(it, direct) }
            } else {
                // If stream resolution failed, try refreshing the token from Koyeb
                val freshRes = apiClient.refreshLectureToken(lecture.courseId, lecture.title)
                if (freshRes.isSuccess && !freshRes.getOrNull().isNullOrBlank()) {
                    val freshTok = freshRes.getOrNull()!!
                    currentToken = freshTok
                    val secondRes = apiClient.resolveDirectStreamUrl(freshTok)
                    if (secondRes.isSuccess && !secondRes.getOrNull().isNullOrBlank()) {
                        val direct = secondRes.getOrNull()!!
                        resolvedStreamUrl = direct
                        videoViewRef?.let { playInVideoView(it, direct) }
                        return@LaunchedEffect
                    }
                }
                isBuffering = false
                playbackError = "Stream link expired or login required (HTTP 403). Tap 'Refresh Link & Retry' below."
            }
        }
    }

    // Auto-update position for native player
    LaunchedEffect(videoViewRef, isPlaying) {
        while (true) {
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    currentPositionMs = vv.currentPosition.toLong()
                    val dur = vv.duration.toLong()
                    if (dur > 0) durationMs = dur
                    isBuffering = false
                }
            }
            delay(500)
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.let { vv ->
                onSaveProgress(vv.currentPosition.toLong(), vv.duration.toLong())
                vv.stopPlayback()
            }
        }
    }

    fun refreshStreamTokenAndReload() {
        isRefreshingToken = true
        playbackError = null
        isBuffering = true
        scope.launch {
            val res = apiClient.refreshLectureToken(lecture.courseId, lecture.title)
            isRefreshingToken = false
            res.onSuccess { freshToken ->
                currentToken = freshToken
                if (useWebEngine) {
                    val url = apiClient.buildWatchUrl(freshToken)
                    apiClient.syncAllCookiesToWebView()
                    webViewRef?.loadUrl(url)
                } else {
                    val streamRes = apiClient.resolveDirectStreamUrl(freshToken)
                    if (streamRes.isSuccess && !streamRes.getOrNull().isNullOrBlank()) {
                        val direct = streamRes.getOrNull()!!
                        resolvedStreamUrl = direct
                        videoViewRef?.let { playInVideoView(it, direct) }
                    }
                }
            }.onFailure {
                playbackError = "Session expired or access restricted (HTTP 403). Please sign in to your registered account."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
            .testTag("video_player_screen")
    ) {
        if (useWebEngine) {
            // Web view player fallback
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        apiClient.syncAllCookiesToWebView()

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isBuffering = false
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                if (request?.isForMainFrame == true) {
                                    isBuffering = false
                                    playbackError = "Network error: ${error?.description ?: "Cannot reach server"}"
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: android.webkit.WebResourceResponse?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    val code = errorResponse?.statusCode ?: 0
                                    isBuffering = false
                                    if (code == 403) {
                                        playbackError = "Stream link expired or login required (HTTP 403). Tap 'Refresh Link' below."
                                    } else if (code >= 400) {
                                        playbackError = "Server HTTP $code error: ${errorResponse?.reasonPhrase ?: "Playback failed"}"
                                    }
                                }
                            }
                        }
                        webChromeClient = WebChromeClient()

                        webViewRef = this
                        val targetUrl = apiClient.buildWatchUrl(currentToken)
                        loadUrl(targetUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Inbuilt Native Android Video Player with full authenticated streaming
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setOnPreparedListener { mp ->
                            isBuffering = false
                            playbackError = null
                            val dur = mp.duration.toLong()
                            if (dur > 0) durationMs = dur
                            mp.start()
                            isPlaying = true
                        }
                        setOnCompletionListener {
                            isPlaying = false
                            onSaveProgress(durationMs, durationMs)
                        }
                        setOnErrorListener { _, what, extra ->
                            isBuffering = false
                            playbackError = "Inbuilt player error ($what, $extra). Tap 'Refresh Link & Retry' below."
                            true
                        }
                        videoViewRef = this
                        resolvedStreamUrl?.let { playInVideoView(this, it) }
                    }
                },
                update = { vv ->
                    videoViewRef = vv
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Buffering indicator
        if ((isBuffering || isRefreshingToken) && playbackError == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanGlow, strokeWidth = 3.dp)
            }
        }

        // Playback Error Card
        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.90f))
                        .padding(24.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = playbackError!!,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { refreshStreamTokenAndReload() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isRefreshingToken) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("🔄 Refresh Link & Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                        Button(
                            onClick = {
                                useWebEngine = !useWebEngine
                                playbackError = null
                                isBuffering = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (useWebEngine) "Try Native Player" else "Use Web Player")
                        }
                    }
                }
            }
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_video_player")) {
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
                            text = if (isOfflineAvailable) "$courseTitle • Offline Playback" else "$courseTitle • Live Stream",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOfflineAvailable) MintGreen else CyanGlow
                        )
                    }

                    // Engine switch button
                    Button(
                        onClick = {
                            useWebEngine = !useWebEngine
                            playbackError = null
                            isBuffering = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (useWebEngine) "Inbuilt Player" else "Web View",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }

                // Center Play/Pause & Seek buttons (when using native player)
                if (!useWebEngine) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                videoViewRef?.let { vv ->
                                    val newPos = (vv.currentPosition - 10000).coerceAtLeast(0)
                                    vv.seekTo(newPos)
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue.copy(alpha = 0.8f))
                                .clickable {
                                    videoViewRef?.let { vv ->
                                        if (vv.isPlaying) {
                                            vv.pause()
                                            isPlaying = false
                                        } else {
                                            vv.start()
                                            isPlaying = true
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                videoViewRef?.let { vv ->
                                    val newPos = (vv.currentPosition + 10000).coerceAtMost(vv.duration)
                                    vv.seekTo(newPos)
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }

                    // Bottom Scrubber Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Slider(
                            value = currentPositionMs.toFloat(),
                            onValueChange = { newPos ->
                                currentPositionMs = newPos.toLong()
                                videoViewRef?.seekTo(newPos.toInt())
                            },
                            valueRange = 0f..durationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = CyanGlow,
                                activeTrackColor = CyanGlow,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
