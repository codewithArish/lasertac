package com.lasertrac.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.lasertrac.app.ui.theme.TextColorLight
import com.lasertrac.app.ui.theme.TopBarColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FTPScreen(onNavigateBack: () -> Unit, ftpViewModel: FTPViewModel) {
    val context = LocalContext.current

    val ftpServer by ftpViewModel.ftpServer.collectAsState()
    val ftpPort by ftpViewModel.ftpPort.collectAsState()
    val ftpUsername by ftpViewModel.ftpUsername.collectAsState()
    val ftpPassword by ftpViewModel.ftpPassword.collectAsState()
    val departmentName by ftpViewModel.departmentName.collectAsState()
    val selectedImageUri by ftpViewModel.selectedImageUri.collectAsState()
    val status by ftpViewModel.status.collectAsState()
    val errorMessage by ftpViewModel.errorMessage.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        ftpViewModel.onSelectedImageUriChange(uri)
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
            Text(
                text = "Device Ip",
                fontSize = 14.sp,
                color = Color(0xFFAAAAAA),
                modifier = Modifier.padding(top = 8.dp)
            )

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
                    InputField(label = "FTP Server", value = ftpServer, onValueChange = ftpViewModel::onFtpServerChange)
                    InputField(label = "FTP Port", value = ftpPort, onValueChange = ftpViewModel::onFtpPortChange)
                    InputField(label = "FTP Username", value = ftpUsername, onValueChange = ftpViewModel::onFtpUsernameChange)
                    PasswordInputField(
                        label = "FTP Password",
                        value = ftpPassword,
                        onValueChange = ftpViewModel::onFtpPasswordChange,
                        passwordVisible = passwordVisible,
                        onVisibilityToggle = { passwordVisible = !passwordVisible }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val isOperationRunning = status == FtpStatus.CONNECTING || status == FtpStatus.SYNCING
                        Button(
                            onClick = { ftpViewModel.connectAndTest() },
                            modifier = Modifier.weight(1f),
                            enabled = !isOperationRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "TEST CONNECTION",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        FtpSyncScreen(ftpViewModel = ftpViewModel)
                    }
                }
            }

            if (status == FtpStatus.CONNECTING) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Connecting...", fontSize = 14.sp, color = Color(0xFF4A90E2))
                }
            }

            if (status == FtpStatus.CONNECTION_SUCCESS) {
                Text(text = "Connection successful!", fontSize = 14.sp, color = Color.Green)
            }

            if (status == FtpStatus.CONNECTION_ERROR) {
                Text(text = "Connection Error: ${errorMessage ?: "Unknown error"}", fontSize = 14.sp, color = Color.Red)
            }

            if (status == FtpStatus.SYNCING) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Syncing...", fontSize = 14.sp, color = Color(0xFF4A90E2))
                }
            }

            if (status == FtpStatus.SYNC_SUCCESS) {
                Text(text = "Sync successful!", fontSize = 14.sp, color = Color.Green)
            }

            if (status == FtpStatus.SYNC_ERROR) {
                Text(text = "Sync Error: ${errorMessage ?: "Unknown error"}", fontSize = 14.sp, color = Color.Red)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = "Department Name", fontSize = 14.sp, color = Color(0xFFAAAAAA))

                    OutlinedTextField(
                        value = departmentName,
                        onValueChange = ftpViewModel::onDepartmentNameChange,
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

                    Text(text = "Department Logo", fontSize = 14.sp, color = Color(0xFFAAAAAA))

                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp).background(Color(0xFF3A3A3A), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedImageUri),
                                contentDescription = "Department Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(text = "No logo selected", color = Color(0xFF888888), fontSize = 14.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("BROWSE", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }

                        if (selectedImageUri != null) {
                            IconButton(
                                onClick = { ftpViewModel.onSelectedImageUriChange(null) },
                                modifier = Modifier.size(56.dp).background(Color(0xFFC62828), RoundedCornerShape(12.dp))
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    Button(
                        onClick = { ftpViewModel.uploadDepartmentData(context) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "UPLOAD", fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 4.dp))
                    }

                    if (status == FtpStatus.UPLOADING) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Uploading...", fontSize = 14.sp, color = Color(0xFF4A90E2))
                        }
                    }

                    if (status == FtpStatus.UPLOAD_SUCCESS) {
                        Text(text = "Upload successful!", fontSize = 14.sp, color = Color.Green)
                    }

                    if (status == FtpStatus.UPLOAD_ERROR) {
                        Text(text = "Upload Error: ${errorMessage ?: "Unknown error"}", fontSize = 14.sp, color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.FtpSyncScreen(ftpViewModel: FTPViewModel) { // Pass your ViewModel
    val context = LocalContext.current
    val status by ftpViewModel.status.collectAsState()

    // Array of permissions your feature needs
    val permissionsToRequest = arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO
    )

    // State to track if permissions have been granted
    var hasPermissions by remember {
        mutableStateOf(
            permissionsToRequest.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    // Create the permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            // Check if all permissions were granted after the user responded
            hasPermissions = permissions.values.all { it }
            if (hasPermissions) {
                // Permissions granted! Now we can start the sync.
                ftpViewModel.syncFiles(context) // Or however you trigger the worker
            } else {
                // Handle the case where the user denies permissions
                // Show a message, disable the button, etc.
            }
        }
    )
    val isOperationRunning = status == FtpStatus.CONNECTING || status == FtpStatus.SYNCING

    Column(modifier = Modifier.weight(1f)) {
        Button(
            onClick = {
                if (hasPermissions) {
                    // If we already have permission, just sync
                    ftpViewModel.syncFiles(context)
                } else {
                    // Otherwise, launch the permission request dialog
                    permissionLauncher.launch(permissionsToRequest)
                }
            },
            enabled = !isOperationRunning,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "SYNC NOW",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (!hasPermissions) {
            Text("Please grant media permissions to sync files.", color = Color.White)
        }
    }

}

@Composable
private fun InputField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, fontSize = 14.sp, color = Color(0xFFCCCCCC))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
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
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
