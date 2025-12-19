package com.lasertrac.app

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lasertrac.app.data.repository.SnapRepository
import com.lasertrac.app.db.SnapDetail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SnapsUiState(
    val isLoading: Boolean = true, // Used for both initial load and subsequent refreshes
    val error: String? = null,
    val snaps: List<SnapDetail> = emptyList(),
    val searchQuery: String = "",
    val selectionMode: Boolean = false,
    val selectedSnapIds: Set<String> = emptySet(),
    val snapForPreview: SnapDetail? = null,
    val selectedDateMillis: Long? = null,
    val selectedDateString: String = "All Snaps"
) {
    val filteredSnaps: List<SnapDetail> by lazy {
        if (searchQuery.isBlank()) {
            snaps
        } else {
            snaps.filter {
                it.regNr.contains(searchQuery, ignoreCase = true) || it.location.contains(searchQuery, ignoreCase = true)
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SnapsViewModel(private val repository: SnapRepository, private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(SnapsUiState())
    val uiState: StateFlow<SnapsUiState> = _uiState.asStateFlow()

    private val _refreshEvent = MutableSharedFlow<String>()
    val refreshEvent: SharedFlow<String> = _refreshEvent.asSharedFlow()

    private val _selectedDate = MutableStateFlow<Long?>(System.currentTimeMillis())

    init {
        _selectedDate.flatMapLatest { dateMillis ->
            _uiState.update {
                it.copy(
                    selectedDateMillis = dateMillis,
                    selectedDateString = if (dateMillis == null) "All Snaps" else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateMillis))
                )
            }

            val snapsFlow = if (dateMillis == null) {
                repository.getLocalDbSnaps()
            } else {
                val formattedDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(dateMillis))
                repository.getSnapsByDate(formattedDate)
            }
            snapsFlow
        }
        .catch { e ->
            _uiState.update { it.copy(error = "Database error: ${e.message}", isLoading = false) }
        }
        .onEach { snaps ->
            _uiState.update { it.copy(isLoading = false, snaps = snaps) }
        }
        .launchIn(viewModelScope)

        // Initial data load
        refreshSnaps(isInitial = true)
    }

    fun refreshSnaps(isInitial: Boolean = false) {
        if (_uiState.value.isLoading) return // Prevent concurrent refreshes

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = withTimeoutOrNull(5000L) {
                repository.refreshSnapsFromLocalMedia()
            }

            if (result == null) {
                val errorMessage = "Refresh timed out."
                if (isInitial) {
                    _uiState.update { it.copy(error = errorMessage, isLoading = false) }
                } else {
                    _refreshEvent.emit(errorMessage)
                    _uiState.update { it.copy(isLoading = false) }
                }
            } else {
                result.onSuccess { newSnapsCount ->
                    if (!isInitial) {
                        val message = if (newSnapsCount > 0) "$newSnapsCount new snaps found." else "No new snaps found."
                        _refreshEvent.emit(message)
                    }
                    _uiState.update { it.copy(isLoading = false) }
                }.onFailure { error ->
                    val errorMessage = "Refresh failed: ${error.message}"
                    if (isInitial) {
                        _uiState.update { it.copy(error = errorMessage, isLoading = false) }
                    } else {
                        _refreshEvent.emit(errorMessage)
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun onDateSelected(dateMillis: Long?) {
        _selectedDate.value = dateMillis
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteSelectedSnaps() {
        viewModelScope.launch {
            val snapsToDelete = _uiState.value.snaps.filter { it.id in _uiState.value.selectedSnapIds }
            repository.deleteSnaps(snapsToDelete.map { it.id })

            // Delete files from storage
            for (snap in snapsToDelete) {
                try {
                    if (snap.id.startsWith("local_")) {
                        val mediaId = snap.id.removePrefix("local_").toLong()
                        val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId)
                        context.contentResolver.delete(contentUri, null, null)
                    }
                } catch (e: Exception) {
                    // Log or handle error if deletion fails
                }
            }

            clearSelectionMode()
        }
    }


    fun toggleSelection(snapId: String) {
        _uiState.update { state ->
            val newIds = if (snapId in state.selectedSnapIds) state.selectedSnapIds - snapId else state.selectedSnapIds + snapId
            state.copy(selectionMode = newIds.isNotEmpty(), selectedSnapIds = newIds)
        }
    }

    fun enterSelectionMode(initialSnapId: String) {
        _uiState.update { it.copy(selectionMode = true, selectedSnapIds = setOf(initialSnapId)) }
    }

    fun clearSelectionMode() {
        _uiState.update { it.copy(selectionMode = false, selectedSnapIds = emptySet()) }
    }

    fun onSnapClicked(snap: SnapDetail) {
        if (uiState.value.selectionMode) {
            toggleSelection(snap.id)
        } else {
            _uiState.update { it.copy(snapForPreview = snap) }
        }
    }

    fun onPreviewDismissed() {
        _uiState.update { it.copy(snapForPreview = null) }
    }

    fun toggleSelectAll() {
        _uiState.update { state ->
            val allFilteredIds = state.filteredSnaps.map { it.id }.toSet()
            val newSelectedIds = if (state.selectedSnapIds.size == allFilteredIds.size) emptySet() else allFilteredIds
            state.copy(selectedSnapIds = newSelectedIds, selectionMode = newSelectedIds.isNotEmpty())
        }
    }
}

class SnapsViewModelFactory(private val repository: SnapRepository, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SnapsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SnapsViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
