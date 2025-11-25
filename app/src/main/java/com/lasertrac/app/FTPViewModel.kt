package com.lasertrac.app

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.lasertrac.app.db.SnapLocationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.IOException

enum class FtpStatus { IDLE, CONNECTING, CONNECTION_SUCCESS, CONNECTION_ERROR, SYNCING, SYNC_SUCCESS, SYNC_ERROR, UPLOADING, UPLOAD_SUCCESS, UPLOAD_ERROR }

class FTPViewModel(private val snapLocationDao: SnapLocationDao) : ViewModel() {

    private val _status = MutableStateFlow(FtpStatus.IDLE)
    val status: StateFlow<FtpStatus> = _status

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _ftpServer = MutableStateFlow("")
    val ftpServer: StateFlow<String> = _ftpServer

    private val _ftpPort = MutableStateFlow("21")
    val ftpPort: StateFlow<String> = _ftpPort

    private val _ftpUsername = MutableStateFlow("TP0003P")
    val ftpUsername: StateFlow<String> = _ftpUsername

    private val _ftpPassword = MutableStateFlow("12345678")
    val ftpPassword: StateFlow<String> = _ftpPassword

    private val _departmentName = MutableStateFlow("")
    val departmentName: StateFlow<String> = _departmentName

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    private val ftpClient = FTPClient()

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
            try {
                withContext(Dispatchers.IO) {
                    Log.d("FTPViewModel", "Connecting to: ${_ftpServer.value}:${_ftpPort.value} with user: ${_ftpUsername.value}")
                    if (ftpClient.isConnected) {
                        ftpClient.disconnect()
                    }
                    ftpClient.connect(_ftpServer.value, _ftpPort.value.toIntOrNull() ?: 21)
                    if (!FTPReply.isPositiveCompletion(ftpClient.replyCode)) {
                        throw IOException("FTP server refused connection.")
                    }
                    val isLoggedIn = ftpClient.login(_ftpUsername.value, _ftpPassword.value)
                    if (!isLoggedIn) {
                        throw IOException("FTP login failed.")
                    }
                    ftpClient.enterLocalPassiveMode()
                    _status.value = FtpStatus.CONNECTION_SUCCESS
                }
            } catch (e: Exception) {
                Log.e("FTPViewModel", "Connection failed", e)
                _errorMessage.value = e.message
                _status.value = FtpStatus.CONNECTION_ERROR
            } finally {
                if (ftpClient.isConnected) {
                    try {
                        withContext(Dispatchers.IO) {
                            ftpClient.logout()
                            ftpClient.disconnect()
                        }
                    } catch (e: IOException) {
                        Log.e("FTPViewModel", "Failed to disconnect", e)
                    }
                }
            }
        }
    }

    fun uploadDepartmentData(context: Context) {
        viewModelScope.launch {
            _status.value = FtpStatus.UPLOADING
            try {
                withContext(Dispatchers.IO) {
                    if (selectedImageUri.value == null || departmentName.value.isBlank()) {
                        throw IOException("Department name and logo are required.")
                    }

                    Log.d("FTPViewModel", "Connecting to: ${_ftpServer.value}:${_ftpPort.value} with user: ${_ftpUsername.value}")
                    if (ftpClient.isConnected) {
                        ftpClient.disconnect()
                    }
                    ftpClient.connect(_ftpServer.value, _ftpPort.value.toIntOrNull() ?: 21)
                    if (!FTPReply.isPositiveCompletion(ftpClient.replyCode)) {
                        throw IOException("FTP server refused connection.")
                    }
                    val isLoggedIn = ftpClient.login(_ftpUsername.value, _ftpPassword.value)
                    if (!isLoggedIn) {
                        throw IOException("FTP login failed.")
                    }
                    ftpClient.enterLocalPassiveMode()

                    // Upload department name
                    val departmentNameByteArray = departmentName.value.toByteArray()
                    val departmentNameInputStream = departmentNameByteArray.inputStream()
                    val remoteDepartmentNameFile = "department_name.txt"
                    val isDepartmentNameUploaded = ftpClient.storeFile(remoteDepartmentNameFile, departmentNameInputStream)
                    departmentNameInputStream.close()
                    if (!isDepartmentNameUploaded) {
                        throw IOException("Failed to upload department name.")
                    }

                    // Upload department logo
                    selectedImageUri.value?.let { uri ->
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val remoteLogoFile = "department_logo.jpg"
                            val isLogoUploaded = ftpClient.storeFile(remoteLogoFile, inputStream)
                            if (!isLogoUploaded) {
                                throw IOException("Failed to upload department logo.")
                            }
                        }
                    }
                    _status.value = FtpStatus.UPLOAD_SUCCESS
                }
            } catch (e: Exception) {
                Log.e("FTPViewModel", "Upload failed", e)
                _errorMessage.value = e.message
                _status.value = FtpStatus.UPLOAD_ERROR
            } finally {
                if (ftpClient.isConnected) {
                    try {
                        withContext(Dispatchers.IO) {
                            ftpClient.logout()
                            ftpClient.disconnect()
                        }
                    } catch (e: IOException) {
                        Log.e("FTPViewModel", "Failed to disconnect", e)
                    }
                }
            }
        }
    }
    fun syncFiles(context: Context) {
        val ftpData = workDataOf(
            FTPWorker.KEY_SERVER to ftpServer.value,
            FTPWorker.KEY_USERNAME to ftpUsername.value,
            FTPWorker.KEY_PASSWORD to ftpPassword.value
        )

        val ftpWorkRequest = OneTimeWorkRequestBuilder<FTPWorker>()
            .setInputData(ftpData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork("ftp_sync", ExistingWorkPolicy.KEEP, ftpWorkRequest)
    }
}
