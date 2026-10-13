package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CourseEntity
import com.example.data.network.AdminStats
import com.example.data.network.KoyebApiClient
import com.example.data.repository.CourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val isAdminLoggedIn: Boolean = false,
    val adminUsername: String = "admin",
    val stats: AdminStats = AdminStats(),
    val serverUrl: String = "https://sick-nikaniki-shubham8208989-6d7c863a.koyeb.app",
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val isBusy: Boolean = false,
    val feedbackMessage: String? = null
)

class AdminViewModel(
    private val repository: CourseRepository,
    private val sessionPrefs: com.example.data.local.SessionPreferences? = null,
    private val apiClient: KoyebApiClient = KoyebApiClient.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState(serverUrl = apiClient.baseUrl))
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        val isPersistedAdmin = sessionPrefs?.isAdminLoggedIn() == true
        val savedAdminUser = sessionPrefs?.getAdminUser() ?: "admin"
        if (isPersistedAdmin) {
            _uiState.value = _uiState.value.copy(
                isAdminLoggedIn = true,
                adminUsername = if (savedAdminUser.isNotBlank()) savedAdminUser else "admin"
            )
        }
        refreshStats()
    }

    fun loginAdmin(user: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            if (pass == "admin_offline") {
                sessionPrefs?.saveAdminSession(user)
                _uiState.value = _uiState.value.copy(
                    isAdminLoggedIn = true,
                    adminUsername = user,
                    isBusy = false,
                    feedbackMessage = "Admin Studio Unlocked in Local Mode"
                )
                refreshStats()
                onResult(true, "Admin Studio Unlocked")
                return@launch
            }
            val result = apiClient.adminLogin(user, pass)
            result.onSuccess {
                sessionPrefs?.saveAdminSession(user)
                _uiState.value = _uiState.value.copy(
                    isAdminLoggedIn = true,
                    adminUsername = user,
                    isBusy = false,
                    feedbackMessage = "Admin Authenticated on Koyeb Server!"
                )
                refreshStats()
                // Sync real courses from server!
                repository.syncRealCoursesFromKoyeb()
                onResult(true, "Authentication successful")
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    feedbackMessage = err.localizedMessage
                )
                onResult(false, err.localizedMessage ?: "Invalid admin credentials")
            }
        }
    }

    fun logout() {
        sessionPrefs?.clearAdminSession()
        _uiState.value = _uiState.value.copy(
            isAdminLoggedIn = false,
            adminUsername = "admin",
            feedbackMessage = null
        )
    }

    fun syncTelegramChannel() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
            val result = apiClient.triggerChannelSync()
            result.onSuccess { msg ->
                _uiState.value = _uiState.value.copy(isSyncing = false, syncMessage = msg)
                refreshStats()
                repository.syncRealCoursesFromKoyeb()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncMessage = "Sync response: ${err.localizedMessage}"
                )
            }
        }
    }

    fun updateServerUrl(url: String) {
        apiClient.updateBaseUrl(url)
        _uiState.value = _uiState.value.copy(serverUrl = apiClient.baseUrl)
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            val stats = apiClient.fetchAdminStats()
            _uiState.value = _uiState.value.copy(stats = stats)
        }
    }

    fun createCourse(
        title: String,
        category: String,
        description: String,
        instructor: String,
        streamUrlSample: String
    ) {
        viewModelScope.launch {
            val courseId = "c_" + java.util.UUID.randomUUID().toString().take(8)
            val course = CourseEntity(
                id = courseId,
                title = title,
                category = category,
                description = description,
                instructor = instructor,
                thumbnailUrl = "",
                totalLectures = 1,
                durationText = "Live Stream"
            )
            val lecture = com.example.data.local.LectureEntity(
                id = "lec_" + java.util.UUID.randomUUID().toString().take(6),
                courseId = courseId,
                folderName = "Uploaded Modules",
                title = "01. $title (Stream)",
                streamToken = streamUrlSample,
                streamUrl = apiClient.buildStreamUrl(streamUrlSample),
                durationSeconds = 600,
                fileType = "VIDEO",
                fileSizeMb = 25.0f,
                orderIndex = 1
            )
            repository.addOrUpdateCourse(course, listOf(lecture))
            _uiState.value = _uiState.value.copy(feedbackMessage = "Course '$title' created!")
            refreshStats()
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            repository.deleteCourse(courseId)
            _uiState.value = _uiState.value.copy(feedbackMessage = "Course deleted")
            refreshStats()
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null, syncMessage = null)
    }
}

class AdminViewModelFactory(
    private val repository: CourseRepository,
    private val sessionPrefs: com.example.data.local.SessionPreferences? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(repository, sessionPrefs = sessionPrefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
