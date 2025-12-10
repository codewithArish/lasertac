package com.lasertrac.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lasertrac.app.data.repository.SnapRepository
import com.lasertrac.app.db.SnapDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SnapsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val snaps: List<SnapDetail> = emptyList(),
    val searchQuery: String = "",
    val selectionMode: Boolean = false,
    val selectedSnapIds: Set<String> = emptySet(),
    val snapForPreview: SnapDetail? = null
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

class SnapsViewModel(private val repository: SnapRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SnapsUiState())
    val uiState: StateFlow<SnapsUiState> = _uiState.asStateFlow()

    // Private flow to hold snaps from the non-reactive local media source
    private val _localMediaSnaps = MutableStateFlow<List<SnapDetail>>(emptyList())

    init {
        // Set initial loading state
        _uiState.update { it.copy(isLoading = true) }

        // 1. Fetch from the suspend function (Local Media) once
        viewModelScope.launch {
            repository.getLocalMediaSnaps()
                .onSuccess { mediaSnaps -> _localMediaSnaps.value = mediaSnaps }
                .onFailure { error -> _uiState.update { it.copy(error = "Failed to load from local media: ${error.message}") } }
        }

        // 2. Combine the reactive Flow from Room with the data from Local Media
        repository.getLocalDbSnaps()
            .combine(_localMediaSnaps) { dbSnaps, mediaSnaps ->
                // 3. Merge, remove duplicates, and sort
                (dbSnaps + mediaSnaps)
                    .distinctBy { it.id }
                    .sortedByDescending { it.dateTime }
            }
            .catch { e ->
                // 4. Handle exceptions from the database flow
                _uiState.update { it.copy(error = "Database error: ${e.message}", isLoading = false) }
            }
            .onEach { combinedSnaps ->
                // 5. Update the final UI state with the combined list
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        snaps = combinedSnaps
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteSelectedSnaps() {
        viewModelScope.launch {
            repository.deleteSnaps(_uiState.value.selectedSnapIds.toList())
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

class SnapsViewModelFactory(private val repository: SnapRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SnapsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SnapsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
