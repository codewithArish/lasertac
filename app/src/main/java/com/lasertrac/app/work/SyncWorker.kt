package com.lasertrac.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lasertrac.app.data.local.AppDatabase
import com.lasertrac.app.network.RetrofitInstance
import com.lasertrac.app.network.models.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val offlineRegistrationDao = AppDatabase.getDatabase(applicationContext).offlineRegistrationDao()
        val apiService = RetrofitInstance.api

        val pendingRegistrations = offlineRegistrationDao.getAll()

        if (pendingRegistrations.isEmpty()) {
            return@withContext Result.success()
        }

        var allSucceeded = true
        pendingRegistrations.forEach { request ->
            try {
                val response = apiService.register(RegisterRequest(request.name, request.email, request.pass))
                if (response.isSuccessful) {
                    offlineRegistrationDao.delete(request.id)
                } else {
                    allSucceeded = false
                }
            } catch (e: Exception) {
                allSucceeded = false
            }
        }

        if (allSucceeded) Result.success() else Result.retry()
    }
}