package com.lasertrac.app.ui.ftp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lasertrac.app.db.SnapDao
import com.lasertrac.app.db.SnapDetail
import com.lasertrac.app.db.SnapStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object FTPManager {

    private const val TAG = "FTPManager"
    private val ftpClient = FTPClient()
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _uiState = MutableStateFlow(FtpUiState.IDLE)
    val uiState: StateFlow<FtpUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _syncedFileCount = MutableStateFlow(0)
    val syncedFileCount: StateFlow<Int> = _syncedFileCount.asStateFlow()

    fun startSync(context: Context, snapDao: SnapDao) {
        if (_uiState.value.isBusy()) return

        managerScope.launch {
            _uiState.value = FtpUiState.CONNECTING
            try {
                if (!ftpClient.isConnected) {
                    connectAndLogin(context)
                }

                _uiState.value = FtpUiState.SYNCING
                val downloadedCount = downloadFilesFromServer(context, snapDao)
                _syncedFileCount.value = downloadedCount
                _uiState.value = FtpUiState.SYNC_COMPLETED

            } catch (e: IOException) {
                Log.e(TAG, "Sync failed.", e)
                _errorMessage.value = e.message ?: "An unknown error occurred."
                handleConnectionError()
            }
        }
    }

    suspend fun syncFromRepository(context: Context, snapDao: SnapDao): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (!ftpClient.isConnected) {
                connectAndLogin(context)
            }
            val downloadedCount = downloadFilesFromServer(context, snapDao)
            Result.success(downloadedCount)
        } catch (e: IOException) {
            Log.e(TAG, "Sync failed during repository sync.", e)
            Result.failure(e)
        }
    }

    private suspend fun connectAndLogin(context: Context) = withContext(Dispatchers.IO) {
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

    private suspend fun downloadFilesFromServer(context: Context, snapDao: SnapDao): Int = withContext(Dispatchers.IO) {
        val remoteBaseDir = "/manual_capture/"
        var downloadedCount = 0

        val remoteItems = ftpClient.listFiles(remoteBaseDir) ?: emptyArray()
        for (item in remoteItems) {
            if (item == null) continue
            downloadedCount += processRemoteItem(context, item, remoteBaseDir, snapDao)
        }
        downloadedCount
    }

    private suspend fun processRemoteItem(context: Context, item: FTPFile, remoteBaseDir: String, snapDao: SnapDao): Int {
        var count = 0
        if (item.isDirectory) {
            val remoteDirPath = "$remoteBaseDir${item.name}/"
            val filesInDir = ftpClient.listFiles(remoteDirPath) ?: emptyArray()
            for (ftpFile in filesInDir) {
                if (ftpFile != null && ftpFile.isFile) {
                    downloadAndSave(context, ftpFile, remoteDirPath, snapDao).onSuccess { count++ }
                }
            }
        } else if (item.isFile) {
            downloadAndSave(context, item, remoteBaseDir, snapDao).onSuccess { count++ }
        }
        _syncedFileCount.value += count
        return count
    }

    private suspend fun downloadAndSave(context: Context, ftpFile: FTPFile, remotePath: String, snapDao: SnapDao): Result<Unit> = withContext(Dispatchers.IO) {
        if (snapDao.snapExists(ftpFile.name)) {
            Log.d(TAG, "File ${ftpFile.name} already exists in database. Skipping.")
            return@withContext Result.failure(IOException("File already exists"))
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = ftpFile.name.hashCode()
        val channelId = "download_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "File Downloads", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Files are fetching")
            .setContentText(ftpFile.name)
            .setOngoing(true)
            .setProgress(100, 0, true)

        notificationManager.notify(notificationId, notificationBuilder.build())

        val remoteFilePath = "$remotePath${ftpFile.name}"
        val downloadedData: ByteArray?

        try {
            downloadedData = ftpClient.retrieveFileStream(remoteFilePath)?.use { inputStream ->
                val outputStream = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    val progress = (totalBytesRead * 100 / ftpFile.size).toInt()
                    notificationBuilder.setProgress(100, progress, false).setContentText("$progress%")
                    notificationManager.notify(notificationId, notificationBuilder.build())
                }
                if (ftpClient.completePendingCommand()) outputStream.toByteArray() else null
            }

            if (downloadedData == null) {
                throw IOException("Failed to download file or complete command.")
            }

            val newSnap = createSnapDetail(ftpFile.name)
            newSnap.mainImage?.let {
                saveToMediaStore(context, downloadedData, it)
                snapDao.insert(newSnap)
            } ?: throw IOException("SnapDetail was created without a main image.")

            notificationBuilder.setContentTitle("Download completed successfully")
                .setOngoing(false).setProgress(0, 0, false)
            notificationManager.notify(notificationId, notificationBuilder.build())

            Result.success(Unit)

        } catch (e: IOException) {
            Log.e(TAG, "Download failed for ${ftpFile.name}", e)
            notificationBuilder.setContentTitle("Download failed").setOngoing(false).setProgress(0, 0, false)
            notificationManager.notify(notificationId, notificationBuilder.build())
            Result.failure(e)
        }
    }

    private fun saveToMediaStore(context: Context, data: ByteArray, fileName: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/com.laserTrac")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { stream -> stream.write(data) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
            }
        } ?: throw IOException("Failed to create MediaStore entry.")
    }

    private fun createSnapDetail(fileName: String): SnapDetail {
        val timestamp = System.currentTimeMillis()
        val date = Date(timestamp)
        val evidenceDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date)
        val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)

        return SnapDetail(
            id = "ftp_${UUID.randomUUID()}",
            regNr = "N/A",
            evidenceDate = evidenceDate, dateTime = dateTime,
            status = SnapStatus.PENDING,
            speed = 0, deviceId = "N/A", operatorId = "N/A", speedLimit = 0, location = "N/A",
            violationDistance = "N/A", recordNr = "N/A", latitude = "N/A", longitude = "N/A",
            district = "N/A", policeStation = "N/A", address = "N/A", uploadStatus = "PENDING",
            mainImage = fileName, // Just store the filename
            licensePlateImage = "", mapImage = "",
            violationSummary = "", violationManagementLink = "", accessLink = "", regNrStatus = "N/A"
        )
    }

    private fun handleConnectionError() {
        _uiState.value = FtpUiState.ERROR
        disconnect()
    }

    fun disconnect() {
        if (!_uiState.value.isBusy()) return
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
