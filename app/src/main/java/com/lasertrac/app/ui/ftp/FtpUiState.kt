package com.lasertrac.app.ui.ftp

/**
 * Represents the various states of the FTP fetching operation.
 */
enum class FtpUiState {
    IDLE,      // The initial state before any operation begins.
    LOADING,   // The state while data is being fetched from the FTP server.
    SUCCESS,   // The state when the data has been successfully fetched.
    ERROR      // The state when an error occurs during the fetching process.
}
