package com.lasertrac.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lasertrac.app.db.SnapDetail
import com.lasertrac.app.db.SnapRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SnapDetailViewModel(private val snapRepository: SnapRepository, private val snapId: String) : ViewModel() {

    private val _snapDetail = MutableStateFlow<SnapDetail?>(null)
    val snapDetail: StateFlow<SnapDetail?> = _snapDetail.asStateFlow()

    init {
        viewModelScope.launch {
            snapRepository.getSnap(snapId).collectLatest {
                _snapDetail.value = it
            }
        }
    }
}

class SnapDetailViewModelFactory(private val snapRepository: SnapRepository, private val snapId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SnapDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SnapDetailViewModel(snapRepository, snapId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
