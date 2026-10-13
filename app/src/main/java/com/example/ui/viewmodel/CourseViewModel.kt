package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CourseEntity
import com.example.data.local.LectureEntity
import com.example.data.local.SessionPreferences
import com.example.data.local.WatchHistoryEntity
import com.example.data.network.Announcement
import com.example.data.network.KoyebApiClient
import com.example.data.network.ServerHealth
import com.example.data.repository.ActiveDownloadProgress
import com.example.data.repository.CourseRepository
import com.example.data.repository.DownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CourseUiState(
    val courses: List<CourseEntity> = emptyList(),
    val filteredCourses: List<CourseEntity> = emptyList(),
    val categories: List<String> = listOf("All", "Enrolled", "Available"),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isDownloadedOnly: Boolean = false,
    val selectedCourse: CourseEntity? = null,
    val selectedCourseLectures: List<LectureEntity> = emptyList(),
    val serverHealth: ServerHealth = ServerHealth(isOnline = true, latencyMs = 120, statusCode = 200, message = "Ready"),
    val isHealthChecking: Boolean = false,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val isStudentLoggedIn: Boolean = false,
    val studentName: String = "Guest Student"
)

class CourseViewModel(
    private val repository: CourseRepository,
    private val downloadManager: DownloadManager,
    private val sessionPrefs: SessionPreferences? = null,
    private val apiClient: KoyebApiClient = KoyebApiClient.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseUiState())
    val uiState: StateFlow<CourseUiState> = _uiState.asStateFlow()

    val downloadProgressMap: StateFlow<Map<String, ActiveDownloadProgress>> =
        downloadManager.downloadProgressMap

    val downloadedLectures: StateFlow<List<LectureEntity>> =
        repository.downloadedLectures.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val watchHistory: StateFlow<List<WatchHistoryEntity>> =
        repository.allWatchHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.allCourses.collect { list ->
                _uiState.value = _uiState.value.copy(courses = list)
                applyFilters()
            }
        }

        val isPersistedLoggedIn = sessionPrefs?.isStudentLoggedIn() == true
        val hasSession = isPersistedLoggedIn || apiClient.hasActiveSession()
        val displayName = if (isPersistedLoggedIn) {
            sessionPrefs?.getStudentDisplayName() ?: "Student"
        } else {
            "Guest Student"
        }

        _uiState.value = _uiState.value.copy(
            isStudentLoggedIn = hasSession,
            studentName = if (hasSession) displayName else "Guest Student"
        )

        checkServerHealth()
        if (hasSession) {
            syncWithServer()
        }
    }

    fun syncWithServer() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
            val res = repository.syncRealCoursesFromKoyeb()
            res.onSuccess { count ->
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncMessage = "Synced $count course(s) from Akshayclasses server!"
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncMessage = err.message
                )
            }
        }
    }

    fun clearCacheAndResync() {
        viewModelScope.launch {
            repository.clearAllData()
            syncWithServer()
        }
    }

    fun selectCategory(cat: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = cat)
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun toggleDownloadedOnly(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDownloadedOnly = enabled)
        applyFilters()
    }

    private fun applyFilters() {
        val current = _uiState.value
        val all = current.courses
        val filtered = all.filter { course ->
            val matchesCategory = when (current.selectedCategory) {
                "Enrolled" -> course.isEnrolled
                "Available" -> !course.isEnrolled
                else -> true
            }
            val matchesSearch = current.searchQuery.isBlank() ||
                    course.title.contains(current.searchQuery, ignoreCase = true) ||
                    course.instructor.contains(current.searchQuery, ignoreCase = true) ||
                    course.description.contains(current.searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
        _uiState.value = current.copy(filteredCourses = filtered)
    }

    fun selectCourse(course: CourseEntity) {
        _uiState.value = _uiState.value.copy(selectedCourse = course)
        viewModelScope.launch {
            // First fetch fresh lectures from Koyeb to update tokens and prevent 403
            val freshLecs = apiClient.fetchCourseLectures(course.id)
            if (freshLecs.isSuccess && !freshLecs.getOrNull().isNullOrEmpty()) {
                repository.updateCourseLectures(course.id, freshLecs.getOrNull()!!)
            }
            repository.getLecturesForCourse(course.id).collect { lectures ->
                _uiState.value = _uiState.value.copy(selectedCourseLectures = lectures)
            }
        }
    }

    fun clearSelectedCourse() {
        _uiState.value = _uiState.value.copy(selectedCourse = null, selectedCourseLectures = emptyList())
    }

    fun checkServerHealth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isHealthChecking = true)
            val health = apiClient.checkServerHealth()
            _uiState.value = _uiState.value.copy(serverHealth = health, isHealthChecking = false)
        }
    }

    fun startDownload(lecture: LectureEntity) {
        downloadManager.startDownload(
            lectureId = lecture.id,
            courseId = lecture.courseId,
            title = lecture.title,
            streamUrl = lecture.streamUrl.ifEmpty { lecture.streamToken },
            fileType = lecture.fileType
        )
    }

    fun pauseDownload(lectureId: String) {
        downloadManager.pauseDownload(lectureId)
    }

    fun deleteDownload(lectureId: String, courseId: String) {
        downloadManager.deleteDownloadedFile(lectureId, courseId)
    }

    fun getStorageMetrics(): Pair<Float, Float> {
        return downloadManager.getStorageMetrics()
    }

    fun loginStudent(name: String) {
        sessionPrefs?.saveStudentSession(identifier = name, displayName = name)
        _uiState.value = _uiState.value.copy(isStudentLoggedIn = true, studentName = name)
        syncWithServer()
    }

    fun logoutStudent() {
        apiClient.clearSession()
        sessionPrefs?.clearStudentSession()
        _uiState.value = _uiState.value.copy(isStudentLoggedIn = false, studentName = "Guest Student")
        // User downloaded content and saved courses are NEVER deleted on sign out
    }

    fun saveProgress(lectureId: String, courseId: String, positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            repository.saveWatchProgress(lectureId, courseId, positionMs, durationMs)
        }
    }
}

class CourseViewModelFactory(
    private val repository: CourseRepository,
    private val downloadManager: DownloadManager,
    private val sessionPrefs: SessionPreferences? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CourseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CourseViewModel(repository, downloadManager, sessionPrefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
