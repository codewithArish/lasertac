package com.lasertrac.app.ui.ftp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FTPViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FtpUiState.IDLE)
    val uiState: StateFlow<FtpUiState> = _uiState

    private val _snapFiles = MutableStateFlow<List<String>>(emptyList())
    val snapFiles: StateFlow<List<String>> = _snapFiles

    fun fetchSnapsForToday() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = FtpUiState.LOADING
            val ftpClient = FTPClient()
            try {
                ftpClient.connect("192.168.10.1", 21)
                if (!FTPReply.isPositiveCompletion(ftpClient.replyCode)) {
                    throw IOException("FTP server refused connection.")
                }
                ftpClient.login("TP0003P", "12345678")
                ftpClient.enterLocalPassiveMode()
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

                val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                val remoteBaseDir = "/manual_capture/"

                val directories = ftpClient.listDirectories(remoteBaseDir)
                val todayDirectories = directories.filter { it.isDirectory && it.name.startsWith(today) }

                val allFiles = mutableListOf<String>()
                for (dir in todayDirectories) {
                    val files = ftpClient.listFiles("${remoteBaseDir}${dir.name}")
                    allFiles.addAll(files.map { it.name })
                }

                _snapFiles.value = allFiles
                _uiState.value = FtpUiState.SUCCESS
            } catch (e: IOException) {
                e.printStackTrace()
                _uiState.value = FtpUiState.ERROR
            } finally {
                try {
                    ftpClient.logout()
                    ftpClient.disconnect()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
