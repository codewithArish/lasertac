package com.lasertrac.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.lasertrac.app.ui.theme.TextColorLight
import com.lasertrac.app.ui.theme.TopBarColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FTPScreen(onNavigateBack: () -> Unit) {
    var ftpServer by remember { mutableStateOf("192.168.10.1") }
    var ftpPort by remember { mutableStateOf("21") }
    var ftpUsername by remember { mutableStateOf("TP0003P") }
    var ftpPassword by remember { mutableStateOf("12345678") }
    var departmentName by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var savedLogoUri by remember { mutableStateOf<Uri?>(null) }

    var passwordVisible by remember { mutableStateOf(false) }

    // Validation flags
    var showFtpAlert by remember { mutableStateOf(false) }
    var showDeptAlert by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FTP", color = TextColorLight) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = TextColorLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TopBarColor.copy(alpha = 0.9f)
                )
            )
        },
        containerColor = Color(0xFF000000)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device IP Section
            Text(
                text = "Device Ip",
                fontSize = 14.sp,
                color = Color(0xFFAAAAAA),
                modifier = Modifier.padding(top = 8.dp)
            )

            // FTP Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A2A2A)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    InputField(label = "FTP Server", value = ftpServer, onValueChange = { ftpServer = it })
                    InputField(label = "FTP Port", value = ftpPort, onValueChange = { ftpPort = it })
                    InputField(label = "FTP Username", value = ftpUsername, onValueChange = { ftpUsername = it })
                    PasswordInputField(
                        label = "FTP Password",
                        value = ftpPassword,
                        onValueChange = { ftpPassword = it },
                        passwordVisible = passwordVisible,
                        onVisibilityToggle = { passwordVisible = !passwordVisible }
                    )

                    if (showFtpAlert && (ftpServer.isBlank() || ftpPort.isBlank() || ftpUsername.isBlank() || ftpPassword.isBlank())) {
                        Text(
                            text = "Please fill in all FTP fields",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (ftpServer.isBlank() || ftpPort.isBlank() || ftpUsername.isBlank() || ftpPassword.isBlank()) {
                                showFtpAlert = true
                            } else {
                                isConnecting = true
                                showFtpAlert = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ftpServer.isBlank() || ftpPort.isBlank() || ftpUsername.isBlank() || ftpPassword.isBlank())
                                Color(0xFFC62828)
                            else Color(0xFF1B5E20)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "UPDATE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            if (isConnecting) {
                Text(
                    text = "connecting...",
                    fontSize = 14.sp,
                    color = Color(0xFF4A90E2),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Department Name Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Department Name",
                        fontSize = 14.sp,
                        color = Color(0xFFAAAAAA)
                    )

                    OutlinedTextField(
                        value = departmentName,
                        onValueChange = { departmentName = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4A4A4A),
                            unfocusedBorderColor = Color(0xFF3A3A3A),
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (showDeptAlert && departmentName.isBlank()) {
                        Text(
                            text = "Department name cannot be empty",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (departmentName.isBlank()) {
                                showDeptAlert = true
                            } else {
                                showDeptAlert = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (departmentName.isBlank())
                                Color(0xFFC62828)
                            else Color(0xFF1B5E20)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "UPDATE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // ---------------- Department Logo Section ----------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Department Logo",
                        fontSize = 14.sp,
                        color = Color(0xFFAAAAAA)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFF3A3A3A), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageToShow = selectedImageUri ?: savedLogoUri
                        if (imageToShow != null) {
                            Image(
                                painter = rememberAsyncImagePainter(imageToShow),
                                contentDescription = "Department Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = "No logo selected",
                                color = Color(0xFF888888),
                                fontSize = 14.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        when {
                            savedLogoUri == null && selectedImageUri == null -> {
                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("BROWSE", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            selectedImageUri != null && savedLogoUri == null -> {
                                Button(
                                    onClick = { savedLogoUri = selectedImageUri },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Confirm", tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("SAVE", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                }

                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("BROWSE", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            savedLogoUri != null -> {
                                IconButton(
                                    onClick = {
                                        savedLogoUri = null
                                        selectedImageUri = null
                                    },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color(0xFFC62828), RoundedCornerShape(12.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputField(label: String, value: String, onValueChange: (String) -> Unit, isPassword: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, fontSize = 14.sp, color = Color(0xFFCCCCCC))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF4A4A4A),
                unfocusedBorderColor = Color(0xFF3A3A3A),
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
    }
}

@Composable
private fun PasswordInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    onVisibilityToggle: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, fontSize = 14.sp, color = Color(0xFFCCCCCC))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color(0xFFAAAAAA)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF4A4A4A),
                unfocusedBorderColor = Color(0xFF3A3A3A),
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
    }
}
