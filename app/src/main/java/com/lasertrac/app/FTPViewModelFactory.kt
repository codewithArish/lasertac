package com.lasertrac.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lasertrac.app.db.SnapLocationDao

class FTPViewModelFactory(private val snapLocationDao: SnapLocationDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FTPViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FTPViewModel(snapLocationDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}