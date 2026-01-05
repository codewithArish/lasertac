package com.lasertrac.app.ui.ftp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lasertrac.app.db.AppDatabase
import com.lasertrac.app.db.SnapDao
import kotlinx.coroutines.flow.StateFlow

/**
 * A simplified ViewModel that acts as a bridge between the UI and the FTPManager singleton.
 * It delegates all logic to the manager.
 */
class FTPViewModel(application: Application) : AndroidViewModel(application) {

    private val snapDao: SnapDao = AppDatabase.getDatabase(application).snapDao()

    // Expose the state, file count, and error message from the manager directly to the UI.
    val uiState: StateFlow<FtpUiState> = FTPManager.uiState
    val syncedFileCount: StateFlow<Int> = FTPManager.syncedFileCount
    val errorMessage: StateFlow<String?> = FTPManager.errorMessage

    /**
     * Initiates the FTP sync process via the FTPManager.
     */
    fun startSync() {
        FTPManager.startSync(getApplication(), snapDao)
    }

    /**
     * Saves the new credentials and triggers a new sync process.
     */
    fun saveCredentialsAndSync(username: String, password: String) {
        val context = getApplication<Application>().applicationContext
        CredentialsManager.saveCredentials(context, username, password)
        // Disconnect first to ensure the new credentials are used for the connection.
        FTPManager.disconnect()
        FTPManager.startSync(context, snapDao)
    }

    /**
     * Overridden to ensure we disconnect from FTP when the ViewModel is cleared.
     * This prevents leaving an open connection if the app process is still alive.
     */
    override fun onCleared() {
        FTPManager.disconnect()
        super.onCleared()
    }
}

class FTPViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FTPViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FTPViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
