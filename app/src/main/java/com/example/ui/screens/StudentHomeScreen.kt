package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.CourseEntity
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MintGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CourseUiState

@Composable
fun StudentHomeScreen(
    uiState: CourseUiState,
    onSelectCategory: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleDownloadedOnly: (Boolean) -> Unit,
    onSelectCourse: (CourseEntity) -> Unit,
    onCheckHealth: () -> Unit,
    onSyncWithServer: () -> Unit,
    onOpenAuth: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("student_home_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top Server Status Header
        item {
            ServerStatusHeader(
                uiState = uiState,
                onCheckHealth = onCheckHealth,
                onSyncWithServer = onSyncWithServer,
                onOpenAuth = onOpenAuth
            )
        }

        // Login callout if not logged in
        if (!uiState.isStudentLoggedIn) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("card_login_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(ElectricBlue.copy(alpha = 0.5f)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Akshayclasses Student Access",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sign in with your Telegram OTP or registered password to load your enrolled courses, stream lectures, and download offline content.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenAuth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_banner_signin"),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In / Login Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Search and Filter Bar
        item {
            SearchAndFilterSection(
                searchQuery = uiState.searchQuery,
                onSearchChange = onSearchQueryChange,
                isDownloadedOnly = uiState.isDownloadedOnly,
                onToggleDownloadedOnly = onToggleDownloadedOnly
            )
        }

        // Category Chips Row
        item {
            CategoryChipsRow(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onSelectCategory = onSelectCategory
            )
        }

        // Sync Message Feedback
        if (uiState.syncMessage != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant)
                ) {
                    Text(
                        text = uiState.syncMessage,
                        fontSize = 12.sp,
                        color = CyanGlow,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Course Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isDownloadedOnly) "Downloaded Courses" else "Akshayclasses Courses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${uiState.filteredCourses.size} loaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Course List / Cards
        if (uiState.filteredCourses.isEmpty()) {
            item {
                EmptyCoursesPlaceholder(
                    isDownloadedOnly = uiState.isDownloadedOnly,
                    isLoggedIn = uiState.isStudentLoggedIn,
                    onSyncWithServer = onSyncWithServer,
                    onOpenAuth = onOpenAuth
                )
            }
        } else {
            items(uiState.filteredCourses, key = { it.id }) { course ->
                CourseCard(
                    course = course,
                    onClick = { onSelectCourse(course) }
                )
            }
        }
    }
}

@Composable
fun ServerStatusHeader(
    uiState: CourseUiState,
    onCheckHealth: () -> Unit,
    onSyncWithServer: () -> Unit,
    onOpenAuth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (uiState.serverHealth.isOnline) MintGreen else AmberAlert)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (uiState.serverHealth.isOnline) "Koyeb Live (${uiState.serverHealth.latencyMs}ms)" else "Connecting...",
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = onCheckHealth,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("btn_recheck_health")
            ) {
                if (uiState.isHealthChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = CyanGlow)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Ping Server", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onSyncWithServer,
                modifier = Modifier.size(32.dp).testTag("btn_sync_courses")
            ) {
                if (uiState.isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CyanGlow)
                } else {
                    Icon(Icons.Default.CloudSync, contentDescription = "Sync from Server", tint = CyanGlow, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurfaceVariant)
                    .clickable { onOpenAuth() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("btn_student_profile")
            ) {
                Text(
                    text = if (uiState.isStudentLoggedIn) uiState.studentName else "Sign In",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanGlow
                )
            }
        }
    }
}

@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isDownloadedOnly: Boolean,
    onToggleDownloadedOnly: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search your courses...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_course_search"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CyberSurface,
                unfocusedContainerColor = CyberSurface,
                focusedBorderColor = ElectricBlue,
                unfocusedBorderColor = CyberBorder
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Show downloaded only",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Switch(
                checked = isDownloadedOnly,
                onCheckedChange = onToggleDownloadedOnly,
                modifier = Modifier.testTag("switch_offline_only"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MintGreen,
                    checkedTrackColor = MintGreen.copy(alpha = 0.3f),
                    uncheckedTrackColor = CyberSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun CategoryChipsRow(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        items(categories) { cat ->
            val isSelected = cat == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick = { onSelectCategory(cat) },
                label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                modifier = Modifier.testTag("chip_category_$cat"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricBlue,
                    selectedLabelColor = TextPrimary,
                    containerColor = CyberSurface,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) ElectricBlue else CyberBorder
                )
            )
        }
    }
}

@Composable
fun CourseCard(
    course: CourseEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("course_card_${course.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
    ) {
        Column {
            // Thumbnail / Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(CyberSurfaceVariant)
            ) {
                if (course.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = course.thumbnailUrl,
                        contentDescription = course.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(CyberSurfaceVariant, CyberSurface)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = CyanGlow.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Category Tag
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBackground.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = if (course.isEnrolled) "Enrolled" else "Available",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (course.isEnrolled) MintGreen else CyanGlow
                    )
                }

                // File count badge
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBackground.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = "${course.totalLectures} items",
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }

            // Info
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = course.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = course.instructor,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "View Content",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCoursesPlaceholder(
    isDownloadedOnly: Boolean,
    isLoggedIn: Boolean,
    onSyncWithServer: () -> Unit,
    onOpenAuth: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = CyanGlow,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isDownloadedOnly) "No downloaded courses found" else if (!isLoggedIn) "Sign In Required" else "No courses loaded yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isDownloadedOnly) "Download lectures from course pages to view them offline."
                else if (!isLoggedIn) "Please sign in with your mobile/email or admin credentials to fetch your courses from the Koyeb backend."
                else "Tap Sync to fetch courses directly from the live Akshayclasses Koyeb server.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!isLoggedIn) {
                Button(
                    onClick = onOpenAuth,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.testTag("btn_empty_login")
                ) {
                    Text("Sign In Now")
                }
            } else {
                Button(
                    onClick = onSyncWithServer,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.testTag("btn_empty_sync")
                ) {
                    Text("Sync with Server")
                }
            }
        }
    }
}
