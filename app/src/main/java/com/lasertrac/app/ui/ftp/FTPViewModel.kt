package com.lasertrac.app.ui.ftp

import android.content.Context
import androidx.lifecycle.ViewModel

/**
 * A simplified ViewModel that acts as a bridge between the UI and the FTPManager singleton.
 * It does not contain any connection logic itself, but instead delegates to the manager.
 */
class FTPViewModel : ViewModel() {

    // Expose the state and file list from the manager directly to the UI.
    val uiState = FTPManager.uiState
    val snapFiles = FTPManager.snapFiles

    /**
     * Initiates the FTP connection and file fetching process via the FTPManager.
     */
    fun startFtpConnection(context: Context) {
        FTPManager.connectOrSync(context)
    }

    /**
     * Saves the new credentials and triggers a reconnection.
     */
    fun saveCredentialsAndReconnect(context: Context, username: String, password: String) {
        CredentialsManager.saveCredentials(context, username, password)
        FTPManager.reconnect(context)
    }

    /**
     * Overridden to ensure that we do not hold a connection indefinitely if the ViewModel is cleared
     * and the app process is still alive. For a truly persistent connection that survives beyond
     * the ViewModel's lifecycle, this might be removed, but it's good practice to clean up.
     */
    override fun onCleared() {
        // For a true singleton manager that persists, this should be commented out.
        // FTPManager.disconnect()
        super.onCleared()
    }
}
