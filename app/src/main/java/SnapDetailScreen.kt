package com.lasertrac.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lasertrac.app.db.SnapRepository
import com.lasertrac.app.utils.OCRUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused")
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

    // Remember when the user opened this detail view so we can show "Viewed at"
    val viewClickedTimeMillis = remember { System.currentTimeMillis() }
    val viewClickedTimeStr = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(viewClickedTimeMillis))
    }

    var ocrText by remember { mutableStateOf("") }

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

            val ctx = LocalContext.current

            // If regNr blank and licensePlateImage exists, run OCR once
            LaunchedEffect(snap.licensePlateImage) {
                if (snap.regNr.isBlank() && !snap.licensePlateImage.isNullOrBlank()) {
                    try {
                        val uri = snap.licensePlateImage.toUri()
                        val textResult = withContext(Dispatchers.IO) {
                            OCRUtil.recognizeTextFromImageUri(ctx, uri)
                        }
                        ocrText = textResult.getOrThrow()
                    } catch (e: Exception) {
                        // ignore
                        ocrText = ""
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // Show main image with an overlayed license plate rectangle and reg number label
                item {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(12.dp))) {

                        AsyncImage(
                            model = snap.mainImage,
                            contentDescription = "Main Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image)
                        )

                        // Overlay: registration text above the small bordered plate box
                        Column(modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)) {

                            // If OCR text available, show that above plate
                            if (ocrText.isNotBlank()) {
                                Text(
                                    text = ocrText,
                                    color = Color.Yellow,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            // Text above the number plate border (fallback)
                            Text(
                                text = snap.regNr.ifBlank { "No Reg Nr" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            // Bordered rectangle that either shows cropped plate image or reg text
                            Box(
                                modifier = Modifier
                                    .size(width = 160.dp, height = 56.dp)
                                    .border(BorderStroke(2.dp, Color.White), shape = RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!snap.licensePlateImage.isNullOrBlank()) {
                                    AsyncImage(
                                        model = snap.licensePlateImage,
                                        contentDescription = "License Plate",
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                    )
                                } else {
                                    Text(
                                        text = snap.regNr.ifBlank { "N/A" },
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Other details below image
                item { Text("Registration: ${snap.regNr}", fontWeight = FontWeight.Bold) }
                item { Text("Date & Time: ${snap.dateTime}") }
                // Show when the image was clicked/opened by the user
                item { Text("Viewed at: $viewClickedTimeStr") }
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