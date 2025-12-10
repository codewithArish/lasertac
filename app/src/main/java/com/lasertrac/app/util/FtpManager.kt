package com.lasertrac.app.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.IOException

// Data class to hold essential information about a file on the FTP server.
data class FtpFileData(
    val name: String,
    val timestamp: Long,
    val size: Long
)

class FtpManager(private val context: Context) {

    /**
     * Lists all .jpg files from a specified path on an FTP server.
     * @return A Result containing a list of FtpFileData on success, or an exception on failure.
     */
    suspend fun listFiles(server: String, port: Int, user: String, pass: String, path: String): Result<List<FtpFileData>> = withContext(Dispatchers.IO) {
        val ftpClient = FTPClient()
        try {
            ftpClient.connect(server, port)
            if (!ftpClient.login(user, pass)) {
                return@withContext Result.failure(IOException("FTP login failed. Server reply: ${ftpClient.replyString}"))
            }
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            val files = ftpClient.listFiles(path)
            val fileDataList = files
                ?.filter { it.isFile && it.name.endsWith(".jpg", ignoreCase = true) }
                ?.map { ftpFile ->
                    FtpFileData(
                        name = ftpFile.name,
                        timestamp = ftpFile.timestamp.timeInMillis,
                        size = ftpFile.size
                    )
                } ?: emptyList()

            Result.success(fileDataList)
        } catch (e: IOException) {
            Result.failure(e)
        } finally {
            try {
                if (ftpClient.isConnected) {
                    ftpClient.logout()
                    ftpClient.disconnect()
                }
            } catch (e: IOException) {
                // Ignore errors on disconnect
            }
        }
    }

    /**
     * Downloads a file from the FTP server and saves it to the "Pictures/LaserTrac" directory.
     * It uses MediaStore to ensure the file is visible in the device's gallery.
     * @return A Result containing the local URI string of the saved file on success, or an exception on failure.
     */
    suspend fun downloadFile(server: String, port: Int, user: String, pass: String, remotePath: String, remoteFileName: String): Result<String> = withContext(Dispatchers.IO) {
        val ftpClient = FTPClient()
        try {
            ftpClient.connect(server, port)
            if (!ftpClient.login(user, pass)) {
                return@withContext Result.failure(IOException("FTP login failed. Server reply: ${ftpClient.replyString}"))
            }
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, remoteFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/LaserTrac")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(IOException("Failed to create MediaStore entry."))

            var success = false
            resolver.openOutputStream(uri)?.use { outputStream ->
                success = ftpClient.retrieveFile("$remotePath/$remoteFileName", outputStream)
            }

            if (!success) {
                resolver.delete(uri, null, null) // Clean up failed download
                return@withContext Result.failure(IOException("Failed to download file from FTP. Server reply: ${ftpClient.replyString}"))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            Result.success(uri.toString())
        } catch (e: IOException) {
            Result.failure(e)
        } finally {
            try {
                if (ftpClient.isConnected) {
                    ftpClient.logout()
                    ftpClient.disconnect()
                }
            } catch (e: IOException) {
                // Ignore errors on disconnect
            }
        }
    }
}
