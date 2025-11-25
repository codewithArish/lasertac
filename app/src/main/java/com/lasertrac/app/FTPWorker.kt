package com.lasertrac.app

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File

class FTPWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SERVER = "key_server"
        const val KEY_USERNAME = "key_username"
        const val KEY_PASSWORD = "key_password"
        const val TAG = "FTPWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ftpClient = FTPClient()
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

            val remoteBaseDir = "manual_capture"
            val dateDirs = ftpClient.listDirectories(remoteBaseDir)

            for (dateDir in dateDirs) {
                val remotePath = "$remoteBaseDir/${dateDir.name}"
                val files = ftpClient.listFiles(remotePath)
                for (file in files) {
                    if (file.isFile) {
                        downloadFile(ftpClient, "$remotePath/${file.name}", file.name)
                    }
                }
            }

            Result.success()
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

    private fun downloadFile(ftpClient: FTPClient, remoteFilePath: String, fileName: String) {
        try {
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
                contentResolver.openOutputStream(it).use { outputStream ->
                    if (outputStream != null) {
                        ftpClient.retrieveFile(remoteFilePath, outputStream)
                    }
                }
            } ?: Log.e(TAG, "Failed to create MediaStore entry for $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download $fileName", e)
        }
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
