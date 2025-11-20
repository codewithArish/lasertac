package com.lasertrac.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lasertrac.app.db.SnapDao
import com.lasertrac.app.db.SnapDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SnapsViewModel(private val snapDao: SnapDao) : ViewModel() {

    // Private mutable state flows for internal management
    private val _searchQuery = MutableStateFlow("")
    private val _selectedDateMillis = MutableStateFlow<Long?>(null)
    private val _selectionMode = MutableStateFlow(false)
    private val _selectedSnapIds = MutableStateFlow<Set<String>>(emptySet())
    private val _snapForPreview = MutableStateFlow<SnapDetail?>(null)

    // Public immutable state flows for the UI to observe
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()
    val selectedSnapIds: StateFlow<Set<String>> = _selectedSnapIds.asStateFlow()
    val snapForPreview: StateFlow<SnapDetail?> = _snapForPreview.asStateFlow()

    // Combined state that derives from other flows
    val filteredSnaps: StateFlow<List<SnapDetail>> = combine(
        snapDao.getAllSnaps(), // Assumes snapDao.getAllSnaps() returns Flow<List<SnapDetail>>
        _searchQuery,
        _selectedDateMillis
    ) { snaps, query, dateMillis ->
        val dateFiltered = if (dateMillis != null) {
            val selectedFilterDate = dateMillis.toFormattedDateString()
            snaps.filter { it.evidenceDate == selectedFilterDate }
        } else {
            snaps
        }

        if (query.isBlank()) {
            dateFiltered
        } else {
            dateFiltered.filter {
                it.regNr.contains(query, ignoreCase = true) || it.location.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // --- Event Handlers ---

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onDateSelected(millis: Long?) {
        _selectedDateMillis.value = millis
    }

    fun deleteSelectedSnaps() {
        viewModelScope.launch {
            snapDao.deleteSnaps(selectedSnapIds.value.toList())
            clearSelectionMode()
        }
    }

    fun toggleSelection(snapId: String) {
        val currentIds = _selectedSnapIds.value
        _selectedSnapIds.value = if (snapId in currentIds) currentIds - snapId else currentIds + snapId
    }

    fun enterSelectionMode(initialSnapId: String) {
        _selectionMode.value = true
        toggleSelection(initialSnapId)
    }

    fun clearSelectionMode() {
        _selectionMode.value = false
        _selectedSnapIds.value = emptySet()
    }
    fun onSnapClicked(snap: SnapDetail) {
        _snapForPreview.value = snap
    }

    fun onPreviewDismissed() {
        _snapForPreview.value = null
    }
    fun toggleSelectAll() {
        if (selectedSnapIds.value.size == filteredSnaps.value.size) {
            _selectedSnapIds.value = emptySet()
        } else {
            _selectedSnapIds.value = filteredSnaps.value.map { it.id }.toSet()
        }
    }

    private fun Long.toFormattedDateString(pattern: String = "yyyy-MM-dd"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
    }
}

/**
 * Factory for creating a [SnapsViewModel] with a constructor that takes a [SnapDao].
 */
class SnapsViewModelFactory(private val snapDao: SnapDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SnapsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SnapsViewModel(snapDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
