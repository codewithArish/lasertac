package com.lasertrac.app.ui.ftp

import android.content.Context
import androidx.lifecycle.ViewModel

/**
 * A simplified ViewModel that acts as a bridge between the UI and the FTPManager singleton.
 * It delegates all logic to the manager.
 */
class FTPViewModel : ViewModel() {

    // Expose the state, file count, and error message from the manager directly to the UI.
    val uiState = FTPManager.uiState
    val syncedFileCount = FTPManager.syncedFileCount
    val errorMessage = FTPManager.errorMessage

    /**
     * Initiates the FTP sync process via the FTPManager.
     */
    fun startSync(context: Context) {
        FTPManager.startSync(context)
    }

    /**
     * Saves the new credentials and triggers a new sync process.
     */
    fun saveCredentialsAndSync(context: Context, username: String, password: String) {
        CredentialsManager.saveCredentials(context, username, password)
        // Disconnect first to ensure the new credentials are used for the connection.
        FTPManager.disconnect()
        FTPManager.startSync(context)
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
