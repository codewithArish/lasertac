package com.lasertrac.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lasertrac.app.db.AppDatabase
import com.lasertrac.app.network.RetrofitInstance
import com.lasertrac.app.network.models.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("SyncWorker", "Starting background sync from the correct worker...")
        val userDao = AppDatabase.getDatabase(applicationContext).userDao()
        val apiService = RetrofitInstance(applicationContext).api

        val unsyncedUsers = userDao.getUnsyncedUsers()

        if (unsyncedUsers.isEmpty()) {
            Log.d("SyncWorker", "No unsynced users found. Work complete.")
            return@withContext Result.success()
        }

        Log.d("SyncWorker", "Found ${unsyncedUsers.size} users to sync.")
        var allSucceeded = true

        for (user in unsyncedUsers) {
            try {
                val response = apiService.register(RegisterRequest(user.name, user.email, user.pass))
                
                if (response.isSuccessful && response.body() != null) {
                    val serverUser = response.body()!!.user
                    if (serverUser != null) {
                        val syncedUser = user.copy(
                            serverId = serverUser.id, // Updated to serverId
                            isSynced = true, 
                            pass = "" // Clear the password
                        )
                        userDao.updateUser(syncedUser)
                        Log.d("SyncWorker", "Successfully synced user and cleared password for: ${user.email}")
                    } else {
                        Log.w("SyncWorker", "Sync failed for ${user.email}: Server response did not contain user data.")
                        allSucceeded = false
                    }
                } else {
                    // This case is unlikely with Retrofit but included for completeness
                    Log.e("SyncWorker", "Sync failed for ${user.email}: Non-successful response with code ${response.code()}")
                    allSucceeded = false
                }
            } catch (e: Exception) {
                if (e is HttpException && e.code() == 409) {
                    // HTTP 409 Conflict: The user already exists on the server.
                    Log.w("SyncWorker", "User ${user.email} already exists on server (HTTP 409). Deleting local unsynced record.")
                    userDao.deleteUser(user)
                } else {
                    // For any other network or parsing exception, mark for retry.
                    Log.e("SyncWorker", "Exception during sync for user ${user.email}. Will retry.", e)
                    allSucceeded = false
                }
            }
        }

        return@withContext if (allSucceeded) {
            Log.d("SyncWorker", "All users synced successfully.")
            Result.success()
        } else {
            Log.w("SyncWorker", "One or more users failed to sync. Will retry later.")
            Result.retry()
        }
    }
}
