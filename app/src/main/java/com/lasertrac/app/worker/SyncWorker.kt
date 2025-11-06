package com.lasertrac.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lasertrac.app.db.AppDatabase
import com.lasertrac.app.network.RetrofitInstance
import com.lasertrac.app.network.models.RegisterRequest

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userDao = AppDatabase.getDatabase(applicationContext).userDao()
        val apiService = RetrofitInstance.api

        val unsyncedUsers = userDao.getUnsyncedUsers()

        if (unsyncedUsers.isEmpty()) {
            return Result.success()
        }

        return try {
            unsyncedUsers.forEach { user ->
                val request = RegisterRequest(user.name, user.email, user.pass)
                val response = apiService.register(request)
                if (response.isSuccessful) {
                    // If server confirms registration, mark the user as synced in the local DB.
                    userDao.markUserAsSynced(user.id)
                }
                // If it fails, it will be retried automatically by WorkManager later.
            }
            Result.success()
        } catch (e: Exception) {
            // If there's a network error or other exception, retry the work.
            Result.retry()
        }
    }
}
