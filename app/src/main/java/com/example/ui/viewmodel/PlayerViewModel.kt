package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.local.LectureEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerUiState(
    val lecture: LectureEntity? = null,
    val courseTitle: String = "",
    val isPlaying: Boolean = true,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isFullscreen: Boolean = false,
    val isOfflineSource: Boolean = false,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null
)

class PlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun loadLecture(lecture: LectureEntity, courseTitle: String, resumePositionMs: Long = 0L) {
        val isOffline = lecture.isDownloaded && !lecture.localFilePath.isNullOrBlank()
        _uiState.value = PlayerUiState(
            lecture = lecture,
            courseTitle = courseTitle,
            isPlaying = true,
            currentPositionMs = resumePositionMs,
            durationMs = (lecture.durationSeconds * 1000L),
            playbackSpeed = 1.0f,
            isFullscreen = false,
            isOfflineSource = isOffline,
            isBuffering = false,
            errorMessage = null
        )
    }

    fun togglePlayPause() {
        val current = _uiState.value
        _uiState.value = current.copy(isPlaying = !current.isPlaying)
    }

    fun setPlaying(playing: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = playing)
    }

    fun seekTo(positionMs: Long) {
        val bounded = positionMs.coerceIn(0L, _uiState.value.durationMs.coerceAtLeast(1000L))
        _uiState.value = _uiState.value.copy(currentPositionMs = bounded)
    }

    fun seekForward10s() {
        seekTo(_uiState.value.currentPositionMs + 10000L)
    }

    fun seekBackward10s() {
        seekTo(_uiState.value.currentPositionMs - 10000L)
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun toggleFullscreen() {
        _uiState.value = _uiState.value.copy(isFullscreen = !_uiState.value.isFullscreen)
    }

    fun updateProgress(currentMs: Long, totalMs: Long) {
        val total = if (totalMs > 0) totalMs else _uiState.value.durationMs
        _uiState.value = _uiState.value.copy(
            currentPositionMs = currentMs,
            durationMs = total,
            isBuffering = false
        )
    }

    fun setBuffering(buffering: Boolean) {
        _uiState.value = _uiState.value.copy(isBuffering = buffering)
    }

    fun setError(msg: String?) {
        _uiState.value = _uiState.value.copy(errorMessage = msg, isBuffering = false)
    }

    fun closePlayer() {
        _uiState.value = PlayerUiState()
    }
}
