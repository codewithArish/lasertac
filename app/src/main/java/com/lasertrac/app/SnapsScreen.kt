package com.lasertrac.app

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lasertrac.app.data.repository.LocalMediaRepository
import com.lasertrac.app.data.repository.SnapRepository
import com.lasertrac.app.db.AppDatabase
import com.lasertrac.app.db.SnapDetail
import com.lasertrac.app.db.SnapStatus
import com.lasertrac.app.ui.theme.TextColorLight
import com.lasertrac.app.ui.theme.TopBarColor
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val appDatabase = AppDatabase.getDatabase(context)
    val snapRepository = remember {
        SnapRepository(
            snapDao = appDatabase.snapDao(),
            localMediaRepository = LocalMediaRepository(context)
        )
    }
    val viewModel: SnapsViewModel = viewModel(factory = SnapsViewModelFactory(snapRepository, context))

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.selectedDateMillis,
        yearRange = IntRange(2000, java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)),
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Only allow dates up to today (disable future dates)
                val today = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                return utcTimeMillis <= today
            }

            override fun isSelectableYear(year: Int): Boolean {
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                return year <= currentYear
            }
        }
    )
    var isSearchActive by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Listener for refresh events
    LaunchedEffect(Unit) {
        viewModel.refreshEvent.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            if (uiState.selectionMode) {
                SelectionTopAppBar(
                    selectedCount = uiState.selectedSnapIds.size,
                    allSelected = if (uiState.filteredSnaps.isNotEmpty()) uiState.selectedSnapIds.size == uiState.filteredSnaps.size else false,
                    onClose = viewModel::clearSelectionMode,
                    onSelectAll = viewModel::toggleSelectAll,
                    onDelete = {
                        viewModel.deleteSelectedSnaps()
                        Toast.makeText(context, "${uiState.selectedSnapIds.size} snaps deleted.", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                NormalTopAppBar(
                    title = uiState.selectedDateString,
                    isSearchActive = isSearchActive,
                    searchQuery = uiState.searchQuery,
                    isRefreshing = uiState.isLoading,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onSearchToggle = { isSearchActive = !isSearchActive },
                    onNavigateBack = onNavigateBack,
                    onCalendarClick = { showDatePicker = true },
                    onInfoClick = { showInfoDialog = true },
                    onRefresh = { viewModel.refreshSnaps() }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
            if (uiState.isLoading && uiState.snaps.isEmpty()) { // Show main loader only on initial load
                CircularProgressIndicator()
            } else if (uiState.error != null) {
                Text(text = "Error: ${uiState.error}", color = colorScheme.error)
            } else if (uiState.filteredSnaps.isEmpty()) {
                Text("No snaps found.", style = typography.bodyLarge)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.filteredSnaps, key = { it.id }) { snap ->
                        SnapCard(
                            snap = snap,
                            isSelected = snap.id in uiState.selectedSnapIds,
                            onClick = { viewModel.onSnapClicked(snap) },
                            onLongClick = { viewModel.enterSelectionMode(snap.id) }
                        )
                    }
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.onDateSelected(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.onDateSelected(null) // Clear filter
                        showDatePicker = false
                    }) { Text("Clear") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        uiState.snapForPreview?.let { snap ->
            SnapPreviewDialog(snap = snap, onDismiss = viewModel::onPreviewDismissed)
        }

        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text("Snaps Screen Information") },
                text = { Text("Long-press a snap to enter selection mode.\n- Tap to preview a snap.\n- Use the search bar to filter by registration number or location.") },
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
    title: String,
    isSearchActive: Boolean,
    isRefreshing: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onNavigateBack: () -> Unit,
    onCalendarClick: () -> Unit,
    onInfoClick: () -> Unit,
    onRefresh: () -> Unit
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
                Text(title, color = TextColorLight)
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
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = TextColorLight,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Snaps", tint = TextColorLight)
                    }
                }
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
        border = if (isSelected) BorderStroke(2.dp, colorScheme.primary) else null,
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
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(image = Icons.Default.BrokenImage)
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(snap.regNr.ifBlank { "No Reg Nr" }, style = typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(snap.location, style = typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(snap.dateTime, style = typography.bodySmall, color = Color.Gray)
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
                modifier = Modifier.fillMaxSize().background(colorScheme.background).padding(it)
            ) {
                TopAppBar(
                    title = { Text("Snap Detail") },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") } }
                )
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { AsyncImage(snap.mainImage, "Main Image", Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)), error = rememberVectorPainter(image = Icons.Default.BrokenImage)) }
                    item { Text("Registration: ${snap.regNr}", fontWeight = FontWeight.Bold) }
                    item { Text("Date & Time: ${snap.dateTime}") }
                    item { Text("Location: ${snap.address}") }
                    item { Text("Status: ${snap.status.displayName}") }
                }
            }
        }
    }
}
