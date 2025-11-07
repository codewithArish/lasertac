package com.lasertrac.app

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.io.FileInputStream
import java.io.IOException

enum class FtpStatus { IDLE, CONNECTING, UPLOADING, CONNECTION_SUCCESS, CONNECTION_ERROR, UPLOAD_SUCCESS, UPLOAD_ERROR }

class FTPViewModel : ViewModel() {

    private val _ftpServer = MutableStateFlow("192.168.1.12")
    val ftpServer: StateFlow<String> = _ftpServer.asStateFlow()

    private val _ftpPort = MutableStateFlow("2221")
    val ftpPort: StateFlow<String> = _ftpPort.asStateFlow()

    private val _ftpUsername = MutableStateFlow("")
    val ftpUsername: StateFlow<String> = _ftpUsername.asStateFlow()

    private val _ftpPassword = MutableStateFlow("")
    val ftpPassword: StateFlow<String> = _ftpPassword.asStateFlow()

    private val _departmentName = MutableStateFlow("")
    val departmentName: StateFlow<String> = _departmentName.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _status = MutableStateFlow(FtpStatus.IDLE)
    val status: StateFlow<FtpStatus> = _status.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun onFtpServerChange(value: String) {
        _ftpServer.value = value
    }

    fun onFtpPortChange(value: String) {
        _ftpPort.value = value
    }

    fun onFtpUsernameChange(value: String) {
        _ftpUsername.value = value
    }

    fun onFtpPasswordChange(value: String) {
        _ftpPassword.value = value
    }

    fun onDepartmentNameChange(value: String) {
        _departmentName.value = value
    }

    fun onSelectedImageUriChange(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun connectAndTest() {
        viewModelScope.launch {
            _status.value = FtpStatus.CONNECTING
            val result = withContext(Dispatchers.IO) {
                try {
                    val ftpClient = FTPClient()
                    ftpClient.connect(_ftpServer.value, _ftpPort.value.toInt())
                    val success = ftpClient.login(_ftpUsername.value, _ftpPassword.value)
                    if (success) {
                        ftpClient.logout()
                        ftpClient.disconnect()
                        FtpStatus.CONNECTION_SUCCESS
                    } else {
                        _errorMessage.value = "Invalid credentials. Please retry."
                        FtpStatus.CONNECTION_ERROR
                    }
                } catch (e: IOException) {
                    _errorMessage.value = e.message
                    FtpStatus.CONNECTION_ERROR
                }
            }
            _status.value = result
        }
    }

    fun uploadDepartmentData(context: Context) {
        if (_departmentName.value.isBlank() || _selectedImageUri.value == null) {
            _errorMessage.value = "Department name and logo are required."
            _status.value = FtpStatus.UPLOAD_ERROR
            return
        }

        viewModelScope.launch {
            _status.value = FtpStatus.UPLOADING

            val result = withContext(Dispatchers.IO) {
                val ftpClient = FTPClient()
                try {
                    ftpClient.connect(_ftpServer.value, _ftpPort.value.toInt())
                    if (!ftpClient.login(_ftpUsername.value, _ftpPassword.value)) {
                        _errorMessage.value = "FTP login failed. Please retry."
                        return@withContext FtpStatus.UPLOAD_ERROR
                    }

                    ftpClient.enterLocalPassiveMode()
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

                    // Upload department name
                    val textFile = File(context.cacheDir, "department.txt")
                    textFile.writeText(_departmentName.value)
                    val textInputStream = FileInputStream(textFile)
                    val textUploaded = ftpClient.storeFile("department.txt", textInputStream)
                    textInputStream.close()
                    if (!textUploaded) {
                        _errorMessage.value = "Failed to upload department name."
                        return@withContext FtpStatus.UPLOAD_ERROR
                    }

                    // Upload department logo
                    context.contentResolver.openInputStream(_selectedImageUri.value!!)?.use { logoInputStream ->
                        val logoUploaded = ftpClient.storeFile("logo.png", logoInputStream)
                        if (!logoUploaded) {
                            _errorMessage.value = "Failed to upload department logo."
                            return@withContext FtpStatus.UPLOAD_ERROR
                        }
                    }

                    FtpStatus.UPLOAD_SUCCESS
                } catch (e: IOException) {
                    _errorMessage.value = e.message
                    FtpStatus.UPLOAD_ERROR
                } finally {
                    try {
                        if (ftpClient.isConnected) {
                            ftpClient.logout()
                            ftpClient.disconnect()
                        }
                    } catch (ex: IOException) {
                        // Ignore
                    }
                }
            }
            _status.value = result ?: FtpStatus.UPLOAD_ERROR
        }
    }
}
