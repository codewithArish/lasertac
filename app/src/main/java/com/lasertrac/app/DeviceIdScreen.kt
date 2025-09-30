package com.lasertrac.app

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lasertrac.app.ui.theme.Lasertac2Theme
import com.lasertrac.app.ui.theme.TextColorLight
import com.lasertrac.app.ui.theme.TopBarColor

data class DeviceInfo(
    val deviceId: String,
    val appId: String,
    val stateCode: String,
    val deptCode: String,
    val violationSource: String,
    val itmsUrl: String,
    val activationStatus: String
)

data class InfoItem(
    val icon: ImageVector,
    val label: String,
    val value: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceIdScreen(onNavigateBack: () -> Unit) {
    val deviceInfo = remember {
        DeviceInfo(
            deviceId = "LT-12345XYZ",
            appId = "com.lasertrac.app.v2",
            stateCode = "HR",
            deptCode = "TRAFFIC_POLICE",
            violationSource = "MOBILE_ENFORCEMENT_V2",
            itmsUrl = "https://hr.itms.gov.in/api/v2/violations",
            activationStatus = "ACTIVATED"
        )
    }

    val infoItems = listOf(
        InfoItem(Icons.Default.VpnKey, "Device ID", deviceInfo.deviceId),
        InfoItem(Icons.Default.Pin, "App ID", deviceInfo.appId),
        InfoItem(Icons.Default.Apartment, "Department Code", deviceInfo.deptCode),
        InfoItem(Icons.Default.Source, "Violation Source", deviceInfo.violationSource),
        InfoItem(Icons.Default.Link, "ITMS URL", deviceInfo.itmsUrl)
    )

    var showJsonDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Identity", color = TextColorLight, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = TextColorLight
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showJsonDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DataObject,
                            contentDescription = "Export as JSON",
                            tint = TextColorLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TopBarColor.copy(alpha = 0.9f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                ActivationStatusCard(status = deviceInfo.activationStatus)
                Spacer(modifier = Modifier.height(24.dp))
            }
            items(infoItems) { item ->
                ModernInfoRow(item = item)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }

    if (showJsonDialog) {
        JsonExportDialog(
            deviceInfo = deviceInfo,
            onDismiss = { showJsonDialog = false }
        )
    }
}

@Composable
fun ActivationStatusCard(status: String) {
    val isActivated = status.equals("ACTIVATED", ignoreCase = true)
    val backgroundColor = if (isActivated) Color(0xFF3F8443) else MaterialTheme.colorScheme.error
    val contentColor = Color.White

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Activation Status",
                tint = contentColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Column {
                Text(
                    text = "Activation Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun ModernInfoRow(item: InfoItem) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = item.value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = {
            clipboardManager.setText(AnnotatedString(item.value))
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy ${item.label}",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun JsonExportDialog(deviceInfo: DeviceInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val jsonString = remember {
        buildString {
            appendLine("{")
            appendLine("  \"Device ID\": \"${deviceInfo.deviceId}\",")
            appendLine("  \"App ID\": \"${deviceInfo.appId}\",")
            appendLine("  \"State Code\": \"${deviceInfo.stateCode}\",")
            appendLine("  \"Dept Code\": \"${deviceInfo.deptCode}\",")
            appendLine("  \"Violation Source\": \"${deviceInfo.violationSource}\",")
            appendLine("  \"ITMS URL\": \"${deviceInfo.itmsUrl}\",")
            appendLine("  \"Activation Status\": \"${deviceInfo.activationStatus}\"")
            append("}")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Device Info as JSON") },
        text = { 
            Text(
                text = jsonString,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
        },
        confirmButton = {
            TextButton(onClick = {
                clipboardManager.setText(AnnotatedString(jsonString))
                Toast.makeText(context, "JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                onDismiss()
            }) {
                Text("Copy & Close")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun DeviceIdScreenPreview() {
    Lasertac2Theme {
        DeviceIdScreen(onNavigateBack = {})
    }
}
