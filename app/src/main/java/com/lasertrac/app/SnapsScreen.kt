package com.lasertrac.app

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.lasertrac.app.db.SavedSnapLocationEntity
import com.lasertrac.app.db.SnapLocationDao
import com.lasertrac.app.ui.theme.TextColorLight
import com.lasertrac.app.ui.theme.TopBarColor
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// DEFINITIVE FIX: The duplicate SnapDetail and SnapStatus classes have been removed from this file.
// They are now centralized in Model.kt.

fun SavedSnapLocationEntity.toSnapDetail(): SnapDetail {
    val currentDateTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(this.timestamp))
    val currentDateStr = currentDateTimeStr.substringBefore(" ")

    return SnapDetail(
        id = this.snapId,
        regNr = "", // Default empty, can be edited
        evidenceDate = currentDateStr,
        dateTime = currentDateTimeStr,
        status = SnapStatus.PENDING,
        speed = 0, // Placeholder
        deviceId = "", // Placeholder
        operatorId = "", // Placeholder
        speedLimit = 0, // Placeholder
        location = this.fullAddress ?: "N/A",
        violationDistance = "", // Placeholder
        recordNr = "REC-${this.snapId.take(4)}",
        latitude = this.latitude?.toString() ?: "N/A",
        longitude = this.longitude?.toString() ?: "N/A",
        district = this.district ?: "N/A",
        policeStation = this.selectedPoliceArea ?: "N/A",
        address = this.fullAddress ?: "N/A",
        uploadStatus = "Pending",
        mainImage = this.imageUri?.toUri() ?: R.drawable.ic_snaps_custom,
        licensePlateImage = this.imageUri?.toUri() ?: R.drawable.ic_snaps_custom,
        mapImage = R.drawable.ic_snaps_custom, // Placeholder
        violationSummary = "", // Placeholder
        violationManagementLink = "", // Placeholder
        accessLink = "", // Placeholder
        regNrStatus = "" // Placeholder
    )
}

fun Long.toFormattedDateString(pattern: String = "dd-MM-yyyy"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SnapsScreen(onNavigateBack: () -> Unit, snapLocationDao: SnapLocationDao) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allSnapDetails = remember { mutableStateListOf<SnapDetail>() }

    LaunchedEffect(key1 = snapLocationDao) {
        snapLocationDao.getAllSnapLocations().map { dbList ->
            dbList.map { it.toSnapDetail() }.sortedByDescending { it.dateTime }
        }.collect { snapsFromDb ->
            allSnapDetails.clear()
            allSnapDetails.addAll(snapsFromDb)
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (selectionMode) {
                SelectionTopAppBar(
                    selectedCount = selectedSnapIds.size,
                    allSelected = if (filteredSnapDetails.isNotEmpty()) selectedSnapIds.size == filteredSnapDetails.size else false,
                    onClose = { exitSelectionMode() },
                    onSelectAll = {
                        if (selectedSnapIds.size == filteredSnapDetails.size) selectedSnapIds.clear() else selectedSnapIds.addAll(filteredSnapDetails.map { it.id })
                    },
                    onDelete = {
                        coroutineScope.launch {
                            snapLocationDao.deleteSnapsByIds(selectedSnapIds.toList())
                            Toast.makeText(context, "${selectedSnapIds.size} snaps deleted.", Toast.LENGTH_SHORT).show()
                            exitSelectionMode()
                        }
                    }
                )
            } else {
                NormalTopAppBar(
                    isSearchActive, searchQuery, { searchQuery = it }, { isSearchActive = !isSearchActive }, onNavigateBack, { showDatePicker = true }, { showInfoDialog = true }
                )
            }
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if (filteredSnapDetails.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No snaps found.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredSnapDetails, key = { it.id }) { snap ->
                        SnapCard(
                            snap = snap,
                            isSelected = snap.id in selectedSnapIds,
                            onClick = {
                                if (selectionMode) {
                                    if (snap.id in selectedSnapIds) selectedSnapIds.remove(snap.id) else selectedSnapIds.add(snap.id)
                                } else {
                                    selectedSnapForPreview = snap
                                }
                            },
                            onLongClick = {
                                if (!selectionMode) {
                                    selectionMode = true
                                    selectedSnapIds.add(snap.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        selectedSnapForPreview?.let { snap ->
            SnapPreviewDialog(snap = snap, onDismiss = { selectedSnapForPreview = null })
        }

        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text("Snaps Screen Information") },
                text = { Text("Long-press a snap to enter selection mode.\n- Tap to preview a snap.\n- Use the search bar to filter by registration number or location.\n- Use the calendar to filter by date.") },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) { Text("OK") }
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
    onQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onNavigateBack: () -> Unit,
    onCalendarClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    TopAppBar(
        title = {
            if (isSearchActive) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = TextColorLight, fontSize = 16.sp),
                    cursorBrush = SolidColor(TextColorLight),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (searchQuery.isEmpty()) Text("Search by RegNr or Location...", color = TextColorLight.copy(alpha = 0.5f))
                            innerTextField()
                        }
                    }
                )
            } else {
                Text("Snaps", color = TextColorLight)
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextColorLight)
            }
        },
        actions = {
            if (isSearchActive) {
                IconButton(onClick = onSearchToggle) {
                    Icon(Icons.Default.Close, contentDescription = "Close Search", tint = TextColorLight)
                }
            } else {
                IconButton(onClick = onSearchToggle) {
                    Icon(Icons.Default.Search, contentDescription = "Search Snaps", tint = TextColorLight)
                }
                IconButton(onClick = onCalendarClick) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", tint = TextColorLight)
                }
                IconButton(onClick = onInfoClick) {
                    Icon(Icons.Default.Info, contentDescription = "Information", tint = TextColorLight)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = TopBarColor)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopAppBar(
    selectedCount: Int,
    allSelected: Boolean,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount selected", color = TextColorLight) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close Selection", tint = TextColorLight)
            }
        },
        actions = {
            Checkbox(checked = allSelected, onCheckedChange = { onSelectAll() })
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = TextColorLight)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.DarkGray)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SnapCard(
    snap: SnapDetail,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = snap.mainImage,
                contentDescription = "Snap preview",
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(snap.regNr.ifBlank { "No Reg Nr" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(snap.location, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(snap.dateTime, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.size(16.dp))
            Box(modifier = Modifier.background(snap.status.color, CircleShape).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(snap.status.displayName, color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SnapPreviewDialog(snap: SnapDetail, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold {
            Column(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(it)
            ) {
                TopAppBar(
                    title = { Text("Snap Detail") },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") } }
                )
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { AsyncImage(snap.mainImage, "Main Image", Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) }
                    item { Text("Registration: ${snap.regNr}", fontWeight = FontWeight.Bold) }
                    item { Text("Date & Time: ${snap.dateTime}") }
                    item { Text("Location: ${snap.address}") }
                    item { Text("Status: ${snap.status.displayName}") }
                }
            }
        }
    }
}