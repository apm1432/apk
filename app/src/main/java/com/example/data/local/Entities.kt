package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val description: String,
    val instructor: String,
    val thumbnailUrl: String = "",
    val totalLectures: Int = 0,
    val durationText: String = "4h 30m",
    val isEnrolled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "lectures")
data class LectureEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val folderName: String = "Module 1: Fundamentals",
    val title: String,
    val streamToken: String = "",
    val streamUrl: String = "",
    val durationSeconds: Int = 1200,
    val fileType: String = "VIDEO", // VIDEO or PDF
    val fileSizeMb: Float = 45.5f,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val orderIndex: Int = 0
)

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
    val lectureId: String,
    val courseId: String,
    val title: String,
    val progressPercent: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val status: String = "PENDING", // PENDING, DOWNLOADING, COMPLETED, PAUSED, FAILED
    val localPath: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val lectureId: String,
    val courseId: String,
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isCompleted: Boolean = false,
    val lastWatchedAt: Long = System.currentTimeMillis()
)
