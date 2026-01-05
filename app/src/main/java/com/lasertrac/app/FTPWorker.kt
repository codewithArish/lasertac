package com.lasertrac.app

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.lasertrac.app.db.AppDatabase
import com.lasertrac.app.db.SnapDetail
import com.lasertrac.app.db.SnapStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class FTPWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SERVER = "key_server"
        const val KEY_USERNAME = "key_username"
        const val KEY_PASSWORD = "key_password"
        const val TAG = "FTPWorker"
        const val KEY_RESULT_COUNT = "key_result_count"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ftpClient = FTPClient()
        val snapDao = AppDatabase.getDatabase(context).snapDao()
        var downloadedFileCount = 0

        try {
            val host = inputData.getString(KEY_SERVER)
            val user = inputData.getString(KEY_USERNAME)
            val pass = inputData.getString(KEY_PASSWORD)

            if (user.isNullOrBlank() || pass.isNullOrBlank() || host.isNullOrBlank()) {
                Log.e(TAG, "Host, username, or password not set.")
                return@withContext Result.failure()
            }

            ftpClient.connect(host)
            if (!ftpClient.login(user, pass)) {
                Log.e(TAG, "FTP login failed.")
                return@withContext Result.failure()
            }

            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            val baseDirs = listOf("manual_capture", "Auto")

            for (baseDir in baseDirs) {
                val dateDirs = try {
                    ftpClient.listDirectories(baseDir)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not list directories for $baseDir. It might not exist.")
                    emptyArray<FTPFile>()
                }

                for (dateDir in dateDirs) {
                    val remotePath = "$baseDir/${dateDir.name}"
                    val files = ftpClient.listFiles(remotePath)
                    for (file in files) {
                        if (file.isFile && !snapDao.snapExists(file.name)) {
                            val localUri = downloadFile(ftpClient, "$remotePath/${file.name}", file.name)
                            if (localUri != null) {
                                // Use the file's timestamp instead of the current date.
                                val newSnap = createSnapDetail(file.name, localUri, file.timestamp)
                                snapDao.insert(newSnap)
                                downloadedFileCount++
                            }
                        }
                    }
                }
            }
            val outputData = Data.Builder().putInt(KEY_RESULT_COUNT, downloadedFileCount).build()
            Result.success(outputData)

        } catch (e: Exception) {
            Log.e(TAG, "FTP operation failed", e)
            Result.failure()
        } finally {
            if (ftpClient.isConnected) {
                ftpClient.logout()
                ftpClient.disconnect()
            }
        }
    }

    private fun downloadFile(ftpClient: FTPClient, remoteFilePath: String, fileName: String): Uri? {
        return try {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(fileName))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/com.lasertrac")
                }
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    if (outputStream != null) {
                        ftpClient.retrieveFile(remoteFilePath, outputStream)
                    }
                }
            }
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download $fileName", e)
            null
        }
    }

    private fun createSnapDetail(fileName: String, localUri: Uri, timestamp: Calendar): SnapDetail {
        // Use the file's timestamp to get the actual clicked date.
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val clickedDate = sdf.format(timestamp.time)

        return SnapDetail(
            id = UUID.randomUUID().toString(),
            regNr = "", // You might want to parse this from the filename or get it from somewhere else
            evidenceDate = clickedDate,
            dateTime = clickedDate,
            status = SnapStatus.SYNCED,
            speed = 0,
            deviceId = "",
            operatorId = "",
            speedLimit = 0,
            location = "",
            violationDistance = "",
            recordNr = fileName,
            latitude = "",
            longitude = "",
            district = "",
            policeStation = "",
            address = "",
            uploadStatus = "",
            mainImage = localUri.toString(),
            licensePlateImage = "",
            mapImage = "",
            violationSummary = "",
            violationManagementLink = "",
            accessLink = "",
            regNrStatus = ""
        )
    }

    private fun getMimeType(fileName: String): String {
        return when (File(fileName).extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            else -> "application/octet-stream"
        }
    }
}
