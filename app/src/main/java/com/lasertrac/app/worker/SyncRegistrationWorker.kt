package com.lasertrac.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lasertrac.app.db.AppDatabase
import com.lasertrac.app.network.RetrofitInstance
import com.lasertrac.app.network.models.RegisterRequest
import com.lasertrac.app.repository.UserRepository
import com.lasertrac.app.security.CredentialsManager
import com.lasertrac.app.utils.NetworkStatusTracker

class SyncRegistrationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userDao = AppDatabase.getDatabase(applicationContext).userDao()
        val apiService = RetrofitInstance.api
        val unsyncedUsers = userDao.getUnsyncedUsers()

        if (unsyncedUsers.isEmpty()) {
            Log.d("SyncWorker", "No unsynced users to process.")
            return Result.success()
        }

        Log.d("SyncWorker", "Found ${unsyncedUsers.size} unsynced users. Starting sync...")

        var successCount = 0
        for (user in unsyncedUsers) {
            try {
                val response = apiService.register(RegisterRequest(user.name, user.email, user.pass))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    if (authResponse.success) {
                        authResponse.user?.let { userResponse ->
                            val updatedUser = user.copy(
                                serverId = userResponse.id,
                                isSynced = true,
                                pass = "" // Clear password after successful sync
                            )
                            userDao.updateUser(updatedUser)
                            successCount++
                            Log.i("SyncWorker", "Successfully synced user: ${user.email}")
                        }
                    } else {
                        Log.e("SyncWorker", "Sync failed for ${user.email}: ${authResponse.message}")
                    }
                } else {
                    Log.e("SyncWorker", "Sync failed for ${user.email}: Server responded with code ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Sync exception for ${user.email}", e)
                // Decide on retry logic. For now, we just let it try again on the next run.
            }
        }

        return if (successCount == unsyncedUsers.size) {
            Log.d("SyncWorker", "All unsynced users have been synced.")
            Result.success()
        } else {
            Log.w("SyncWorker", "Sync partially failed. Will retry later.")
            Result.retry()
        }
    }
}
