package com.lasertrac.app.ui.ftp

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * A singleton manager for handling FTP connections and file synchronization.
 */
object FTPManager {

    private const val TAG = "FTPManager"
    private val ftpClient = FTPClient()
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    enum class FtpUiState {
        IDLE, // Koi operation nahi chal raha
        CONNECTING, // Connection ho raha hai
        SYNCING, // Files sync ho rahi hain
        SYNC_COMPLETED, // Sync poora ho gaya
        ERROR // Koi galti hui
    }

    private val _uiState = MutableStateFlow(FtpUiState.IDLE)
    val uiState: StateFlow<FtpUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _syncedFileCount = MutableStateFlow(0)
    val syncedFileCount: StateFlow<Int> = _syncedFileCount.asStateFlow()

    /**
     * Connects to the FTP server and starts the sync process automatically.
     * Can be triggered manually or automatically.
     */
    fun startSync(context: Context) {
        if (_uiState.value == FtpUiState.CONNECTING || _uiState.value == FtpUiState.SYNCING) return

        managerScope.launch {
            _uiState.value = FtpUiState.CONNECTING
            try {
                // Agar pehle se connected nahi hai to connect karein
                if (!ftpClient.isConnected) {
                    val username = CredentialsManager.getUsername(context) ?: ""
                    val password = CredentialsManager.getPassword(context) ?: ""

                    if (username.isBlank() || password.isBlank()) {
                        throw IOException("Username or password not set.")
                    }

                    ftpClient.connect("192.168.10.1", 21)
                    if (!FTPReply.isPositiveCompletion(ftpClient.replyCode)) {
                        throw IOException("FTP server refused connection.")
                    }
                    if (!ftpClient.login(username, password)) {
                        throw IOException("FTP login failed.")
                    }
                    ftpClient.enterLocalPassiveMode()
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
                }

                // Connection safal hone par, syncing shuru karein
                _uiState.value = FtpUiState.SYNCING
                val downloadedCount = downloadFilesFromServer(context)
                _syncedFileCount.value = downloadedCount
                _uiState.value = FtpUiState.SYNC_COMPLETED

            } catch (e: IOException) {
                Log.e(TAG, "Connection or sync failed.", e)
                _errorMessage.value = e.message ?: "An unknown error occurred."
                handleConnectionError()
            }
        }
    }

    /**
     * Downloads files from the /manual_capture/ directory on the FTP server.
     * Saves them to the device's Pictures/com.lasertrac/ directory.
     */
    private suspend fun downloadFilesFromServer(context: Context): Int {
        val remoteBaseDir = "/manual_capture/"
        val localDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "com.lasertrac")

        if (!localDir.exists()) {
            localDir.mkdirs()
        }

        var downloadedCount = 0
        try {
            val dateDirectories = ftpClient.listFiles(remoteBaseDir) ?: emptyArray()

            for (dir in dateDirectories) {
                if (dir == null || !dir.isDirectory) continue

                val remoteDirPath = "$remoteBaseDir${dir.name}/"
                val filesToDownload = ftpClient.listFiles(remoteDirPath) ?: emptyArray()

                for (ftpFile in filesToDownload) {
                    if (ftpFile == null || ftpFile.isDirectory) continue

                    val localFile = File(localDir, ftpFile.name)

                    // Agar file pehle se मौजूद nahi hai, to download karein
                    if (!localFile.exists()) {
                        val remoteFilePath = "$remoteDirPath${ftpFile.name}"
                        FileOutputStream(localFile).use { outputStream ->
                            if (ftpClient.retrieveFile(remoteFilePath, outputStream)) {
                                downloadedCount++
                                Log.i(TAG, "Downloaded: ${ftpFile.name}")
                            } else {
                                Log.w(TAG, "Failed to download: ${ftpFile.name}")
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error during file download.", e)
            throw e // Error ko aage bhej dein taaki UI state update ho sake
        }
        return downloadedCount
    }

    private fun handleConnectionError() {
        _uiState.value = FtpUiState.ERROR
        disconnect()
    }

    /**
     * Disconnects from the FTP server and resets the state.
     */
    fun disconnect() {
        managerScope.launch {
            try {
                if (ftpClient.isConnected) {
                    ftpClient.logout()
                    ftpClient.disconnect()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error during disconnect.", e)
            } finally {
                 if (_uiState.value != FtpUiState.ERROR) {
                    _uiState.value = FtpUiState.IDLE
                }
            }
        }
    }
}
