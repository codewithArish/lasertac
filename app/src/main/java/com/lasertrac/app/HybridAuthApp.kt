package com.lasertrac.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lasertrac.app.work.SyncWorker
import java.util.concurrent.TimeUnit

class HybridAuthApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupSyncWorker()
    }

    private fun setupSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES // Run every 15 minutes
        )
        .setConstraints(constraints)
        .build()

        WorkManager.getInstance(this).enqueue(syncRequest)
    }
}