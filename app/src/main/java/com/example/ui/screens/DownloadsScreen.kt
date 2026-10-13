package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LectureEntity
import com.example.data.repository.ActiveDownloadProgress
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MintGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DownloadsScreen(
    downloadedLectures: List<LectureEntity>,
    downloadProgressMap: Map<String, ActiveDownloadProgress>,
    storageMetrics: Pair<Float, Float>, // (AppDownloadGb, FreeDeviceGb)
    onPlayLecture: (LectureEntity) -> Unit,
    onPauseDownload: (String) -> Unit,
    onDeleteDownload: (LectureEntity) -> Unit,
    onNavigateToCourses: () -> Unit
) {
    val activeDownloads = downloadProgressMap.values.filter { it.status == "DOWNLOADING" || it.status == "PAUSED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("downloads_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MintGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DownloadDone, contentDescription = null, tint = MintGreen)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Offline Library",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Access your lectures anytime without internet connection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Storage Space Card
        item {
            StorageUsageCard(
                usedGb = storageMetrics.first,
                freeGb = storageMetrics.second,
                downloadCount = downloadedLectures.size
            )
        }

        // Active Downloads (if any)
        if (activeDownloads.isNotEmpty()) {
            item {
                Text(
                    text = "Active Downloads (${activeDownloads.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(activeDownloads, key = { it.lectureId }) { active ->
                ActiveDownloadRow(
                    active = active,
                    onPause = { onPauseDownload(active.lectureId) }
                )
            }
        }

        // Downloaded Lectures
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Lectures",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${downloadedLectures.size} files",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        if (downloadedLectures.isEmpty() && activeDownloads.isEmpty()) {
            item {
                EmptyDownloadsCard(onExplore = onNavigateToCourses)
            }
        } else {
            items(downloadedLectures, key = { it.id }) { lecture ->
                DownloadedLectureCard(
                    lecture = lecture,
                    onPlay = { onPlayLecture(lecture) },
                    onDelete = { onDeleteDownload(lecture) }
                )
            }
        }
    }
}

@Composable
fun StorageUsageCard(usedGb: Float, freeGb: Float, downloadCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SdStorage, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Device Storage", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Text(
                    text = String.format("%.2f MB Used", usedGb * 1024),
                    fontSize = 12.sp,
                    color = MintGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val total = (usedGb + freeGb).coerceAtLeast(1f)
            val fraction = (usedGb / total).coerceIn(0.01f, 1f)

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MintGreen,
                trackColor = CyberSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "$downloadCount offline files cached", fontSize = 11.sp, color = TextSecondary)
                Text(text = String.format("%.1f GB Free", freeGb), fontSize = 11.sp, color = TextMuted)
            }
        }
    }
}

@Composable
fun ActiveDownloadRow(
    active: ActiveDownloadProgress,
    onPause: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Downloading...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "${(active.progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintGreen
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { active.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = MintGreen,
                trackColor = CyberSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${active.bytesDownloaded / (1024 * 1024)} MB of ${active.totalBytes / (1024 * 1024)} MB",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                IconButton(onClick = onPause, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun DownloadedLectureCard(
    lecture: LectureEntity,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("downloaded_lecture_${lecture.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MintGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MintGreen)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lecture.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${lecture.durationSeconds / 60}m", fontSize = 11.sp, color = TextSecondary)
                    Text(text = " • ${lecture.fileSizeMb} MB", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "• Offline Ready", fontSize = 11.sp, color = MintGreen)
                }
            }

            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("btn_play_offline_${lecture.id}")
            ) {
                Text("Play", color = CyberBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp).testTag("btn_delete_${lecture.id}")) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun EmptyDownloadsCard(onExplore: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MintGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.WifiOff, contentDescription = null, tint = MintGreen, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Offline Downloads Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap the download icon next to any lecture in a course to watch it anywhere, even without mobile data or Wi-Fi.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onExplore,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_explore_from_empty_downloads")
            ) {
                Text("Browse Courses", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
