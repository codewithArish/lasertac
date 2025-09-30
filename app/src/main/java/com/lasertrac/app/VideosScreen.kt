package com.lasertrac.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircleOutline
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lasertrac.app.ui.theme.Lasertac2Theme
import com.lasertrac.app.ui.theme.TextColorLight
import com.lasertrac.app.ui.theme.TopBarColor

data class VideoDetail(
    val id: String,
    val thumbnailResId: Int,
    val regNr: String,
    val dateTime: String,
    val duration: String,
    val evidenceDate: String,
    val status: SnapStatus
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VideosScreen(onNavigateBack: () -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = null)
    val selectedDateFormatted by remember {
        derivedStateOf { datePickerState.selectedDateMillis?.toFormattedDateString() ?: "All Videos" }
    }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedVideoIds = remember { mutableStateListOf<String>() }

    val allVideoDetails = remember {
        mutableStateListOf(
            VideoDetail("1", R.drawable.ic_videos_custom, "HR26AS0001", "2024-05-21 10:30 AM", "0:35", "2024-05-21", SnapStatus.PENDING),
            VideoDetail("2", R.drawable.ic_videos_custom, "DL7CS2107", "2024-05-22 11:00 AM", "1:15", "2024-05-22", SnapStatus.UPDATED),
            VideoDetail("3", R.drawable.ic_videos_custom, "DL7CV3601", "2024-05-21 11:30 AM", "0:50", "2024-05-21", SnapStatus.UPLOADED),
            VideoDetail("4", R.drawable.ic_videos_custom, "DL9CBC9917", "2024-05-23 12:00 PM", "2:05", "2024-05-23", SnapStatus.REJECTED),
        )
    }

    val filteredVideoDetails = remember(searchQuery, allVideoDetails.toList(), datePickerState.selectedDateMillis) {
        val selectedFilterDate = datePickerState.selectedDateMillis?.toFormattedDateString("yyyy-MM-dd")
        val dateFiltered = if (selectedFilterDate != null) {
            allVideoDetails.filter { it.evidenceDate == selectedFilterDate }
        } else {
            allVideoDetails
        }
        if (searchQuery.isBlank()) {
            dateFiltered
        } else {
            dateFiltered.filter { it.regNr.contains(searchQuery, ignoreCase = true) }
        }
    }

    fun exitSelectionMode() {
        selectionMode = false
        selectedVideoIds.clear()
    }

    BackHandler(enabled = selectionMode) {
        if (selectionMode) {
            exitSelectionMode()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Crossfade(targetState = selectionMode, label = "TopBarCrossfade") { isSelectionMode ->
                if (isSelectionMode) {
                    val allVisibleSelected = selectedVideoIds.size == filteredVideoDetails.size && filteredVideoDetails.isNotEmpty()
                    SelectionTopAppBar(
                        selectedCount = selectedVideoIds.size,
                        allSelected = allVisibleSelected,
                        onClose = { exitSelectionMode() },
                        onSelectAll = {
                            if (allVisibleSelected) {
                                selectedVideoIds.clear()
                            } else {
                                selectedVideoIds.clear()
                                selectedVideoIds.addAll(filteredVideoDetails.map { it.id })
                            }
                        },
                        onDelete = {
                            allVideoDetails.removeAll { selectedVideoIds.contains(it.id) }
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
                        onShowDatePicker = { showDatePicker = true }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { showDatePicker = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Displaying videos for: $selectedDateFormatted",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                if (datePickerState.selectedDateMillis != null) {
                    IconButton(onClick = { datePickerState.selectedDateMillis = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Date Filter", modifier = Modifier.size(16.dp))
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredVideoDetails, key = { it.id }) { videoDetail ->
                    VideoCard(
                        videoDetail = videoDetail,
                        isSelected = selectedVideoIds.contains(videoDetail.id),
                        selectionMode = selectionMode,
                        onClick = {
                            if (selectionMode) {
                                if (selectedVideoIds.contains(videoDetail.id)) {
                                    selectedVideoIds.remove(videoDetail.id)
                                } else {
                                    selectedVideoIds.add(videoDetail.id)
                                }
                            } else {
                                // No-op for now
                            }
                        },
                        onLongClick = {
                            if (!selectionMode) {
                                selectionMode = true
                                selectedVideoIds.add(videoDetail.id)
                            }
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
            ) {
                DatePicker(state = datePickerState)
            }
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
    onShowDatePicker: () -> Unit
) {
    TopAppBar(
        title = {
            Crossfade(targetState = isSearchActive, label = "SearchCrossfade") { isSearching ->
                if (isSearching) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        textStyle = TextStyle(color = TextColorLight),
                        cursorBrush = SolidColor(TextColorLight),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text("Search by vehicle number...", color = TextColorLight.copy(alpha = 0.5f))
                                }
                                innerTextField()
                            }
                        }
                    )
                } else {
                    Text("Videos", color = TextColorLight, fontWeight = FontWeight.Bold)
                }
            }
        },
        navigationIcon = {
            if (!isSearchActive) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = TextColorLight
                    )
                }
            }
        },
        actions = {
            Crossfade(targetState = isSearchActive, label = "ActionsCrossfade") { isSearching ->
                if (isSearching) {
                    IconButton(onClick = { onSearchToggled(); onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Search",
                            tint = TextColorLight
                        )
                    }
                } else {
                    Row {
                        IconButton(onClick = onShowDatePicker) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = "Select Date",
                                tint = TextColorLight
                            )
                        }
                        IconButton(onClick = onSearchToggled) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search Snaps",
                                tint = TextColorLight
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TopBarColor.copy(alpha = 0.9f)
        )
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
        title = { Text("$selectedCount Selected", color = TextColorLight) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Selection", tint = TextColorLight)
            }
        },
        actions = {
            Checkbox(checked = allSelected, onCheckedChange = { onSelectAll() })
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Selected", tint = TextColorLight)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TopBarColor.copy(alpha = 0.9f)
        )
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoCard(
    videoDetail: VideoDetail, 
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = if(isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else videoDetail.status.color)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = videoDetail.thumbnailResId),
                    contentDescription = "Thumbnail of ${videoDetail.regNr}",
                    modifier = Modifier.size(width = 100.dp, height = 56.dp).clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
                Icon(Icons.Default.PlayCircleOutline, contentDescription = "Play Video", tint=Color.White.copy(alpha=0.8f), modifier = Modifier.size(32.dp))
            }
            
            Spacer(modifier = Modifier.padding(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = videoDetail.regNr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = videoDetail.dateTime.substringBefore(" "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            
            if (selectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
            } else {
                 Text(
                    text = videoDetail.duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun VideosScreenPreview() {
    Lasertac2Theme {
        VideosScreen(onNavigateBack = {})
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun VideoCardPreview() {
    Lasertac2Theme {
        val dummyVideo = remember {
            VideoDetail("1", R.drawable.ic_videos_custom, "KA01XY1234", "2024-05-21 01:30 PM", "1:25", "2024-05-21", SnapStatus.PENDING)
        }
        Box(modifier = Modifier.padding(8.dp).background(MaterialTheme.colorScheme.background)){
            VideoCard(videoDetail = dummyVideo, isSelected = false, selectionMode = false, onClick={}, onLongClick = {})
        }
    }
}
