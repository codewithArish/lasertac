package com.lasertrac.app.ui.ftp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A singleton manager for handling a persistent FTP connection with automatic background synchronization.
 */
object FTPManager {

    private const val TAG = "FTPManager"
    private val ftpClient = FTPClient()
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    enum class FtpUiState {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    private val _uiState = MutableStateFlow(FtpUiState.IDLE)
    val uiState: StateFlow<FtpUiState> = _uiState

    private val _snapFiles = MutableStateFlow<List<String>>(emptyList())
    val snapFiles: StateFlow<List<String>> = _snapFiles

    init {
        startStateMonitor()
    }

    fun connectOrSync(context: Context) {
        if (_uiState.value == FtpUiState.LOADING) return

        managerScope.launch {
            _uiState.value = FtpUiState.LOADING
            try {
                val username = CredentialsManager.getUsername(context) ?: ""
                val password = CredentialsManager.getPassword(context) ?: ""

                if (username.isBlank() || password.isBlank()) {
                    throw IOException("Username or password not set.")
                }

                if (!ftpClient.isConnected) {
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

                val files = fetchFilesFromServer()
                _snapFiles.value = files
                _uiState.value = FtpUiState.SUCCESS

            } catch (e: IOException) {
                Log.e(TAG, "Connection or initial sync failed.", e)
                handleConnectionError()
            }
        }
    }

    fun reconnect(context: Context) {
        disconnect()
        connectOrSync(context)
    }

    private suspend fun fetchFilesFromServer(): List<String> {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val remoteBaseDir = "/manual_capture/"

        val allEntries = ftpClient.listFiles(remoteBaseDir) ?: emptyArray()
        val todayDirectories = allEntries.filter { it != null && it.isDirectory && it.name.startsWith(today) }

        val allFiles = mutableListOf<String>()
        for (dir in todayDirectories) {
            val files = ftpClient.listFiles("${remoteBaseDir}${dir.name}") ?: emptyArray()
            allFiles.addAll(files.filterNotNull().map { it.name })
        }
        return allFiles
    }

    private fun fileSyncFlow() = flow {
        while (ftpClient.isConnected) {
            emit(fetchFilesFromServer())
            delay(30_000) // 30-second sync interval
        }
    }

    private fun startStateMonitor() {
        uiState
            .flatMapLatest { state ->
                if (state == FtpUiState.SUCCESS) {
                    fileSyncFlow().catch { e ->
                        Log.e(TAG, "Background sync failed.", e)
                        handleConnectionError()
                    }
                } else {
                    emptyFlow()
                }
            }
            .onEach { files ->
                _snapFiles.value = files
            }
            .launchIn(managerScope)
    }

    private fun handleConnectionError() {
        _uiState.value = FtpUiState.ERROR
        disconnect()
    }

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
                _uiState.value = FtpUiState.IDLE
                _snapFiles.value = emptyList()
            }
        }
    }
}
