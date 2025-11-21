package com.lasertrac.app

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lasertrac.app.db.AppDatabase
import com.lasertrac.app.db.SavedSnapLocationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FTPWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_SERVER = "key_server"
        const val KEY_USERNAME = "key_username"
        const val KEY_PASSWORD = "key_password"
        const val TAG = "FTPWorker"

        private const val PREFS_NAME = "com.lasertrac.app.prefs"
        private const val KEY_LATITUDE = "last_known_latitude"
        private const val KEY_LONGITUDE = "last_known_longitude"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val server = inputData.getString(KEY_SERVER)
        val username = inputData.getString(KEY_USERNAME)
        val password = inputData.getString(KEY_PASSWORD)

        if (server.isNullOrBlank() || username.isNullOrBlank() || password.isNullOrBlank()) {
            Log.e(TAG, "FTP credentials not provided")
            return@withContext Result.failure()
        }

        val sharedPrefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLatitude = if (sharedPrefs.contains(KEY_LATITUDE)) {
            sharedPrefs.getFloat(KEY_LATITUDE, 0.0f).toDouble()
        } else {
            null
        }
        val savedLongitude = if (sharedPrefs.contains(KEY_LONGITUDE)) {
            sharedPrefs.getFloat(KEY_LONGITUDE, 0.0f).toDouble()
        } else {
            null
        }

        val ftpClient = FTPClient()
        try {
            // 1. Connect and login
            ftpClient.connect(server)
            if (!ftpClient.login(username, password)) {
                Log.e(TAG, "FTP login failed.")
                return@withContext Result.failure()
            }
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            // 2. Create local directory
            val localDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "com.lasertrac"
            )
            if (!localDir.exists()) {
                localDir.mkdirs()
            }

            // 3. List files and download
            val ftpFiles = ftpClient.listFiles()
            if (ftpFiles.isNullOrEmpty()) {
                Log.i(TAG, "No files found on FTP server.")
            } else {
                val snapLocationDao = AppDatabase.getDatabase(applicationContext).snapLocationDao()
                val existingLocalFiles = localDir.listFiles()?.map { it.name } ?: emptyList()

                for (ftpFile in ftpFiles) {
                    if (!ftpFile.isFile) continue

                    val remoteFileName = ftpFile.name
                    if (remoteFileName in existingLocalFiles) {
                        Log.i(TAG, "Skipping duplicate file: $remoteFileName")
                        continue
                    }

                    val localFile = File(localDir, remoteFileName)
                    FileOutputStream(localFile).use { fos ->
                        Log.i(TAG, "Downloading file: $remoteFileName")
                        if (ftpClient.retrieveFile(remoteFileName, fos)) {
                            // 4. Update gallery
                            MediaScannerConnection.scanFile(
                                applicationContext,
                                arrayOf(localFile.absolutePath),
                                null
                            ) { path, uri ->
                                Log.i(TAG, "Scanned $path -> URI: $uri")
                            }

                            // 5. Save to database
                            val entity = SavedSnapLocationEntity(
                                snapId = remoteFileName, // Use filename as a unique ID
                                timestamp = System.currentTimeMillis(),
                                imageUri = localFile.absolutePath,
                                latitude = savedLatitude,
                                longitude = savedLongitude
                            )
                            snapLocationDao.insertOrUpdateSnapLocation(entity)
                            Log.i(TAG, "Saved record to DB for: ${localFile.absolutePath}")

                        } else {
                            Log.e(TAG, "Failed to download file: $remoteFileName")
                            // Clean up partially downloaded file
                            localFile.delete()
                        }
                    }
                }
            }

            Log.i(TAG, "FTP Sync finished successfully.")
            return@withContext Result.success()

        } catch (e: IOException) {
            Log.e(TAG, "FTP operation failed", e)
            return@withContext Result.failure(workDataOf("error" to e.message))
        } finally {
            try {
                if (ftpClient.isConnected) {
                    ftpClient.logout()
                    ftpClient.disconnect()
                }
            } catch (ex: IOException) {
                Log.e(TAG, "Error while disconnecting FTP client", ex)
            }
        }
    }
}
