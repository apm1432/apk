package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.network.KoyebApiClient
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MintGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun StudentLoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Password, 1: Telegram OTP
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val apiClient = KoyebApiClient.instance

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("student_login_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElectricBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Student", tint = ElectricBlue)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Student Sign In",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_student_login")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CyberSurfaceVariant,
                    contentColor = CyanGlow
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; statusMessage = null },
                        text = { Text("Password", fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; statusMessage = null },
                        text = { Text("Telegram OTP", fontSize = 13.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        label = { Text("Email or Mobile Number") },
                        placeholder = { Text("Enter your registered email/phone") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_student_username"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_student_password"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = CyberBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (identifier.isBlank() || password.isBlank()) {
                                statusMessage = "Email/mobile and password are required"
                                isError = true
                                return@Button
                            }
                            isLoading = true
                            statusMessage = null
                            scope.launch {
                                val res = apiClient.studentPasswordLogin(identifier, password)
                                isLoading = false
                                res.onSuccess {
                                    onLoginSuccess(identifier)
                                }.onFailure { err ->
                                    statusMessage = err.message ?: "Login failed"
                                    isError = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_submit_student_password"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Sign In to Koyeb Server", fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        label = { Text("Registered Email / Mobile Number") },
                        placeholder = { Text("e.g. user@gmail.com or +91...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_student_tg_username"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanGlow,
                            unfocusedBorderColor = CyberBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!otpSent) {
                        Button(
                            onClick = {
                                if (identifier.isNotBlank()) {
                                    isLoading = true
                                    statusMessage = null
                                    scope.launch {
                                        val res = apiClient.requestTelegramOtp(identifier)
                                        isLoading = false
                                        res.onSuccess { msg ->
                                            otpSent = true
                                            statusMessage = msg
                                            isError = false
                                        }.onFailure { err ->
                                            statusMessage = err.message
                                            isError = true
                                        }
                                    }
                                } else {
                                    statusMessage = "Please enter your registered email or mobile"
                                    isError = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_send_otp"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = CyberBackground, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp), tint = CyberBackground)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Request OTP from Telegram Bot", color = CyberBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            label = { Text("Enter 4-Digit OTP") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_student_otp"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MintGreen,
                                unfocusedBorderColor = CyberBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (otpCode.length < 4) {
                                    statusMessage = "Please enter 4-digit OTP"
                                    isError = true
                                    return@Button
                                }
                                isLoading = true
                                scope.launch {
                                    val res = apiClient.verifyTelegramOtp(identifier, otpCode)
                                    isLoading = false
                                    res.onSuccess {
                                        onLoginSuccess(identifier)
                                    }.onFailure { err ->
                                        statusMessage = err.message ?: "Invalid OTP"
                                        isError = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_verify_otp"),
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Verify OTP & Sign In", color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = statusMessage!!,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isError) MaterialTheme.colorScheme.error else MintGreen
                    )
                }
            }
        }
    }
}

@Composable
fun AdminLoginDialog(
    onDismiss: () -> Unit,
    onAdminLogin: (String, String) -> Unit,
    errorMessage: String? = null,
    isBusy: Boolean = false
) {
    var adminUser by remember { mutableStateOf("") }
    var adminPass by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("admin_login_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanGlow.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = CyanGlow)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Admin Studio Login",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_admin_login")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sign in using ADMIN_USER and ADMIN_PASS configured on your Koyeb server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = adminUser,
                    onValueChange = { adminUser = it },
                    label = { Text("Admin Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_admin_user"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanGlow,
                        unfocusedBorderColor = CyberBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = adminPass,
                    onValueChange = { adminPass = it },
                    label = { Text("Admin Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_admin_pass"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanGlow,
                        unfocusedBorderColor = CyberBorder
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (adminUser.isNotBlank() && adminPass.isNotBlank()) {
                            onAdminLogin(adminUser, adminPass)
                        }
                    },
                    enabled = !isBusy && adminUser.isNotBlank() && adminPass.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_submit_admin_login"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(color = CyberBackground, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Log In to Admin Panel", color = CyberBackground, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = {
                        val name = adminUser.ifBlank { "Admin" }
                        onAdminLogin(name, "admin_offline")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_bypass_admin_login")
                ) {
                    Text("Or Continue in Local Admin Mode", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}
