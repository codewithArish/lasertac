package com.lasertrac.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.lasertrac.app.db.SnapLocationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.IOException

enum class FtpStatus { IDLE, CONNECTING, CONNECTION_SUCCESS, CONNECTION_ERROR, SYNCING, SYNC_SUCCESS, SYNC_ERROR, UPLOADING, UPLOAD_SUCCESS, UPLOAD_ERROR }

class FTPViewModel(application: Application, private val snapLocationDao: SnapLocationDao) : AndroidViewModel(application) {

    private val _status = MutableStateFlow(FtpStatus.IDLE)
    val status: StateFlow<FtpStatus> = _status

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _ftpServer = MutableStateFlow("192.168.10.1")
    val ftpServer: StateFlow<String> = _ftpServer

    private val _ftpPort = MutableStateFlow("21")
    val ftpPort: StateFlow<String> = _ftpPort

    private val _ftpUsername = MutableStateFlow("TP2")
    val ftpUsername: StateFlow<String> = _ftpUsername

    private val _ftpPassword = MutableStateFlow("12345678")
    val ftpPassword: StateFlow<String> = _ftpPassword

    private val _departmentName = MutableStateFlow("")
    val departmentName: StateFlow<String> = _departmentName

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val ftpClient = FTPClient()
    private val workManager = WorkManager.getInstance(application)

    init {
        // Observe the FTPWorker
        workManager.getWorkInfosForUniqueWorkLiveData("ftp_sync").observeForever { workInfos ->
            workInfos.firstOrNull()?.let { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> _status.value = FtpStatus.SYNCING
                    WorkInfo.State.SUCCEEDED -> {
                        _status.value = FtpStatus.SYNC_SUCCESS
                        val downloadedCount = workInfo.outputData.getInt(FTPWorker.KEY_RESULT_COUNT, 0)
                        if (downloadedCount > 0) {
                            showSyncCompleteNotification(downloadedCount)
                            viewModelScope.launch {
                                _toastMessage.emit("$downloadedCount new files synced.")
                            }
                        } else {
                            viewModelScope.launch {
                                _toastMessage.emit("Files are already synced.")
                            }
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        _errorMessage.value = workInfo.outputData.getString(FTPWorker.TAG) ?: "Sync failed"
                        _status.value = FtpStatus.SYNC_ERROR
                        viewModelScope.launch {
                            _toastMessage.emit("Sync failed. Please check the logs.")
                        }
                    }
                    else -> {
                        // You can reset status for CANCELLED or BLOCKED if needed
                        if(workInfo.state == WorkInfo.State.CANCELLED || workInfo.state == WorkInfo.State.BLOCKED){
                            _status.value = FtpStatus.IDLE
                        }
                    }
                }
            }
        }
    }


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
                    _status.value = FtpStatus.CONNECTION_SUCCESS
                    _toastMessage.emit("FTP Connection Successful")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _status.value = FtpStatus.CONNECTION_ERROR
                _toastMessage.emit("FTP Connection Failed: ${e.message}")
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
                    // Similar connection logic as connectAndTest
                }
            } catch (e: Exception) {
                 _errorMessage.value = e.message
                _status.value = FtpStatus.UPLOAD_ERROR
            }
        }
    }
    fun syncFiles() {
        val ftpData = workDataOf(
            FTPWorker.KEY_SERVER to ftpServer.value,
            FTPWorker.KEY_USERNAME to ftpUsername.value,
            FTPWorker.KEY_PASSWORD to ftpPassword.value
        )

        val ftpWorkRequest = OneTimeWorkRequestBuilder<FTPWorker>()
            .setInputData(ftpData)
            .build()

        workManager.enqueueUniqueWork("ftp_sync", ExistingWorkPolicy.REPLACE, ftpWorkRequest)
    }

    private fun showSyncCompleteNotification(fileCount: Int) {
        val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "ftp_sync_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "FTP Sync", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(getApplication(), channelId)
            .setContentTitle("FTP Sync Complete")
            .setContentText("$fileCount new files have been successfully synced.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app's icon
            .build()

        notificationManager.notify(1, notification)
    }

    override fun onCleared() {
        super.onCleared()
        try {
             if (ftpClient.isConnected) {
                ftpClient.disconnect()
             }
        } catch (e: IOException) {
            Log.e("FTPViewModel", "Failed to disconnect on onCleared", e)
        }
    }
}
