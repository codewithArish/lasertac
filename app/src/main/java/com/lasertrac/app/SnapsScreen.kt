package com.lasertrac.app

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
// import android.graphics.Rect // Removed unused import
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// import androidx.compose.ui.unit.sp // Removed unused import
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
// import com.lasertrac.app.db.SnapLocationDao // Keep if MockSnapLocationDao is in another file and uses it
import com.lasertrac.app.ui.theme.Lasertac2Theme
import com.lasertrac.app.ui.theme.TextColorLight
import com.lasertrac.app.ui.theme.TopBarColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
// import java.io.FileOutputStream // Removed unused import, will be used in actual saveBitmapToFile
// import java.io.IOException // Removed unused import, will be used in actual saveBitmapToFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Helper function to format millis to date string (from previous version)
fun Long.toFormattedDateString(pattern: String = "dd-MM-yyyy"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

// UI state for the new snap processing flow
data class SnapProcessingState(
    val isLoading: Boolean = false,
    val originalImageUri: Uri? = null,
    val croppedPlateBitmap: Bitmap? = null,
    val extractedNumber: String? = null,
    val processingError: String? = null,
    val showProcessingDialog: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SnapsScreen(onNavigateBack: () -> Unit /* snapLocationDao: SnapLocationDao // Removed unused parameter */) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = null)
    val selectedDateFormatted by remember { derivedStateOf { datePickerState.selectedDateMillis?.toFormattedDateString() ?: "All Snaps" } }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }

    var selectionMode by remember { mutableStateOf(false) }
    val selectedSnapIds = remember { mutableStateListOf<String>() }

    var snapProcessingState by remember { mutableStateOf(SnapProcessingState()) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // Used for launching image processing

    val allSnapDetails = remember {
        mutableStateListOf(
            SnapDetail("1", R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, "2024-05-21 10:30 AM", "LT-12345", "OP-6789", 85, 70, "Highway A1", "150 m", "REC-1", "HR26AS0001", "Valid", "28.6139", "77.2090", "2024-05-21", "New Delhi", "Central Delhi PS", "Near India Gate, New Delhi", "Overspeeding", "http://example.com/1", "http://example.com/1", "Uploaded", SnapStatus.PENDING),
            SnapDetail("2", R.drawable.ic_videos_custom, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, "2024-05-22 11:00 AM", "LT-12345", "OP-6789", 72, 60, "Highway A1", "150 m", "REC-2", "DL7CS2107", "Valid", "28.6139", "77.2090", "2024-05-22", "New Delhi", "Central Delhi PS", "Near India Gate, New Delhi", "Overspeeding", "http://example.com/2", "http://example.com/2", "Uploaded", SnapStatus.UPDATED),
            SnapDetail("3", R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, "2024-05-23 09:10 AM", "LT-22334", "OP-1111", 58, 50, "Ring Road", "120 m", "REC-3", "KA03MN4567", "Valid", "12.9716", "77.5946", "2024-05-23", "Bengaluru", "Cubbon Park PS", "MG Road", "Lane Discipline", "http://example.com/3", "http://example.com/3", "Pending", SnapStatus.PENDING),
            SnapDetail("4", R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, "2024-05-24 07:45 PM", "LT-99887", "OP-2222", 96, 80, "Expressway E1", "300 m", "REC-4", "MH12AB1234", "Valid", "18.5204", "73.8567", "2024-05-24", "Pune", "Shivajinagar PS", "JM Road", "Overspeeding", "http://example.com/4", "http://example.com/4", "Uploaded", SnapStatus.UPLOADED),
            SnapDetail("5", R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, "2024-05-24 08:15 PM", "LT-99887", "OP-2222", 40, 60, "Expressway E1", "90 m", "REC-5", "GJ05XY7890", "Valid", "22.3072", "73.1812", "2024-05-24", "Vadodara", "Raopura PS", "Alkapuri", "Wrong Parking", "http://example.com/5", "http://example.com/5", "Updated", SnapStatus.UPDATED),
            SnapDetail("6", R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, "2024-05-25 10:05 AM", "LT-55667", "OP-3333", 67, 50, "NH48", "180 m", "REC-6", "DL8CAF9087", "Valid", "28.7041", "77.1025", "2024-05-25", "Delhi", "Karol Bagh PS", "Pusa Road", "Red Light Jump", "http://example.com/6", "http://example.com/6", "Rejected", SnapStatus.REJECTED),
            SnapDetail("7", R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, "2024-05-26 01:25 PM", "LT-11223", "OP-4444", 62, 60, "Outer Ring Road", "130 m", "REC-7", "TN09CZ4321", "Valid", "13.0827", "80.2707", "2024-05-26", "Chennai", "T Nagar PS", "Usman Road", "Helmet Violation", "http://example.com/7", "http://example.com/7", "Uploaded", SnapStatus.UPLOADED),
            SnapDetail("8", R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, "2024-05-27 03:55 PM", "LT-77889", "OP-5555", 78, 70, "City Center", "110 m", "REC-8", "RJ14UV2468", "Valid", "26.9124", "75.7873", "2024-05-27", "Jaipur", "MI Road PS", "MI Road", "Seatbelt Violation", "http://example.com/8", "http://example.com/8", "Pending", SnapStatus.PENDING),
            SnapDetail("9", R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, "2024-05-27 04:20 PM", "LT-77889", "OP-5555", 82, 60, "City Center", "140 m", "REC-9", "UP32GH7654", "Valid", "26.8467", "80.9462", "2024-05-27", "Lucknow", "Hazratganj PS", "Hazratganj", "Overspeeding", "http://example.com/9", "http://example.com/9", "Uploaded", SnapStatus.UPLOADED),
            SnapDetail("10", R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, "2024-05-28 09:40 AM", "LT-33445", "OP-6666", 54, 50, "NH44", "100 m", "REC-10", "PB10JK1234", "Valid", "31.6330", "74.8723", "2024-05-28", "Amritsar", "Civil Lines PS", "Mall Road", "Signal Jump", "http://example.com/10", "http://example.com/10", "Updated", SnapStatus.UPDATED)
        )
    }

    val filteredSnapDetails = remember(searchQuery, allSnapDetails.toList(), datePickerState.selectedDateMillis) {
        val selectedFilterDate = datePickerState.selectedDateMillis?.toFormattedDateString("yyyy-MM-dd")
        val dateFiltered = if (selectedFilterDate != null) {
            allSnapDetails.filter { it.evidenceDate == selectedFilterDate }
        } else {
            allSnapDetails
        }
        if (searchQuery.isBlank()) dateFiltered else dateFiltered.filter { it.regNr.contains(searchQuery, ignoreCase = true) }
    }

    var selectedSnapForPreview by remember { mutableStateOf<SnapDetail?>(null) }

    fun exitSelectionMode() {
        selectionMode = false
        selectedSnapIds.clear()
    }

    BackHandler(enabled = selectedSnapForPreview != null || selectionMode || snapProcessingState.showProcessingDialog) {
        when {
            snapProcessingState.showProcessingDialog -> snapProcessingState = SnapProcessingState() // Close dialog
            selectedSnapForPreview != null -> selectedSnapForPreview = null
            selectionMode -> exitSelectionMode()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            snapProcessingState = snapProcessingState.copy(originalImageUri = it, isLoading = true, croppedPlateBitmap = null, extractedNumber = null, processingError = null)
            // Launch in the composable's scope
            coroutineScope.launch {
                processImageAndCreateSnapDetail(context, it, snapProcessingState, this) { updatedState -> snapProcessingState = updatedState }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            tempImageUri?.let {
                snapProcessingState = snapProcessingState.copy(originalImageUri = it, isLoading = true, croppedPlateBitmap = null, extractedNumber = null, processingError = null)
                coroutineScope.launch {
                    processImageAndCreateSnapDetail(context, it, snapProcessingState, this) { updatedState -> snapProcessingState = updatedState }
                }
            }
        } else {
            snapProcessingState = snapProcessingState.copy(isLoading = false, processingError = "Image capture failed.")
        }
    }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            val newTempImageUri = createImageUriForCapture(context)
            tempImageUri = newTempImageUri
            cameraLauncher.launch(newTempImageUri)
        } else {
            snapProcessingState = snapProcessingState.copy(isLoading = false, processingError = "Camera permission denied.")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Crossfade(targetState = selectionMode, label = "TopBarCrossfade") { isSelectionMode ->
                if (isSelectionMode) {
                    val allVisibleSelected = selectedSnapIds.size == filteredSnapDetails.size && filteredSnapDetails.isNotEmpty()
                    SelectionTopAppBar(
                        selectedCount = selectedSnapIds.size,
                        allSelected = allVisibleSelected,
                        onClose = { exitSelectionMode() },
                        onSelectAll = {
                            if (allVisibleSelected) selectedSnapIds.clear()
                            else { selectedSnapIds.clear(); selectedSnapIds.addAll(filteredSnapDetails.map { it.id }) }
                        },
                        onDelete = {
                            allSnapDetails.removeAll { selectedSnapIds.contains(it.id) }
                            // TODO: Also delete from DAO
                            exitSelectionMode()
                        }
                    )
                } else {
                    NormalTopAppBar(
                        isSearchActive = isSearchActive,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onSearchToggled = { isSearchActive = !isSearchActive },
                        onNavigateBack = onNavigateBack,
                        onShowDatePicker = { showDatePicker = true },
                        onShowInfoDialog = { showInfoDialog = true },
                        onAddSnap = { snapProcessingState = snapProcessingState.copy(showProcessingDialog = true, isLoading = false, processingError = null, croppedPlateBitmap = null, extractedNumber = null, originalImageUri = null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { showDatePicker = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Displaying snaps for: $selectedDateFormatted", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                if (datePickerState.selectedDateMillis != null) {
                    IconButton(onClick = { datePickerState.selectedDateMillis = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Date Filter", modifier = Modifier.size(16.dp))
                    }
                }
            }

            LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(filteredSnapDetails, key = { it.id }) { snapDetail ->
                    SnapCard(
                        snapDetail = snapDetail,
                        isSelected = selectedSnapIds.contains(snapDetail.id),
                        selectionMode = selectionMode,
                        onClick = {
                            if (selectionMode) {
                                if (selectedSnapIds.contains(snapDetail.id)) selectedSnapIds.remove(snapDetail.id) else selectedSnapIds.add(snapDetail.id)
                            } else {
                                selectedSnapForPreview = snapDetail
                            }
                        },
                        onLongClick = {
                            if (!selectionMode) { selectionMode = true; selectedSnapIds.add(snapDetail.id) }
                        }
                    )
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
            ) { DatePicker(state = datePickerState) }
        }

        if (showInfoDialog) {
            StatusInfoDialog(onDismiss = { showInfoDialog = false })
        }

        if (selectedSnapForPreview != null) {
            Dialog(onDismissRequest = { selectedSnapForPreview = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                val currentIndex = filteredSnapDetails.indexOf(selectedSnapForPreview)
                ImagePreviewScreen(
                    snapDetail = selectedSnapForPreview!!,
                    onClose = { selectedSnapForPreview = null },
                    onPrev = { if (currentIndex > 0) selectedSnapForPreview = filteredSnapDetails[currentIndex - 1] },
                    onNext = { if (currentIndex < filteredSnapDetails.size - 1) selectedSnapForPreview = filteredSnapDetails[currentIndex + 1] },
                    isPrevEnabled = currentIndex > 0,
                    isNextEnabled = currentIndex < filteredSnapDetails.size - 1,
                    onStatusChange = { updatedSnap ->
                        val indexInOriginalList = allSnapDetails.indexOfFirst { it.id == updatedSnap.id }
                        if (indexInOriginalList != -1) {
                            allSnapDetails[indexInOriginalList] = updatedSnap
                            selectedSnapForPreview = updatedSnap
                            // TODO: Update in DAO
                        }
                    }
                )
            }
        }

        if (snapProcessingState.showProcessingDialog) {
            ProcessSnapDialog(
                uiState = snapProcessingState,
                onDismiss = { snapProcessingState = snapProcessingState.copy(showProcessingDialog = false) },
                onPickFromGallery = { imagePickerLauncher.launch("image/*") },
                onTakePhoto = { requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                onSaveSnap = {
                    if (snapProcessingState.extractedNumber != null && snapProcessingState.originalImageUri != null) {
                        val newId = UUID.randomUUID().toString()
                        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        val newSnap = SnapDetail(
                            id = newId,
                            mainImageResId = R.drawable.ic_snaps_custom, // Placeholder
                            licensePlateImageResId = R.drawable.ic_snaps_custom, // Placeholder for cropped plate
                            mapImageResId = R.drawable.ic_snaps_custom, // Placeholder, (was ic_map_placeholder)
                            dateTime = currentTime,
                            deviceId = "DeviceX",
                            operatorId = "OperatorY",
                            speed = 0,
                            speedLimit = 0,
                            location = "N/A",
                            violationDistance = "N/A",
                            recordNr = "REC-${newId.substring(0,4)}",
                            regNr = snapProcessingState.extractedNumber!!,
                            regNrStatus = "Pending Validation",
                            latitude = "0.0",
                            longitude = "0.0",
                            evidenceDate = currentTime.substringBefore(" "),
                            district = "N/A",
                            policeStation = "N/A",
                            address = "N/A",
                            violationSummary = "Processed via app upload",
                            violationManagementLink = "",
                            accessLink = "",
                            uploadStatus = "Pending Upload",
                            status = SnapStatus.PENDING
                        )
                        allSnapDetails.add(0, newSnap)
                        // TODO: Save newSnap to DAO
                        snapProcessingState = snapProcessingState.copy(showProcessingDialog = false)
                    } else {
                        snapProcessingState = snapProcessingState.copy(processingError = "Cannot save, missing number or image.")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NormalTopAppBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggled: () -> Unit,
    onNavigateBack: () -> Unit,
    onShowDatePicker: () -> Unit,
    onShowInfoDialog: () -> Unit,
    onAddSnap: () -> Unit
) {
    TopAppBar(
        title = {
            Crossfade(targetState = isSearchActive, label = "SearchCrossfade") {
                if (it) {
                    BasicTextField(value = searchQuery, onValueChange = onSearchQueryChange, textStyle = TextStyle(color = TextColorLight), cursorBrush = SolidColor(TextColorLight), singleLine = true, modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField -> Box(contentAlignment = Alignment.CenterStart) { if (searchQuery.isEmpty()) Text("Search by vehicle number...", color = TextColorLight.copy(alpha = 0.5f)); innerTextField() } }
                    )
                } else Text("Snaps", color = TextColorLight, fontWeight = FontWeight.Bold)
            }
        },
        navigationIcon = { if (!isSearchActive) IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Navigate back", tint = TextColorLight) } },
        actions = {
            Crossfade(targetState = isSearchActive, label = "ActionsCrossfade") {
                if (it) {
                    IconButton(onClick = { onSearchToggled(); onSearchQueryChange("") }) { Icon(Icons.Default.Close, "Close Search", tint = TextColorLight) }
                } else {
                    Row {
                        IconButton(onClick = onAddSnap) { Icon(Icons.Filled.AddAPhoto, "Add New Snap", tint = TextColorLight) }
                        IconButton(onClick = onShowDatePicker) { Icon(Icons.Filled.CalendarToday, "Select Date", tint = TextColorLight) }
                        IconButton(onClick = onShowInfoDialog) { Icon(Icons.Filled.Info, "Status Info", tint = TextColorLight) }
                        IconButton(onClick = onSearchToggled) { Icon(Icons.Filled.Search, "Search Snaps", tint = TextColorLight) }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = TopBarColor.copy(alpha = 0.9f))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopAppBar(
    selectedCount: Int, allSelected: Boolean, onClose: () -> Unit, onSelectAll: () -> Unit, onDelete: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount Selected", color = TextColorLight) },
        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close Selection", tint = TextColorLight) } },
        actions = { Checkbox(checked = allSelected, onCheckedChange = { onSelectAll() }); IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete Selected", tint = TextColorLight) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = TopBarColor.copy(alpha = 0.9f))
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SnapCard(
    snapDetail: SnapDetail, isSelected: Boolean, selectionMode: Boolean, onClick: () -> Unit, onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = if(isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else snapDetail.status.color)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = snapDetail.mainImageResId), contentDescription = "Thumbnail of ${snapDetail.regNr}", modifier = Modifier.size(56.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.padding(8.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(snapDetail.regNr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Speed: ${snapDetail.speed} km/h", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }
            if (selectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
            } else {
                 Text(snapDetail.dateTime.substringBefore(" "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun StatusInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Status Indicators") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { StatusIndicator(SnapStatus.PENDING.color, "Pending: Not yet updated."); StatusIndicator(SnapStatus.UPDATED.color, "Updated: Saved locally."); StatusIndicator(SnapStatus.UPLOADED.color, "Uploaded: Synced with server."); StatusIndicator(SnapStatus.REJECTED.color, "Rejected: Challan cancelled.") } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
fun StatusIndicator(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(20.dp).background(color, CircleShape)); Spacer(modifier = Modifier.padding(start = 8.dp)); Text(text, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
fun ProcessSnapDialog(
    uiState: SnapProcessingState,
    onDismiss: () -> Unit,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onSaveSnap: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Process New Snap") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 20.dp))
                    Text("Processing image...")
                } else if (uiState.croppedPlateBitmap != null && uiState.extractedNumber != null) {
                    Text("Processed Result:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                    Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Cropped Number Plate:")
                            Image(bitmap = uiState.croppedPlateBitmap.asImageBitmap(), contentDescription = "Cropped Number Plate", modifier = Modifier.height(80.dp).padding(vertical = 8.dp))
                            Text("Extracted: ${uiState.extractedNumber}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                } else {
                    Button(onClick = onPickFromGallery, modifier = Modifier.fillMaxWidth()) { Text("Upload from Gallery") }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) { Text("Take Photo") }
                }
                uiState.processingError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp))
                }
            }
        },
        confirmButton = {
            if (uiState.croppedPlateBitmap != null && uiState.extractedNumber != null && !uiState.isLoading) {
                TextButton(onClick = onSaveSnap) { Text("Save Snap") }
            }
            TextButton(onClick = onDismiss) { Text(if (uiState.croppedPlateBitmap != null && !uiState.isLoading) "Discard" else "Cancel") }
        }
    )
}

fun createImageUriForCapture(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
}

fun processImageAndCreateSnapDetail(
    context: Context,
    imageUri: Uri,
    currentState: SnapProcessingState,
    scope: CoroutineScope, // Added CoroutineScope parameter
    onResult: (SnapProcessingState) -> Unit
) {
    val initialOriginalBitmap = try {
        context.contentResolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        Log.e("SnapsScreen", "Error loading original bitmap: ${e.message}")
        onResult(currentState.copy(isLoading = false, processingError = "Failed to load image."))
        return
    }

    if (initialOriginalBitmap == null) {
        Log.e("SnapsScreen", "Failed to decode bitmap from URI: $imageUri")
        onResult(currentState.copy(isLoading = false, processingError = "Failed to load image."))
        return
    }

    Log.d("SnapsScreen", "Placeholder: Number plate detection would happen here.")
    val placeholderCroppedBitmap = Bitmap.createBitmap(initialOriginalBitmap, 0, 0, initialOriginalBitmap.width / 2, initialOriginalBitmap.height / 4)
    Log.d("SnapsScreen", "Placeholder: Cropping would happen here.")
    val placeholderExtractedText = "AB12CD3456 (Simulated)"
    Log.d("SnapsScreen", "Placeholder: OCR would happen here.")

    val picturesDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    if (picturesDirectory == null) {
        onResult(currentState.copy(isLoading = false, croppedPlateBitmap = placeholderCroppedBitmap, extractedNumber = placeholderExtractedText, processingError = "Storage directory not found."))
        return
    }
    Log.d("SnapsScreen", "Placeholder: Saving images would happen here.")

    scope.launch { // Use the passed-in scope
        kotlinx.coroutines.delay(1000) // Simulate processing time
        onResult(
            currentState.copy(
                isLoading = false,
                croppedPlateBitmap = placeholderCroppedBitmap,
                extractedNumber = placeholderExtractedText,
                originalImageUri = imageUri
            )
        )
    }
}

/*
// --- Placeholder function definitions (TO BE IMPLEMENTED BY YOU) ---

private fun detectPlateRegionWithOpenCV(bitmap: Bitmap): android.graphics.Rect? {
    Log.w("SnapsScreen", "detectPlateRegionWithOpenCV: Needs implementation!")
    // TODO: Implement OpenCV detection. Return a Rect if plate found, else null.
    return null
}

private fun cropBitmap(source: Bitmap, region: android.graphics.Rect): Bitmap? {
    Log.w("SnapsScreen", "cropBitmap: Needs implementation!")
    // TODO: Crop bitmap based on region. Ensure region is within bounds.
    // return Bitmap.createBitmap(source, region.left, region.top, region.width(), region.height())
    return source // Placeholder
}

private fun runOcrWithMLKit(bitmap: Bitmap): String? {
    Log.w("SnapsScreen", "runOcrWithMLKit: Needs implementation!")
    // TODO: Implement ML Kit Text Recognition (on-device).
    return null // Placeholder
}

private fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
    Log.w("SnapsScreen", "saveBitmapToFile: Needs implementation! Path: ${file.absolutePath}")
    // try { java.io.FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it); return true } }
    // catch (e: java.io.IOException) { Log.e("SnapsScreen", "Error saving bitmap: ${e.message}"); return false }
    return true // Placeholder
}

private fun renameOriginalImage(context: Context, originalUri: Uri, newFile: File): Boolean {
    Log.w("SnapsScreen", "renameOriginalImage: Needs robust implementation! Original: $originalUri, New Path: ${newFile.absolutePath}")
    return false // Placeholder
}
*/

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun SnapsScreenWithProcessingPreview() {
    Lasertac2Theme {
        SnapsScreen(onNavigateBack = {} /* snapLocationDao parameter removed */)
    }
}

@Preview(showBackground = true)
@Composable
fun ProcessSnapDialogPreview_Initial() {
    Lasertac2Theme {
        ProcessSnapDialog(
            uiState = SnapProcessingState(showProcessingDialog = true),
            onDismiss = {},
            onPickFromGallery = {},
            onTakePhoto = {},
            onSaveSnap = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProcessSnapDialogPreview_Processed() {
    val placeholderBitmap = Bitmap.createBitmap(150, 75, Bitmap.Config.ARGB_8888).apply {
        eraseColor(android.graphics.Color.LTGRAY)
    }
    Lasertac2Theme {
        ProcessSnapDialog(
            uiState = SnapProcessingState(
                showProcessingDialog = true,
                isLoading = false,
                croppedPlateBitmap = placeholderBitmap,
                extractedNumber = "MH20EE9090"
            ),
            onDismiss = {},
            onPickFromGallery = {},
            onTakePhoto = {},
            onSaveSnap = {}
        )
    }
}
