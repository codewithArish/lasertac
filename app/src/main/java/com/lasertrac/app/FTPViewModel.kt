package com.lasertrac.app

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lasertrac.app.db.SavedSnapLocationEntity
import com.lasertrac.app.db.SnapLocationDao
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
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class FtpStatus { IDLE, CONNECTING, UPLOADING, SYNCING, CONNECTION_SUCCESS, CONNECTION_ERROR, UPLOAD_SUCCESS, UPLOAD_ERROR, SYNC_SUCCESS, SYNC_ERROR }

class FTPViewModel(private val snapLocationDao: SnapLocationDao) : ViewModel() {

    private val _ftpServer = MutableStateFlow("192.168.10.1")
    val ftpServer: StateFlow<String> = _ftpServer.asStateFlow()

    private val _ftpPort = MutableStateFlow("21")
    val ftpPort: StateFlow<String> = _ftpPort.asStateFlow()

    private val _ftpUsername = MutableStateFlow("TP0003P")
    val ftpUsername: StateFlow<String> = _ftpUsername.asStateFlow()

    private val _ftpPassword = MutableStateFlow("12345678")
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

    fun syncSnaps(context: Context) {
        viewModelScope.launch {
            _status.value = FtpStatus.SYNCING
            val result = withContext(Dispatchers.IO) {
                val ftpClient = FTPClient()
                try {
                    ftpClient.connect(_ftpServer.value, _ftpPort.value.toInt())
                    if (!ftpClient.login(_ftpUsername.value, _ftpPassword.value)) {
                        _errorMessage.value = "FTP login failed. Please retry."
                        return@withContext FtpStatus.SYNC_ERROR
                    }

                    ftpClient.enterLocalPassiveMode()
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

                    val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                    val remoteBaseDir = "/manual_capture/"

                    val directories = ftpClient.listDirectories(remoteBaseDir)
                    val todayDirectories = directories.filter { it.isDirectory && it.name.startsWith(today) }

                    for (dir in todayDirectories) {
                        val files = ftpClient.listFiles("${remoteBaseDir}${dir.name}")
                        for (file in files) {
                            val localFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), file.name)
                            val outputStream = FileOutputStream(localFile)
                            outputStream.use { 
                                ftpClient.retrieveFile("${remoteBaseDir}${dir.name}/${file.name}", it)
                            }
                            val snap = SavedSnapLocationEntity(
                                snapId = UUID.randomUUID().toString(),
                                imageUri = localFile.absolutePath,
                                timestamp = System.currentTimeMillis()
                            )
                            snapLocationDao.insertOrUpdateSnapLocation(snap)
                        }
                    }
                    FtpStatus.SYNC_SUCCESS
                } catch (e: IOException) {
                    _errorMessage.value = e.message
                    FtpStatus.SYNC_ERROR
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
            _status.value = result ?: FtpStatus.SYNC_ERROR
        }
    }
}

class FTPViewModelFactory(private val snapLocationDao: SnapLocationDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FTPViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FTPViewModel(snapLocationDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
