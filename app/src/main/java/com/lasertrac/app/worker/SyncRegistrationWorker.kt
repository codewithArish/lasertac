package com.lasertrac.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lasertrac.app.db.AppDatabase
import com.lasertrac.app.network.RetrofitInstance
import com.lasertrac.app.network.models.RegisterRequest

class SyncRegistrationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userDao = AppDatabase.getDatabase(applicationContext).userDao()
        val apiService = RetrofitInstance.api
        // Fetch all users that have not been synced to the server yet.
        val unsyncedUsers = userDao.getUnsyncedUsers()

        if (unsyncedUsers.isEmpty()) {
            Log.i("SyncRegistrationWorker", "No unsynced users to process. Work is complete.")
            return Result.success()
        }

        Log.i("SyncRegistrationWorker", "Found ${unsyncedUsers.size} unsynced users. Starting sync process...")

        var needsRetry = false

        for (user in unsyncedUsers) {
            try {
                val registerRequest = RegisterRequest(user.name, user.email, user.pass)
                val response = apiService.register(registerRequest)

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    // **THE FIX**: Check for the 'status' string, not the old 'success' boolean.
                    if (authResponse.status == "success") {
                        // The server has successfully created the user.
                        // Update the local user to mark them as synced and clear the password.
                        val updatedUser = user.copy(isSynced = true, pass = "")
                        userDao.updateUser(updatedUser)
                        Log.i("SyncRegistrationWorker", "Successfully synced registration for user: ${user.email}")
                    } else {
                        // The server returned a known error (e.g., "user already exists").
                        // This is a permanent failure for this user. Mark as synced to prevent retrying.
                        Log.e("SyncRegistrationWorker", "Permanent sync failure for ${user.email}: ${authResponse.message}")
                        userDao.updateUser(user.copy(isSynced = true, pass = ""))
                    }
                } else {
                    // A server error (500, 404, etc.) occurred. We should retry this.
                    Log.e("SyncRegistrationWorker", "Sync failed for ${user.email}: Server responded with code ${response.code()}")
                    needsRetry = true
                }
            } catch (e: Exception) {
                // A network or other exception occurred. We should retry this.
                Log.e("SyncRegistrationWorker", "Sync exception for ${user.email}. Will retry later.", e)
                needsRetry = true
                break // If the network is down, stop trying to sync other users in this batch
            }
        }

        return if (needsRetry) {
            Log.w("SyncRegistrationWorker", "One or more users failed to sync due to a recoverable error. Retrying later.")
            Result.retry()
        } else {
            Log.i("SyncRegistrationWorker", "Sync batch finished. Some users may have had permanent failures.")
            Result.success()
        }
    }
}
