package com.lasertrac.app

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.lasertrac.app.db.SavedSnapLocationEntity
import com.lasertrac.app.db.SnapLocationDao
import com.lasertrac.app.ui.theme.Lasertac2Theme
import com.lasertrac.app.ui.theme.TextColorLight
import com.lasertrac.app.ui.theme.TopBarColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

// Mapper from DB Entity to UI Model
fun SavedSnapLocationEntity.toSnapDetail(): SnapDetail {
    val currentDateTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    val currentDateStr = currentDateTimeStr.substringBefore(" ")

    return SnapDetail(
        id = this.snapId,
        regNr = "", // Default empty, can be edited
        evidenceDate = currentDateStr,
        dateTime = currentDateTimeStr,
        status = SnapStatus.PENDING,
        speed = 0,
        deviceId = "",
        operatorId = "",
        speedLimit = 0,
        location = this.fullAddress,
        violationDistance = "",
        recordNr = "REC-${this.snapId.take(4)}",
        latitude = this.latitude.toString(),
        longitude = this.longitude.toString(),
        district = this.district,
        policeStation = this.selectedPoliceArea,
        address = this.fullAddress,
        uploadStatus = "Pending",
        mainImage = R.drawable.ic_snaps_custom, // Using default placeholder
        licensePlateImage = R.drawable.ic_snaps_custom,
        mapImage = R.drawable.ic_snaps_custom,
        violationSummary = "",
        violationManagementLink = "",
        accessLink = "",
        regNrStatus = ""
    )
}

fun SnapDetail.toDbSnapLocationEntity(): SavedSnapLocationEntity {
    return SavedSnapLocationEntity(
        snapId = this.id,
        latitude = this.latitude.toDoubleOrNull() ?: 0.0,
        longitude = this.longitude.toDoubleOrNull() ?: 0.0,
        fullAddress = this.address,
        district = this.district,
        country = "", // Assuming default values for these
        selectedCity = "",
        selectedState = "",
        selectedPoliceArea = this.policeStation
    )
}


fun Long.toFormattedDateString(pattern: String = "dd-MM-yyyy"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

// Returns a list of mock snaps for development and preview purposes
fun getTemporaryDevelopmentSnaps(): List<SnapDetail> {
    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val yesterdayDateTime = Calendar.getInstance().apply { add(Calendar.DATE, -1) }.time
    val yesterdayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(yesterdayDateTime)
    val todayDateTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    return listOf(
        SnapDetail("dev_mock1", "DL5CQ1234", todayDateTimeStr, 95, 80, "City Highway", todayDateStr, "Metro District", "Metro PS", "Address for Today 1", "10.1", "11.1", "MREC-TODAY-1", "MOCK-DEV-TODAY-1", "MOCK-OP", "110 m", "Pending", SnapStatus.PENDING, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, "Overspeeding by 15 km/h", "", "", "Valid"),
        SnapDetail("dev_mock2", "MH14AB5678", todayDateTimeStr, 65, 50, "Suburban Road", todayDateStr, "Suburban District", "Suburban PS", "Address for Today 2", "10.2", "11.2", "MREC-TODAY-2", "MOCK-DEV-TODAY-2", "MOCK-OP", "90 m", "Uploaded", SnapStatus.UPLOADED, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, "Overspeeding by 15 km/h", "", "", "Valid"),
        SnapDetail("dev_mock3", "KA01XY9999", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(yesterdayDateTime), 80, 80, "State Highway 5", yesterdayDateStr, "Rural District", "Rural PS", "Address for Yesterday 1", "10.3", "11.3", "MREC-YDAY-1", "MOCK-DEV-YDAY-1", "MOCK-OP-2", "0 m", "Rejected", SnapStatus.REJECTED, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, R.drawable.ic_snaps_custom, "No violation.", "", "", "Invalid")
    )
}

fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir = File(context.cacheDir, "images")
    if (!storageDir.exists()) storageDir.mkdirs()
    return File.createTempFile(imageFileName, ".jpg", storageDir)
}

fun createNewSnap(imageUri: Uri, deviceId: String): SnapDetail {
    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayDateTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    return SnapDetail(
        id = UUID.randomUUID().toString(),
        regNr = "",
        dateTime = todayDateTimeStr,
        speed = 0,
        speedLimit = 0,
        location = "From $deviceId",
        evidenceDate = todayDateStr,
        district = "",
        policeStation = "",
        address = "",
        latitude = "0.0",
        longitude = "0.0",
        recordNr = "",
        deviceId = deviceId,
        operatorId = "USER",
        violationDistance = "",
        uploadStatus = "Pending",
        status = SnapStatus.PENDING,
        mainImage = imageUri,
        licensePlateImage = imageUri, // Placeholder, can be updated after processing
        mapImage = imageUri, // Placeholder
        violationSummary = "",
        violationManagementLink = "",
        accessLink = "",
        regNrStatus = ""
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SnapsScreen(onNavigateBack: () -> Unit, snapLocationDao: SnapLocationDao) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showAddSnapDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allSnapDetails = remember { mutableStateListOf<SnapDetail>() }

    // Load snaps from the database
    LaunchedEffect(key1 = snapLocationDao) {
        snapLocationDao.getAllSnapLocations().map { dbList ->
            dbList.map { it.toSnapDetail() }
        }.collect { mappedList ->
            if (allSnapDetails.isEmpty()) { // Initial load
                allSnapDetails.addAll(mappedList.ifEmpty { getTemporaryDevelopmentSnaps() })
            }
        }
    }

    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { allSnapDetails.add(0, createNewSnap(it, "Gallery")) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempImageUri?.let { allSnapDetails.add(0, createNewSnap(it, "Camera")) }
        } else {
            Toast.makeText(context, "Camera capture failed", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val newImageFile = createImageFile(context)
            val newImageUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", newImageFile)
            tempImageUri = newImageUri
            cameraLauncher.launch(newImageUri)
        } else {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    var selectionMode by remember { mutableStateOf(false) }
    val selectedSnapIds = remember { mutableStateListOf<String>() }
    var selectedSnapForPreview by remember { mutableStateOf<SnapDetail?>(null) }

    fun exitSelectionMode() {
        selectionMode = false
        selectedSnapIds.clear()
    }

    val filteredSnapDetails by remember(searchQuery, allSnapDetails.toList(), datePickerState.selectedDateMillis) {
        derivedStateOf {
            val selectedFilterDate = datePickerState.selectedDateMillis?.toFormattedDateString("yyyy-MM-dd")
            val dateFiltered = if (selectedFilterDate != null) {
                allSnapDetails.filter { it.evidenceDate == selectedFilterDate }
            } else {
                allSnapDetails
            }
            if (searchQuery.isBlank()) dateFiltered else dateFiltered.filter { it.regNr.contains(searchQuery, true) || it.location.contains(searchQuery, true) }
        }
    }

    if (showAddSnapDialog) {
        AddSnapDialog(
            onDismiss = { showAddSnapDialog = false },
            onFromGallery = { showAddSnapDialog = false; galleryLauncher.launch("image/*") },
            onFromCamera = { showAddSnapDialog = false; cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (selectionMode) {
                SelectionTopAppBar(
                    selectedCount = selectedSnapIds.size,
                    allSelected = selectedSnapIds.size == filteredSnapDetails.size && filteredSnapDetails.isNotEmpty(),
                    onClose = { exitSelectionMode() },
                    onSelectAll = {
                        if (selectedSnapIds.size == filteredSnapDetails.size) selectedSnapIds.clear() else selectedSnapIds.addAll(filteredSnapDetails.map { it.id })
                    },
                    onDelete = {
                        // TODO: Also delete from DAO
                        allSnapDetails.removeAll { selectedSnapIds.contains(it.id) }
                        exitSelectionMode()
                    }
                )
            } else {
                NormalTopAppBar(
                    isSearchActive, searchQuery, { searchQuery = it }, { isSearchActive = !isSearchActive }, onNavigateBack, { showDatePicker = true }, { showInfoDialog = true }, { showAddSnapDialog = true }
                )
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton({ showAddSnapDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, "Add Snap", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            val selectedDateFormatted by remember { derivedStateOf { datePickerState.selectedDateMillis?.toFormattedDateString() ?: "All Snaps" } }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { showDatePicker = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Displaying snaps for: $selectedDateFormatted", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                if (datePickerState.selectedDateMillis != null) {
                    IconButton({ datePickerState.selectedDateMillis = null }, Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Clear Date Filter", modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (filteredSnapDetails.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(16.dp), Alignment.Center) {
                    Text(if (allSnapDetails.isEmpty()) "No snaps available." else "No snaps match filter.")
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp, 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredSnapDetails, key = { it.id }) { snapDetail ->
                        SnapCard(snapDetail, selectedSnapIds.contains(snapDetail.id), selectionMode,
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
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = { TextButton({ showDatePicker = false }) { Text("OK") } },
                dismissButton = { TextButton({ showDatePicker = false }) { Text("Cancel") } })
            {
                DatePicker(state = datePickerState)
            }
        }

        if (showInfoDialog) {
            StatusInfoDialog { showInfoDialog = false }
        }

        if (selectedSnapForPreview != null) {
            Dialog({ selectedSnapForPreview = null }, DialogProperties(usePlatformDefaultWidth = false)) {
                val currentIndex = filteredSnapDetails.indexOf(selectedSnapForPreview)
                ImagePreviewScreen(
                    snapDetail = selectedSnapForPreview!!,
                    onClose = { selectedSnapForPreview = null },
                    onPrev = { if (currentIndex > 0) selectedSnapForPreview = filteredSnapDetails[currentIndex - 1] },
                    onNext = { if (currentIndex < filteredSnapDetails.size - 1) selectedSnapForPreview = filteredSnapDetails[currentIndex + 1] },
                    isPrevEnabled = currentIndex > 0,
                    isNextEnabled = currentIndex < filteredSnapDetails.size - 1,
                    onStatusChange = { updatedSnap ->
                        coroutineScope.launch {
                            val indexInAll = allSnapDetails.indexOfFirst { it.id == updatedSnap.id }
                            if (indexInAll != -1) {
                                // TODO: Also update in DAO
                                allSnapDetails[indexInAll] = updatedSnap
                                selectedSnapForPreview = updatedSnap
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NormalTopAppBar(
    isSearchActive: Boolean, 
    q: String, 
    onQ: (String) -> Unit, 
    onTog: () -> Unit, 
    onBack: () -> Unit, 
    onDate: () -> Unit, 
    onInfo: () -> Unit,
    onAddSnap: () -> Unit
) {
    TopAppBar(
        title = {
            if (isSearchActive) {
                BasicTextField(q, onQ, textStyle = TextStyle(color = TextColorLight), cursorBrush = SolidColor(TextColorLight), singleLine = true, modifier = Modifier.fillMaxWidth()) {
                    if (q.isEmpty()) Text("Search by vehicle number or location...", color = TextColorLight.copy(0.5f))
                    it()
                }
            } else {
                Text("Snaps", color = TextColorLight, fontWeight = FontWeight.Bold)
            }
        },
        navigationIcon = { if (!isSearchActive) IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextColorLight) } },
        actions = {
            if (isSearchActive) {
                IconButton({ onQ(""); onTog() }) { Icon(Icons.Default.Close, "Close Search", tint = TextColorLight) }
            } else {
                IconButton(onAddSnap) { Icon(Icons.Default.Add, "Add Snap", tint = TextColorLight) }
                IconButton(onDate) { Icon(Icons.Default.CalendarToday, "Select Date", tint = TextColorLight) }
                IconButton(onInfo) { Icon(Icons.Default.Info, "Status Info", tint = TextColorLight) }
                IconButton(onTog) { Icon(Icons.Default.Search, "Search Snaps", tint = TextColorLight) }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = TopBarColor.copy(0.9f))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopAppBar(selectedCount: Int, allSelected: Boolean, onClose: () -> Unit, onSelectAll: () -> Unit, onDelete: () -> Unit) {
    TopAppBar(
        title = { Text("$selectedCount Selected", color = TextColorLight) },
        navigationIcon = { IconButton(onClose) { Icon(Icons.Default.Close, "Close Selection", tint = TextColorLight) } },
        actions = {
            Checkbox(allSelected, { onSelectAll() })
            IconButton(onDelete) { Icon(Icons.Default.Delete, "Delete Selected", tint = TextColorLight) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = TopBarColor.copy(0.9f))
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SnapCard(snap: SnapDetail, sel: Boolean, selMode: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(2.dp, if (sel) MaterialTheme.colorScheme.primary else snap.status.color)
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = snap.mainImage, contentDescription = "Thumb", modifier = Modifier.size(56.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(Modifier.padding(8.dp))
            Column(Modifier.weight(1f)) {
                Text(snap.regNr.ifEmpty { "N/A" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Speed: ${snap.speed} km/h", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f))
            }
            if (selMode) Checkbox(sel, { onClick() }) else Text(snap.dateTime.substringBefore(" "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
        }
    }
}

@Composable
fun AddSnapDialog(onDismiss: () -> Unit, onFromGallery: () -> Unit, onFromCamera: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Snap") },
        text = { Text("Choose an option to add a new snap.") },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TextButton(onFromGallery) { Text("From Gallery") }
                TextButton(onFromCamera) { Text("From Camera") }
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun StatusInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text("Status Indicators") }, 
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SnapStatus.entries.forEach { StatusIndicator(it.color, "${it.name}: explanation text here.") }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("OK") } }
    )
}

@Composable
fun StatusIndicator(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp).background(color, CircleShape))
        Spacer(Modifier.padding(start = 8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// A mock DAO for previewing the screen without a real database.
class MockSnapLocationDao : SnapLocationDao {
    override suspend fun insertOrUpdateSnapLocation(snapLocation: SavedSnapLocationEntity) {}
    override fun getSnapLocationById(snapId: String): Flow<SavedSnapLocationEntity?> = flowOf(null)
    override fun getAllSnapLocations(): Flow<List<SavedSnapLocationEntity>> = flowOf(getTemporaryDevelopmentSnaps().map { it.toDbSnapLocationEntity() })
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun SnapsScreenPreview() {
    Lasertac2Theme {
        SnapsScreen(onNavigateBack = {}, snapLocationDao = MockSnapLocationDao())
    }
}
