package com.example.data.repository

import com.example.data.local.CourseDao
import com.example.data.local.CourseEntity
import com.example.data.local.LectureEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.network.KoyebApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CourseRepository(
    private val courseDao: CourseDao,
    private val apiClient: KoyebApiClient = KoyebApiClient.instance
) {
    val allCourses: Flow<List<CourseEntity>> = courseDao.getAllCourses()
    val downloadedLectures: Flow<List<LectureEntity>> = courseDao.getAllDownloadedLectures()
    val allWatchHistory: Flow<List<WatchHistoryEntity>> = courseDao.getAllWatchHistory()

    fun getLecturesForCourse(courseId: String): Flow<List<LectureEntity>> {
        return courseDao.getLecturesForCourse(courseId)
    }

    fun getCourseById(courseId: String): Flow<CourseEntity?> {
        return courseDao.getCourseById(courseId)
    }

    fun getWatchHistoryForLecture(lectureId: String): Flow<WatchHistoryEntity?> {
        return courseDao.getWatchHistory(lectureId)
    }

    suspend fun saveWatchProgress(lectureId: String, courseId: String, positionMs: Long, durationMs: Long) {
        val isCompleted = durationMs > 0 && positionMs >= (durationMs * 0.95)
        val history = WatchHistoryEntity(
            lectureId = lectureId,
            courseId = courseId,
            lastPositionMs = positionMs,
            durationMs = durationMs,
            isCompleted = isCompleted,
            lastWatchedAt = System.currentTimeMillis()
        )
        courseDao.insertWatchHistory(history)
    }

    suspend fun updateCourseLectures(courseId: String, freshLectures: List<LectureEntity>) = withContext(Dispatchers.IO) {
        fun normKey(t: String) = t.trim().lowercase()
            .replace(Regex("(?i)\\.(mp4|pdf|mkv|mov|avi|jpg|jpeg|png)$"), "")
            .replace(Regex("^[0-9]+[.\\s-]+"), "")
            .trim()

        // 1. Get existing downloaded lectures for this course so we NEVER lose offline content
        val currentDownloads = courseDao.getDownloadedLecturesForCourse(courseId)
        val downloadMap = currentDownloads.associateBy { normKey(it.title) }

        // 2. Merge existing download local files and status into fresh lectures
        val deduplicated = freshLectures.distinctBy { normKey(it.title) }.mapIndexed { idx, lec ->
            val existingDownloaded = downloadMap[normKey(lec.title)]
            if (existingDownloaded != null && existingDownloaded.isDownloaded && !existingDownloaded.localFilePath.isNullOrBlank()) {
                lec.copy(
                    isDownloaded = true,
                    localFilePath = existingDownloaded.localFilePath,
                    orderIndex = idx + 1
                )
            } else {
                lec.copy(orderIndex = idx + 1)
            }
        }

        // 3. Clear old lectures for this course to prevent duplicates
        courseDao.deleteLecturesByCourse(courseId)

        // 4. Insert clean deduplicated lectures
        if (deduplicated.isNotEmpty()) {
            courseDao.insertLectures(deduplicated)
        }
    }

    suspend fun addOrUpdateCourse(course: CourseEntity, lectures: List<LectureEntity> = emptyList()) {
        courseDao.insertCourse(course)
        if (lectures.isNotEmpty()) {
            updateCourseLectures(course.id, lectures)
        }
    }

    suspend fun deleteCourse(courseId: String) {
        courseDao.deleteCourseById(courseId)
        courseDao.deleteLecturesByCourse(courseId)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        // Note: Preserves downloads and user content safely
        courseDao.clearAllCourses()
    }

    /**
     * Fast course synchronization from the live Koyeb server.
     * Courses are inserted immediately into the database for instantaneous UI update (< 500ms).
     * Lectures are loaded on-demand or in the background.
     */
    suspend fun syncRealCoursesFromKoyeb(): Result<Int> = withContext(Dispatchers.IO) {
        var totalLoaded = 0

        // 1. Try Student HTML parser (/courses) first
        val studentRes = apiClient.fetchStudentCoursesHtml()
        if (studentRes.isSuccess && !studentRes.getOrNull().isNullOrEmpty()) {
            val list = studentRes.getOrNull()!!.distinctBy { it.id }
            courseDao.insertCourses(list)
            totalLoaded += list.size
            return@withContext Result.success(totalLoaded)
        }

        // 2. Try Admin API (/api/admin/courses)
        val adminRes = apiClient.fetchAdminCourses()
        if (adminRes.isSuccess && !adminRes.getOrNull().isNullOrEmpty()) {
            val list = adminRes.getOrNull()!!.distinctBy { it.id }
            courseDao.insertCourses(list)
            totalLoaded += list.size
            return@withContext Result.success(totalLoaded)
        }

        if (!apiClient.hasActiveSession()) {
            Result.failure(Exception("Please Sign In with your registered mobile/email or admin account to access your courses."))
        } else {
            Result.failure(Exception("Connected to Koyeb server. No courses found on this account yet."))
        }
    }

    suspend fun initializeSeedDataIfNeeded() = withContext(Dispatchers.IO) {
        if (apiClient.hasActiveSession()) {
            syncRealCoursesFromKoyeb()
        }
    }
}
