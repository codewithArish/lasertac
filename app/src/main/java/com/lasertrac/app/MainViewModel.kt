package com.lasertrac.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lasertrac.app.db.SnapLocationDao

/**
 * ViewModel to hold and manage UI-related data in a lifecycle-conscious way.
 * This ViewModel holds a reference to the SnapLocationDao.
 */
class MainViewModel(val snapLocationDao: SnapLocationDao) : ViewModel() {
    // Business logic can be added here in the future
}

/**
 * Factory for creating a `MainViewModel` with a constructor that takes a SnapLocationDao.
 */
class MainViewModelFactory(private val snapLocationDao: SnapLocationDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(snapLocationDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
