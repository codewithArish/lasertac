package com.lasertrac.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lasertrac.app.db.SnapLocationDao

class MainViewModel(val snapLocationDao: SnapLocationDao) : ViewModel() {
}

class MainViewModelFactory(private val snapLocationDao: SnapLocationDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(snapLocationDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
