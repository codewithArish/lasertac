package com.lasertrac.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lasertrac.app.db.SnapRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapDetailScreen(
    snapId: String,
    snapRepository: SnapRepository,
    onNavigateBack: () -> Unit
) {
    val viewModel: SnapDetailViewModel = viewModel(
        factory = SnapDetailViewModelFactory(snapRepository, snapId)
    )
    val snapDetail by viewModel.snapDetail.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Snap Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        snapDetail?.let { snap ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                item { AsyncImage(snap.mainImage, "Main Image", Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) }
                item { Text("Registration: ${snap.regNr}", fontWeight = FontWeight.Bold) }
                item { Text("Date & Time: ${snap.dateTime}") }
                item { Text("Location: ${snap.address}") }
                item { Text("Status: ${snap.status.displayName}") }
                item { Text("Speed: ${snap.speed} km/h (Limit: ${snap.speedLimit} km/h)") }
                item { Text("Device ID: ${snap.deviceId}") }
                item { Text("Operator ID: ${snap.operatorId}") }
                item { Text("Violation Distance: ${snap.violationDistance}") }
                item { Text("Record Nr: ${snap.recordNr}") }
                item { Text("GPS: ${snap.latitude}, ${snap.longitude}") }
                item { Text("District: ${snap.district}") }
                item { Text("Police Station: ${snap.policeStation}") }
                item { Text("Upload Status: ${snap.uploadStatus}") }
                item { Text("Violation Summary: ${snap.violationSummary}") }
            }
        } ?: run {
            Column(modifier = Modifier.padding(innerPadding)) {
                Text("Snap not found.")
            }
        }
    }
}
