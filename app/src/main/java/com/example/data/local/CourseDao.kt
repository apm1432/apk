package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    // Courses
    @Query("SELECT * FROM courses ORDER BY createdAt DESC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    fun getCourseById(courseId: String): Flow<CourseEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteCourseById(courseId: String)

    @Query("DELETE FROM courses")
    suspend fun clearAllCourses()

    @Query("DELETE FROM lectures")
    suspend fun clearAllLectures()

    // Lectures
    @Query("SELECT * FROM lectures WHERE courseId = :courseId ORDER BY orderIndex ASC")
    fun getLecturesForCourse(courseId: String): Flow<List<LectureEntity>>

    @Query("SELECT * FROM lectures WHERE courseId = :courseId ORDER BY orderIndex ASC")
    suspend fun getLecturesForCourseList(courseId: String): List<LectureEntity>

    @Query("SELECT * FROM lectures WHERE courseId = :courseId AND isDownloaded = 1")
    suspend fun getDownloadedLecturesForCourse(courseId: String): List<LectureEntity>

    @Query("SELECT * FROM lectures WHERE id = :lectureId LIMIT 1")
    fun getLectureById(lectureId: String): Flow<LectureEntity?>

    @Query("SELECT * FROM lectures WHERE isDownloaded = 1")
    fun getAllDownloadedLectures(): Flow<List<LectureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLectures(lectures: List<LectureEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecture(lecture: LectureEntity)

    @Query("UPDATE lectures SET isDownloaded = :isDownloaded, localFilePath = :localPath WHERE id = :lectureId")
    suspend fun updateLectureDownloadStatus(lectureId: String, isDownloaded: Boolean, localPath: String?)

    @Query("DELETE FROM lectures WHERE courseId = :courseId")
    suspend fun deleteLecturesByCourse(courseId: String)

    // Downloads
    @Query("SELECT * FROM download_tasks ORDER BY updatedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE id = :id LIMIT 1")
    suspend fun getDownloadTaskById(id: String): DownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadTask(task: DownloadTaskEntity)

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun deleteDownloadTask(id: String)

    // Watch History
    @Query("SELECT * FROM watch_history WHERE lectureId = :lectureId LIMIT 1")
    fun getWatchHistory(lectureId: String): Flow<WatchHistoryEntity?>

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    fun getAllWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchHistory(history: WatchHistoryEntity)
}
