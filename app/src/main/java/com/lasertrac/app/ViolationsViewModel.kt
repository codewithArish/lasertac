package com.lasertrac.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lasertrac.app.db.Violation
import com.lasertrac.app.repository.ViolationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ViolationsViewModel(private val repository: ViolationRepository) : ViewModel() {

    val allViolations: Flow<List<Violation>> = repository.getAllViolations()

    fun insert(violation: Violation) = viewModelScope.launch {
        repository.insert(violation)
    }

    fun update(violation: Violation) = viewModelScope.launch {
        repository.update(violation)
    }

    fun delete(violationId: Int) = viewModelScope.launch {
        repository.deleteViolation(violationId)
    }
}

class ViolationsViewModelFactory(private val repository: ViolationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ViolationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ViolationsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}