package com.lasertrac.app.ui.ftp

/**
 * Represents the various states of the FTP fetching operation.
 */
enum class FtpUiState {
    IDLE,              // The initial state before any operation begins.
    CONNECTING,        // Connection is being established.
    SYNCING,           // Files are being synced.
    SYNCING_PROGRESS,  // Actively syncing with progress updates.
    SYNC_COMPLETED,    // Sync operation completed successfully.
    LOADING,           // The state while data is being fetched from the FTP server.
    SUCCESS,           // The state when the data has been successfully fetched.
    ERROR;             // The state when an error occurs during the fetching process.

    /**
     * Checks if the state indicates an active/busy operation.
     */
    fun isBusy(): Boolean = this in listOf(CONNECTING, SYNCING, SYNCING_PROGRESS)
}
