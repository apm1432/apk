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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BroadcastOnPersonal
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.CourseEntity
import com.example.ui.theme.AmberAlert
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
import com.example.ui.viewmodel.AdminUiState
import com.example.ui.viewmodel.AdminViewModel

@Composable
fun AdminStudioScreen(
    adminState: AdminUiState,
    adminViewModel: AdminViewModel,
    courses: List<CourseEntity>,
    onOpenAdminLogin: () -> Unit
) {
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showServerUrlDialog by remember { mutableStateOf(false) }

    if (!adminState.isAdminLoggedIn) {
        // Locked Gate
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground)
                .padding(24.dp)
                .testTag("admin_locked_screen"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(CyanGlow.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Admin Studio Locked",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Access course management, Telegram channel synchronization, live stream analytics, and server endpoints.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onOpenAdminLogin,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_unlock_admin")
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = CyberBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log In as Administrator", color = CyberBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    // Unlocked Admin Dashboard
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("admin_studio_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyanGlow.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = CyanGlow)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Admin Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Manager: ${adminState.adminUsername}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MintGreen
                        )
                    }
                }

                IconButton(
                    onClick = { adminViewModel.logout() },
                    modifier = Modifier.testTag("btn_admin_logout")
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Log out", tint = TextSecondary)
                }
            }
        }

        // Stats Overview Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminStatCard(
                    title = "Streams",
                    value = "${adminState.stats.activeStreams}",
                    icon = Icons.Default.LiveTv,
                    accentColor = MintGreen,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Students",
                    value = "${adminState.stats.totalUsers}",
                    icon = Icons.Default.Group,
                    accentColor = CyanGlow,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Courses",
                    value = "${courses.size}",
                    icon = Icons.Default.VideoLibrary,
                    accentColor = ElectricBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Telegram Channel Sync Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("card_tg_sync"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = ElectricBlue)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Telegram Channel Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Pull newly uploaded lecture media from Telegram", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { adminViewModel.syncTelegramChannel() },
                        enabled = !adminState.isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_trigger_sync"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (adminState.isSyncing) {
                            CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing from Telegram...")
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Trigger Channel Sync (/api/admin/sync)", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (adminState.syncMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = adminState.syncMessage, color = MintGreen, fontSize = 12.sp)
                    }
                }
            }
        }

        // Server Endpoint Config Card
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Koyeb Backend Endpoint", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        TextButton(onClick = { showServerUrlDialog = true }, modifier = Modifier.testTag("btn_change_server_url")) {
                            Text("Edit", color = CyanGlow, fontSize = 12.sp)
                        }
                    }
                    Text(
                        text = adminState.serverUrl,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        // Course Catalog Management
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manage Courses (${courses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Button(
                    onClick = { showAddCourseDialog = true },
                    modifier = Modifier.height(34.dp).testTag("btn_add_course_dialog"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = CyberBackground, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Course", color = CyberBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // List of courses for admin to edit or delete
        items(courses, key = { it.id }) { course ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(course.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                        Text("${course.category} • ${course.instructor}", fontSize = 11.sp, color = TextSecondary)
                    }
                    IconButton(
                        onClick = { adminViewModel.deleteCourse(course.id) },
                        modifier = Modifier.size(32.dp).testTag("btn_admin_delete_${course.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    // Add Course Dialog
    if (showAddCourseDialog) {
        AddCourseModal(
            onDismiss = { showAddCourseDialog = false },
            onCreate = { title, cat, desc, instructor, streamUrl ->
                adminViewModel.createCourse(title, cat, desc, instructor, streamUrl)
                showAddCourseDialog = false
            }
        )
    }

    // Edit Server URL Dialog
    if (showServerUrlDialog) {
        EditServerUrlModal(
            currentUrl = adminState.serverUrl,
            onDismiss = { showServerUrlDialog = false },
            onSave = { newUrl ->
                adminViewModel.updateServerUrl(newUrl)
                showServerUrlDialog = false
            }
        )
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(title, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
fun AddCourseModal(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Programming") }
    var description by remember { mutableStateOf("") }
    var instructor by remember { mutableStateOf("Vortex Lab") }
    var streamUrl by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Create New Course", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_create_course_title"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanGlow, unfocusedBorderColor = CyberBorder)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Programming, Backend)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_create_course_category"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanGlow, unfocusedBorderColor = CyberBorder)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().testTag("input_create_course_desc"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanGlow, unfocusedBorderColor = CyberBorder)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = { streamUrl = it },
                    label = { Text("Sample Lecture Stream URL") },
                    singleLine = true,
                    placeholder = { Text("e.g. https://.../stream/id") },
                    modifier = Modifier.fillMaxWidth().testTag("input_create_course_stream_url"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanGlow, unfocusedBorderColor = CyberBorder)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onCreate(title, category, description, instructor, streamUrl)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                        modifier = Modifier.testTag("btn_confirm_add_course")
                    ) {
                        Text("Create Course", color = CyberBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditServerUrlModal(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Update Koyeb Backend URL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Point the Android applet to any deployed instance of TG-coursestreamBot.", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Server URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_server_url"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanGlow, unfocusedBorderColor = CyberBorder)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(url) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                        modifier = Modifier.testTag("btn_save_server_url")
                    ) {
                        Text("Save URL", color = CyberBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
