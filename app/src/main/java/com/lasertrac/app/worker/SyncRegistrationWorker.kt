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

class SyncRegistrationWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("SyncRegWorker", "Doing registration sync work")
        val userDao = AppDatabase.getDatabase(applicationContext).userDao()
        val apiService = RetrofitInstance(applicationContext).api

        val unsyncedUsers = userDao.getUnsyncedUsers()

        if (unsyncedUsers.isEmpty()) {
            Log.d("SyncRegWorker", "No unsynced users to register.")
            return@withContext Result.success()
        }

        var allSucceeded = true
        for (user in unsyncedUsers) {
            try {
                Log.d("SyncRegWorker", "Attempting to sync user: ${user.email}")
                val response = apiService.register(RegisterRequest(user.name, user.email, user.pass))

                if (response.isSuccessful && response.body() != null) {
                    val serverUser = response.body()?.user
                    if(serverUser != null){
                        val syncedUser = user.copy(isSynced = true, serverId = serverUser.id, pass = "")
                        userDao.updateUser(syncedUser)
                        Log.d("SyncRegWorker", "Successfully synced user: ${user.email}")
                    }else{
                         Log.e("SyncRegWorker", "Sync failed for ${user.email}: server response error")
                        allSucceeded = false
                    }
                    
                } else {
                    Log.e("SyncRegWorker", "Sync failed for ${user.email}: ${response.errorBody()?.string()}")
                    allSucceeded = false
                }
            } catch (e: Exception) {
                Log.e("SyncRegWorker", "Exception during sync for user ${user.email}", e)
                allSucceeded = false
            }
        }

        if (allSucceeded) {
            Log.d("SyncRegWorker", "All users synced successfully.")
            Result.success()
        } else {
            Log.d("SyncRegWorker", "One or more users failed to sync. Retrying...")
            Result.retry()
        }
    }
}