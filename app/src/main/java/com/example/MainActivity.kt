package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.AppDatabase
import com.example.data.local.CourseEntity
import com.example.data.local.LectureEntity
import com.example.data.network.KoyebApiClient
import com.example.data.repository.CourseRepository
import com.example.data.repository.DownloadManager
import com.example.ui.screens.AdminLoginDialog
import com.example.ui.screens.AdminStudioScreen
import com.example.ui.screens.CourseDetailScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.ImageViewerScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.StudentHomeScreen
import com.example.ui.screens.StudentLoginDialog
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AdminViewModelFactory
import com.example.ui.viewmodel.CourseViewModel
import com.example.ui.viewmodel.CourseViewModelFactory
import kotlinx.coroutines.launch

sealed class Screen {
    object Main : Screen()
    data class Detail(val course: CourseEntity) : Screen()
    data class Player(val lecture: LectureEntity, val courseTitle: String) : Screen()
    data class Pdf(val lecture: LectureEntity, val courseTitle: String) : Screen()
    data class ImageView(val lecture: LectureEntity, val courseTitle: String) : Screen()
}

enum class NavigationTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    COURSES("Courses", Icons.Default.School),
    DOWNLOADS("Offline", Icons.Default.DownloadDone),
    ADMIN("Admin Studio", Icons.Default.AdminPanelSettings)
}

class MainActivity : ComponentActivity() {

    private val db by lazy { AppDatabase.getDatabase(this) }
    private val downloadManager by lazy { DownloadManager(this, db.courseDao()) }
    private val courseRepository by lazy { CourseRepository(db.courseDao()) }
    private val sessionPrefs by lazy { com.example.data.local.SessionPreferences.getInstance(this) }

    private val courseViewModel: CourseViewModel by viewModels {
        CourseViewModelFactory(courseRepository, downloadManager, sessionPrefs)
    }

    private val adminViewModel: AdminViewModel by viewModels {
        AdminViewModelFactory(courseRepository, sessionPrefs)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize persistent cookies and session from context
        KoyebApiClient.instance.initContext(this)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                MainAppContent(
                    courseViewModel = courseViewModel,
                    adminViewModel = adminViewModel
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    courseViewModel: CourseViewModel,
    adminViewModel: AdminViewModel
) {
    val courseUiState by courseViewModel.uiState.collectAsState()
    val adminUiState by adminViewModel.uiState.collectAsState()
    val downloadedLectures by courseViewModel.downloadedLectures.collectAsState()
    val downloadProgressMap by courseViewModel.downloadProgressMap.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
    var currentTab by remember { mutableStateOf(NavigationTab.COURSES) }

    var showStudentAuthDialog by remember { mutableStateOf(false) }
    var showAdminAuthDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CyberBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentScreen is Screen.Main) {
                NavigationBar(
                    containerColor = CyberSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_navigation_bar")
                ) {
                    NavigationTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                currentTab = tab
                                if (tab == NavigationTab.ADMIN && !adminUiState.isAdminLoggedIn) {
                                    showAdminAuthDialog = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    color = if (isSelected) CyanGlow else TextSecondary
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricBlue,
                                unselectedIconColor = TextSecondary,
                                indicatorColor = ElectricBlue.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.Main -> {
                    when (currentTab) {
                        NavigationTab.COURSES -> {
                            StudentHomeScreen(
                                uiState = courseUiState,
                                onSelectCategory = { courseViewModel.selectCategory(it) },
                                onSearchQueryChange = { courseViewModel.setSearchQuery(it) },
                                onToggleDownloadedOnly = { courseViewModel.toggleDownloadedOnly(it) },
                                onSelectCourse = { course ->
                                    courseViewModel.selectCourse(course)
                                    currentScreen = Screen.Detail(course)
                                },
                                onCheckHealth = { courseViewModel.checkServerHealth() },
                                onSyncWithServer = { courseViewModel.syncWithServer() },
                                onOpenAuth = { showStudentAuthDialog = true }
                            )
                        }

                        NavigationTab.DOWNLOADS -> {
                            DownloadsScreen(
                                downloadedLectures = downloadedLectures,
                                downloadProgressMap = downloadProgressMap,
                                storageMetrics = courseViewModel.getStorageMetrics(),
                                onPlayLecture = { lecture ->
                                    val courseTitle = courseUiState.courses.find { it.id == lecture.courseId }?.title ?: "Downloaded Media"
                                    val type = lecture.fileType.uppercase()
                                    when {
                                        type.contains("PDF") || type.contains("DOC") -> currentScreen = Screen.Pdf(lecture, courseTitle)
                                        type.contains("IMAGE") || type.contains("PHOTO") -> currentScreen = Screen.ImageView(lecture, courseTitle)
                                        else -> currentScreen = Screen.Player(lecture, courseTitle)
                                    }
                                },
                                onPauseDownload = { courseViewModel.pauseDownload(it) },
                                onDeleteDownload = { lecture ->
                                    courseViewModel.deleteDownload(lecture.id, lecture.courseId)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Download removed from device")
                                    }
                                },
                                onNavigateToCourses = { currentTab = NavigationTab.COURSES }
                            )
                        }

                        NavigationTab.ADMIN -> {
                            AdminStudioScreen(
                                adminState = adminUiState,
                                adminViewModel = adminViewModel,
                                courses = courseUiState.courses,
                                onOpenAdminLogin = { showAdminAuthDialog = true }
                            )
                        }
                    }
                }

                is Screen.Detail -> {
                    CourseDetailScreen(
                        course = screen.course,
                        lectures = courseUiState.selectedCourseLectures,
                        downloadProgressMap = downloadProgressMap,
                        onBack = {
                            courseViewModel.clearSelectedCourse()
                            currentScreen = Screen.Main
                        },
                        onPlayLecture = { lecture ->
                            val type = lecture.fileType.uppercase()
                            when {
                                type.contains("PDF") || type.contains("DOC") -> currentScreen = Screen.Pdf(lecture, screen.course.title)
                                type.contains("IMAGE") || type.contains("PHOTO") -> currentScreen = Screen.ImageView(lecture, screen.course.title)
                                else -> currentScreen = Screen.Player(lecture, screen.course.title)
                            }
                        },
                        onOpenPdf = { lecture ->
                            currentScreen = Screen.Pdf(lecture, screen.course.title)
                        },
                        onOpenImage = { lecture ->
                            currentScreen = Screen.ImageView(lecture, screen.course.title)
                        },
                        onStartDownload = { lecture ->
                            courseViewModel.startDownload(lecture)
                            scope.launch {
                                snackbarHostState.showSnackbar("Starting download: ${lecture.title}")
                            }
                        },
                        onDeleteDownload = { lecture ->
                            courseViewModel.deleteDownload(lecture.id, lecture.courseId)
                            scope.launch {
                                snackbarHostState.showSnackbar("Deleted downloaded file")
                            }
                        }
                    )
                }

                is Screen.Player -> {
                    VideoPlayerScreen(
                        lecture = screen.lecture,
                        courseTitle = screen.courseTitle,
                        onBack = { currentScreen = Screen.Main },
                        onSaveProgress = { pos, dur ->
                            courseViewModel.saveProgress(screen.lecture.id, screen.lecture.courseId, pos, dur)
                        }
                    )
                }

                is Screen.Pdf -> {
                    PdfViewerScreen(
                        lecture = screen.lecture,
                        courseTitle = screen.courseTitle,
                        onBack = { currentScreen = Screen.Main },
                        onDownload = {
                            courseViewModel.startDownload(screen.lecture)
                            scope.launch {
                                snackbarHostState.showSnackbar("Downloading PDF for offline reading")
                            }
                        }
                    )
                }

                is Screen.ImageView -> {
                    ImageViewerScreen(
                        lecture = screen.lecture,
                        courseTitle = screen.courseTitle,
                        onBack = { currentScreen = Screen.Main },
                        onDownload = {
                            courseViewModel.startDownload(screen.lecture)
                            scope.launch {
                                snackbarHostState.showSnackbar("Downloading Image for offline viewing")
                            }
                        }
                    )
                }
            }
        }
    }

    // Student Authentication Dialog
    if (showStudentAuthDialog) {
        StudentLoginDialog(
            onDismiss = { showStudentAuthDialog = false },
            onLoginSuccess = { user ->
                courseViewModel.loginStudent(user)
                showStudentAuthDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Signed in as $user")
                }
            }
        )
    }

    // Admin Authentication Dialog
    if (showAdminAuthDialog) {
        AdminLoginDialog(
            onDismiss = { showAdminAuthDialog = false },
            onAdminLogin = { user, pass ->
                adminViewModel.loginAdmin(user, pass) { success, msg ->
                    if (success) {
                        showAdminAuthDialog = false
                        currentTab = NavigationTab.ADMIN
                        scope.launch {
                            snackbarHostState.showSnackbar("Admin Studio Unlocked")
                        }
                    }
                }
            },
            errorMessage = adminUiState.feedbackMessage,
            isBusy = adminUiState.isBusy
        )
    }
}
