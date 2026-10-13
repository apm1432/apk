package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CourseEntity
import com.example.data.local.LectureEntity
import com.example.data.repository.ActiveDownloadProgress
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MintGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CourseDetailScreen(
    course: CourseEntity,
    lectures: List<LectureEntity>,
    downloadProgressMap: Map<String, ActiveDownloadProgress>,
    onBack: () -> Unit,
    onPlayLecture: (LectureEntity) -> Unit,
    onOpenPdf: (LectureEntity) -> Unit,
    onOpenImage: (LectureEntity) -> Unit,
    onStartDownload: (LectureEntity) -> Unit,
    onDeleteDownload: (LectureEntity) -> Unit
) {
    BackHandler { onBack() }

    // Deduplicate lectures by normalized title so duplicate cards NEVER appear
    val uniqueLectures = remember(lectures) {
        lectures.distinctBy { lec ->
            lec.title.trim().lowercase()
                .replace(Regex("(?i)\\.(mp4|pdf|mkv|mov|avi|jpg|jpeg|png)$"), "")
                .replace(Regex("^[0-9]+[.\\s-]+"), "")
                .trim()
        }
    }

    // Group lectures by folder
    val groupedLectures = remember(uniqueLectures) {
        uniqueLectures.groupBy { it.folderName }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("course_detail_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // App bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_from_detail")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    text = course.category,
                    style = MaterialTheme.typography.titleSmall,
                    color = CyanGlow,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { /* Share link */ }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondary)
                }
            }
        }

        // Course Info Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = course.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Instructor", fontSize = 11.sp, color = TextMuted)
                            Text(course.instructor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(course.durationText, fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Curriculum Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Course Curriculum",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${uniqueLectures.size} Items",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Folder & Lecture List
        if (uniqueLectures.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No items loaded for this course yet.", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            groupedLectures.forEach { (folder, lectureList) ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = folder,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow
                        )
                    }
                }

                items(lectureList, key = { it.id }) { lecture ->
                    val progress = downloadProgressMap[lecture.id]
                    LectureItemRow(
                        lecture = lecture,
                        downloadProgress = progress,
                        onPlay = {
                            val type = lecture.fileType.uppercase()
                            when {
                                type.contains("PDF") -> onOpenPdf(lecture)
                                type.contains("PHOTO") || type.contains("IMAGE") -> onOpenImage(lecture)
                                else -> onPlayLecture(lecture)
                            }
                        },
                        onStartDownload = { onStartDownload(lecture) },
                        onDeleteDownload = { onDeleteDownload(lecture) }
                    )
                }
            }
        }
    }
}

@Composable
fun LectureItemRow(
    lecture: LectureEntity,
    downloadProgress: ActiveDownloadProgress?,
    onPlay: () -> Unit,
    onStartDownload: () -> Unit,
    onDeleteDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onPlay() }
            .testTag("lecture_row_${lecture.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isPdf = lecture.fileType.equals("PDF", ignoreCase = true)
            val isImg = lecture.fileType.contains("IMAGE", ignoreCase = true) || lecture.fileType.contains("PHOTO", ignoreCase = true)

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            isPdf -> CyanGlow.copy(alpha = 0.15f)
                            isImg -> MintGreen.copy(alpha = 0.15f)
                            else -> ElectricBlue.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isPdf -> Icons.Default.MenuBook
                        isImg -> Icons.Default.Image
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    tint = when {
                        isPdf -> CyanGlow
                        isImg -> MintGreen
                        else -> ElectricBlue
                    }
                )
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
                    val durationMins = lecture.durationSeconds / 60
                    val durStr = when {
                        isPdf -> "PDF Document"
                        isImg -> "Image File"
                        else -> "${durationMins}m"
                    }
                    Text(durStr, fontSize = 11.sp, color = TextSecondary)
                    Text(" • ${lecture.fileSizeMb} MB", fontSize = 11.sp, color = TextMuted)

                    if (lecture.isDownloaded) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("• Offline Ready", fontSize = 11.sp, color = MintGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Download Action
            if (lecture.isDownloaded) {
                IconButton(onClick = onDeleteDownload, modifier = Modifier.testTag("btn_delete_download_${lecture.id}")) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded", tint = MintGreen, modifier = Modifier.size(20.dp))
                }
            } else if (downloadProgress?.status == "DOWNLOADING") {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { downloadProgress.progress },
                        color = MintGreen,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "${(downloadProgress.progress * 100).toInt()}%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            } else {
                IconButton(onClick = onStartDownload, modifier = Modifier.testTag("btn_download_${lecture.id}")) {
                    Icon(Icons.Default.Download, contentDescription = "Download Offline", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
